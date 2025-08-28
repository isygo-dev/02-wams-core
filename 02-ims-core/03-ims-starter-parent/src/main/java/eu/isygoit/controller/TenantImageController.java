package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedImageTenantController;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.data.KmsTenantDto;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.TenantMapper;
import eu.isygoit.model.Tenant;
import eu.isygoit.remote.kms.KmsTenantService;
import eu.isygoit.service.impl.TenantService;
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
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = TenantMapper.class, minMapper = TenantMapper.class, service = TenantService.class)
@RequestMapping(path = "/api/v1/private/tenant")
public class TenantImageController extends MappedImageTenantController<Long, Tenant, TenantDto, TenantDto, TenantService> {

    @Autowired
    private KmsTenantService kmsDomainService;

    @Override
    public Tenant afterUpdate(Tenant tenant) throws Exception {
        try {
            ResponseEntity<Boolean> result = kmsDomainService.updateDomain(ContextRequestDto.builder().build(),
                    KmsTenantDto.builder()
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
