package eu.isygoit.enums;

import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

public enum IEnumSignatureAlgorithm {

    RSASSA_PKCS1_V1_5_SHA_256("RSASSA_PKCS1_V1_5_SHA_256", "SHA256withRSA", "RSA", null),
    RSASSA_PKCS1_V1_5_SHA_384("RSASSA_PKCS1_V1_5_SHA_384", "SHA384withRSA", "RSA", null),
    RSASSA_PKCS1_V1_5_SHA_512("RSASSA_PKCS1_V1_5_SHA_512", "SHA512withRSA", "RSA", null),
    RSASSA_PSS_SHA_256("RSASSA_PSS_SHA_256", "RSASSA-PSS", "RSA", createPSSParameterSpec("SHA-256", 32)),
    RSASSA_PSS_SHA_384("RSASSA_PSS_SHA_384", "RSASSA-PSS", "RSA", createPSSParameterSpec("SHA-384", 48)),
    RSASSA_PSS_SHA_512("RSASSA_PSS_SHA_512", "RSASSA-PSS", "RSA", createPSSParameterSpec("SHA-512", 64)),
    ECDSA_SHA_256("ECDSA_SHA_256", "SHA256withECDSA", "EC", null),
    ECDSA_SHA_384("ECDSA_SHA_384", "SHA384withECDSA", "EC", null),
    ECDSA_SHA_512("ECDSA_SHA_512", "SHA512withECDSA", "EC", null),
    SM2DSA("SM2DSA", "SM3withSM2", "EC", null); // requires Bouncy Castle

    private final String kmsName;
    private final String javaAlgorithm;
    private final String keyAlgorithm;
    private final PSSParameterSpec pssSpec;

    IEnumSignatureAlgorithm(String kmsName, String javaAlgorithm, String keyAlgorithm, PSSParameterSpec pssSpec) {
        this.kmsName = kmsName;
        this.javaAlgorithm = javaAlgorithm;
        this.keyAlgorithm = keyAlgorithm;
        this.pssSpec = pssSpec;
    }

    public String getKmsName() { return kmsName; }
    public String getJavaAlgorithm() { return javaAlgorithm; }
    public String getKeyAlgorithm() { return keyAlgorithm; }
    public PSSParameterSpec getPssSpec() { return pssSpec; }

    public static IEnumSignatureAlgorithm fromKmsName(String kmsName) {
        for (IEnumSignatureAlgorithm algo : values()) {
            if (algo.kmsName.equals(kmsName)) {
                return algo;
            }
        }
        throw new IllegalArgumentException("Unsupported signature algorithm: " + kmsName);
    }

    private static PSSParameterSpec createPSSParameterSpec(String hashAlgo, int saltLength) {
        return new PSSParameterSpec(hashAlgo, "MGF1", MGF1ParameterSpec.SHA256, saltLength, 1);
    }
}