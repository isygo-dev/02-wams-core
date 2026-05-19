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
    public KeyPairMaterial generateKeyMaterial(IEnumKeySpec.Types keySpec) {
        try {
            switch (keySpec) {
                // ========== Symmetric and HMAC keys ==========
                case SYMMETRIC_DEFAULT:
                    KeyGenerator aesGen = KeyGenerator.getInstance("AES");
                    aesGen.init(keySpec.getKeySizeBits()); // 256 bits
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

                // ========== RSA keys ==========
                case RSA_2048, RSA_3072, RSA_4096:
                    KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
                    rsaGen.initialize(keySpec.getKeySizeBits());
                    KeyPair rsaPair = rsaGen.generateKeyPair();
                    return new KeyPairMaterial(rsaPair.getPrivate().getEncoded(), rsaPair.getPublic().getEncoded());

                // ========== Elliptic Curve keys ==========
                case ECC_NIST_P256, ECC_NIST_P384, ECC_NIST_P521, ECC_SECG_P256K1:
                    KeyPairGenerator ecGen = KeyPairGenerator.getInstance("EC");
                    ecGen.initialize(new ECGenParameterSpec(keySpec.getCurveName()));
                    KeyPair ecPair = ecGen.generateKeyPair();
                    return new KeyPairMaterial(ecPair.getPrivate().getEncoded(), ecPair.getPublic().getEncoded());

                // ========== SM2 (requires Bouncy Castle) ==========
                case SM2:
                    throw new UnsupportedOperationException("SM2 key generation not yet implemented");

                default:
                    throw new IllegalArgumentException("Unsupported key spec: " + keySpec);
            }
        } catch (Exception e) {
            throw new RuntimeException("Key material generation failed for " + keySpec, e);
        }
    }

    private byte[] generateRsaPrivateKey(int keySize) throws Exception {
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        rsaGen.initialize(keySize);
        KeyPair keyPair = rsaGen.generateKeyPair();
        return keyPair.getPrivate().getEncoded(); // PKCS#8 encoding
    }

    private byte[] generateEcPrivateKey(String curveName) throws Exception {
        KeyPairGenerator ecGen = KeyPairGenerator.getInstance("EC");
        java.security.spec.ECGenParameterSpec ecSpec = new java.security.spec.ECGenParameterSpec(curveName);
        ecGen.initialize(ecSpec);
        KeyPair keyPair = ecGen.generateKeyPair();
        return keyPair.getPrivate().getEncoded(); // PKCS#8 encoding
    }

    /**
     * Encrypts byte array data using JCA (AES for symmetric).
     */
    @Override
    public byte[] encryptData(byte[] plaintext, byte[] keyMaterial, Map<String, String> encryptionContext) {
        if (plaintext == null || plaintext.length == 0) return new byte[0];

        try {
            // Simple AES/GCM implementation for KMS-style encryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            SecretKeySpec keySpec = new SecretKeySpec(keyMaterial, "AES");

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
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
    public byte[] signData(byte[] message, byte[] keyMaterial, IEnumSignatureAlgorithm algorithm) {
        try {
            if (algorithm == null) {
                throw new IllegalArgumentException("Signature algorithm is required");
            }
            // 1. Get Signature instance
            Signature signature = Signature.getInstance(algorithm.getJavaAlgorithm());

            // 2. Load private key (use the key algorithm from the enum)
            PrivateKey privateKey = KeyFactory.getInstance(algorithm.getKeyAlgorithm())
                    .generatePrivate(new PKCS8EncodedKeySpec(keyMaterial));

            // 3. For RSA-PSS, set the PSS parameters (must be done before initSign)
            if (algorithm.getPssSpec() != null) {
                signature.setParameter(algorithm.getPssSpec());
            }

            // 4. Initialize and sign
            signature.initSign(privateKey);
            signature.update(message);
            return signature.sign();
        } catch (Exception e) {
            log.error("Signing failed for algorithm: {}", algorithm, e);
            throw new RuntimeException("Signing failed", e);
        }
    }

    /**
     * Verifies digital signature.
     */
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
    public KeyPairMaterial generateWrappingKey() {
        return generateKeyMaterial(IEnumKeySpec.Types.RSA_2048); // Reuse existing logic
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