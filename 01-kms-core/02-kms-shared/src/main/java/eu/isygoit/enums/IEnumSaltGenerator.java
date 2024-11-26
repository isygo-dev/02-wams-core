package eu.isygoit.enums;

/**
 * The interface Enum salt generator.
 */
public interface IEnumSaltGenerator {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 27;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {

        /**
         * Byte array fixed salt generator types.
         */
        ByteArrayFixedSaltGenerator("ByteArrayFixedSaltGenerator"),
        /**
         * Random salt generator types.
         */
        RandomSaltGenerator("RandomSaltGenerator"),
        /**
         * String fixed salt generator types.
         */
        StringFixedSaltGenerator("StringFixedSaltGenerator"),

        /**
         * Zero salt generator types.
         */
        ZeroSaltGenerator("ZeroSaltGenerator");


        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}
