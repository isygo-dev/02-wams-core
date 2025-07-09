package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.AnnexControllerApi;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.AnnexMapper;
import eu.isygoit.model.Annex;
import eu.isygoit.service.IAnnexService;
import eu.isygoit.service.impl.AnnexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * The type Annex controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = AnnexMapper.class, minMapper = AnnexMapper.class, service = AnnexService.class)
@RequestMapping(path = "/api/v1/private/annex")
public class AnnexController extends MappedCrudTenantController<Long, Annex, AnnexDto, AnnexDto, AnnexService>
        implements AnnexControllerApi {

    @Autowired
    private IAnnexService annexService;

    @Override
    public ResponseEntity<List<AnnexDto>> getAnnexByTableCode(RequestContextDto requestContext,
                                                              String code) {
        try {
            List<AnnexDto> list = mapper().listEntityToDto(annexService.findAnnexByTableCode(code));
            if (!CollectionUtils.isEmpty(list)) {
                return new ResponseEntity<>(list, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<AnnexDto>> getAnnexByTableCodeAndReference(RequestContextDto requestContext, String code, String reference) {
        try {
            List<AnnexDto> list = mapper().listEntityToDto(annexService.findAnnexByTableCodeAndRef(code, reference));
            if (!CollectionUtils.isEmpty(list)) {
                return new ResponseEntity<>(list, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}
