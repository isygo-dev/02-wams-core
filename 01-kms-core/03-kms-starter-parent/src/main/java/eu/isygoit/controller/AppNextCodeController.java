package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.AppNextCodeMapper;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.service.impl.AppNextCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type App next code controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = KmsExceptionHandler.class, mapper = AppNextCodeMapper.class, minMapper = AppNextCodeMapper.class, service = AppNextCodeService.class)
@RequestMapping(path = "/api/v1/private/code")
public class AppNextCodeController extends MappedCrudTenantController<Long, AppNextCode, NextCodeDto, NextCodeDto, AppNextCodeService> {
}
