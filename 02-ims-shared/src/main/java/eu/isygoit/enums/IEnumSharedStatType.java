package eu.isygoit.enums;


/**
 * The interface Enum shared stat type.
 */
public interface IEnumSharedStatType {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 30;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {
        /**
         * Total count types.
         */
        TOTAL_COUNT("TOTAL_COUNT"),
        /**
         * Active count types.
         */
        ACTIVE_COUNT("ACTIVE_COUNT"),
        /**
         * Confirmed count types.
         */
        CONFIRMED_COUNT("CONFIRMED_COUNT"),
        /**
         * Admins count types.
         */
        ADMINS_COUNT("ADMINS_COUNT"),
        /**
         * Expired count types.
         */
        EXPIRED_COUNT("EXPIRED_COUNT");
        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return this.meaning;
        }
    }
}
