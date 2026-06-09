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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

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
    public String generateRandomKey(int length, IEnumCharSet.Types charSetType) {
        return randomKeyGenerator.nextGuid(length, charSetType);
    }

    @Override
    public String generateRandomKey(int length, IEnumCharSet.Types charSetType, String pattern) {
        return randomKeyGenerator.nextGuid(length, charSetType);
    }

    @Override
    public String generateRandomKey(String tenant, Integer length, IEnumCharSet.Types charSetType) {
        return this.createOrUpdateKeyByName(tenant, UUID.randomUUID().toString(), randomKeyGenerator.nextGuid(length, charSetType));
    }

    @Override
    public String createRandomKey(String tenant, String keyName, Integer length, IEnumCharSet.Types charSetType) {
        return this.createOrUpdateKeyByName(tenant, keyName, randomKeyGenerator.nextGuid(length, charSetType));
    }

    @Override
    public String createOrUpdateKeyByName(String senderTenant, String name, String value) {
        Optional<RandomKey> optional = randomKeyRepository.findByTenantIgnoreCaseAndName(senderTenant, name);
        if (optional.isPresent()) {
            optional.get().setValue(value);
            return randomKeyRepository.save(optional.get()).getValue();
        } else {
            return randomKeyRepository.save(RandomKey.builder()
                    .tenant(senderTenant)
                    .name(name)
                    .value(value)
                    .build()).getValue();
        }
    }

    @Override
    public String getKeyByName(String senderTenant, String name) {
        Optional<RandomKey> optional = randomKeyRepository.findByTenantIgnoreCaseAndName(senderTenant, name);
        if (optional.isPresent()) {
            return optional.get().getValue();
        }
        return null;
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
    public String getIncrementalKey(String senderTenant, String entityName, String attribute) throws IncrementalConfigNotFoundException {
        AppNextCode appNextCode = null;
        Optional<AppNextCode> optional = appNextCodeRepository.findByTenantIgnoreCaseAndEntityAndAttribute(senderTenant, entityName, attribute);
        if (optional.isPresent()) {
            appNextCode = optional.get();
        } else {
            Optional<AppNextCode> defaultOptional = appNextCodeRepository.findByTenantIgnoreCaseAndEntityAndAttribute(TenantConstants.DEFAULT_TENANT_NAME, entityName, attribute);
            if (defaultOptional.isPresent()) {
                appNextCode = defaultOptional.get();
                appNextCode.setId(null);
                appNextCode.setTenant(senderTenant);
                appNextCode.setCodeValue(0L);
                appNextCode = appNextCodeRepository.save(appNextCode);
            } else {
                throw new IncrementalConfigNotFoundException("with tenant/entity/attribute " + senderTenant + "/" + entityName + "/" + attribute);
            }
        }
        appNextCodeRepository.increment(senderTenant, entityName, appNextCode.getIncrement());
        return appNextCode.getCode();
    }

    @Override
    public Page<RandomKey> listRandomKeys(String tenant, int page, int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createDate").descending()
        );

        return randomKeyRepository.findByTenantIgnoreCase(tenant, pageable);
    }

    @Override
    public void deleteByTenantAndName(String tenant, String keyName) {
        randomKeyRepository.deleteByTenantIgnoreCaseAndName(tenant, keyName);
    }
}
