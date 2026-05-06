package eu.isygoit.enums;

/**
 * The interface Enum signing algorithm.
 */
public interface IEnumSigningAlgorithm {
    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 2;

    /**
     * The enum Types.
     */
    enum Types implements IEnum {

        /**
         * Rsassa pss sha256 types.
         */
        RSASSA_PSS_SHA256("RSASSA_PSS_SHA256"),
        /**
         * Ecdsa sha256 types.
         */
        ECDSA_SHA256("ECDSA_SHA256");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        public String meaning() {
            return meaning;
        }
    }
}

