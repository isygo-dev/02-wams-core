package eu.isygoit.enums;

/**
 * The interface Enum direction.
 */
public interface IEnumDirection {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 10;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {
        /**
         * Ltr types.
         */
        LTR("ltr"),
        /**
         * Rtl types.
         */
        RTL("rtl");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}
