package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.LinkedFileApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.common.LinkedFileRequestDto;
import eu.isygoit.dto.common.LinkedFileResponseDto;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.exception.handler.DmsExceptionHandler;
import eu.isygoit.mapper.LinkedFileMapper;
import eu.isygoit.service.ILinkedFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * The type Linked file controller.
 */
@Slf4j
@Validated
@RestController
@InjectExceptionHandler(DmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/linked-files")
public class LinkedFileController extends ControllerExceptionHandler implements LinkedFileApi {

    @Autowired
    private ILinkedFileService linkedFileService;
    @Autowired
    private LinkedFileMapper linkedFileMapper;

    @Override
    public ResponseEntity<List<LinkedFileRequestDto>> searchByTags(RequestContextDto requestContext,
                                                                   String tenant, String tags) {
        log.info("Search file by tags from tenant: {} / tags:{}", tenant, tags);
        try {
            return ResponseFactory.responseOk(linkedFileMapper.listEntityToDto(linkedFileService.searchByTags(tenant, tags)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> deleteFile(RequestContextDto requestContext,
                                              String tenant,
                                              String code) {
        log.info("Delete file by tenant: {} / code: {}", tenant, code);
        try {
            linkedFileService.deleteFile(tenant, code);
            return ResponseFactory.responseOk(true);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<LinkedFileRequestDto> searchByOriginalName(RequestContextDto requestContext,
                                                                     String tenant,
                                                                     String originalFileName) {
        log.info("Search file by original name from tenant {} : {}", tenant, originalFileName);
        try {
            return ResponseFactory.responseOk(linkedFileMapper.entityToDto(linkedFileService.searchByOriginalFileName(tenant, originalFileName)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<LinkedFileRequestDto> renameFile(RequestContextDto requestContext,
                                                           String tenant,
                                                           String code,
                                                           String newName) {
        log.info("rename file by tenant: {} / code: {} to new name: {}", code, newName);
        try {
            return ResponseFactory.responseOk(linkedFileMapper.entityToDto(linkedFileService.renameFile(tenant, code, newName)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<LinkedFileRequestDto>> searchByCategories(RequestContextDto requestContext,
                                                                         String tenant,
                                                                         String categories) {
        log.info("Search files by categories from tenant {} : {}", tenant, categories);
        try {
            String[] catArray = categories.split(",");
            if (catArray.length > 0) {
                List<LinkedFileRequestDto> list = linkedFileMapper.listEntityToDto(linkedFileService.searchByCategories(tenant, Arrays.stream(categories.split(",")).toList()));
                if (CollectionUtils.isEmpty(list)) {
                    return ResponseFactory.responseNoContent();
                }
                return ResponseFactory.responseOk(list);
            }
            return ResponseFactory.responseBadRequest();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<LinkedFileResponseDto> upload(//RequestContextDto requestContext,
                                                        LinkedFileRequestDto linkedFile) throws IOException {
        log.info("Uploading file from tenant {} : {}", linkedFile.getTenant(), linkedFile.getFile().getOriginalFilename());
        if (linkedFile.getFile() == null) {
            return ResponseFactory.responseBadRequest();
        }
        try {
            return ResponseFactory.responseOk(LinkedFileResponseDto.builder()
                    .code(linkedFileService.upload(linkedFile, linkedFile.getFile()))
                    .build());
        } catch (Throwable e) {
            log.info(e.getMessage());
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Resource> download(RequestContextDto requestContext,
                                             String tenant,
                                             String code) throws IOException {
        log.info("Downloading file from tenant {} : {}", tenant, code);
        try {
            Resource resource = linkedFileService.download(tenant, code);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}



