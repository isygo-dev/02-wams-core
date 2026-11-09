package eu.isygoit.exception.handler;

import eu.isygoit.config.AppProperties;
import eu.isygoit.dto.response.DiagnosticIssue;
import eu.isygoit.dto.response.ValidationReportDto;
import eu.isygoit.exception.ConflictException;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.exception.JobNotFoundException;
import eu.isygoit.exception.LimitExceededException;
import eu.isygoit.exception.ObjectNotFoundException;
import eu.isygoit.exception.OperationNotSupportedException;
import eu.isygoit.exception.SolverBusyException;
import eu.isygoit.exception.handler.PlanExceptionHandler.ApiError;
import eu.isygoit.exception.handler.PlanExceptionHandler.ApiIssue;
import eu.isygoit.i18n.service.LocaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mapping of the exceptions to the {@link ApiError} body by the merged {@link PlanExceptionHandler}.
 * No Spring context: the locale service is a stub echoing the message key; the admin e-mail collaborators are absent
 * (their failure must never alter the response).
 */
class PlanExceptionHandlerTest {

    private static final String PATH = "/api/v1/private/plan/job/1";

    private PlanExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PlanExceptionHandler(mock(AppProperties.class));
        LocaleService localeService = mock(LocaleService.class);
        when(localeService.getMessage(anyString(), any(java.util.Locale.class))).thenAnswer(i -> i.getArgument(0));
        handler.setLocaleService(localeService);
    }

    private ResponseEntity<ApiError> map(Throwable t) {
        return handler.toResponse(t, PATH, "GET");
    }

    @Test
    void jobNotFound_is404WithMessageKeyAsCode() {
        ResponseEntity<ApiError> response = map(new JobNotFoundException(1L));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ApiError body = response.getBody();
        assertEquals(404, body.status());
        assertEquals("plan.error.job.not.found", body.code());
        assertEquals(PATH, body.path());
        assertNotNull(body.timestamp());
        assertNotNull(body.detail());
    }

    @Test
    void limitExceeded_is400WithItsOwnCode() {
        ResponseEntity<ApiError> response = map(new LimitExceededException("tasks", 10, 5));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("LIMIT_EXCEEDED", response.getBody().code());
    }

    @Test
    void invalidRequest_usesItsCode() {
        ResponseEntity<ApiError> response = map(new InvalidRequestException("TENANT_REQUIRED", "no tenant"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("TENANT_REQUIRED", response.getBody().code());
        assertEquals("no tenant", response.getBody().detail());
    }

    @Test
    void solverBusy_is503() {
        ResponseEntity<ApiError> response = map(new SolverBusyException(2, 30));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(503, response.getBody().status());
    }

    @Test
    void stackExceptionsWithoutExplicitStatusGetAMeaningfulOne() {
        assertEquals(HttpStatus.CONFLICT, map(new ConflictException("not CREATED")).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, map(new ObjectNotFoundException("gone")).getStatusCode());
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED,
                map(new OperationNotSupportedException("use submit")).getStatusCode());
    }

    @Test
    void illegalArgument_is400InvalidRequest() {
        ResponseEntity<ApiError> response = map(new IllegalArgumentException("bad value"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_REQUEST", response.getBody().code());
        assertEquals("bad value", response.getBody().detail());
        assertEquals("plan.error.invalid.request", response.getBody().message());
    }

    @Test
    void unreadableBody_reportsTheIllegalArgumentCause() {
        HttpMessageNotReadableException e = new HttpMessageNotReadableException(
                "JSON parse error", new RuntimeException(new IllegalArgumentException("meta.clientRef too long")),
                mock(HttpInputMessage.class));
        ResponseEntity<ApiError> response = map(e);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("meta.clientRef too long", response.getBody().detail());

        ResponseEntity<ApiError> plain = map(new HttpMessageNotReadableException("x", mock(HttpInputMessage.class)));
        assertEquals("Malformed or unreadable request body", plain.getBody().detail());
    }

    @Test
    void typeMismatch_is400() {
        ResponseEntity<ApiError> response = map(new TypeMismatchException("abc", Long.class));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_REQUEST", response.getBody().code());
    }

    @Test
    void springMvcErrorResponse_isPassedThrough() {
        ResponseEntity<ApiError> response = map(new HttpRequestMethodNotSupportedException("PATCH"));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals("INVALID_REQUEST", response.getBody().code());
        assertNull(response.getBody().detail());
    }

    @Test
    void unexpectedException_is500WithoutInternals() {
        ResponseEntity<ApiError> response = map(new IllegalStateException("secret table name"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiError body = response.getBody();
        assertEquals("INTERNAL_ERROR", body.code());
        assertEquals("An unexpected error occurred", body.message());
        assertNull(body.detail());
        assertFalse(body.toString().contains("secret"));
    }

    // ------------------------------------------------------------------ code based localization

    /**
     * A handler whose bundle knows only the given key (any other key behaves like the stack service when a key is
     * missing: it echoes the key, or throws when {@code failOnMissing}).
     */
    private PlanExceptionHandler handlerKnowing(Map<String, String> bundle, boolean failOnMissing) {
        PlanExceptionHandler specific = new PlanExceptionHandler(mock(AppProperties.class));
        LocaleService localeService = mock(LocaleService.class);
        when(localeService.getMessage(anyString(), any(Locale.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (bundle.containsKey(key)) {
                return bundle.get(key);
            }
            if (failOnMissing) {
                throw new org.springframework.context.NoSuchMessageException(key);
            }
            return key;
        });
        specific.setLocaleService(localeService);
        return specific;
    }

    @Test
    void invalidRequest_isLocalizedByItsCodeWhenTheBundleKnowsIt() {
        PlanExceptionHandler specific = handlerKnowing(Map.of("plan.error.precedence.cycle", "The precedences loop"), false);

        ApiError body = specific.toResponse(new InvalidRequestException("PRECEDENCE_CYCLE", "A -> B -> A"),
                PATH, "POST").getBody();

        assertEquals("PRECEDENCE_CYCLE", body.code());
        assertEquals("The precedences loop", body.message());
        assertEquals("A -> B -> A", body.detail());
        assertTrue(body.issues().isEmpty());
    }

    @Test
    void invalidRequest_fallsBackToTheClassKeyWhenTheCodeHasNoText() {
        for (boolean failOnMissing : new boolean[]{false, true}) {
            PlanExceptionHandler specific = handlerKnowing(Map.of("plan.error.invalid.request", "Invalid"), failOnMissing);

            ApiError body = specific.toResponse(new InvalidRequestException("SOMETHING_NEW", "x"), PATH, "POST")
                    .getBody();

            assertEquals("SOMETHING_NEW", body.code());
            assertEquals("Invalid", body.message(), "failOnMissing=" + failOnMissing);
        }
    }

    @Test
    void errorKeyOfACodeIsLowerCaseWithDots() {
        assertEquals("plan.error.precedence.cycle", PlanExceptionHandler.keyOf("plan.error.", "PRECEDENCE_CYCLE"));
        assertEquals("plan.warning.calendar.rounded", PlanExceptionHandler.keyOf("plan.warning.", " CALENDAR_ROUNDED "));
    }

    // ------------------------------------------------------------------ issues rendering

    @Test
    void issuesAreRenderedWithTheLocalizedTextOfTheirCodeOrTheirOwnMessage() {
        PlanExceptionHandler specific = handlerKnowing(Map.of(
                "plan.error.unknown.reference", "Unknown reference",
                "plan.warning.calendar.rounded", "Calendar rounded"), false);

        List<ApiIssue> issues = specific.renderIssues(List.of(
                new DiagnosticIssue("UNKNOWN_REFERENCE", "ERROR", "tasks[3].predecessors[0].taskId", "no task t9"),
                new DiagnosticIssue("CALENDAR_ROUNDED", "WARNING", "calendars[0]", "rounded"),
                new DiagnosticIssue("DUPLICATE_ID", "ERROR", "tasks[7].id", "duplicate t7"),
                ValidationReportDto.Issue.builder().code("UNKNOWN_REFERENCE").severity("ERROR")
                        .path("resources[0].calendarId").message("no calendar c1").args(Map.of("id", "c1")).build()));

        assertEquals(4, issues.size());
        ApiIssue first = issues.get(0);
        assertEquals("UNKNOWN_REFERENCE", first.code());
        assertEquals("tasks[3].predecessors[0].taskId", first.path());
        assertEquals("no task t9", first.message());
        assertEquals("Unknown reference", first.localizedMessage());
        assertEquals("Calendar rounded", issues.get(1).localizedMessage(), "warnings use the plan.warning prefix");
        assertEquals("duplicate t7", issues.get(2).localizedMessage(), "no text for the code: the message");
        assertEquals("Unknown reference", issues.get(3).localizedMessage());
        assertEquals(Map.of("id", "c1"), issues.get(3).args());
        assertTrue(first.args().isEmpty());
    }

    @Test
    void issuesWithoutCodeKeepTheirMessageAndTheListIsBounded() {
        List<Object> many = new ArrayList<>();
        many.add(new DiagnosticIssue(null, "ERROR", null, "free text"));
        many.add(null);
        many.add("plain");
        for (int i = 0; i < PlanExceptionHandler.MAX_ISSUES + 20; i++) {
            many.add(new DiagnosticIssue("DUPLICATE_ID", "ERROR", "tasks[" + i + "].id", "dup " + i));
        }

        List<ApiIssue> issues = handler.renderIssues(many);

        assertEquals(PlanExceptionHandler.MAX_ISSUES, issues.size());
        assertEquals("free text", issues.get(0).localizedMessage());
        assertEquals("plain", issues.get(1).message());
        assertTrue(handler.renderIssues(null).isEmpty());
    }

    @Test
    void anErrorWithoutIssuesHasAnEmptyIssueList() {
        ApiError body = map(new JobNotFoundException(1L)).getBody();
        assertTrue(body.issues().isEmpty());
        assertTrue(new ApiError(java.time.Instant.EPOCH, 400, "Bad Request", "X", "m", "d", PATH).issues().isEmpty());
    }

    @Test
    void securityExceptionsAreRethrownToTheFilterChain() {
        assertThrows(AccessDeniedException.class, () -> map(new AccessDeniedException("no")));
        assertThrows(BadCredentialsException.class, () -> map(new BadCredentialsException("no")));
    }
}
