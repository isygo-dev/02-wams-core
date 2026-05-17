package eu.isygoit.service;

import eu.isygoit.enums.IEnumKeySpec;
import jakarta.validation.constraints.NotNull;
import org.jasypt.digest.StringDigester;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.util.password.PasswordEncryptor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The interface Crypto service.
 */
public interface ICryptoService {

    /**
     * Gets peb encryptor.
     *
     * @param tenant the tenant
     * @return the peb encryptor
     */
    StringEncryptor getPebEncryptor(String tenant /*senderTenant*/);

    /**
     * Gets digest encryptor.
     *
     * @param tenant the tenant
     * @return the digest encryptor
     */
    StringDigester getDigestEncryptor(String tenant /*senderTenant*/);

    /**
     * Gets password encryptor.
     *
     * @param tenant the tenant
     * @return the password encryptor
     */
    PasswordEncryptor getPasswordEncryptor(String tenant /*senderTenant*/);

    // ===== WAMS KMS Specific Cryptographic Operations =====

    /**
     * Generate key material for specified key spec
     *
     * @param keySpec (AES_256, RSA_2048, EC_P256, etc.)
     * @return encrypted key material bytes
     */
    byte[] generateKeyMaterial(IEnumKeySpec.Types keySpec);

    /**
     * Encrypt plaintext using specified key material
     *
     * @param plaintext         plaintext bytes to encrypt
     * @param keyMaterial       the key material (must be decrypted first)
     * @param encryptionContext optional encryption context
     * @return ciphertext bytes
     */
    byte[] encryptData(byte[] plaintext, byte[] keyMaterial, Map<String, String> encryptionContext);

    /**
     * Decrypt ciphertext using specified key material
     *
     * @param tenant            the tenant
     * @param ciphertext        ciphertext bytes to decrypt
     * @param keyMaterial       the key material (must be decrypted first)
     * @param encryptionContext optional encryption context
     * @return plaintext bytes
     */
    byte[] decryptData(String tenant, byte[] ciphertext, byte[] keyMaterial, Map<String, String> encryptionContext);

    /**
     * Sign data using asymmetric key
     *
     * @param message     message bytes to sign
     * @param keyMaterial asymmetric key material
     * @param algorithm   signing algorithm (RSASSA_PSS_SHA256, ECDSA_SHA256, etc.)
     * @return signature bytes
     */
    byte[] signData(byte[] message, byte[] keyMaterial, String algorithm);

    /**
     * Verify signature
     *
     * @param message     original message bytes
     * @param signature   signature bytes to verify
     * @param keyMaterial public key material
     * @param algorithm   signing algorithm
     * @return true if signature is valid
     */
    boolean verifySignature(byte[] message, byte[] signature, byte[] keyMaterial, String algorithm);

    /**
     * Generate data key for envelope encryption
     *
     * @param keyMaterial master key material
     * @param keySize     size of data key in bits (typically 256)
     * @return map containing "plaintextKey" and "encryptedKey"
     */
    Map<String, byte[]> generateDataKey(byte[] keyMaterial, Integer keySize);

    byte[] extractPublicKey(byte[] keyMaterial, IEnumKeySpec.Types keySpec);

    byte[] generateWrappingKey();

    byte[] generateImportToken();

    /**
     * Decrypt key material
     *
     * @param tenant               the tenant
     * @param encryptedKeyMaterial encrypted key material bytes
     * @param importToken          import token bytes
     * @return decrypted key material bytes
     */
    byte[] decryptKeyMaterial(String tenant, @NotNull(message = "Encrypted key material is required") byte[] encryptedKeyMaterial, @NotNull(message = "Import token is required") byte[] importToken);

    long getEncryptCount(String tenant, String keyId);

    long getDecryptCount(String tenant, String keyId);

    LocalDateTime getLastUsedDate(String tenant, String keyId);

    boolean validateKeyIntegrity(byte[] keyMaterial, IEnumKeySpec.Types keySpec);
}
