package eu.isygoit.enums;

/**
 * WAMS KMS Grant Status.
 * <p>
 * Represents the lifecycle state of a grant.
 * </p>
 *
 * @see <a href="https://docs.wams.amazon.com/kms/latest/APIReference/API_GrantListEntry.html">WAMS GrantListEntry</a>
 */
public interface IEnumGrantStatus {

    int STR_ENUM_SIZE = 3;

    enum Types implements IEnum {
        /**
         * Grant is active and can be used to authorize operations.
         */
        ACTIVE("ACTIVE"),

        /**
         * Grant was explicitly revoked by the key owner via RevokeGrant API.
         */
        REVOKED("REVOKED"),

        /**
         * Grant was retired by the grantee or retiring principal via RetireGrant API.
         */
        RETIRED("RETIRED");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        @Override
        public String meaning() {
            return meaning;
        }
    }
}