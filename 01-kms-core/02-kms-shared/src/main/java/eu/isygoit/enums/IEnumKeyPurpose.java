package eu.isygoit.enums;

/**
 * The interface Enum key purpose.
 */
public interface IEnumKeyPurpose {
    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 2;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {

        /**
         * Encrypt decrypt types.
         */
        ENCRYPT_DECRYPT("ENCRYPT_DECRYPT"),
        /**
         * Sign verify types.
         */
        SIGN_VERIFY("SIGN_VERIFY");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}

