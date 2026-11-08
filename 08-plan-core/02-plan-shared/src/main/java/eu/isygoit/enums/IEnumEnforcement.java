package eu.isygoit.enums;

/**
 * The interface Enum enforcement.
 */
public interface IEnumEnforcement {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 32;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {
        STRICTLY_REQUIRED("Strictly required"),
        CONFIGURABLE_HARD("Configurable hard constraint"),
        SOFT("Soft constraint"),
        RELAXABLE_WITH_AUTHORIZATION("Relaxable with authorization");

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
