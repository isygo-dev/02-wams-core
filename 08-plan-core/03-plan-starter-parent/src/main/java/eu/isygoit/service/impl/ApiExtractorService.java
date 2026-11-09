package eu.isygoit.service.impl;

import eu.isygoit.api.AbstractApiExtractor;
import eu.isygoit.async.kafka.KafkaRegisterApisProducer;
import eu.isygoit.mapper.ApiPermissionMapper;
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

    @Value("${spring.application.name}")
    private String serviceName;

    @Autowired(required = false)
    private KafkaRegisterApisProducer kafkaRegisterApisProducer;
    @Autowired
    private ApiPermissionRepository apiPermissionRepository;
    @Autowired
    private ApiPermissionMapper apiPermissionMapper;

    @Transactional
    @Override
    public ApiPermission saveApi(ApiPermission api) {
        Optional<ApiPermission> optional = apiPermissionRepository.findByServiceNameAndObjectAndMethodAndRqTypeAndPath(api.getServiceName()
                , api.getObject()
                , api.getMethod()
                , api.getRqType()
                , api.getPath());
        if (optional.isPresent()) {
            api = optional.get();
        } else {
            api = apiPermissionRepository.save(api);
        }

        try {
            kafkaRegisterApisProducer.send(apiPermissionMapper.entityToDto(api));
        } catch (RuntimeException e) {
            log.error("<Error>: Register api permission via kafka topic failed with error: {} ", e);
        }

        return api;
    }

    @Override
    public ApiPermission newInstance() {
        return ApiPermission.builder().serviceName(serviceName).build();
    }
}
