package eu.isygoit.service.impl;

import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.com.rest.service.impl.CrudService;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.dto.exception.StorageConfigNotFoundException;
import eu.isygoit.model.StorageConfig;
import eu.isygoit.repository.StorageConfigRepository;
import eu.isygoit.service.IMinIOApiService;
import eu.isygoit.service.IStorageConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The type Storage config service.
 */
@Service
@Transactional
@SrvRepo(value = StorageConfigRepository.class)
public class StorageConfigService extends CrudService<Long, StorageConfig, StorageConfigRepository> implements IStorageConfigService {

    @Autowired
    private StorageConfigRepository storageConfigRepository;
    @Autowired
    private IMinIOApiService minIOApiService;

    @Override
    public StorageConfig findByDomainIgnoreCase(String domain) {
        Optional<StorageConfig> optional = storageConfigRepository.findFirstByDomainIgnoreCase(domain);
        if (!optional.isPresent()) {
            optional = storageConfigRepository.findFirstByDomainIgnoreCase(DomainConstants.DEFAULT_DOMAIN_NAME);
        }

        if (optional.isPresent()) {
            return optional.get();
        } else {
            throw new StorageConfigNotFoundException("for domain: " + domain);
        }
    }

    @Override
    public StorageConfig afterUpdate(StorageConfig storageConfig) {
        switch (storageConfig.getType()) {
            case MINIO_STORAGE: {
                minIOApiService.updateConnection(storageConfig);
            }
            break;
            case CEPH_STORAGE: {
                //minIOApiService.updateConnection(storageConfig);
            }
            break;
            case LAKEFS_STORAGE: {
                //minIOApiService.updateConnection(storageConfig);
            }
            break;
            case OPENIO_STORAGE: {
                //minIOApiService.updateConnection(storageConfig);
            }
            break;
        }
        return super.afterUpdate(storageConfig);
    }
}
