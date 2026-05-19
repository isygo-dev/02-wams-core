package eu.isygoit.mapper;

import eu.isygoit.enums.IEnumEncryptionAlgorithm;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumMacAlgorithm;
import eu.isygoit.enums.IEnumSignatureAlgorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to map key specifications to supported cryptographic algorithms.
 */
public final class AlgorithmMapper {

    private AlgorithmMapper() {
        // private constructor to prevent instantiation
    }

    public static List<IEnumEncryptionAlgorithm> keySpecToEncryptionAlgo(IEnumKeySpec.Types keySpec) {
        List<IEnumEncryptionAlgorithm> algorithms = new ArrayList<>();
        if (keySpec == null) {
            return algorithms;
        }
        if (keySpec == IEnumKeySpec.Types.SYMMETRIC_DEFAULT) {
            algorithms.add(IEnumEncryptionAlgorithm.SYMMETRIC_DEFAULT);
        } else if (keySpec.name().startsWith("RSA")) {
            algorithms.add(IEnumEncryptionAlgorithm.RSAES_OAEP_SHA_256);
            algorithms.add(IEnumEncryptionAlgorithm.RSAES_PKCS1_V1_5);
            // Optionally also RSAES_OAEP_SHA_1 if you support it
            // algorithms.add(IEnumEncryptionAlgorithm.RSAES_OAEP_SHA_1);
        }
        // For other key specs (ECC, HMAC), encryption is not supported
        return algorithms;
    }

    /**
     * Returns the list of signature algorithms supported by the given key specification.
     *
     * @param keySpec the key specification (must not be null)
     * @return a list of IEnumSignatureAlgorithm constants
     */
    public static List<IEnumSignatureAlgorithm> keySpecToSigningAlgo(IEnumKeySpec.Types keySpec) {
        List<IEnumSignatureAlgorithm> algorithms = new ArrayList<>();
        if (keySpec == null) {
            return algorithms;
        }
        if (keySpec.name().startsWith("RSA")) {
            algorithms.add(IEnumSignatureAlgorithm.RSASSA_PKCS1_V1_5_SHA_256);
            algorithms.add(IEnumSignatureAlgorithm.RSASSA_PKCS1_V1_5_SHA_384);
            algorithms.add(IEnumSignatureAlgorithm.RSASSA_PKCS1_V1_5_SHA_512);
            algorithms.add(IEnumSignatureAlgorithm.RSASSA_PSS_SHA_256);
            algorithms.add(IEnumSignatureAlgorithm.RSASSA_PSS_SHA_384);
            algorithms.add(IEnumSignatureAlgorithm.RSASSA_PSS_SHA_512);
        } else if (keySpec.name().startsWith("ECC") || keySpec == IEnumKeySpec.Types.ECC_NIST_P256 ||
                keySpec == IEnumKeySpec.Types.ECC_NIST_P384 || keySpec == IEnumKeySpec.Types.ECC_NIST_P521 ||
                keySpec == IEnumKeySpec.Types.ECC_SECG_P256K1) {
            algorithms.add(IEnumSignatureAlgorithm.ECDSA_SHA_256);
            algorithms.add(IEnumSignatureAlgorithm.ECDSA_SHA_384);
            algorithms.add(IEnumSignatureAlgorithm.ECDSA_SHA_512);
        } else if (keySpec == IEnumKeySpec.Types.SM2) {
            algorithms.add(IEnumSignatureAlgorithm.SM2DSA);
        }
        // Note: symmetric and HMAC keys do not support signing
        return algorithms;
    }

    /**
     * Returns the list of MAC algorithms supported by the given key specification.
     *
     * @param keySpec the key specification (must not be null)
     * @return a list of IEnumMacAlgorithm constants
     */
    public static List<IEnumMacAlgorithm> keySpecToMacAlgo(IEnumKeySpec.Types keySpec) {
        List<IEnumMacAlgorithm> algorithms = new ArrayList<>();
        if (keySpec == null) {
            return algorithms;
        }
        if (keySpec.name().startsWith("HMAC")) {
            algorithms.add(IEnumMacAlgorithm.HMAC_SHA_224);
            algorithms.add(IEnumMacAlgorithm.HMAC_SHA_256);
            algorithms.add(IEnumMacAlgorithm.HMAC_SHA_384);
            algorithms.add(IEnumMacAlgorithm.HMAC_SHA_512);
        }
        return algorithms;
    }
}