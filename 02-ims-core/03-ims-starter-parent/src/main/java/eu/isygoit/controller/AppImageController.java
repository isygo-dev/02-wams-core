package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.ApplicationMapper;
import eu.isygoit.model.Application;
import eu.isygoit.service.impl.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type App image controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = ApplicationMapper.class, minMapper = ApplicationMapper.class, service = ApplicationService.class)
@RequestMapping(path = "/api/v1/private/application")
public class AppImageController extends eu.isygoit.com.rest.controller.impl.tenancy.MappedImageTenantController<Long, Application, ApplicationDto, ApplicationDto, ApplicationService> {

}
