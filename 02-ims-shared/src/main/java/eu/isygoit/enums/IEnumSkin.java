package eu.isygoit.enums;

/**
 * The interface Enum skin.
 */
public interface IEnumSkin {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 10;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {
        /**
         * Default types.
         */
        DEFAULT("default"),
        /**
         * Bordered types.
         */
        BORDERED("bordered");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}
