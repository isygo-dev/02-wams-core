package eu.isygoit.enums;

import java.util.ArrayList;
import java.util.List;

/**
 * AWS KMS KeySpec alignment with Java JCA algorithm names.
 * <p>
 * Represents the cryptographic key specification (type and length) as defined by AWS KMS,
 * mapped to the corresponding Java Cryptography Architecture (JCA) algorithm names,
 * key sizes, and other metadata needed for key generation and cryptographic operations.
 * </p>
 *
 * @see <a href="https://docs.aws.amazon.com/kms/latest/APIReference/API_KeyMetadata.html#KMS-Type-KeyMetadata-KeySpec">AWS KeySpec</a>
 */
public interface IEnumKeySpec {

    int STR_ENUM_SIZE = 14;

    /**
     * AWS KMS Key specifications with Java algorithm mappings.
     */
    enum Types implements IEnum {

        /**
         * Symmetric default key (AES-256-GCM).
         * <p>
         * <b>Java algorithm:</b> {@code "AES"}<br>
         * <b>Key size:</b> 256 bits<br>
         * <b>Asymmetric:</b> false<br>
         * <b>Allowed usages:</b> ENCRYPT_DECRYPT only.
         * </p>
         */
        SYMMETRIC_DEFAULT("SYMMETRIC_DEFAULT", "AES", 256, false, IEnumKeyUsage.Types.ENCRYPT_DECRYPT),

        /**
         * RSA 2048-bit key pair.
         * <p>
         * <b>Java algorithm:</b> {@code "RSA"}<br>
         * <b>Key size:</b> 2048 bits<br>
         * <b>Asymmetric:</b> true<br>
         * <b>Allowed usages:</b> ENCRYPT_DECRYPT, SIGN_VERIFY.
         * </p>
         */
        RSA_2048("RSA_2048", "RSA", 2048, true, IEnumKeyUsage.Types.ENCRYPT_DECRYPT, IEnumKeyUsage.Types.SIGN_VERIFY),

        /**
         * RSA 3072-bit key pair.
         * <p>
         * <b>Java algorithm:</b> {@code "RSA"}<br>
         * <b>Key size:</b> 3072 bits<br>
         * <b>Asymmetric:</b> true<br>
         * <b>Allowed usages:</b> ENCRYPT_DECRYPT, SIGN_VERIFY.
         * </p>
         */
        RSA_3072("RSA_3072", "RSA", 3072, true, IEnumKeyUsage.Types.ENCRYPT_DECRYPT, IEnumKeyUsage.Types.SIGN_VERIFY),

        /**
         * RSA 4096-bit key pair.
         * <p>
         * <b>Java algorithm:</b> {@code "RSA"}<br>
         * <b>Key size:</b> 4096 bits<br>
         * <b>Asymmetric:</b> true<br>
         * <b>Allowed usages:</b> ENCRYPT_DECRYPT, SIGN_VERIFY.
         * </p>
         */
        RSA_4096("RSA_4096", "RSA", 4096, true, IEnumKeyUsage.Types.ENCRYPT_DECRYPT, IEnumKeyUsage.Types.SIGN_VERIFY),

        /**
         * Elliptic Curve NIST P-256 (secp256r1). Used for signing/verification.
         * <p>
         * <b>Java algorithm:</b> {@code "EC"}<br>
         * <b>Key size:</b> 256 bits (curve strength)<br>
         * <b>Asymmetric:</b> true<br>
         * <b>Curve name (for ECGenParameterSpec):</b> {@code "secp256r1"}<br>
         * <b>Allowed usages:</b> SIGN_VERIFY only.
         * </p>
         */
        ECC_NIST_P256("ECC_NIST_P256", "EC", 256, true, IEnumKeyUsage.Types.SIGN_VERIFY),

        /**
         * Elliptic Curve NIST P-384 (secp384r1). Used for signing/verification.
         * <p>
         * <b>Java algorithm:</b> {@code "EC"}<br>
         * <b>Key size:</b> 384 bits<br>
         * <b>Asymmetric:</b> true<br>
         * <b>Curve name (for ECGenParameterSpec):</b> {@code "secp384r1"}<br>
         * <b>Allowed usages:</b> SIGN_VERIFY only.
         * </p>
         */
        ECC_NIST_P384("ECC_NIST_P384", "EC", 384, true, IEnumKeyUsage.Types.SIGN_VERIFY),

