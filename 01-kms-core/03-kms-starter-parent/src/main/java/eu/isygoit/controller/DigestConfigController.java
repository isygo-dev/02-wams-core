package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.DigestConfigMapper;
import eu.isygoit.model.DigestConfig;
import eu.isygoit.service.impl.DigestConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Digest config controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = KmsExceptionHandler.class, mapper = DigestConfigMapper.class, minMapper = DigestConfigMapper.class, service = DigestConfigService.class)
@RequestMapping(path = "/api/v1/private/config/digest")
public class DigestConfigController extends MappedCrudTenantController<Long, DigestConfig, DigestConfigDto, DigestConfigDto, DigestConfigService> {
}
