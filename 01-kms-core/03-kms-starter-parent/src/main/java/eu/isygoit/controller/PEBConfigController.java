package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.data.PEBConfigDto;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.PEBConfigMapper;
import eu.isygoit.model.PEBConfig;
import eu.isygoit.service.impl.PEBConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Peb config controller.
 */
@Slf4j
@Validated
@RestController
@CtrlDef(handler = KmsExceptionHandler.class, mapper = PEBConfigMapper.class, minMapper = PEBConfigMapper.class, service = PEBConfigService.class)
@RequestMapping(path = "/api/v1/private/config/peb")
public class PEBConfigController extends MappedCrudController<Long, PEBConfig, PEBConfigDto, PEBConfigDto, PEBConfigService> {

}