        /**
         * Elliptic Curve NIST P-521 (secp521r1). Used for signing/verification.
         * <p>
         * <b>Java algorithm:</b> {@code "EC"}<br>
         * <b>Key size:</b> 521 bits<br>
         * <b>Asymmetric:</b> true<br>
         * <b>Curve name (for ECGenParameterSpec):</b> {@code "secp521r1"}<br>
         * <b>Allowed usages:</b> SIGN_VERIFY only.
         * </p>
         */
        ECC_NIST_P521("ECC_NIST_P521", "EC", 521, true, IEnumKeyUsage.Types.SIGN_VERIFY),

        /**
         * Elliptic Curve SECG P-256k1 (Bitcoin curve). Used for signing/verification.
         * <p>
         * <b>Java algorithm:</b> {@code "EC"}<br>
         * <b>Key size:</b> 256 bits<br>
         * <b>Asymmetric:</b> true<br>
         * <b>Curve name (for ECGenParameterSpec):</b> {@code "secp256k1"}<br>
         * <b>Allowed usages:</b> SIGN_VERIFY only.
         * </p>
         */
        ECC_SECG_P256K1("ECC_SECG_P256K1", "EC", 256, true, IEnumKeyUsage.Types.SIGN_VERIFY),

        /**
         * HMAC 224-bit key. Used for generating/verifying MACs.
         * <p>
         * <b>Java algorithm:</b> {@code "HmacSHA224"}<br>
         * <b>Key size:</b> 224 bits (28 bytes)<br>
         * <b>Asymmetric:</b> false<br>
         * <b>Allowed usages:</b> GENERATE_VERIFY_MAC only.
         * </p>
         */
        HMAC_224("HMAC_224", "HmacSHA224", 224, false, IEnumKeyUsage.Types.GENERATE_VERIFY_MAC),

        /**
         * HMAC 256-bit key. Used for generating/verifying MACs.
         * <p>
         * <b>Java algorithm:</b> {@code "HmacSHA256"}<br>
         * <b>Key size:</b> 256 bits (32 bytes)<br>
         * <b>Asymmetric:</b> false<br>
         * <b>Allowed usages:</b> GENERATE_VERIFY_MAC only.
         * </p>
         */
        HMAC_256("HMAC_256", "HmacSHA256", 256, false, IEnumKeyUsage.Types.GENERATE_VERIFY_MAC),

        /**
         * HMAC 384-bit key. Used for generating/verifying MACs.
         * <p>
         * <b>Java algorithm:</b> {@code "HmacSHA384"}<br>
         * <b>Key size:</b> 384 bits (48 bytes)<br>
         * <b>Asymmetric:</b> false<br>
         * <b>Allowed usages:</b> GENERATE_VERIFY_MAC only.
         * </p>
         */
        HMAC_384("HMAC_384", "HmacSHA384", 384, false, IEnumKeyUsage.Types.GENERATE_VERIFY_MAC),

        /**
         * HMAC 512-bit key. Used for generating/verifying MACs.
         * <p>
         * <b>Java algorithm:</b> {@code "HmacSHA512"}<br>
         * <b>Key size:</b> 512 bits (64 bytes)<br>
         * <b>Asymmetric:</b> false<br>
         * <b>Allowed usages:</b> GENERATE_VERIFY_MAC only.
         * </p>
         */
        HMAC_512("HMAC_512", "HmacSHA512", 512, false, IEnumKeyUsage.Types.GENERATE_VERIFY_MAC),

        /**
         * SM2 (Chinese national standard) key pair. Supports both encryption and signing.
         * <p>
         * <b>Java algorithm:</b> {@code "SM2"} (requires Bouncy Castle provider)<br>
         * <b>Key size:</b> 256 bits (curve strength)<br>
         * <b>Asymmetric:</b> true<br>
         * <b>Allowed usages:</b> ENCRYPT_DECRYPT, SIGN_VERIFY.
         * </p>
         */
        SM2("SM2", "SM2", 256, true, IEnumKeyUsage.Types.ENCRYPT_DECRYPT, IEnumKeyUsage.Types.SIGN_VERIFY);

        private final String awsName;
        private final String javaAlgorithm;
        private final int keySizeBits;
        private final boolean asymmetric;
        private final List<IEnumKeyUsage.Types> allowedUsages;

