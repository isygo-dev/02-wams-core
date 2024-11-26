package eu.isygoit.enums;

/**
 * The interface Enum menu layout.
 */
public interface IEnumMenuLayout {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 10;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {
        /**
         * Vertical types.
         */
        VERTICAL("vertical"),
        /**
         * Horizontal types.
         */
        HORIZONTAL("horizontal");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}
