package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedImageTenantController;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.DomainDto;
import eu.isygoit.dto.data.KmsDomainDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.DomainMapper;
import eu.isygoit.model.Domain;
import eu.isygoit.remote.kms.KmsDomainService;
import eu.isygoit.service.impl.DomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.security.auth.login.AccountException;

/**
 * The type Domain image controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = DomainMapper.class, minMapper = DomainMapper.class, service = DomainService.class)
@RequestMapping(path = "/api/v1/private/tenant")
public class DomainImageController extends MappedImageTenantController<Long, Domain, DomainDto, DomainDto, DomainService> {

    @Autowired
    private KmsDomainService kmsDomainService;

    @Override
    public Domain afterUpdate(Domain tenant) throws Exception {
        try {
            ResponseEntity<Boolean> result = kmsDomainService.updateDomain(RequestContextDto.builder().build(),
                    KmsDomainDto.builder()
                            .name(tenant.getName())
                            .description(tenant.getDescription())
                            .url(tenant.getUrl())
                            .adminStatus(tenant.getAdminStatus())
                            .build());
            if (result.getStatusCode().is2xxSuccessful() && result.hasBody() && result.getBody()) {
                return super.afterUpdate(tenant);
            }
        } catch (Exception e) {
            log.error("Remote feign call failed : ", e);
            //throw new RemoteCallFailedException(e);
        }

        throw new AccountException("Update tenant issue in KMS");
    }
}
