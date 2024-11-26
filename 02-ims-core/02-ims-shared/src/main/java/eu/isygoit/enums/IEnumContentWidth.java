package eu.isygoit.enums;

/**
 * The interface Enum content width.
 */
public interface IEnumContentWidth {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 10;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {
        /**
         * Full types.
         */
        FULL("full"),
        /**
         * Boxed types.
         */
        BOXED("boxed");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}
