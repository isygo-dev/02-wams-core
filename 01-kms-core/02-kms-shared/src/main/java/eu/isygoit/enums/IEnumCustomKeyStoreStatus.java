package eu.isygoit.enums;

/**
 * Custom Key Store Status Enumeration
 * <p>
 * Defines the possible states of a custom key store
 *
 * @author Isygoit Team
 * @version 1.0
 */
public interface IEnumCustomKeyStoreStatus {

    enum Types implements IEnum {
        /**
         * Store is created but not connected
         */
        DISCONNECTED("DISCONNECTED"),

        /**
         * Store is currently establishing connection
         */
        CONNECTING("CONNECTING"),

        /**
         * Store is successfully connected and operational
         */
        CONNECTED("CONNECTED"),

        /**
         * Store is in the process of disconnecting
         */
        DISCONNECTING("DISCONNECTING"),

        /**
         * Store connection failed or is unavailable
         */
        FAILED("FAILED"),

        /**
         * Store is pending deletion
         */
        PENDING_DELETION("PENDING_DELETION"),

        /**
         * Store is being updated
         */
        UPDATING("UPDATING");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public static Types fromValue(String value) {
            for (Types status : Types.values()) {
                if (status.meaning.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown custom key store status: " + value);
        }

        public String meaning() {
            return meaning;
        }

        public boolean isOperational() {
            return this == CONNECTED;
        }

        public boolean isTransitional() {
            return this == CONNECTING || this == DISCONNECTING || this == UPDATING;
        }

        public boolean isTerminal() {
            return this == FAILED || this == PENDING_DELETION;
        }
    }
}