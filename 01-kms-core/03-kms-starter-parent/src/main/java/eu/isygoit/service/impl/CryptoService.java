package eu.isygoit.service.impl;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.dto.data.KeyPairMaterial;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumSignatureAlgorithm;
import eu.isygoit.enums.IKmsActionType;
import eu.isygoit.model.DigestConfig;
import eu.isygoit.model.KmsAuditLog;
import eu.isygoit.model.PEBConfig;
import eu.isygoit.repository.DigesterConfigRepository;
import eu.isygoit.repository.KmsAuditLogRepository;
import eu.isygoit.repository.PEBConfigRepository;
import eu.isygoit.service.ICryptoService;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.digest.PooledStringDigester;
import org.jasypt.digest.StringDigester;
import org.jasypt.digest.config.SimpleStringDigesterConfig;
import org.jasypt.encryption.ByteEncryptor;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEByteEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jasypt.util.password.ConfigurablePasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * The type Crypto service.
 * <p>
 * Provides cryptographic operations using Jasypt for consistent configuration-driven
 * encryption/digestion and standard JCA for key generation and asymmetric operations.
 * </p>
 * <p>
 * <strong>Fix applied:</strong> Now supports hybrid encryption for RSA keys.
 * {@code encryptData} and {@code decryptData} accept a {@code keySpec} parameter
 * to determine the correct algorithm. Asymmetric keys (RSA) trigger hybrid encryption:
 * AES‑256/GCM for the data, RSA‑OAEP for the AES key.
 * </p>
 */
@Slf4j
@Service
@Transactional
public class CryptoService implements ICryptoService {

    @Autowired
    private PEBConfigRepository pebConfigRepository;

    @Autowired
    private DigesterConfigRepository digesterConfigRepository;

    @Autowired
    private KmsAuditLogRepository kmsAuditLogRepository;

    // ====================== Existing Jasypt Helpers ======================

