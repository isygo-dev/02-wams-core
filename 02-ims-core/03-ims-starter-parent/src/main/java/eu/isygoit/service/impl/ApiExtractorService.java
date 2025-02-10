package eu.isygoit.service.impl;

import eu.isygoit.api.AbstractApiExtractor;
import eu.isygoit.model.ApiPermission;
import eu.isygoit.repository.ApiPermissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The type Api extractor service.
 */
@Slf4j
@Service
@Transactional
public class ApiExtractorService extends AbstractApiExtractor<ApiPermission> {

    private final ApiPermissionRepository apiPermissionRepository;
    @Value("${spring.application.name}")
    private String serviceName;

    @Autowired
    public ApiExtractorService(ApiPermissionRepository apiPermissionRepository) {
        this.apiPermissionRepository = apiPermissionRepository;
    }

    @Transactional
    @Override
    public ApiPermission saveApi(ApiPermission api) {
        Optional<ApiPermission> optional = apiPermissionRepository.findByServiceNameAndObjectAndMethodAndRqTypeAndPath(api.getServiceName()
                , api.getObject()
                , api.getMethod()
                , api.getRqType()
                , api.getPath());
        if (optional.isPresent()) {
            return optional.get();
        }
        return apiPermissionRepository.save(api);
    }

    @Override
    public ApiPermission newInstance() {
        return ApiPermission.builder().serviceName(serviceName).build();
    }
}
