package eu.isygoit.service.impl;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.model.DigestConfig;
import eu.isygoit.model.PEBConfig;
import eu.isygoit.repository.DigesterConfigRepository;
import eu.isygoit.repository.PEBConfigRepository;
import eu.isygoit.service.ICryptoService;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.digest.PooledStringDigester;
import org.jasypt.digest.StringDigester;
import org.jasypt.digest.config.SimpleStringDigesterConfig;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jasypt.util.password.ConfigurablePasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * The type Crypto service.
 */
@Slf4j
@Service
@Transactional
public class CryptoService implements ICryptoService {

    @Autowired
    private PEBConfigRepository pebConfigRepository;

    @Autowired
    private DigesterConfigRepository digesterConfigRepository;

    private StringEncryptor pebStringEncryptor(PEBConfig pebConfig) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(pebConfig.getPassword());
        config.setAlgorithm(pebConfig.getAlgorithm().name());
        config.setKeyObtentionIterations(pebConfig.getKeyObtentionIterations());
        config.setPoolSize(pebConfig.getPoolSize());
        config.setProviderName(pebConfig.getProviderName());
        config.setProviderClassName(pebConfig.getProviderClassName());
        config.setSaltGeneratorClassName("org.jasypt.iv." + pebConfig.getSaltGenerator().name());
        config.setStringOutputType(pebConfig.getStringOutputType().name());
        encryptor.setConfig(config);
        return encryptor;
    }

    private StringDigester digestStringEncryptor(DigestConfig digestConfig) {
        PooledStringDigester encryptor = new PooledStringDigester();
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

    @Override
    public StringEncryptor getPebEncryptor(String tenant /*senderTenant*/) {
        Optional<PEBConfig> optional = pebConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (!optional.isPresent()) {
            optional = pebConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }

        if (optional.isPresent()) {
            return this.pebStringEncryptor(optional.get());
        }

        log.warn("peb config not found with tenant {}", tenant);
        return stringEncryptorDefault();
    }

    @Override
    public StringDigester getDigestEncryptor(String tenant /*senderTenant*/) {
        Optional<DigestConfig> optional = digesterConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (!optional.isPresent()) {
            optional = digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }

        if (optional.isPresent()) {
            return this.digestStringEncryptor(optional.get());
        }

        log.warn("digest config not found with tenant {}", tenant);
        return stringDigesterDefault();
    }

    @Override
    public PasswordEncryptor getPasswordEncryptor(String tenant /*senderTenant*/) {
        Optional<DigestConfig> optional = digesterConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (!optional.isPresent()) {
            optional = digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }

        if (optional.isPresent()) {
            return this.passwordEncryptor(optional.get());
        }

        log.warn("password config not found with tenant {}", tenant);
        return passwordEncryptorDefault();
    }

    @Override
    public byte[] generateKeyMaterial(IEnumKeySpec.Types keySpec) {
        return new byte[0];
    }

    @Override
    public byte[] encryptData(byte[] plaintext, byte[] keyMaterial, Map<String, String> encryptionContext) {
        return new byte[0];
    }

    @Override
    public byte[] decryptData(byte[] ciphertext, byte[] keyMaterial, Map<String, String> encryptionContext) {
        return new byte[0];
    }

    @Override
    public byte[] signData(byte[] message, byte[] keyMaterial, String algorithm) {
        return new byte[0];
    }

    @Override
    public boolean verifySignature(byte[] message, byte[] signature, byte[] keyMaterial, String algorithm) {
        return false;
    }

    @Override
    public Map<String, byte[]> generateDataKey(byte[] keyMaterial, Integer keySize) {
        return Map.of();
    }

    @Override
    public byte[] extractPublicKey(byte[] keyMaterial, IEnumKeySpec.Types keySpec) {
        return new byte[0];
    }

    @Override
    public byte[] generateWrappingKey() {
        return new byte[0];
    }

    @Override
    public byte[] generateImportToken() {
        return new byte[0];
    }

    @Override
    public byte[] decryptKeyMaterial(@NotNull(message = "Encrypted key material is required") byte[] encryptedKeyMaterial, @NotNull(message = "Import token is required") byte[] importToken) {
        return new byte[0];
    }

    @Override
    public long getEncryptCount(Long keyId) {
        return 0;
    }

    @Override
    public long getDecryptCount(Long keyId) {
        return 0;
    }

    @Override
    public LocalDateTime getLastUsedDate(Long keyId) {
        return null;
    }

    @Override
    public boolean validateKeyIntegrity(byte[] keyMaterial, IEnumKeySpec.Types keySpec) {
        return false;
    }

    private StringEncryptor stringEncryptorDefault() {
        Optional<PEBConfig> optional = pebConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        if (optional.isPresent()) {
            return this.pebStringEncryptor(optional.get());
        }

        log.warn("No default peb config found");
        return new PooledPBEStringEncryptor();
    }

    private StringDigester stringDigesterDefault() {
        Optional<DigestConfig> optional = digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        if (optional.isPresent()) {
            return this.digestStringEncryptor(optional.get());
        }

        log.warn("No default digest config found");
        return new PooledStringDigester();
    }

    private PasswordEncryptor passwordEncryptorDefault() {
        Optional<DigestConfig> optional = digesterConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        if (optional.isPresent()) {
            return this.passwordEncryptor(optional.get());
        }

        log.warn("No default digest config found");
        return new StrongPasswordEncryptor();
    }
}
