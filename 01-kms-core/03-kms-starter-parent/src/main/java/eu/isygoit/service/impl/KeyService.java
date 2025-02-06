package eu.isygoit.service.impl;

import eu.isygoit.constants.DomainConstants;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.exception.IncrementalConfigNotFoundException;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.RandomKey;
import eu.isygoit.repository.AppNextCodeRepository;
import eu.isygoit.repository.RandomKeyRepository;
import eu.isygoit.service.IKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The type Key service.
 */
@Slf4j
@Service
@Transactional
public class KeyService implements IKeyService {

    private final AppNextCodeRepository appNextCodeRepository;
    private final RandomKeyGenerator randomKeyGenerator;
    private final RandomKeyRepository randomKeyRepository;

    /**
     * Instantiates a new Key service.
     *
     * @param appNextCodeRepository the app next code repository
     * @param randomKeyGenerator    the random key generator
     * @param randomKeyRepository   the random key repository
     */
    @Autowired
    public KeyService(AppNextCodeRepository appNextCodeRepository, RandomKeyGenerator randomKeyGenerator, RandomKeyRepository randomKeyRepository) {
        this.appNextCodeRepository = appNextCodeRepository;
        this.randomKeyGenerator = randomKeyGenerator;
        this.randomKeyRepository = randomKeyRepository;
    }

    @Override
    public String getRandomKey(int length, IEnumCharSet.Types charSetType) {
        return randomKeyGenerator.nextGuid(length, charSetType);
    }

    @Override
    public String getRandomKey(int length, IEnumCharSet.Types charSetType, String pattern) {
        return randomKeyGenerator.nextGuid(length, charSetType);
    }

    @Override
    public void subscribeIncrementalKeyGenerator(AppNextCode appNextCode) {
        appNextCodeRepository.findByDomainIgnoreCaseAndEntityAndAttribute(appNextCode.getDomain(), appNextCode.getEntity(), appNextCode.getAttribute())
                .ifPresentOrElse(
                        existingCode -> log.warn("Incremental key is already defined: {}", appNextCode),
                        () -> appNextCodeRepository.save(appNextCode)
                );
    }

    @Override
    public String getIncrementalKey(String domain, String entityName, String attribute) throws IncrementalConfigNotFoundException {
        AppNextCode appNextCode = appNextCodeRepository.findByDomainIgnoreCaseAndEntityAndAttribute(domain, entityName, attribute)
                .orElseGet(() -> appNextCodeRepository.findByDomainIgnoreCaseAndEntityAndAttribute(DomainConstants.DEFAULT_DOMAIN_NAME, entityName, attribute)
                        .map(defaultCode -> {
                            defaultCode.setId(null);
                            defaultCode.setDomain(domain);
                            defaultCode.setValue(0L);
                            return appNextCodeRepository.save(defaultCode);
                        })
                        .orElseThrow(() -> new IncrementalConfigNotFoundException("with domain/entity/attribute " + domain + "/" + entityName + "/" + attribute))
                );

        appNextCodeRepository.increment(domain, entityName, appNextCode.getIncrement());
        return appNextCode.getCode();
    }

    @Override
    public RandomKey createOrUpdateKeyByName(String domain, String name, String value) {
        return randomKeyRepository.findByDomainIgnoreCaseAndName(domain, name)
                .map(existingKey -> {
                    existingKey.setValue(value);
                    return randomKeyRepository.save(existingKey);
                })
                .orElseGet(() -> randomKeyRepository.save(RandomKey.builder()
                        .domain(domain)
                        .name(name)
                        .value(value)
                        .build())
                );
    }

    @Override
    public Optional<RandomKey> getKeyByName(String domain, String name) {
        return randomKeyRepository.findByDomainIgnoreCaseAndName(domain, name);
    }
}