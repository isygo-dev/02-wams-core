package eu.isygoit.service.impl;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.dto.data.KeyPairMaterial;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumSignatureAlgorithm;
import eu.isygoit.enums.IKmsActionType;
import eu.isygoit.exception.*;
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
import org.springframework.util.StringUtils;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    // ====================== Jasypt Helpers ======================

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

    // ====================== Public API ======================

    @Override
    public StringEncryptor getPebEncryptor(String tenant) {
        Optional<PEBConfig> optional = pebConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (optional.isEmpty()) {
            optional = pebConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }
        return optional.map(this::pebStringEncryptor).orElseGet(this::stringEncryptorDefault);
    }

    @Override
    public StringDigester getDigestEncryptor(String tenant) {
        Optional<DigestConfig> optional = digesterConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (optional.isEmpty()) {
            optional = digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }
        return optional.map(this::digestStringEncryptor).orElseGet(this::stringDigesterDefault);
    }

    @Override
    public PasswordEncryptor getPasswordEncryptor(String tenant) {
        Optional<DigestConfig> optional = digesterConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (optional.isEmpty()) {
            optional = digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }
        return optional.map(this::passwordEncryptor).orElseGet(this::passwordEncryptorDefault);
    }

    @Override
    public KeyPairMaterial generateKeyMaterial(IEnumKeySpec.Types keySpec) {
        try {
            switch (keySpec) {
                case SYMMETRIC_DEFAULT:
                    KeyGenerator aesGen = KeyGenerator.getInstance("AES");
                    aesGen.init(keySpec.getKeySizeBits());
                    return new KeyPairMaterial(aesGen.generateKey().getEncoded(), null);

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
                    throw new KeySpecNotYetSupportedException("SM2 not yet implemented");
                default:
                    throw new KeySpecNotSupportedException("Unsupported key spec: " + keySpec);
            }
        } catch (Exception e) {
            log.error("Key generation failed for spec: {}", keySpec, e);
            throw new GenerateKeyException("Key generation failed: " + keySpec, e);
        }
    }

    // -----------------------------------------------------------------
    //  ENCRYPTION / DECRYPTION
    // -----------------------------------------------------------------

    @Override
    public byte[] encryptData(byte[] plaintext, byte[] keyMaterial,
                              IEnumKeySpec.Types keySpec,
                              String encryptionAlgorithmSpec,
                              Map<String, String> encryptionContext) {
        if (plaintext == null || plaintext.length == 0) return new byte[0];
        try {
            if (keySpec != null && keySpec.isAsymmetric()) {
                return hybridEncrypt(plaintext, keyMaterial, keySpec, encryptionAlgorithmSpec, encryptionContext);
            } else {
                return symmetricEncrypt(plaintext, keyMaterial, encryptionContext);
            }
        } catch (Exception e) {
            log.error("Encryption failed", e);
            return new byte[0];
        }
    }

    @Override
    public byte[] decryptData(String tenant, byte[] ciphertext, byte[] keyMaterial,
                              IEnumKeySpec.Types keySpec,
                              String encryptionAlgorithmSpec,
                              Map<String, String> encryptionContext) {
        if (ciphertext == null || ciphertext.length < 2)
            throw new InvalidCipherTextException("Invalid or short ciphertext");
        try {
            if (keySpec != null && keySpec.isAsymmetric()) {
                return hybridDecrypt(ciphertext, keyMaterial, keySpec, encryptionAlgorithmSpec, encryptionContext);
            } else {
                return symmetricDecrypt(ciphertext, keyMaterial, encryptionContext);
            }
        } catch (AEADBadTagException e) {
            log.error("Decryption failed due to bad tag/context", e);
            throw new CryptBadTagException("Bad tag/context in ciphertext");
        } catch (BadPaddingException e) {
            log.error("Decryption failed due to bad padding", e);
            throw new CryptBadPaddingException("Bad padding, possibly wrong key or corrupted data");
        } catch (GeneralSecurityException e) {
            log.error("Decryption failed due to security error", e);
            throw new CryptSecurityException("Decryption failed: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    //  HYBRID ENCRYPTION (RSA + AES)
    // -----------------------------------------------------------------

    private byte[] hybridEncrypt(byte[] plaintext, byte[] publicKeyMaterial,
                                 IEnumKeySpec.Types keySpec,
                                 String algorithmSpec,
                                 Map<String, String> context) throws Exception {
        // 1. Ephemeral AES-256 key
        SecretKey aesKey = generateAESKey(256);

        // 2. Encrypt data with AES/GCM + AAD
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        if (context != null && !context.isEmpty()) {
            aesCipher.updateAAD(serializeContext(context));
        }
        byte[] encryptedData = aesCipher.doFinal(plaintext);

        // 3. Encrypt AES key with RSA public key (selected padding)
        PublicKey rsaPublicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(publicKeyMaterial));
        String rsaTransform = mapAlgorithmSpecToRsaPadding(algorithmSpec);
        Cipher rsaCipher = Cipher.getInstance(rsaTransform);
        rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

        // 4. Pack result
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

    private byte[] hybridDecrypt(byte[] ciphertext, byte[] privateKeyMaterial,
                                 IEnumKeySpec.Types keySpec,
                                 String algorithmSpec,
                                 Map<String, String> context) {
        if (ciphertext == null || ciphertext.length < 2)
            throw new InvalidCipherTextException("Invalid or short ciphertext");
        try {
            int encKeyLen = ((ciphertext[0] & 0xFF) << 8) | (ciphertext[1] & 0xFF);
            if (ciphertext.length < 2 + encKeyLen + 12) throw new MalformedCipherTextException("Malformed ciphertext");
            int pos = 2;
            byte[] encAesKey = new byte[encKeyLen];
            System.arraycopy(ciphertext, pos, encAesKey, 0, encKeyLen);
            pos += encKeyLen;
            byte[] iv = new byte[12];
            System.arraycopy(ciphertext, pos, iv, 0, 12);
            pos += 12;
            byte[] encryptedData = new byte[ciphertext.length - pos];
            System.arraycopy(ciphertext, pos, encryptedData, 0, encryptedData.length);

            // Decrypt AES key with RSA private key
            PrivateKey rsaPrivateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(privateKeyMaterial));
            String rsaTransform = mapAlgorithmSpecToRsaPadding(algorithmSpec);
            Cipher rsaCipher = Cipher.getInstance(rsaTransform);
            rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encAesKey);

            // Decrypt data with AES/GCM + AAD
            SecretKeySpec aesKeySpec = new SecretKeySpec(aesKeyBytes, "AES");
            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            aesCipher.init(Cipher.DECRYPT_MODE, aesKeySpec, gcmSpec);
            if (context != null && !context.isEmpty()) {
                aesCipher.updateAAD(serializeContext(context));
            }
            return aesCipher.doFinal(encryptedData);
        } catch (AEADBadTagException e) {
            log.error("Decryption failed due to bad tag/context", e);
            throw new CryptBadTagException("Bad tag/context in ciphertext", e);
        } catch (BadPaddingException e) {
            log.error("Decryption failed due to bad padding", e);
            throw new CryptBadPaddingException("Bad padding, possibly wrong key or corrupted data", e);
        } catch (GeneralSecurityException e) {
            log.error("Decryption failed due to security error", e);
            throw new CryptSecurityException("Decryption failed: ", e);
        }
    }

    // -----------------------------------------------------------------
    //  SYMMETRIC ENCRYPTION (AES/GCM) with AAD
    // -----------------------------------------------------------------

    private byte[] symmetricEncrypt(byte[] plaintext, byte[] key, Map<String, String> context) throws Exception {
        if (key.length != 16 && key.length != 24 && key.length != 32)
            throw new InvalidKeyLengthException("AES key length must be 16,24,32 bytes");
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
        if (context != null && !context.isEmpty()) {
            cipher.updateAAD(serializeContext(context));
        }
        byte[] encrypted = cipher.doFinal(plaintext);
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return result;
    }

    private byte[] symmetricDecrypt(byte[] ciphertext, byte[] key, Map<String, String> context) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (ciphertext == null || ciphertext.length < 12)
            throw new InvalidCipherTextException("Invalid or short ciphertext");
        if (key.length != 16 && key.length != 24 && key.length != 32)
            throw new InvalidKeyLengthException("AES key length must be 16,24,32 bytes");
        byte[] iv = new byte[12];
        System.arraycopy(ciphertext, 0, iv, 0, 12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
        if (context != null && !context.isEmpty()) {
            cipher.updateAAD(serializeContext(context));
        }
        return cipher.doFinal(ciphertext, 12, ciphertext.length - 12);
    }

    private SecretKey generateAESKey(int bits) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(bits);
        return kg.generateKey();
    }

    // Serialize context as sorted "key=value&key2=value2" (UTF-8)
    private byte[] serializeContext(Map<String, String> ctx) {
        if (ctx == null || ctx.isEmpty()) return new byte[0];
        String s = ctx.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private String mapAlgorithmSpecToRsaPadding(String spec) {
        if (!StringUtils.hasText(spec)) return "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
        switch (spec) {
            case "RSAES_OAEP_SHA_1":
                return "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";
            case "RSAES_OAEP_SHA_256":
                return "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
            case "RSAES_PKCS1_V1_5":
                return "RSA/ECB/PKCS1Padding";
            default:
                log.warn("Unknown algorithm spec: {}, using default OAEP-SHA-256", spec);
                return "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
        }
    }

    // -----------------------------------------------------------------
    //  SIGNATURE & OTHER METHODS
    // -----------------------------------------------------------------

    @Override
    public byte[] signData(byte[] message, byte[] keyMaterial, IEnumSignatureAlgorithm algorithm) {
        try {
            Signature signature = Signature.getInstance(algorithm.getJavaAlgorithm());
            PrivateKey privateKey = KeyFactory.getInstance(algorithm.getKeyAlgorithm())
                    .generatePrivate(new PKCS8EncodedKeySpec(keyMaterial));
            if (algorithm.getPssSpec() != null) signature.setParameter(algorithm.getPssSpec());
            signature.initSign(privateKey);
            signature.update(message);
            return signature.sign();
        } catch (Exception e) {
            log.error("Signing failed", e);
            throw new SignDataException("Signing failed", e);
        }
    }

    @Override
    public boolean verifySignature(byte[] message, byte[] signatureBytes, byte[] publicKeyBytes, IEnumSignatureAlgorithm algorithm) {
        try {
            Signature sig = Signature.getInstance(algorithm.getJavaAlgorithm());
            PublicKey publicKey = KeyFactory.getInstance(algorithm.getKeyAlgorithm())
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            if (algorithm.getPssSpec() != null) sig.setParameter(algorithm.getPssSpec());
            sig.initVerify(publicKey);
            sig.update(message);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            log.error("Verification failed", e);
            return false;
        }
    }

    @Override
    public Map<String, byte[]> generateDataKey(byte[] keyMaterial, Integer keySize) {
        try {
            int size = (keySize != null) ? keySize : 256;
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(size);
            byte[] plain = kg.generateKey().getEncoded();
            byte[] encrypted = symmetricEncrypt(plain, keyMaterial, null);
            return Map.of("plaintextKey", plain, "encryptedKey", encrypted);
        } catch (Exception e) {
            log.error("Generate data key failed", e);
            return Map.of();
        }
    }

    @Override
    public byte[] extractPublicKey(byte[] keyMaterial, IEnumKeySpec.Types keySpec) {
        log.warn("extractPublicKey not fully implemented");
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
    public byte[] decryptWithPrivateKey(byte[] privateKeyDer, byte[] ciphertext) throws Exception {
        try {
            PrivateKey privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(privateKeyDer));
            // Use OAEP with SHA-256 (matches the wrapping encryption)
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            log.error("Failed to decrypt with private wrapping key", e);
            throw new CryptSecurityException("Private key decryption failed", e);
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
                .map(KmsAuditLog::getTimestamp).orElse(null);
    }

    @Override
    public boolean validateKeyIntegrity(byte[] keyMaterial, IEnumKeySpec.Types keySpec) {
        return keyMaterial != null && keyMaterial.length > 0;
    }

    // ====================== Default Fallbacks ======================

    private StringEncryptor stringEncryptorDefault() {
        return pebConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME)
                .map(this::pebStringEncryptor).orElseGet(PooledPBEStringEncryptor::new);
    }

    private StringDigester stringDigesterDefault() {
        return digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME)
                .map(this::digestStringEncryptor).orElseGet(PooledStringDigester::new);
    }

    private PasswordEncryptor passwordEncryptorDefault() {
        return digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME)
                .map(this::passwordEncryptor).orElseGet(StrongPasswordEncryptor::new);
    }

    private PEBConfig getDefaultPebConfig() {
        return pebConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME).orElse(null);
    }
}