package eu.isygoit.exception.handler;

import eu.isygoit.exception.ManagedException;
import eu.isygoit.i18n.service.LocaleService;
import eu.isygoit.plan.exception.InvalidRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class PlanApiExceptionHandler {
    private final LocaleService localeService;

    public PlanApiExceptionHandler(LocaleService localeService) {
        this.localeService = localeService;
    }

    @ExceptionHandler(ManagedException.class)
    public ResponseEntity<ApiError> handleManagedException(
            ManagedException exception,
            HttpServletRequest request) {
        int status = exception.getHttpStatus().value();
        ApiError body = new ApiError(
                Instant.now(),
                status,
                exception.getHttpStatus().getReasonPhrase(),
                exception instanceof InvalidRequestException requestException
                        ? requestException.code()
                        : exception.getMsgLocale(),
                localeService.getMessage(exception.getMsgLocale(), LocaleContextHolder.getLocale()),
                exception.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    public record ApiError(
            Instant timestamp, int status, String error, String code,
            String message, String detail, String path) {
    }
}
