package eu.isygoit.enums;

/**
 * WAMS KMS Key Status alignment.
 *
 * Represents lifecycle states of a KMS Key as defined by WAMS KMS API.
 */
public interface IEnumKeyStatus {

    int STR_ENUM_SIZE = 7;

    /**
     * WAMS KMS key states.
     */
    enum Types implements IEnum {

        /**
         * The key is active and can be used.
         */
        ENABLED("Enabled"),

        /**
         * The key is disabled and cannot be used.
         */
        DISABLED("Disabled"),

        /**
         * The key is scheduled for deletion.
         */
        PENDING_DELETION("PendingDeletion"),

        /**
         * The key is being created.
         */
        CREATING("Creating"),

        /**
         * The key is being updated.
         */
        UPDATING("Updating"),

        /**
         * The key material is pending import.
         */
        PENDING_IMPORT("PendingImport"),

        /**
         * Replica key is pending deletion (multi-region keys).
         */
        PENDING_REPLICA_DELETION("PendingReplicaDeletion");

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