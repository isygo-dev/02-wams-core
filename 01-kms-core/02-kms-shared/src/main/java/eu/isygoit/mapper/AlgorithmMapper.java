package eu.isygoit.mapper;

import eu.isygoit.enums.*;

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

    /**
     * Returns the default algorithm for a given key specification and usage.
     * The algorithm is chosen based on the key size (bits) to match the appropriate strength.
     *
     * @param keySpec the key specification (must not be null)
     * @param usage   the key usage (ENCRYPT_DECRYPT, SIGN_VERIFY, GENERATE_VERIFY_MAC)
     * @return the default algorithm name, or null if not defined
     */
    public static String getDefaultAlgorithm(IEnumKeySpec.Types keySpec, IEnumKeyUsage.Types usage) {
        if (keySpec == null) return null;

        // ----- ENCRYPTION -----
        if (usage == IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
            if (keySpec.name().startsWith("RSA")) {
                // RSA encryption uses OAEP with SHA-256 regardless of key size (AWS KMS behaviour)
                return IEnumEncryptionAlgorithm.RSAES_OAEP_SHA_256.name();
            } else if (keySpec == IEnumKeySpec.Types.SYMMETRIC_DEFAULT) {
                return IEnumEncryptionAlgorithm.SYMMETRIC_DEFAULT.name();
            } else {
                return IEnumEncryptionAlgorithm.SYMMETRIC_DEFAULT.name();
            }
        }
        // ----- SIGNING -----
        else if (usage == IEnumKeyUsage.Types.SIGN_VERIFY) {
            if (keySpec.name().startsWith("RSA")) {
                int size = keySpec.getKeySizeBits();
                switch (size) {
                    case 2048: return IEnumSignatureAlgorithm.RSASSA_PKCS1_V1_5_SHA_256.name();
                    case 3072: return IEnumSignatureAlgorithm.RSASSA_PKCS1_V1_5_SHA_384.name();
                    case 4096: return IEnumSignatureAlgorithm.RSASSA_PKCS1_V1_5_SHA_512.name();
                    default:   return IEnumSignatureAlgorithm.RSASSA_PKCS1_V1_5_SHA_256.name();
                }
            } else if (keySpec.name().startsWith("ECC") ||
                    keySpec == IEnumKeySpec.Types.ECC_NIST_P256 ||
                    keySpec == IEnumKeySpec.Types.ECC_NIST_P384 ||
                    keySpec == IEnumKeySpec.Types.ECC_NIST_P521 ||
                    keySpec == IEnumKeySpec.Types.ECC_SECG_P256K1) {
                int size = keySpec.getKeySizeBits();
                switch (size) {
                    case 256: return IEnumSignatureAlgorithm.ECDSA_SHA_256.name();
                    case 384: return IEnumSignatureAlgorithm.ECDSA_SHA_384.name();
                    case 521: return IEnumSignatureAlgorithm.ECDSA_SHA_512.name();
                    default:  return IEnumSignatureAlgorithm.ECDSA_SHA_256.name();
                }
            } else if (keySpec == IEnumKeySpec.Types.SM2) {
                return IEnumSignatureAlgorithm.SM2DSA.name();
            }
        }
        // ----- MAC -----
        else if (usage == IEnumKeyUsage.Types.GENERATE_VERIFY_MAC) {
            if (keySpec.name().startsWith("HMAC")) {
                int size = keySpec.getKeySizeBits();
                switch (size) {
                    case 224: return IEnumMacAlgorithm.HMAC_SHA_224.name();
                    case 256: return IEnumMacAlgorithm.HMAC_SHA_256.name();
                    case 384: return IEnumMacAlgorithm.HMAC_SHA_384.name();
                    case 512: return IEnumMacAlgorithm.HMAC_SHA_512.name();
                    default:  return IEnumMacAlgorithm.HMAC_SHA_256.name();
                }
            }
        }
        return null;
    }
}