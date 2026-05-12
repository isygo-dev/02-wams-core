package eu.isygoit.enums;

/**
 * WAMS KMS KeySpec alignment.
 *
 * Maps directly to WAMS SDK KeySpec values.
 */
public interface IEnumKeySpec {

    int STR_ENUM_SIZE = 6;

    /**
     * WAMS KMS Key specifications.
     */
    enum Types implements IEnum {

        /**
         * Symmetric encryption key (AES-256 under the hood).
         * WAMS equivalent: SYMMETRIC_DEFAULT
         */
        SYMMETRIC_DEFAULT("SYMMETRIC_DEFAULT"),

        /**
         * RSA 2048-bit key pair.
         */
        RSA_2048("RSA_2048"),

        /**
         * RSA 3072-bit key pair.
         */
        RSA_3072("RSA_3072"),

        /**
         * RSA 4096-bit key pair.
         */
        RSA_4096("RSA_4096"),

        /**
         * Elliptic Curve P-256 (NIST).
         * WAMS equivalent: ECC_NIST_P256
         */
        ECC_NIST_P256("ECC_NIST_P256"),

        /**
         * Elliptic Curve P-384 (NIST).
         */
        ECC_NIST_P384("ECC_NIST_P384"),

        /**
         * Elliptic Curve P-521 (NIST).
         */
        ECC_NIST_P521("ECC_NIST_P521");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        @Override
        public String meaning() {
            return meaning;
        }
    }
}