package eu.isygoit.enums;

/**
 * The interface Enum nav toggle.
 */
public interface IEnumNavToggle {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 10;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {
        /**
         * Accordion types.
         */
        ACCORDION("accordion"),
        /**
         * Collapse types.
         */
        COLLAPSE("collapse");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}
