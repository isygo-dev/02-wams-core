package eu.isygoit.dto.response;

/**
 * A non-fatal observation of the solve pipeline (rounded calendars, clipped values, ignored hints...).
 *
 * @param code     stable code, for example CALENDAR_ROUNDED
 * @param severity INFO | WARNING
 * @param path     location in the request, may be null
 * @param message  human readable detail
 */
public record DiagnosticIssue(String code, String severity, String path, String message) {
}
