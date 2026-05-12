package eu.isygoit.enums;

/**
 * WAMS KMS Custom Key Store Status alignment.
 *
 * Represents lifecycle states of CloudHSM / XKS custom key stores.
 */
public interface IEnumCustomKeyStoreStatus {

    enum Types implements IEnum {

        /**
         * The custom key store is being created.
         */
        CREATING("CREATING"),

        /**
         * Connection is being established.
         */
        CONNECTING("CONNECTING"),

        /**
         * The custom key store is fully connected and operational.
         */
        CONNECTED("CONNECTED"),

        /**
         * The custom key store is disconnected but still exists.
         */
        DISCONNECTED("DISCONNECTED"),

        /**
         * Connection is being torn down.
         */
        DISCONNECTING("DISCONNECTING"),

        /**
         * The custom key store operation failed.
         */
        FAILED("FAILED"),

        /**
         * The custom key store is being deleted.
         */
        DELETING("DELETING");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public static Types fromValue(String value) {
            for (Types status : values()) {
                if (status.meaning.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown custom key store status: " + value);
        }

        @Override
        public String meaning() {
            return meaning;
        }

        // =========================================================================
        // State classification helpers (kept from your design - good abstraction)
        // =========================================================================

        public boolean isOperational() {
            return this == CONNECTED;
        }

        public boolean isTransitional() {
            return this == CREATING
                    || this == CONNECTING
                    || this == DISCONNECTING;
        }

        public boolean isTerminal() {
            return this == FAILED
                    || this == DELETING;
        }
    }
}