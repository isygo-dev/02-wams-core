package eu.isygoit.dto.request;

/**
 * Request metadata. {@code clientRef} is an optional client-chosen idempotency reference (at most
 * {@value #CLIENT_REF_MAX_LENGTH} characters): submitting twice the same reference for a tenant returns the
 * existing job instead of creating a second one.
 * <p>
 * {@code version} must be {@value #SCHEMA_VERSION}, the only accepted value (checked by the validator, error
 * UNSUPPORTED_SCHEMA_VERSION; it is deliberately not defaulted here). {@code timezone} (IANA id) defaults to
 * {@value #DEFAULT_TIMEZONE}; its validity is checked by the validator (INVALID_TIMEZONE). The zone is used only to
 * expand recurring calendar rules and DAY/WEEK periods; the solver itself only sees ticks.
 * <pre>{@code
 * { "clientRef": "school-timetable-v2", "version": "2.0", "domainName": "school-timetable",
 *   "locale": "en-US", "timezone": "Europe/Paris" }
 * }</pre>
 *
 * @param clientRef  optional idempotency reference
 * @param version    schema version, "2.0"
 * @param domainName free label of the business domain
 * @param locale     display locale hint
 * @param timezone   IANA zone id, default "UTC"
 */
public record PlanningMeta(
        String clientRef,
        String version,
        String domainName,
        String locale,
        String timezone) {

    public static final String SCHEMA_VERSION = "2.0";
    public static final int CLIENT_REF_MAX_LENGTH = 128;
    public static final String DEFAULT_TIMEZONE = "UTC";

    public PlanningMeta {
        if (clientRef != null && clientRef.length() > CLIENT_REF_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "meta.clientRef must not exceed " + CLIENT_REF_MAX_LENGTH + " characters");
        }
        timezone = timezone == null || timezone.isBlank() ? DEFAULT_TIMEZONE : timezone;
    }
}
