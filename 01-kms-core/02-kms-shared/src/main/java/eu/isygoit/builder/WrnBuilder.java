package eu.isygoit.builder;

import eu.isygoit.exception.WrnValidationException;

/**
 * Builder for creating WAMS Resource Names (WRN) for your custom KMS service.
 * <p>
 * The WRN format is:
 * <pre>
 * wrn:wams:kms:region:account-id:resource
 * </pre>
 * </p>
 * <p>
 * It is inspired by AWS ARN but adapted to your specific infrastructure:
 * <ul>
 *   <li><b>wrn</b> – fixed prefix (WAMS Resource Name).</li>
 *   <li><b>wams</b> – fixed partition (your cloud platform).</li>
 *   <li><b>kms</b> – fixed service (Key Management Service).</li>
 *   <li><b>region</b> – one of the four allowed regions: <b>north, south, east, west</b>.</li>
 *   <li><b>account-id</b> – optional tenant/account identifier (e.g., "default" or a UUID).</li>
 *   <li><b>resource</b> – resource type and identifier (e.g., "key/1234abcd-...").</li>
 * </ul>
 * </p>
 *
 * <h2>Allowed regions</h2>
 * The builder validates that the region is one of the following (case‑sensitive):
 * <ul>
 *   <li>{@value #REGION_NORTH}</li>
 *   <li>{@value #REGION_SOUTH}</li>
 *   <li>{@value #REGION_EAST}</li>
 *   <li>{@value #REGION_WEST}</li>
 * </ul>
 *
 * <h2>Usage examples</h2>
 *
 * <h3>1. KMS key in the north region</h3>
 * <pre>{@code
 * String wrn = WrnBuilder.builder()
 *         .region(WrnBuilder.REGION_NORTH)
 *         .accountId("tenant-123")
 *         .resource("key", "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
 *         .build();
 * // Result: wrn:wams:kms:north:tenant-123:key/a1b2c3d4-e5f6-7890-abcd-ef1234567890
 * }</pre>
 *
 * <h3>2. KMS key in the south region (default tenant)</h3>
 * <pre>{@code
 * String wrn = WrnBuilder.builder()
 *         .region(WrnBuilder.REGION_SOUTH)
 *         .resource("alias", "my-app-key")
 *         .build();
 * // Result: wrn:wams:kms:south::alias/my-app-key
 * }</pre>
 *
 * <h3>3. Using convenience factory method</h3>
 * <pre>{@code
 * String wrn = WrnBuilder.forKmsKey(WrnBuilder.REGION_EAST, "12345", "my-key-id");
 * // Result: wrn:wams:kms:east:12345:key/my-key-id
 * }</pre>
 *
 * <h3>4. Custom resource type (e.g., alias, grant, etc.)</h3>
 * <pre>{@code
 * String aliasWrn = WrnBuilder.builder()
 *         .region(WrnBuilder.REGION_WEST)
 *         .accountId("finance-dept")
 *         .resource("alias", "finance-master-key")
 *         .build();
 * // Result: wrn:wams:kms:west:finance-dept:alias/finance-master-key
 * }</pre>
 *
 * <h2>Important notes</h2>
 * <ul>
 *   <li>The region is <b>mandatory</b> in this builder (unlike AWS ARN) because your KMS is region‑bound.</li>
 *   <li>The account‑id is optional; if omitted, it becomes an empty string, resulting in two consecutive colons (`::`).</li>
 *   <li>The resource part <b>must not be empty</b> and is typically formatted as "type/id".</li>
 *   <li>This builder does <b>not</b> validate the existence of the account or key – only the format.</li>
 * </ul>
 *
 * @author WAMS KMS Team
 * @version 1.0
 */
public final class WrnBuilder {

    // -------------------------------------------------------------------------
    // Fixed constants for your KMS
    // -------------------------------------------------------------------------

    /**
     * North region identifier.
     */
    public static final String REGION_NORTH = "north";
    /**
     * South region identifier.
     */
    public static final String REGION_SOUTH = "south";
    /**
     * East region identifier.
     */
    public static final String REGION_EAST = "east";

    // -------------------------------------------------------------------------
    // Allowed regions
    // -------------------------------------------------------------------------
    /**
     * West region identifier.
     */
    public static final String REGION_WEST = "west";
    /**
     * WRN prefix – always "wrn".
     */
    private static final String PREFIX = "wrn";
    /**
     * Partition – always "wams".
     */
    private static final String PARTITION = "wams";
    /**
     * Service – always "kms".
     */
    private static final String SERVICE = "kms";
    private static final String[] ALLOWED_REGIONS = {
            REGION_NORTH, REGION_SOUTH, REGION_EAST, REGION_WEST
    };

