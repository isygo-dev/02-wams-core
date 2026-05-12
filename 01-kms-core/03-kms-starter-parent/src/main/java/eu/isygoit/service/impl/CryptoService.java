package eu.isygoit.service.impl;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumKeySpec;
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * The type Crypto service.
 * <p>
 * Provides cryptographic operations using Jasypt for consistent configuration-driven
 * encryption/digestion and standard JCA for key generation and asymmetric operations.
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

    /**
     * Retrieves a configured PBE StringEncryptor for the given tenant.
     * Falls back to default tenant if tenant-specific configuration is not found.
     */
    @Override
    public StringEncryptor getPebEncryptor(String tenant) {
        Optional<PEBConfig> optional = pebConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (optional.isEmpty()) {
            optional = pebConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }
        return optional.map(this::pebStringEncryptor)
                .orElseGet(this::stringEncryptorDefault);
    }

    /**
     * Retrieves a configured StringDigester for the given tenant.
     */
    @Override
    public StringDigester getDigestEncryptor(String tenant) {
        Optional<DigestConfig> optional = digesterConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (optional.isEmpty()) {
            optional = digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }
        return optional.map(this::digestStringEncryptor)
                .orElseGet(this::stringDigesterDefault);
    }

    /**
     * Retrieves a configured PasswordEncryptor for the given tenant.
     */
    @Override
    public PasswordEncryptor getPasswordEncryptor(String tenant) {
        Optional<DigestConfig> optional = digesterConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (optional.isEmpty()) {
            optional = digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }
        return optional.map(this::passwordEncryptor)
                .orElseGet(this::passwordEncryptorDefault);
    }

    /**
     * Generates cryptographic key material (symmetric secret key or asymmetric private key).
     *
     * @param keySpec the type of key to generate (e.g. AES_256, RSA_2048)
     * @return raw key bytes
     */
    @Override
    public byte[] generateKeyMaterial(IEnumKeySpec.Types keySpec) {
        try {
            if (keySpec.name().startsWith("AES") || keySpec.name().startsWith("DES")) {
                int keySize = extractKeySize(keySpec);
                javax.crypto.KeyGenerator keyGen = javax.crypto.KeyGenerator.getInstance("AES");
                keyGen.init(keySize);
                return keyGen.generateKey().getEncoded();
            } else if (keySpec.name().startsWith("RSA") || keySpec.name().startsWith("EC")) {
                int keySize = extractKeySize(keySpec);
                KeyPairGenerator generator = KeyPairGenerator.getInstance(extractAlgorithm(keySpec));
                generator.initialize(keySize);
                KeyPair keyPair = generator.generateKeyPair();
                return keyPair.getPrivate().getEncoded(); // Return private key (standard practice)
            }
        } catch (Exception e) {
            log.error("Failed to generate key material for {}", keySpec, e);
        }
        return new byte[0];
    }

    /**
     * Encrypts byte array data using JCA (AES for symmetric).
     */
    @Override
    public byte[] encryptData(byte[] plaintext, byte[] keyMaterial, Map<String, String> encryptionContext) {
        if (plaintext == null || plaintext.length == 0) return new byte[0];

        try {
            // Simple AES/GCM implementation for KMS-style encryption
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(128, iv);
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyMaterial, "AES");

            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, spec);
            byte[] encrypted = cipher.doFinal(plaintext);

            // Return IV + Ciphertext
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return result;
        } catch (Exception e) {
            log.error("Data encryption failed", e);
            return new byte[0];
        }
    }

    /**
     * Decrypts byte array data using JCA (AES for symmetric).
     */
    @Override
    public byte[] decryptData(String tenant, byte[] ciphertext, byte[] keyMaterial, Map<String, String> encryptionContext) {
        if (ciphertext == null || ciphertext.length == 0) return new byte[0];

        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            System.arraycopy(ciphertext, 0, iv, 0, iv.length);
            javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(128, iv);
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyMaterial, "AES");

            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, spec);
            return cipher.doFinal(ciphertext, iv.length, ciphertext.length - iv.length);
        } catch (Exception e) {
            log.error("Data decryption failed", e);
            return new byte[0];
        }
    }

    /**
     * Signs data using standard Java Signature API.
     */
    @Override
    public byte[] signData(byte[] message, byte[] keyMaterial, String algorithm) {
        try {
            String sigAlgo = algorithm != null ? algorithm : "SHA256withRSA";
            java.security.Signature signature = java.security.Signature.getInstance(sigAlgo);
            String keyAlgo = sigAlgo.contains("ECDSA") ? "EC" : "RSA";
            java.security.PrivateKey privateKey = java.security.KeyFactory.getInstance(keyAlgo)
                    .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(keyMaterial));
            signature.initSign(privateKey);
            signature.update(message);
            return signature.sign();
        } catch (Exception e) {
            log.error("Signing failed", e);
            return new byte[0];
        }
    }

    /**
     * Verifies digital signature.
     */
    @Override
    public boolean verifySignature(byte[] message, byte[] signature, byte[] keyMaterial, String algorithm) {
        try {
            String sigAlgo = algorithm != null ? algorithm : "SHA256withRSA";
            java.security.Signature sig = java.security.Signature.getInstance(sigAlgo);
            String keyAlgo = sigAlgo.contains("ECDSA") ? "EC" : "RSA";
            java.security.PublicKey publicKey = java.security.KeyFactory.getInstance(keyAlgo)
                    .generatePublic(new java.security.spec.X509EncodedKeySpec(keyMaterial));
            sig.initVerify(publicKey);
            sig.update(message);
            return sig.verify(signature);
        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    @Override
    public Map<String, byte[]> generateDataKey(byte[] keyMaterial, Integer keySize) {
        try {
            int size = (keySize != null) ? keySize : 256;
            javax.crypto.KeyGenerator keyGen = javax.crypto.KeyGenerator.getInstance("AES");
            keyGen.init(size);
            byte[] plaintextKey = keyGen.generateKey().getEncoded();
            byte[] encryptedKey = encryptData(plaintextKey, keyMaterial, null);

            return Map.of(
                    "plaintextKey", plaintextKey,
                    "encryptedKey", encryptedKey
            );
        } catch (Exception e) {
            log.error("Generate data key failed", e);
            return Map.of();
        }
    }

    @Override
    public byte[] extractPublicKey(byte[] keyMaterial, IEnumKeySpec.Types keySpec) {
        log.warn("extractPublicKey - full implementation pending (requires key pair storage)");
        return new byte[0];
    }

    @Override
    public byte[] generateWrappingKey() {
        return generateKeyMaterial(IEnumKeySpec.Types.AES_256); // Reuse existing logic
    }

    @Override
    public byte[] generateImportToken() {
        byte[] token = new byte[32];
        new SecureRandom().nextBytes(token);
        return token;
    }

    @Override
    public byte[] decryptKeyMaterial(String tenant, @NotNull byte[] encryptedKeyMaterial, @NotNull byte[] importToken) {
        // Simple implementation using Jasypt-style PBE
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
    public long getEncryptCount(String keyId) {
        return kmsAuditLogRepository.countByActionAndKeyId(IKmsActionType.Types.ENCRYPT, keyId);
    }

    @Override
    public long getDecryptCount(String keyId) {
        return kmsAuditLogRepository.countByActionAndKeyId(IKmsActionType.Types.DECRYPT, keyId);
    }

    @Override
    public LocalDateTime getLastUsedDate(String keyId) {
        return kmsAuditLogRepository.findFirstByKeyIdOrderByTimestampDesc(keyId)
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

    // ====================== Helpers ======================

    private int extractKeySize(IEnumKeySpec.Types keySpec) {
        try {
            String name = keySpec.name();
            if (name.contains("_")) {
                return Integer.parseInt(name.substring(name.lastIndexOf('_') + 1));
            }
        } catch (Exception ignored) {
        }
        return 256;
    }

    private String extractAlgorithm(IEnumKeySpec.Types keySpec) {
        String name = keySpec.name();
        if (name.startsWith("RSA")) return "RSA";
        if (name.startsWith("EC")) return "EC";
        return "AES";
    }
}