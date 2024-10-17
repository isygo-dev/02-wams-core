package eu.isygoit.controller;


import eu.isygoit.annotation.CtrlHandler;
import eu.isygoit.api.PropertyControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.PropertyDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.PropertyMapper;
import eu.isygoit.service.IPropertyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * The type Property controller.
 */
@Slf4j
@Validated
@RestController
@CtrlHandler(ImsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/property")
public class PropertyController extends ControllerExceptionHandler implements PropertyControllerApi {

    @Autowired
    private IPropertyService propertyService;

    @Autowired
    private PropertyMapper propertyMapper;

    @Override
    public ResponseEntity<PropertyDto> updatePropertyAccount(RequestContextDto requestContext,
                                                             String accountCode, PropertyDto property) {
        try {
            return ResponseFactory.ResponseOk(propertyMapper.entityToDto(propertyService.updateProperty(accountCode, propertyMapper.dtoToEntity(property))));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<PropertyDto> getPropertyByAccount(RequestContextDto requestContext,
                                                            String accountCode, String guiName, String name) {
        try {
            return ResponseFactory.ResponseOk(propertyMapper.entityToDto(propertyService.getPropertyByAccount(accountCode, guiName, name)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<PropertyDto>> getPropertyByAccountAndGui(RequestContextDto requestContext,
                                                                        String accountCode, String guiName) {
        try {
            return ResponseFactory.ResponseOk(propertyMapper.listEntityToDto(propertyService.getPropertyByAccountAndGui(accountCode, guiName)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}