        /**
         * Constructor for the enum constant.
         *
         * @param awsName        the AWS KeySpec name (e.g., "SYMMETRIC_DEFAULT")
         * @param javaAlgorithm  the JCA algorithm name for key generation (e.g., "AES", "RSA", "EC", "HmacSHA256")
         * @param keySizeBits    the key size in bits (for symmetric/HMAC) or curve strength (for EC)
         * @param asymmetric     true if this key spec represents an asymmetric key pair (RSA/EC/SM2)
         * @param allowedUsages  the IEnumKeyUsage types that are allowed for this key spec
         */
        Types(String awsName, String javaAlgorithm, int keySizeBits, boolean asymmetric, IEnumKeyUsage.Types... allowedUsages) {
            this.awsName = awsName;
            this.javaAlgorithm = javaAlgorithm;
            this.keySizeBits = keySizeBits;
            this.asymmetric = asymmetric;
            this.allowedUsages = new ArrayList<>();
            for (IEnumKeyUsage.Types usage : allowedUsages) {
                this.allowedUsages.add(usage);
            }
        }

        @Override
        public String meaning() {
            return awsName;
        }

        /**
         * Returns the JCA algorithm name to be used for key generation or cryptographic operations.
         * Examples: "AES", "RSA", "EC", "HmacSHA256".
         *
         * @return the Java algorithm name
         */
        public String getJavaAlgorithm() {
            return javaAlgorithm;
        }

        /**
         * Returns the key size in bits. For symmetric and HMAC keys this is the actual key length.
         * For RSA keys it is the modulus size. For EC keys it is the curve strength (e.g., 256 for P-256).
         *
         * @return the key size in bits
         */
        public int getKeySizeBits() {
            return keySizeBits;
        }

        /**
         * Indicates whether this key spec represents an asymmetric key pair (RSA, EC, SM2).
         *
         * @return true if asymmetric, false for symmetric/HMAC
         */
        public boolean isAsymmetric() {
            return asymmetric;
        }

        /**
         * Returns the list of IEnumKeyUsage types that are allowed for this key spec.
         *
         * @return the allowed usages
         */
        public List<IEnumKeyUsage.Types> allowedUsages() {
            return allowedUsages;
        }

        /**
         * For EC curves, returns the standard curve name to be used with {@link java.security.spec.ECGenParameterSpec}.
         * For non‑EC key specs, returns null.
         *
         * @return the curve name (e.g., "secp256r1") or null if not applicable
         */
        public String getCurveName() {
            switch (this) {
                case ECC_NIST_P256:
                    return "secp256r1";
                case ECC_NIST_P384:
                    return "secp384r1";
                case ECC_NIST_P521:
                    return "secp521r1";
                case ECC_SECG_P256K1:
                    return "secp256k1";
                case SM2:
                    return "sm2p256v1";  // Standard SM2 curve name with Bouncy Castle
                default:
                    return null;
            }
        }

        /**
         * Returns the key size in bytes (convenience method).
         *
         * @return the key size in bytes
         */
        public int getKeySizeBytes() {
            return keySizeBits / 8;
        }
    }

    public static String mapMacAlgorithm(String yourAlgorithm) {
        switch (yourAlgorithm) {
            case "HMAC_SHA_224":
                return "HmacSHA224";
            case "HMAC_SHA_256":
                return "HmacSHA256";
            case "HMAC_SHA_384":
                return "HmacSHA384";
            case "HMAC_SHA_512":
                return "HmacSHA512";
            // Add your own custom algorithm names if they differ
            default:
                throw new IllegalArgumentException("Unsupported MAC algorithm: " + yourAlgorithm);
        }
    }

    public static String mapSigningAlgorithm(String awsAlgo) {
        switch (awsAlgo) {
            case "RSASSA_PKCS1_V1_5_SHA_256":
                return "SHA256withRSA";
            case "RSASSA_PKCS1_V1_5_SHA_384":
                return "SHA384withRSA";
            case "RSASSA_PKCS1_V1_5_SHA_512":
                return "SHA512withRSA";
            case "RSASSA_PSS_SHA_256":
                return "SHA256withRSA/PSS";
            case "RSASSA_PSS_SHA_384":
                return "SHA384withRSA/PSS";
            case "RSASSA_PSS_SHA_512":
                return "SHA512withRSA/PSS";
            case "ECDSA_SHA_256":
                return "SHA256withECDSA";
            case "ECDSA_SHA_384":
                return "SHA384withECDSA";
            case "ECDSA_SHA_512":
                return "SHA512withECDSA";
            case "SM2DSA":
                return "SM3withSM2"; // Requires Bouncy Castle
            default:
                throw new IllegalArgumentException("Unsupported signing algorithm: " + awsAlgo);
        }
    }
}