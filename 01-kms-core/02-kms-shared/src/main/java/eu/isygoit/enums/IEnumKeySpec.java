package eu.isygoit.enums;

/**
 * AWS KMS KeySpec alignment.
 * <p>
 * Represents the cryptographic key specification (type and length).
 * </p>
 * @see <a href="https://docs.aws.amazon.com/kms/latest/APIReference/API_KeyMetadata.html#KMS-Type-KeyMetadata-KeySpec">AWS KeySpec</a>
 */
public interface IEnumKeySpec {

    int STR_ENUM_SIZE = 14;  // Increased to accommodate all values

    /**
     * AWS KMS Key specifications.
     */
    enum Types implements IEnum {

        /**
         * Symmetric default key (AES-256‑GCM). Used for encryption/decryption.
         */
        SYMMETRIC_DEFAULT("SYMMETRIC_DEFAULT"),

        /**
         * RSA 2048-bit key pair. Can be used for encryption/decryption or signing/verification.
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
         * Elliptic Curve P-256 (NIST). Used for signing/verification.
         */
        ECC_NIST_P256("ECC_NIST_P256"),

        /**
         * Elliptic Curve P-384 (NIST).
         */
        ECC_NIST_P384("ECC_NIST_P384"),

        /**
         * Elliptic Curve P-521 (NIST).
         */
        ECC_NIST_P521("ECC_NIST_P521"),

        /**
         * Elliptic Curve SECG P-256k1 (Bitcoin curve). Used for signing/verification.
         */
        ECC_SECG_P256K1("ECC_SECG_P256K1"),

        /**
         * HMAC 224-bit key. Used for generating/verifying MACs.
         */
        HMAC_224("HMAC_224"),

        /**
         * HMAC 256-bit key.
         */
        HMAC_256("HMAC_256"),

        /**
         * HMAC 384-bit key.
         */
        HMAC_384("HMAC_384"),

        /**
         * HMAC 512-bit key.
         */
        HMAC_512("HMAC_512"),

        /**
         * SM2 (Chinese national standard) key pair. Supports both encryption and signing.
         */
        SM2("SM2");

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