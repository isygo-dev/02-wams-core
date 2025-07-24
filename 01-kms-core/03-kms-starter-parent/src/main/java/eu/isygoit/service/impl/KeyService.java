package eu.isygoit.service.impl;

import eu.isygoit.constants.TenantConstants;
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

    @Autowired
    private AppNextCodeRepository appNextCodeRepository;

    @Autowired
    private RandomKeyGenerator randomKeyGenerator;

    @Autowired
    private RandomKeyRepository randomKeyRepository;

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
        Optional<AppNextCode> optional = appNextCodeRepository.findByTenantIgnoreCaseAndEntityAndAttribute(appNextCode.getTenant()
                , appNextCode.getEntity(), appNextCode.getAttribute());
        if (!optional.isPresent()) {
            appNextCodeRepository.save(appNextCode);
        } else {
            log.warn("Incremental key is already defined: {}", appNextCode);
        }
    }

    @Override
    public String getIncrementalKey(String tenant, String entityName, String attribute) throws IncrementalConfigNotFoundException {
        AppNextCode appNextCode = null;
        Optional<AppNextCode> optional = appNextCodeRepository.findByTenantIgnoreCaseAndEntityAndAttribute(tenant, entityName, attribute);
        if (optional.isPresent()) {
            appNextCode = optional.get();
        } else {
            Optional<AppNextCode> defaultOptional = appNextCodeRepository.findByTenantIgnoreCaseAndEntityAndAttribute(TenantConstants.DEFAULT_TENANT_NAME, entityName, attribute);
            if (defaultOptional.isPresent()) {
                appNextCode = defaultOptional.get();
                appNextCode.setId(null);
                appNextCode.setTenant(tenant);
                appNextCode.setCodeValue(0L);
                appNextCode = appNextCodeRepository.save(appNextCode);
            } else {
                throw new IncrementalConfigNotFoundException("with tenant/entity/attribute " + tenant + "/" + entityName + "/" + attribute);
            }
        }
        appNextCodeRepository.increment(tenant, entityName, appNextCode.getIncrement());
        return appNextCode.getCode();
    }

    @Override
    public RandomKey createOrUpdateKeyByName(String tenant, String name, String value) {
        Optional<RandomKey> optional = randomKeyRepository.findByTenantIgnoreCaseAndName(tenant, name);
        if (optional.isPresent()) {
            optional.get().setValue(value);
            return randomKeyRepository.save(optional.get());
        } else {
            return randomKeyRepository.save(RandomKey.builder()
                    .tenant(tenant)
                    .name(name)
                    .value(value)
                    .build());
        }
    }

    @Override
    public RandomKey getKeyByName(String tenant, String name) {
        Optional<RandomKey> optional = randomKeyRepository.findByTenantIgnoreCaseAndName(tenant, name);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }
}
