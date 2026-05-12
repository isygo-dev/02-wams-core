package eu.isygoit.enums;

/**
 * WAMS KMS Key Usage alignment.
 *
 * Maps to WAMS KMS KeyUsageType:
 * - ENCRYPT_DECRYPT (SYMMETRIC_DEFAULT)
 * - SIGN_VERIFY (ASYMMETRIC_SIGNING)
 */
public interface IEnumKeyUsage {

    int STR_ENUM_SIZE = 2;

    /**
     * WAMS KMS Key Usage types.
     */
    enum Types implements IEnum {

        /**
         * Symmetric encryption / decryption (AES-256 keys, KMS-managed).
         * WAMS equivalent: SYMMETRIC_DEFAULT
         */
        ENCRYPT_DECRYPT("SYMMETRIC_ENCRYPTION"),

        /**
         * Asymmetric signing and verification (RSA / ECC keys).
         * WAMS equivalent: ASYMMETRIC_SIGNING
         */
        SIGN_VERIFY("ASYMMETRIC_SIGNING");

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