    // -------------------------------------------------------------------------
    // Builder fields
    // -------------------------------------------------------------------------

    private String region;
    private String accountId;
    private String resource;

    private WrnBuilder() {
        // private constructor
    }

    /**
     * Creates a new WrnBuilder instance.
     *
     * @return a fresh builder
     */
    public static WrnBuilder builder() {
        return new WrnBuilder();
    }

    /**
     * Creates a WRN for a KMS key.
     *
     * @param region    one of the four allowed regions (north, south, east, west)
     * @param accountId account/tenant identifier (may be null or empty)
     * @param keyId     the key ID (UUID or custom identifier)
     * @return WRN string
     */
    public static String forKmsKey(String region, String accountId, String keyId) {
        return WrnBuilder.builder()
                .region(region)
                .accountId(accountId)
                .resource("key", keyId)
                .build();
    }

    /**
     * Creates a WRN for a KMS alias.
     *
     * @param region    one of the four allowed regions
     * @param accountId account/tenant identifier (may be null)
     * @param aliasName the alias name (e.g., "my-key-alias")
     * @return WRN string
     */
    public static String forAlias(String region, String accountId, String aliasName) {
        return WrnBuilder.builder()
                .region(region)
                .accountId(accountId)
                .resource("alias", aliasName)
                .build();
    }

    /**
     * Sets the region (mandatory). Must be one of: {@value #REGION_NORTH}, {@value #REGION_SOUTH},
     * {@value #REGION_EAST}, {@value #REGION_WEST}.
     *
     * @param region the region (case‑sensitive, must be exactly one of the allowed values)
     * @return this builder
     * @throws WrnValidationException if region is null, blank, or not in the allowed set
     */
    public WrnBuilder region(String region) {
        if (region == null || region.isBlank()) {
            throw new WrnValidationException("Region must not be null or blank");
        }
        boolean allowed = false;
        for (String allowedRegion : ALLOWED_REGIONS) {
            if (allowedRegion.equals(region)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new WrnValidationException("Invalid region: " + region +
                    ". Allowed values: north, south, east, west");
        }
        this.region = region;
        return this;
    }

    /**
     * Sets the account/tenant identifier (optional).
     * <p>
     * If omitted or set to null, the account part will be empty,
     * resulting in two consecutive colons in the WRN.
     * </p>
     *
     * @param accountId the account identifier (e.g., "tenant-123", "default") or null
     * @return this builder
     */
    public WrnBuilder accountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    /**
     * Sets the resource part directly.
     * <p>
     * Expected format: "type/identifier", for example "key/abcd-1234" or "alias/my-key".
     * </p>
     *
     * @param resource the resource string (must not be null or blank)
     * @return this builder
     * @throws WrnValidationException if resource is null or blank
     */
    public WrnBuilder resource(String resource) {
        if (resource == null || resource.isBlank()) {
            throw new WrnValidationException("Resource must not be null or blank");
        }
        this.resource = resource;
        return this;
    }

    // -------------------------------------------------------------------------
    // Convenience factory methods
    // -------------------------------------------------------------------------

    /**
     * Helper method to build a resource string as "resourceType/resourceId".
     * This is the recommended way to keep resource strings consistent.
     *
     * @param resourceType the type of resource (e.g., "key", "alias", "grant")
     * @param resourceId   the identifier (e.g., UUID, alias name, grant ID)
     * @return this builder
     * @throws WrnValidationException if either parameter is null or blank
     */
    public WrnBuilder resource(String resourceType, String resourceId) {
        if (resourceType == null || resourceType.isBlank()) {
            throw new WrnValidationException("Resource type must not be null or blank");
        }
        if (resourceId == null || resourceId.isBlank()) {
            throw new WrnValidationException("Resource ID must not be null or blank");
        }
        this.resource = resourceType + "/" + resourceId;
        return this;
    }

    /**
     * Builds the final WRN string.
     * <p>
     * Format: {@code wrn:wams:kms:region:account-id:resource}
     * If account‑id is null/empty, it becomes an empty string, producing a double colon.
     * </p>
     *
     * @return the WRN as a string
     * @throws IllegalStateException if region or resource is missing
     */
    public String build() {
        if (region == null) {
            throw new IllegalStateException("Region is required");
        }
        if (resource == null) {
            throw new IllegalStateException("Resource is required");
        }

        String accountPart = (accountId != null && !accountId.isBlank()) ? accountId : "";

        return String.format("%s:%s:%s:%s:%s:%s",
                PREFIX,
                PARTITION,
                SERVICE,
                region,
                accountPart,
                resource);
    }
}