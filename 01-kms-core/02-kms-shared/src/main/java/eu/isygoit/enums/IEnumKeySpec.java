package eu.isygoit.enums;

/**
 * The interface Enum key spec.
 */
public interface IEnumKeySpec {
    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 3;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {

        /**
         * Aes 256 types.
         */
        AES_256("AES_256"),
        /**
         * Rsa 2048 types.
         */
        RSA_2048("RSA_2048"),
        /**
         * Ec p256 types.
         */
        EC_P256("EC_P256");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}

