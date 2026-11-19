package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.LinkedFileApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.com.rest.service.media.FileDownloadResponses;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.LinkedFileRequestDto;
import eu.isygoit.dto.common.LinkedFileResponseDto;
import eu.isygoit.exception.handler.DmsExceptionHandler;
import eu.isygoit.mapper.LinkedFileMapper;
import eu.isygoit.service.ILinkedFileService;
import eu.isygoit.service.RequestContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
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
    @Autowired
    private RequestContextService requestContextService;

    @Override
    public ResponseEntity<List<LinkedFileResponseDto>> searchByTags(
            @RequestParam(name = RestApiConstants.TAGS, required = false) List<String> tags) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        log.info("Search file by tags from tenant: {} / tags:{}", tenant, tags);
        try {
            List<LinkedFileResponseDto> list = linkedFileMapper.listEntityToDto(
                    isUnfiltered(tags) ? linkedFileService.findAll(tenant) : linkedFileService.searchByTags(tenant, tags));
            return ResponseFactory.responseOk(list);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> deleteFile(String code) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
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
    public ResponseEntity<LinkedFileResponseDto> searchByOriginalName(
            String originalFileName) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        log.info("Search file by original name from tenant {} : {}", tenant, originalFileName);
        try {
            return ResponseFactory.responseOk(linkedFileMapper.entityToDto(linkedFileService.searchByOriginalFileName(tenant, originalFileName)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<LinkedFileResponseDto> renameFile(
            String code,
            String newName) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        log.info("rename file by tenant: {} / code: {} to new name: {}", tenant, code, newName);
        try {
            return ResponseFactory.responseOk(linkedFileMapper.entityToDto(linkedFileService.renameFile(tenant, code, newName)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<LinkedFileResponseDto>> searchByCategories(
            @RequestParam(name = RestApiConstants.CATEGORIES, required = false) List<String> categories) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        log.info("Search files by categories from tenant {} : {}", tenant, categories);
        try {
            List<LinkedFileResponseDto> list = linkedFileMapper.listEntityToDto(
                    isUnfiltered(categories) ? linkedFileService.findAll(tenant)
                            : linkedFileService.searchByCategories(tenant, categories));
            if (CollectionUtils.isEmpty(list)) {
                return ResponseFactory.responseNoContent();
            }
            return ResponseFactory.responseOk(list);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    private boolean isUnfiltered(List<String> values) {
        return CollectionUtils.isEmpty(values) || values.stream().noneMatch(StringUtils::hasText);
    }

    @Override
    public ResponseEntity<LinkedFileResponseDto> upload(
            LinkedFileRequestDto linkedFile) throws IOException {
        // Validate BEFORE touching linkedFile or its file — otherwise the log statement
        // below would throw a NullPointerException on a missing file.
        if (linkedFile == null || linkedFile.getFile() == null) {
            log.warn("Upload rejected: empty request or missing file part");
            return ResponseFactory.responseBadRequest();
        }

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        log.info("Uploading file from tenant {} : {}", tenant, linkedFile.getFile().getOriginalFilename());
        try {
            return ResponseFactory.responseOk(LinkedFileResponseDto.builder()
                    .code(linkedFileService.upload(tenant, linkedFile, linkedFile.getFile()))
                    .build());
        } catch (Throwable e) {
            // Use log.error (not log.info) so the stack trace is recorded.
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Resource> download(String code) throws IOException {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        log.info("Downloading file from tenant {} : {}", tenant, code);
        try {
            Resource resource = linkedFileService.download(tenant, code);
            if (resource == null) {
                log.warn("No file found for code {} in tenant {}", code, tenant);
                return ResponseEntity.notFound().build();
            }

            // Do NOT call Files.probeContentType(resource.getFile().toPath()): the resource
            // may be an in-memory byte array (e.g. backed by a Feign call), and probing the
            // filesystem would throw FileNotFoundException. "multipart/form-data" was also
            // incorrect here — that is a request format, not a file download response.
            String contentType = FileDownloadResponses.resolveContentType(resource, resource.getFilename());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}