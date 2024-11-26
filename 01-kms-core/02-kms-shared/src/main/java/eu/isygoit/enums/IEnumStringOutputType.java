package eu.isygoit.enums;

/**
 * The interface Enum string output type.
 */
public interface IEnumStringOutputType {
    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 11;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {

        /**
         * Base 64 types.
         */
        Base64("Base64"),
        /**
         * Hexadecimal types.
         */
        Hexadecimal("Hexadecimal");


        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}
