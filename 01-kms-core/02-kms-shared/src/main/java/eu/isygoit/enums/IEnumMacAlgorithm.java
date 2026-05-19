package eu.isygoit.enums;

/**
 * Supported MAC (Message Authentication Code) algorithms for KMS.
 * Maps KMS algorithm names to Java Security algorithm names and key sizes.
 */
public enum IEnumMacAlgorithm {

    HMAC_SHA_224("HMAC_SHA_224", "HmacSHA224", 224),
    HMAC_SHA_256("HMAC_SHA_256", "HmacSHA256", 256),
    HMAC_SHA_384("HMAC_SHA_384", "HmacSHA384", 384),
    HMAC_SHA_512("HMAC_SHA_512", "HmacSHA512", 512);

    private final String kmsName;
    private final String javaAlgorithm;
    private final int keySizeBits;

    IEnumMacAlgorithm(String kmsName, String javaAlgorithm, int keySizeBits) {
        this.kmsName = kmsName;
        this.javaAlgorithm = javaAlgorithm;
        this.keySizeBits = keySizeBits;
    }

    public String getKmsName() {
        return kmsName;
    }

    public String getJavaAlgorithm() {
        return javaAlgorithm;
    }

    public int getKeySizeBits() {
        return keySizeBits;
    }

    public int getKeySizeBytes() {
        return keySizeBits / 8;
    }

    /**
     * Returns the enum constant from the KMS algorithm name.
     *
     * @param kmsName the KMS algorithm name (e.g., "HMAC_SHA_256")
     * @return the corresponding enum constant
     * @throws IllegalArgumentException if the name is not supported
     */
    public static IEnumMacAlgorithm fromKmsName(String kmsName) {
        for (IEnumMacAlgorithm algo : values()) {
            if (algo.kmsName.equals(kmsName)) {
                return algo;
            }
        }
        throw new IllegalArgumentException("Unsupported MAC algorithm: " + kmsName);
    }
}