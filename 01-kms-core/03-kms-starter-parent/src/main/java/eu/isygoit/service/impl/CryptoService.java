package eu.isygoit.service.impl;

import eu.isygoit.constants.DomainConstants;
import eu.isygoit.model.DigestConfig;
import eu.isygoit.model.PEBConfig;
import eu.isygoit.repository.DigesterConfigRepository;
import eu.isygoit.repository.PEBConfigRepository;
import eu.isygoit.service.ICryptoService;
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

import java.util.Optional;

/**
 * The type Crypto service.
 */
@Slf4j
@Service
@Transactional
public class CryptoService implements ICryptoService {

    private final PEBConfigRepository pebConfigRepository;
    private final DigesterConfigRepository digesterConfigRepository;

    /**
     * Instantiates a new Crypto service.
     *
     * @param pebConfigRepository      the peb config repository
     * @param digesterConfigRepository the digester config repository
     */
    @Autowired
    public CryptoService(PEBConfigRepository pebConfigRepository, DigesterConfigRepository digesterConfigRepository) {
        this.pebConfigRepository = pebConfigRepository;
        this.digesterConfigRepository = digesterConfigRepository;
    }

    private StringEncryptor createPEBEncryptor(PEBConfig config) {
        var encryptor = new PooledPBEStringEncryptor();
        var stringConfig = new SimpleStringPBEConfig();
        stringConfig.setPassword(config.getPassword());
        stringConfig.setAlgorithm(config.getAlgorithm().name());
        stringConfig.setKeyObtentionIterations(config.getKeyObtentionIterations());
        stringConfig.setPoolSize(config.getPoolSize());
        stringConfig.setProviderName(config.getProviderName());
        stringConfig.setProviderClassName(config.getProviderClassName());
        stringConfig.setSaltGeneratorClassName("org.jasypt.iv." + config.getSaltGenerator().name());
        stringConfig.setStringOutputType(config.getStringOutputType().name());
        encryptor.setConfig(stringConfig);
        return encryptor;
    }

    private StringDigester createStringDigester(DigestConfig config) {
        var digester = new PooledStringDigester();
        var stringConfig = configureDigester(config);
        digester.setConfig(stringConfig);
        return digester;
    }

    private PasswordEncryptor createPasswordEncryptor(DigestConfig config) {
        var encryptor = new ConfigurablePasswordEncryptor();
        var stringConfig = configureDigester(config);
        encryptor.setConfig(stringConfig);
        return encryptor;
    }

    private SimpleStringDigesterConfig configureDigester(DigestConfig config) {
        var stringConfig = new SimpleStringDigesterConfig();
        stringConfig.setAlgorithm(config.getAlgorithm().name().replace("_", "-"));
        stringConfig.setIterations(config.getIterations());
        stringConfig.setSaltSizeBytes(config.getSaltSizeBytes());
        stringConfig.setSaltGeneratorClassName("org.jasypt.iv." + config.getSaltGenerator().name());
        stringConfig.setProviderName(config.getProviderName());
        stringConfig.setProviderClassName(config.getProviderClassName());
        stringConfig.setInvertPositionOfSaltInMessageBeforeDigesting(config.getInvertPositionOfSaltInMessageBeforeDigesting());
        stringConfig.setInvertPositionOfPlainSaltInEncryptionResults(config.getInvertPositionOfPlainSaltInEncryptionResults());
        stringConfig.setUseLenientSaltSizeCheck(config.getUseLenientSaltSizeCheck());
        stringConfig.setPoolSize(config.getPoolSize());
        stringConfig.setStringOutputType(config.getStringOutputType().name());
        return stringConfig;
    }

    private <T> Optional<T> getConfig(String domain, String defaultDomain, ConfigFetcher<T> fetcher) {
        return fetcher.fetch(domain)
                .or(() -> fetcher.fetch(defaultDomain));
    }

    @Override
    public StringEncryptor getPebEncryptor(String domain) {
        return getConfig(domain, DomainConstants.DEFAULT_DOMAIN_NAME, pebConfigRepository::findFirstByDomainIgnoreCase)
                .map(this::createPEBEncryptor)
                .orElseGet(() -> {
                    log.warn("PEB config not found for domain {}", domain);
                    return new PooledPBEStringEncryptor();
                });
    }

    @Override
    public StringDigester getDigestEncryptor(String domain) {
        return getConfig(domain, DomainConstants.DEFAULT_DOMAIN_NAME, digesterConfigRepository::findFirstByDomainIgnoreCase)
                .map(this::createStringDigester)
                .orElseGet(() -> {
                    log.warn("Digest config not found for domain {}", domain);
                    return new PooledStringDigester();
                });
    }

    @Override
    public PasswordEncryptor getPasswordEncryptor(String domain) {
        return getConfig(domain, DomainConstants.DEFAULT_DOMAIN_NAME, digesterConfigRepository::findFirstByDomainIgnoreCase)
                .map(this::createPasswordEncryptor)
                .orElseGet(() -> {
                    log.warn("Password config not found for domain {}", domain);
                    return new StrongPasswordEncryptor();
                });
    }

    @FunctionalInterface
    private interface ConfigFetcher<T> {
        /**
         * Fetch optional.
         *
         * @param domain the domain
         * @return the optional
         */
        Optional<T> fetch(String domain);
    }
}