package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.com.rest.service.CrudService;
import eu.isygoit.com.rest.service.tenancy.CrudTenantService;
import eu.isygoit.constants.TenantConstants;
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
@InjectRepository(value = StorageConfigRepository.class)
public class StorageConfigService extends CrudTenantService<Long, StorageConfig, StorageConfigRepository> implements IStorageConfigService {

    @Autowired
    private StorageConfigRepository storageConfigRepository;
    @Autowired
    private IMinIOApiService minIOApiService;

    @Override
    public StorageConfig findByTenantIgnoreCase(String tenant) {
        Optional<StorageConfig> optional = storageConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (!optional.isPresent()) {
            optional = storageConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }

        if (optional.isPresent()) {
            return optional.get();
        } else {
            throw new StorageConfigNotFoundException("for tenant: " + tenant);
        }
    }

    @Override
    public StorageConfig afterUpdate(String tenant, StorageConfig storageConfig) {
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
        return super.afterUpdate(tenant, storageConfig);
    }
}
