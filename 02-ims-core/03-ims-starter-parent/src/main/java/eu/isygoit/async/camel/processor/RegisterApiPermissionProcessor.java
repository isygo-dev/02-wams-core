package eu.isygoit.async.camel.processor;

import eu.isygoit.com.camel.processor.AbstractCamelProcessor;
import eu.isygoit.com.camel.processor.AbstractStringProcessor;
import eu.isygoit.dto.data.ApiPermissionDto;
import eu.isygoit.helper.JsonHelper;
import eu.isygoit.mapper.ApiPermissionMapper;
import eu.isygoit.model.ApiPermission;
import eu.isygoit.repository.ApiPermissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;


/**
 * The type Register api permission processor.
 */
@Slf4j
@Component
@Qualifier("registerApiPermissionProcessor")
public class RegisterApiPermissionProcessor extends AbstractStringProcessor {

    private final ApiPermissionRepository apiPermissionRepository;
    private final ApiPermissionMapper apiPermissionMapper;

    @Autowired
    public RegisterApiPermissionProcessor(ApiPermissionRepository apiPermissionRepository, ApiPermissionMapper apiPermissionMapper) {
        this.apiPermissionRepository = apiPermissionRepository;
        this.apiPermissionMapper = apiPermissionMapper;
    }

    @Override
    public void performProcessor(Exchange exchange, String apiPermissionMsg) throws Exception {
        ApiPermissionDto apiPermission = JsonHelper.fromJson(apiPermissionMsg, ApiPermissionDto.class);
        apiPermission.setId(null);
        exchange.getIn().setHeader("code", apiPermission.getMethod());

        //Verify if the api is already registered
        Optional<ApiPermission> optional = apiPermissionRepository.findByServiceNameAndObjectAndMethodAndRqTypeAndPath(apiPermission.getServiceName()
                , apiPermission.getObject()
                , apiPermission.getMethod()
                , apiPermission.getRqType()
                , apiPermission.getPath());
        if (optional.isPresent()) {
            // Update the existing one
            apiPermission.setId(optional.get().getId());
            apiPermissionRepository.save(apiPermissionMapper.dtoToEntity(apiPermission));
        }

        //Create a new One
        apiPermissionRepository.save(apiPermissionMapper.dtoToEntity(apiPermission));

        exchange.getIn().setHeader(AbstractCamelProcessor.RETURN_HEADER, true);
    }
}