    private StringEncryptor pebStringEncryptor(PEBConfig pebConfig) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = createPBEConfig(pebConfig);
        encryptor.setConfig(config);
        return encryptor;
    }

    private ByteEncryptor pebByteEncryptor(PEBConfig pebConfig) {
        PooledPBEByteEncryptor encryptor = new PooledPBEByteEncryptor();
        SimpleStringPBEConfig config = createPBEConfig(pebConfig);
        encryptor.setConfig(config);
        return encryptor;
    }

    private SimpleStringPBEConfig createPBEConfig(PEBConfig pebConfig) {
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(pebConfig.getPassword());
        config.setAlgorithm(pebConfig.getAlgorithm().name());
        config.setKeyObtentionIterations(pebConfig.getKeyObtentionIterations());
        config.setPoolSize(pebConfig.getPoolSize());
        config.setProviderName(pebConfig.getProviderName());
        config.setProviderClassName(pebConfig.getProviderClassName());
        config.setSaltGeneratorClassName("org.jasypt.iv." + pebConfig.getSaltGenerator().name());
        config.setStringOutputType(pebConfig.getStringOutputType().name());
        return config;
    }

    private StringDigester digestStringEncryptor(DigestConfig digestConfig) {
        PooledStringDigester digester = new PooledStringDigester();
        SimpleStringDigesterConfig config = new SimpleStringDigesterConfig();
        config.setAlgorithm(digestConfig.getAlgorithm().name().replace("_", "-"));
        config.setIterations(digestConfig.getIterations());
        config.setSaltSizeBytes(digestConfig.getSaltSizeBytes());
        config.setSaltGeneratorClassName("org.jasypt.iv." + digestConfig.getSaltGenerator().name());
        config.setProviderName(digestConfig.getProviderName());
        config.setProviderClassName(digestConfig.getProviderClassName());
        config.setInvertPositionOfSaltInMessageBeforeDigesting(digestConfig.getInvertPositionOfSaltInMessageBeforeDigesting());
        config.setInvertPositionOfPlainSaltInEncryptionResults(digestConfig.getInvertPositionOfPlainSaltInEncryptionResults());
        config.setUseLenientSaltSizeCheck(digestConfig.getUseLenientSaltSizeCheck());
        config.setPoolSize(digestConfig.getPoolSize());
        config.setStringOutputType(digestConfig.getStringOutputType().name());
        digester.setConfig(config);
        return digester;
    }

    private PasswordEncryptor passwordEncryptor(DigestConfig digestConfig) {
        ConfigurablePasswordEncryptor encryptor = new ConfigurablePasswordEncryptor();
        SimpleStringDigesterConfig config = new SimpleStringDigesterConfig();
        config.setAlgorithm(digestConfig.getAlgorithm().name().replace("_", "-"));
        config.setIterations(digestConfig.getIterations());
        config.setSaltSizeBytes(digestConfig.getSaltSizeBytes());
        config.setSaltGeneratorClassName("org.jasypt.iv." + digestConfig.getSaltGenerator().name());
        config.setProviderName(digestConfig.getProviderName());
        config.setProviderClassName(digestConfig.getProviderClassName());
        config.setInvertPositionOfSaltInMessageBeforeDigesting(digestConfig.getInvertPositionOfSaltInMessageBeforeDigesting());
        config.setInvertPositionOfPlainSaltInEncryptionResults(digestConfig.getInvertPositionOfPlainSaltInEncryptionResults());
        config.setUseLenientSaltSizeCheck(digestConfig.getUseLenientSaltSizeCheck());
        config.setPoolSize(digestConfig.getPoolSize());
        config.setStringOutputType(digestConfig.getStringOutputType().name());
        encryptor.setConfig(config);
        return encryptor;
    }

    // ====================== Public API Methods ======================

    @Override
    public StringEncryptor getPebEncryptor(String tenant) {
        Optional<PEBConfig> optional = pebConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (optional.isEmpty()) {
            optional = pebConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }
        return optional.map(this::pebStringEncryptor)
                .orElseGet(this::stringEncryptorDefault);
    }

    @Override
    public StringDigester getDigestEncryptor(String tenant) {
        Optional<DigestConfig> optional = digesterConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (optional.isEmpty()) {
            optional = digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }
        return optional.map(this::digestStringEncryptor)
                .orElseGet(this::stringDigesterDefault);
    }

    @Override
    public PasswordEncryptor getPasswordEncryptor(String tenant) {
        Optional<DigestConfig> optional = digesterConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (optional.isEmpty()) {
            optional = digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }
        return optional.map(this::passwordEncryptor)
                .orElseGet(this::passwordEncryptorDefault);
    }

    @Override
    public KeyPairMaterial generateKeyMaterial(IEnumKeySpec.Types keySpec) {
        try {
            switch (keySpec) {
                case SYMMETRIC_DEFAULT:
                    KeyGenerator aesGen = KeyGenerator.getInstance("AES");
                    aesGen.init(keySpec.getKeySizeBits());
                    byte[] aesKey = aesGen.generateKey().getEncoded();
                    return new KeyPairMaterial(aesKey, null);

                case HMAC_224:
                    KeyGenerator hmac224 = KeyGenerator.getInstance("HmacSHA224");
                    hmac224.init(224);
                    return new KeyPairMaterial(hmac224.generateKey().getEncoded(), null);

                case HMAC_256:
                    KeyGenerator hmac256 = KeyGenerator.getInstance("HmacSHA256");
                    hmac256.init(256);
                    return new KeyPairMaterial(hmac256.generateKey().getEncoded(), null);

                case HMAC_384:
                    KeyGenerator hmac384 = KeyGenerator.getInstance("HmacSHA384");
                    hmac384.init(384);
                    return new KeyPairMaterial(hmac384.generateKey().getEncoded(), null);

                case HMAC_512:
                    KeyGenerator hmac512 = KeyGenerator.getInstance("HmacSHA512");
                    hmac512.init(512);
                    return new KeyPairMaterial(hmac512.generateKey().getEncoded(), null);

                case RSA_2048:
                case RSA_3072:
                case RSA_4096:
                    KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
                    rsaGen.initialize(keySpec.getKeySizeBits());
                    KeyPair rsaPair = rsaGen.generateKeyPair();
                    return new KeyPairMaterial(rsaPair.getPrivate().getEncoded(), rsaPair.getPublic().getEncoded());

                case ECC_NIST_P256:
                case ECC_NIST_P384:
                case ECC_NIST_P521:
                case ECC_SECG_P256K1:
                    KeyPairGenerator ecGen = KeyPairGenerator.getInstance("EC");
                    ecGen.initialize(new ECGenParameterSpec(keySpec.getCurveName()));
                    KeyPair ecPair = ecGen.generateKeyPair();
                    return new KeyPairMaterial(ecPair.getPrivate().getEncoded(), ecPair.getPublic().getEncoded());

                case SM2:
                    throw new UnsupportedOperationException("SM2 key generation not yet implemented");

                default:
                    throw new IllegalArgumentException("Unsupported key spec: " + keySpec);
            }
        } catch (Exception e) {
            throw new RuntimeException("Key material generation failed for " + keySpec, e);
        }
    }

    // -----------------------------------------------------------------
    //  ENCRYPTION / DECRYPTION (with key type awareness)
    // -----------------------------------------------------------------

    @Override
    public byte[] encryptData(byte[] plaintext, byte[] keyMaterial,
                              IEnumKeySpec.Types keySpec,
                              Map<String, String> encryptionContext) {
        if (plaintext == null || plaintext.length == 0) return new byte[0];

        try {
            if (keySpec.isAsymmetric()) {
                // RSA hybrid encryption
                return hybridEncrypt(plaintext, keyMaterial, keySpec);
            } else {
                // Symmetric encryption (AES/GCM)
                return symmetricEncrypt(plaintext, keyMaterial);
            }
        } catch (Exception e) {
            log.error("Data encryption failed", e);
            return new byte[0];
        }
    }

    @Override
    public byte[] decryptData(String tenant, byte[] ciphertext, byte[] keyMaterial,
                              IEnumKeySpec.Types keySpec,
                              Map<String, String> encryptionContext) {
        if (ciphertext == null || ciphertext.length == 0) return new byte[0];

        try {
            if (keySpec.isAsymmetric()) {
                return hybridDecrypt(ciphertext, keyMaterial, keySpec);
            } else {
                return symmetricDecrypt(ciphertext, keyMaterial);
            }
        } catch (Exception e) {
            log.error("Data decryption failed", e);
            return new byte[0];
        }
    }

    // -----------------------------------------------------------------
    //  HYBRID ENCRYPTION (RSA + AES)
    // -----------------------------------------------------------------

    /**
     * Hybrid encryption: generate ephemeral AES‑256 key, encrypt data with AES/GCM,
     * encrypt the AES key with RSA public key (OAEP with SHA‑256), then pack the result.
     * Output format: [ encryptedAesKeyLength (2 bytes) ][ encryptedAesKey ][ iv (12 bytes) ][ ciphertext + auth tag ]
     */
    private byte[] hybridEncrypt(byte[] plaintext, byte[] publicKeyMaterial, IEnumKeySpec.Types keySpec) throws Exception {
        // 1. Generate ephemeral AES‑256 key
        SecretKey aesKey = generateAESKey(256);

        // 2. Encrypt data with AES/GCM
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        byte[] encryptedData = aesCipher.doFinal(plaintext);  // includes auth tag

        // 3. Encrypt the AES key with RSA public key (OAEP)
        PublicKey rsaPublicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(publicKeyMaterial));
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

        // 4. Pack: [ encryptedAesKeyLength (2 bytes) ][ encryptedAesKey ][ iv ][ encryptedData ]
        byte[] result = new byte[2 + encryptedAesKey.length + iv.length + encryptedData.length];
        int pos = 0;
        result[pos++] = (byte) ((encryptedAesKey.length >> 8) & 0xFF);
        result[pos++] = (byte) (encryptedAesKey.length & 0xFF);
        System.arraycopy(encryptedAesKey, 0, result, pos, encryptedAesKey.length);
        pos += encryptedAesKey.length;
        System.arraycopy(iv, 0, result, pos, iv.length);
        pos += iv.length;
        System.arraycopy(encryptedData, 0, result, pos, encryptedData.length);
        return result;
    }

    /**
     * Hybrid decryption: extract the encrypted AES key, decrypt it with RSA private key,
     * then decrypt the data with AES/GCM.
     */
    private byte[] hybridDecrypt(byte[] ciphertext, byte[] privateKeyMaterial, IEnumKeySpec.Types keySpec) throws Exception {
        if (ciphertext.length < 2) throw new IllegalArgumentException("Invalid hybrid ciphertext");

        // 1. Read encrypted AES key length
        int encryptedKeyLen = ((ciphertext[0] & 0xFF) << 8) | (ciphertext[1] & 0xFF);
        if (ciphertext.length < 2 + encryptedKeyLen + 12) throw new IllegalArgumentException("Malformed hybrid ciphertext");

        int pos = 2;
        byte[] encryptedAesKey = new byte[encryptedKeyLen];
        System.arraycopy(ciphertext, pos, encryptedAesKey, 0, encryptedKeyLen);
        pos += encryptedKeyLen;

        // 2. Read IV
        byte[] iv = new byte[12];
        System.arraycopy(ciphertext, pos, iv, 0, 12);
        pos += 12;

        // 3. Rest is encrypted data
        byte[] encryptedData = new byte[ciphertext.length - pos];
        System.arraycopy(ciphertext, pos, encryptedData, 0, encryptedData.length);

        // 4. Decrypt AES key with RSA private key
        PrivateKey rsaPrivateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKeyMaterial));
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
        byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);

        // 5. Decrypt data with AES/GCM
        SecretKeySpec aesKeySpec = new SecretKeySpec(aesKeyBytes, "AES");
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        aesCipher.init(Cipher.DECRYPT_MODE, aesKeySpec, gcmSpec);
        return aesCipher.doFinal(encryptedData);
    }

    // -----------------------------------------------------------------
    //  SYMMETRIC ENCRYPTION (AES/GCM)
    // -----------------------------------------------------------------

    private byte[] symmetricEncrypt(byte[] plaintext, byte[] symmetricKey) throws Exception {
        if (symmetricKey.length != 16 && symmetricKey.length != 24 && symmetricKey.length != 32) {
            throw new InvalidKeyException("Symmetric key must be 16, 24, or 32 bytes. Got: " + symmetricKey.length);
        }
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        SecretKeySpec keySpec = new SecretKeySpec(symmetricKey, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
        byte[] encrypted = cipher.doFinal(plaintext);
        // Pack IV + ciphertext
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return result;
    }

    private byte[] symmetricDecrypt(byte[] ciphertext, byte[] symmetricKey) throws Exception {
        if (ciphertext.length < 12) throw new IllegalArgumentException("Ciphertext too short");
        if (symmetricKey.length != 16 && symmetricKey.length != 24 && symmetricKey.length != 32) {
            throw new InvalidKeyException("Symmetric key must be 16, 24, or 32 bytes. Got: " + symmetricKey.length);
        }
        byte[] iv = new byte[12];
        System.arraycopy(ciphertext, 0, iv, 0, 12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        SecretKeySpec keySpec = new SecretKeySpec(symmetricKey, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
        return cipher.doFinal(ciphertext, 12, ciphertext.length - 12);
    }

    private SecretKey generateAESKey(int keySizeBits) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(keySizeBits);
        return kg.generateKey();
    }

    // -----------------------------------------------------------------
    //  SIGNATURE METHODS (unchanged)
    // -----------------------------------------------------------------

    @Override
    public byte[] signData(byte[] message, byte[] keyMaterial, IEnumSignatureAlgorithm algorithm) {
        try {
            if (algorithm == null) {
                throw new IllegalArgumentException("Signature algorithm is required");
            }
            Signature signature = Signature.getInstance(algorithm.getJavaAlgorithm());

            PrivateKey privateKey = KeyFactory.getInstance(algorithm.getKeyAlgorithm())
                    .generatePrivate(new PKCS8EncodedKeySpec(keyMaterial));

            if (algorithm.getPssSpec() != null) {
                signature.setParameter(algorithm.getPssSpec());
            }

            signature.initSign(privateKey);
            signature.update(message);
            return signature.sign();
        } catch (Exception e) {
            log.error("Signing failed for algorithm: {}", algorithm, e);
            throw new RuntimeException("Signing failed", e);
        }
    }

    @Override
    public boolean verifySignature(byte[] message, byte[] signatureBytes, byte[] publicKeyBytes, IEnumSignatureAlgorithm algorithm) {
        try {
            if (algorithm == null) throw new IllegalArgumentException("Algorithm required");
            Signature sig = Signature.getInstance(algorithm.getJavaAlgorithm());
            PublicKey publicKey = KeyFactory.getInstance(algorithm.getKeyAlgorithm())
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            if (algorithm.getPssSpec() != null) sig.setParameter(algorithm.getPssSpec());
            sig.initVerify(publicKey);
            sig.update(message);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    // -----------------------------------------------------------------
    //  DATA KEY GENERATION (updated to pass keySpec)
    // -----------------------------------------------------------------

    @Override
    public Map<String, byte[]> generateDataKey(byte[] keyMaterial, Integer keySize) {
        try {
            int size = (keySize != null) ? keySize : 256;
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(size);
            byte[] plaintextKey = keyGen.generateKey().getEncoded();
            // For data key wrapping, we assume the keyMaterial is from a symmetric KEK.
            // If the KEK is RSA, this method would need the keySpec as well.
            // Here we keep the original behaviour: use symmetric encryption with the given keyMaterial.
            // To support RSA KEK, you would need to modify the method signature similarly.
            byte[] encryptedKey = symmetricEncrypt(plaintextKey, keyMaterial);
            return Map.of(
                    "plaintextKey", plaintextKey,
                    "encryptedKey", encryptedKey
            );
        } catch (Exception e) {
            log.error("Generate data key failed", e);
            return Map.of();
        }
    }

    // -----------------------------------------------------------------
    //  OTHER METHODS (unchanged or adapted)
    // -----------------------------------------------------------------

    @Override
    public byte[] extractPublicKey(byte[] keyMaterial, IEnumKeySpec.Types keySpec) {
        log.warn("extractPublicKey - full implementation pending (requires key pair storage)");
        return new byte[0];
    }

    @Override
    public KeyPairMaterial generateWrappingKey() {
        return generateKeyMaterial(IEnumKeySpec.Types.RSA_2048);
    }

    @Override
    public byte[] generateImportToken() {
        byte[] token = new byte[32];
        new SecureRandom().nextBytes(token);
        return token;
    }

    @Override
    public byte[] decryptKeyMaterial(String tenant, @NotNull byte[] encryptedKeyMaterial, @NotNull byte[] importToken) {
        try {
            PooledPBEByteEncryptor encryptor = new PooledPBEByteEncryptor();
            PEBConfig pebConfig = pebConfigRepository.findFirstByTenantIgnoreCase(tenant)
                    .orElseGet(this::getDefaultPebConfig);

            if (pebConfig != null) {
                encryptor.setConfig(createPBEConfig(pebConfig));
            } else {
                SimpleStringPBEConfig config = new SimpleStringPBEConfig();
                config.setPassword(new String(importToken));
                config.setAlgorithm("PBEWithHmacSHA256AndAES_256");
                config.setKeyObtentionIterations(1000);
                encryptor.setConfig(config);
            }
            return encryptor.decrypt(encryptedKeyMaterial);
        } catch (Exception e) {
            log.error("Failed to decrypt key material", e);
            return new byte[0];
        }
    }

    @Override
    public long getEncryptCount(String tenant, String keyId) {
        return kmsAuditLogRepository.countByTenantAndActionAndKeyId(tenant, IKmsActionType.Types.ENCRYPT, keyId);
    }

    @Override
    public long getDecryptCount(String tenant, String keyId) {
        return kmsAuditLogRepository.countByTenantAndActionAndKeyId(tenant, IKmsActionType.Types.DECRYPT, keyId);
    }

    @Override
    public LocalDateTime getLastUsedDate(String tenant, String keyId) {
        return kmsAuditLogRepository.findFirstByTenantAndKeyIdOrderByTimestampDesc(tenant, keyId)
                .map(KmsAuditLog::getTimestamp)
                .orElse(null);
    }

    @Override
    public boolean validateKeyIntegrity(byte[] keyMaterial, IEnumKeySpec.Types keySpec) {
        return keyMaterial != null && keyMaterial.length > 0;
    }

    // ====================== Default Fallbacks ======================

    private StringEncryptor stringEncryptorDefault() {
        return pebConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME)
                .map(this::pebStringEncryptor)
                .orElseGet(PooledPBEStringEncryptor::new);
    }

    private StringDigester stringDigesterDefault() {
        return digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME)
                .map(this::digestStringEncryptor)
                .orElseGet(PooledStringDigester::new);
    }

    private PasswordEncryptor passwordEncryptorDefault() {
        return digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME)
                .map(this::passwordEncryptor)
                .orElseGet(StrongPasswordEncryptor::new);
    }

    private PEBConfig getDefaultPebConfig() {
        return pebConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME)
                .orElse(null);
    }
}