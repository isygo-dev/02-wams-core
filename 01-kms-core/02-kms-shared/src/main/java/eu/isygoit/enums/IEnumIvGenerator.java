package eu.isygoit.enums;

/**
 * The interface Enum iv generator.
 */
public interface IEnumIvGenerator {
    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 25;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {

        /**
         * Byte array fixed iv generator types.
         */
        ByteArrayFixedIvGenerator("ByteArrayFixedIvGenerator"),
        /**
         * No iv generator types.
         */
        NoIvGenerator("NoIvGenerator"),
        /**
         * Random iv generator types.
         */
        RandomIvGenerator("RandomIvGenerator"),

        /**
         * String fixed iv generator types.
         */
        StringFixedIvGenerator("StringFixedIvGenerator");


        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}
