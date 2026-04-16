package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.TokenConfigMapper;
import eu.isygoit.model.TokenConfig;
import eu.isygoit.service.impl.TokenConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Token config controller.
 */
@Slf4j
@Validated
@Tag(name = "Token Config", description = "Endpoints for managing token configurations")
@RestController
@RequestMapping(path = "/api/v1/private/config/token")
@InjectMapperAndService(handler = KmsExceptionHandler.class, mapper = TokenConfigMapper.class, minMapper = TokenConfigMapper.class, service = TokenConfigService.class)
public class TokenConfigController extends MappedCrudTenantController<Long, TokenConfig, TokenConfigDto, TokenConfigDto, TokenConfigService> {
}
