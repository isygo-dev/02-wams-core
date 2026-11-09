package eu.isygoit.exception.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.api.AppParameterServiceApi;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.dto.response.DiagnosticIssue;
import eu.isygoit.dto.response.ValidationReportDto;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.exception.BadArgumentException;
import eu.isygoit.exception.BadRequestException;
import eu.isygoit.exception.ConflictException;
import eu.isygoit.exception.EmptyListException;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.exception.ManagedException;
import eu.isygoit.exception.NullIdentifierException;
import eu.isygoit.exception.ObjectAlreadyExistsException;
import eu.isygoit.exception.ObjectNotFoundException;
import eu.isygoit.exception.OperationNotAllowedException;
import eu.isygoit.exception.OperationNotSupportedException;
import eu.isygoit.exception.RequestValidationException;
import eu.isygoit.service.IMsgService;
import eu.isygoit.types.EmailSubjects;
import eu.isygoit.types.MsgTemplateVariables;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The plan exception handler: the single exception mechanism of the plan service.
 * <p>
 * As in the pilot it extends the stack {@link ControllerExceptionHandler} and is wired into the controllers through
 * {@code @InjectMapperAndService(handler = PlanExceptionHandler.class)}; it also e-mails unmanaged exceptions to the
 * technical admin ({@link #processUnmanagedException(String)}).
 * <p>
 * The former standalone {@code PlanApiExceptionHandler} is merged here: every error leaves the service as the same
 * {@link ApiError} body, whether it is raised
 * <ul>
 *     <li>by a controller method (the controllers delegate their {@code getBackExceptionResponse} to
 *     {@link #toResponse(Throwable)}), or</li>
 *     <li>outside a controller method (message conversion, bean validation, argument binding), through the
 *     {@link RestControllerAdvice} entry point {@link #handle(Exception, HttpServletRequest)}.</li>
 * </ul>
 * An {@link InvalidRequestException} is localized by its CODE first (key {@code plan.error.<code lower-case, dots>},
 * for example {@code PRECEDENCE_CYCLE} gives {@code plan.error.precedence.cycle}), falling back to the key of its
 * class; a {@link RequestValidationException} additionally renders ALL its issues in {@link ApiError#issues()}
 * (code, severity, path, English message, args, localized message when the bundle knows the code; at most
 * {@value #MAX_ISSUES} entries).
 * <p>
 * Mapping: {@link ManagedException} -> its own HTTP status (SolverBusy / SolverUnavailable / JobQueue 503,
 * LimitExceeded / InvalidRequest 400, JobNotFound 404, ...); {@link IllegalArgumentException},
 * {@link HttpMessageNotReadableException}, validation and type mismatch -> 400 {@code INVALID_REQUEST};
 * {@link ErrorResponse} (Spring MVC errors) passthrough; Spring Security exceptions rethrown to the filter chain;
 * anything else -> 500 {@code INTERNAL_ERROR} without internals (and an e-mail to the technical admin).
 */
@Slf4j
@RestControllerAdvice
public class PlanExceptionHandler extends ControllerExceptionHandler {

    private static final String INVALID_REQUEST_KEY = "plan.error.invalid.request";
    private static final String INVALID_REQUEST_CODE = "INVALID_REQUEST";
    private static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";
    private static final String ERROR_KEY_PREFIX = "plan.error.";
    private static final String WARNING_KEY_PREFIX = "plan.warning.";
    /** Upper bound of the issues rendered in one error body. */
    static final int MAX_ISSUES = 100;

    /**
     * Status of the stack's managed exceptions that carry no explicit HTTP status in their {@code @MsgLocale}
     * (the stack defaults them to 500).
     */
    private static final Map<Class<? extends ManagedException>, HttpStatus> STACK_STATUS = Map.of(
            BadArgumentException.class, HttpStatus.BAD_REQUEST,
            BadRequestException.class, HttpStatus.BAD_REQUEST,
            EmptyListException.class, HttpStatus.BAD_REQUEST,
            NullIdentifierException.class, HttpStatus.BAD_REQUEST,
            ObjectNotFoundException.class, HttpStatus.NOT_FOUND,
            ConflictException.class, HttpStatus.CONFLICT,
            ObjectAlreadyExistsException.class, HttpStatus.CONFLICT,
            OperationNotAllowedException.class, HttpStatus.CONFLICT,
            OperationNotSupportedException.class, HttpStatus.METHOD_NOT_ALLOWED);

    private final AppProperties appProperties;

    @Autowired
    private IMsgService msgService;
    @Autowired
    private AppParameterServiceApi appParameterService;


    /**
     * Instantiates a new Plan exception handler.
     *
     * @param appProperties the app properties
     */
    public PlanExceptionHandler(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Advice entry point for every exception raised outside a controller method body (or rethrown by it).
     *
     * @param exception the exception
     * @param request   the current request
     * @return the {@link ApiError} response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handle(Exception exception, HttpServletRequest request) {
        return toResponse(exception, request.getRequestURI(), request.getMethod());
    }

    /**
     * Builds the response of an exception caught by a controller method (the path is read from the current request).
     *
     * @param throwable the exception
     * @return the {@link ApiError} response
     */
    public ResponseEntity<ApiError> toResponse(Throwable throwable) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return toResponse(throwable, attributes.getRequest().getRequestURI(), attributes.getRequest().getMethod());
        }
        return toResponse(throwable, null, null);
    }

    /**
     * Builds the response of an exception.
     *
     * @param throwable the exception
     * @param path      the request path (may be null)
     * @param method    the request method (may be null, used for logging only)
     * @return the {@link ApiError} response
     * @throws RuntimeException the Spring Security exceptions, rethrown so the security filter chain answers 401/403
     */
    public ResponseEntity<ApiError> toResponse(Throwable throwable, String path, String method) {
        if (throwable instanceof AccessDeniedException || throwable instanceof AuthenticationException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof ManagedException managed) {
            return managed(managed, path);
        }
        if (throwable instanceof IllegalArgumentException illegal) {
            return invalidRequest(illegal.getMessage(), path);
        }
        if (throwable instanceof HttpMessageNotReadableException notReadable) {
            return notReadable(notReadable, path);
        }
        if (throwable instanceof MethodArgumentNotValidException notValid) {
            String detail = notValid.getBindingResult().getAllErrors().stream()
                    .map(error -> (error instanceof FieldError field ? field.getField() + ": " : "")
                            + error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            return invalidRequest(detail, path);
        }
        if (throwable instanceof ConstraintViolationException violations) {
            String detail = violations.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining("; "));
            return invalidRequest(detail, path);
        }
        if (throwable instanceof TypeMismatchException mismatch) {
            return invalidRequest("Invalid value for parameter '" + mismatch.getPropertyName() + "'", path);
        }
        if (throwable instanceof ErrorResponse errorResponse) {
            HttpStatusCode code = errorResponse.getStatusCode();
            HttpStatus resolved = HttpStatus.resolve(code.value());
            String reason = resolved != null ? resolved.getReasonPhrase() : "Error";
            return ResponseEntity.status(code).body(new ApiError(
                    Instant.now(), code.value(), reason,
                    code.is4xxClientError() ? INVALID_REQUEST_CODE : INTERNAL_ERROR_CODE,
                    reason, null, path));
        }
        // Safe fallback: never leaks exception messages or stack traces to the client.
        log.error("Unexpected error on {} {}", method, path, throwable);
        processUnmanagedException("Unexpected error on " + method + " " + path + ": "
                + throwable.getClass().getName() + ": " + throwable.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                INTERNAL_ERROR_CODE,
                "An unexpected error occurred",
                null,
                path));
    }

    /**
     * Unmanaged exceptions are e-mailed to the technical admin. The notification must never alter or delay the
     * (internals-free) 500 body returned to the client: every failure is swallowed.
     */
    @Override
    public void processUnmanagedException(String message) {
        //Email message error to system admin
        log.error(message);
        try {
            ResponseEntity<String> result = appParameterService.getTechnicalAdminEmail();
            if (result.getStatusCode().is2xxSuccessful() && result.hasBody() && StringUtils.hasText(result.getBody())) {
                String techAdminEmail = result.getBody();
                try {
                    MailMessageDto mailMessageDto = MailMessageDto.builder()
                            .subject(EmailSubjects.UNMANAGED_EXCEPTION)
                            .senderTenant(TenantConstants.SUPER_TENANT_NAME)
                            .toAddr(techAdminEmail)
                            .fromAddr("noreply@" + TenantConstants.DEFAULT_TENANT_NAME.toLowerCase() + ".com")
                            .templateName(IEnumEmailTemplate.Types.UNMANAGED_EXCEPTION_TEMPLATE)
                            .variables(MailMessageDto.getVariablesAsString(Map.of(
                                    //Common vars
                                    MsgTemplateVariables.V_EXCEPTION, message
                            )))
                            .build();
                    //Send the email message
                    msgService.sendMessage(TenantConstants.SUPER_TENANT_NAME, mailMessageDto, appProperties.isSendAsyncEmail());
                } catch (JsonProcessingException e) {
                    log.error("<Error>: send unmanaged exception email : {} ", e);
                }
            } else {
                log.error("<Error>: technical email not found");
            }
        } catch (Exception e) {
            log.error("Remote feign call failed : ", e);
            //throw new RemoteCallFailedException(e);
        }
    }

    private ResponseEntity<ApiError> managed(ManagedException exception, String path) {
        HttpStatus status = resolveStatus(exception);
        String code = exception instanceof InvalidRequestException requestException
                ? requestException.code()
                : exception.getMsgLocale();
        String message = exception instanceof InvalidRequestException requestException
                ? messageOfCode(requestException.code(), exception.getMsgLocale())
                : message(exception.getMsgLocale());
        List<ApiIssue> issues = exception instanceof RequestValidationException validation
                ? renderIssues(validation.issues())
                : List.of();
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                exception.getMessage(),
                path,
                issues);
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Converts the issues of a validation failure (diagnostic issues or validation report issues) to API issues:
     * the bundle text of the code when there is one, else the issue message. Bounded by {@link #MAX_ISSUES}.
     */
    List<ApiIssue> renderIssues(List<?> issues) {
        List<ApiIssue> rendered = new ArrayList<>();
        if (issues == null) {
            return rendered;
        }
        for (Object issue : issues) {
            if (rendered.size() >= MAX_ISSUES) {
                break;
            }
            if (issue instanceof DiagnosticIssue diagnostic) {
                rendered.add(apiIssue(diagnostic.code(), diagnostic.severity(), diagnostic.path(),
                        diagnostic.message(), Map.of()));
            } else if (issue instanceof ValidationReportDto.Issue reported) {
                rendered.add(apiIssue(reported.getCode(), reported.getSeverity(), reported.getPath(),
                        reported.getMessage(), reported.getArgs() == null ? Map.of() : reported.getArgs()));
            } else if (issue != null) {
                rendered.add(apiIssue(null, null, null, String.valueOf(issue), Map.of()));
            }
        }
        return rendered;
    }

    private ApiIssue apiIssue(String code, String severity, String path, String message, Map<String, Object> args) {
        String prefix = severity != null && severity.toUpperCase(Locale.ROOT).startsWith("WARN")
                ? WARNING_KEY_PREFIX : ERROR_KEY_PREFIX;
        String localized = code == null || code.isBlank() ? null : lookup(keyOf(prefix, code));
        return new ApiIssue(code, severity, path, message, localized == null ? message : localized, args);
    }

    /**
     * i18n key of a code: prefix + lower-case code with dots, for example {@code PRECEDENCE_CYCLE} to
     * {@code plan.error.precedence.cycle}.
     */
    static String keyOf(String prefix, String code) {
        return prefix + code.trim().toLowerCase(Locale.ROOT).replace('_', '.');
    }

    /**
     * The localized message of an error code, or of the fallback key when the bundle has no text for the code.
     */
    private String messageOfCode(String code, String fallbackKey) {
        String localized = code == null || code.isBlank() ? null : lookup(keyOf(ERROR_KEY_PREFIX, code));
        return localized != null ? localized : message(fallbackKey);
    }

    /**
     * The bundle text of a key, or {@code null} when the locale service is absent, does not know the key (it returns
     * the key itself, or throws) or yields a blank text.
     */
    private String lookup(String key) {
        if (getLocaleService() == null) {
            return null;
        }
        try {
            String text = getLocaleService().getMessage(key, LocaleContextHolder.getLocale());
            return text == null || text.isBlank() || text.equals(key) ? null : text;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * The explicit {@code @MsgLocale} status wins; stack exceptions that kept the 500 default get a meaningful one.
     */
    private static HttpStatus resolveStatus(ManagedException exception) {
        HttpStatus declared = exception.getHttpStatus();
        if (declared != null && declared != HttpStatus.INTERNAL_SERVER_ERROR) {
            return declared;
        }
        return STACK_STATUS.entrySet().stream()
                .filter(entry -> entry.getKey().isInstance(exception))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Thrown e.g. by the SolverOptions constructor while the JSON body is deserialized: the first
     * {@link IllegalArgumentException} cause carries the reason.
     */
    private ResponseEntity<ApiError> notReadable(HttpMessageNotReadableException exception, String path) {
        for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof IllegalArgumentException illegal && illegal.getMessage() != null) {
                return invalidRequest(illegal.getMessage(), path);
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return invalidRequest("Malformed or unreadable request body", path);
    }

    private ResponseEntity<ApiError> invalidRequest(String detail, String path) {
        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                INVALID_REQUEST_CODE,
                message(INVALID_REQUEST_KEY),
                detail,
                path);
        return ResponseEntity.badRequest().body(body);
    }

    private String message(String key) {
        if (getLocaleService() == null) {
            return key;
        }
        return getLocaleService().getMessage(key, LocaleContextHolder.getLocale());
    }

    /**
     * The error body of the plan API.
     *
     * @param timestamp when the error occurred
     * @param status    the HTTP status code
     * @param error     the HTTP reason phrase
     * @param code      the functional error code (e.g. {@code INVALID_REQUEST}, {@code LIMIT_EXCEEDED})
     * @param message   the localized message
     * @param detail    technical detail safe for the client (null for unexpected errors)
     * @param path      the request path
     * @param issues    every issue of a validation failure (empty, and omitted from the JSON, for other errors)
     */
    public record ApiError(
            Instant timestamp, int status, String error, String code,
            String message, String detail, String path,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ApiIssue> issues) {

        public ApiError {
            issues = issues == null ? List.of() : List.copyOf(issues);
        }

        /**
         * An error without issues.
         */
        public ApiError(Instant timestamp, int status, String error, String code,
                        String message, String detail, String path) {
            this(timestamp, status, error, code, message, detail, path, List.of());
        }
    }

    /**
     * One issue of a validation failure.
     *
     * @param code             the stable issue code (for example {@code UNKNOWN_REFERENCE}), may be null
     * @param severity         ERROR | WARNING | INFO, may be null
     * @param path             location in the request (for example {@code tasks[3].duration}), may be null
     * @param message          English message produced by the validator
     * @param localizedMessage the bundle text of the code in the caller locale, else the English message
     * @param args             values of the message placeholders (never null)
     */
    public record ApiIssue(
            String code, String severity, String path, String message, String localizedMessage,
            Map<String, Object> args) {

        public ApiIssue {
            args = args == null ? Map.of() : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(args));
        }
    }
}
