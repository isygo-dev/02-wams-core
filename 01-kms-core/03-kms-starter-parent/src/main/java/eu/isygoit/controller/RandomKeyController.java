package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.com.rest.controller.impl.ControllerUtils;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.common.RandomKeyDto;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.mapper.RandomKeyMapper;
import eu.isygoit.model.RandomKey;
import eu.isygoit.service.IKeyService;
import eu.isygoit.service.RandomKeyServiceApi;
import eu.isygoit.service.RequestContextService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Key controller.
 */
@Slf4j
@Validated
@RestController
@InjectExceptionHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/key")
@Tag(name = "KMS Keys", description = "Key Management Service - All cryptographic operations and key management endpoints")
public class RandomKeyController extends ControllerUtils implements RandomKeyServiceApi {

    @Autowired
    private RandomKeyMapper randomKeyMapper;
    @Autowired
    private IKeyService keyService;
    

    /*
     * Create new random key without saving
     */
    @Override
    public ResponseEntity<String> newRandomKey(Integer length, IEnumCharSet.Types charSetType) {
        log.info("Call generateRandomKey");
        String tenant = requestContextService().getCurrentContext().getSenderTenant();
        try {
            return ResponseFactory.responseOk(keyService.generateRandomKey(length, charSetType));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    /*
     * Create new random key by name for tenant and saving
     */
    @Override
    public ResponseEntity<String> renewRandomKey(String keyName,
                                                 Integer length, IEnumCharSet.Types charSetType) {
        log.info("Call generateRandomKeyName");
        String tenant = requestContextService().getCurrentContext().getSenderTenant();
        try {
            String keyValue = keyService.generateRandomKey(length, charSetType);
            keyService.createOrUpdateKeyByName(tenant, keyName, keyValue);
            return ResponseFactory.responseOk(keyValue);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    /*
     * Getting saved random key for tenant and name
     */
    @Override
    public ResponseEntity<String> getRandomKey(String keyName) {
        log.info("Call getRandomKeyName");
        String tenant = requestContextService().getCurrentContext().getSenderTenant();
        try {
            return ResponseFactory.responseOk(keyService.getKeyByName(tenant, keyName));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<PaginatedResponseDto<RandomKeyDto>> listRandomKeys(int page, int size) {
        log.info("Call listRandomKeys");
        String tenant = requestContextService().getCurrentContext().getSenderTenant();
        try {
            Page<RandomKey> randomKeys = keyService.listRandomKeys(tenant, page, size);
            if (randomKeys.isEmpty()) {
                return ResponseFactory.responseNoContent();
            }
            return ResponseFactory.responseOk(PaginatedResponseDto.<RandomKeyDto>builder()
                    .content(randomKeyMapper.listEntityToDto(randomKeys.getContent()))
                    .totalElements(randomKeys.getTotalElements())
                    .totalPages(randomKeys.getTotalPages())
                    .pageNumber(randomKeys.getNumber())
                    .pageSize(randomKeys.getSize())
                    .build());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Void> deleteRandomKey(String keyName) {
        log.info("Call deleteRandomKey");
        String tenant = requestContextService().getCurrentContext().getSenderTenant();
        try {
            keyService.deleteByTenantAndName(tenant, keyName);
            return ResponseFactory.responseOk();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}