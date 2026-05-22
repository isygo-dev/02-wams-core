package eu.isygoit.enums;

/**
 * AWS KMS Key Usage alignment.
 * <p>
 * Represents the intended use of the key material.
 * </p>
 *
 * @see <a href="https://docs.aws.amazon.com/kms/latest/APIReference/API_KeyMetadata.html#KMS-Type-KeyMetadata-KeyUsage">AWS KeyUsage</a>
 */
public interface IEnumKeyUsage {

    int STR_ENUM_SIZE = 3;

    /**
     * AWS KMS Key Usage types.
     */
    enum Types implements IEnum {

        /**
         * The key can be used for encryption and decryption.
         * Applicable to symmetric keys and asymmetric RSA keys.
         */
        ENCRYPT_DECRYPT("ENCRYPT_DECRYPT"),

        /**
         * The key can be used for signing and verification of messages/digests.
         * Applicable to asymmetric RSA, ECC, and SM2 keys.
         */
        SIGN_VERIFY("SIGN_VERIFY"),

        /**
         * The key can be used to generate and verify MAC (Message Authentication Code) tags.
         * Applicable to HMAC keys.
         */
        GENERATE_VERIFY_MAC("GENERATE_VERIFY_MAC");

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