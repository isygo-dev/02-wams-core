package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.com.rest.controller.impl.MappedFileController;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.exception.handler.MmsExceptionHandler;
import eu.isygoit.mapper.MsgTemplateMapper;
import eu.isygoit.model.MsgTemplate;
import eu.isygoit.service.impl.MsgTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Template controller.
 */
@Slf4j
@Validated
@RestController
@CtrlDef(handler = MmsExceptionHandler.class, mapper = MsgTemplateMapper.class, minMapper = MsgTemplateMapper.class, service = MsgTemplateService.class)
@RequestMapping(path = "/api/v1/private/mail/template")
public class MsgTemplateFileController extends MappedFileController<Long, MsgTemplate, MsgTemplateDto, MsgTemplateDto, MsgTemplateService> {


}
