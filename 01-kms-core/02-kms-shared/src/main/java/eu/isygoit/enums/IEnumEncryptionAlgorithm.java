package eu.isygoit.enums;

/**
 * Supported encryption algorithms for KMS.
 * Maps KMS algorithm names to Java Cipher transformations.
 */
public enum IEnumEncryptionAlgorithm {

    SYMMETRIC_DEFAULT("SYMMETRIC_DEFAULT", "AES/GCM/NoPadding", true),
    RSAES_OAEP_SHA_1("RSAES_OAEP_SHA_1", "RSA/ECB/OAEPWithSHA-1AndMGF1Padding", false),
    RSAES_OAEP_SHA_256("RSAES_OAEP_SHA_256", "RSA/ECB/OAEPWithSHA-256AndMGF1Padding", false),
    RSAES_PKCS1_V1_5("RSAES_PKCS1_V1_5", "RSA/ECB/PKCS1Padding", false);

    private final String kmsName;
    private final String javaTransformation;
    private final boolean needsIv;

    IEnumEncryptionAlgorithm(String kmsName, String javaTransformation, boolean needsIv) {
        this.kmsName = kmsName;
        this.javaTransformation = javaTransformation;
        this.needsIv = needsIv;
    }

    public String getKmsName() {
        return kmsName;
    }

    public String getJavaTransformation() {
        return javaTransformation;
    }

    public boolean needsIv() {
        return needsIv;
    }

    /**
     * Returns the enum constant from the KMS algorithm name.
     *
     * @param kmsName the KMS algorithm name (e.g., "RSAES_OAEP_SHA_256")
     * @return the corresponding enum constant
     * @throws IllegalArgumentException if the name is not supported
     */
    public static IEnumEncryptionAlgorithm fromKmsName(String kmsName) {
        for (IEnumEncryptionAlgorithm algo : values()) {
            if (algo.kmsName.equals(kmsName)) {
                return algo;
            }
        }
        throw new IllegalArgumentException("Unsupported encryption algorithm: " + kmsName);
    }
}