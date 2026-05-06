package eu.isygoit.enums;

/**
 * The interface Enum key status.
 */
public interface IEnumKeyStatus {
    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 3;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {

        /**
         * Enabled types.
         */
        ENABLED("ENABLED"),
        /**
         * Disabled types.
         */
        DISABLED("DISABLED"),
        /**
         * Pending deletion types.
         */
        PENDING_DELETION("PENDING_DELETION");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}

