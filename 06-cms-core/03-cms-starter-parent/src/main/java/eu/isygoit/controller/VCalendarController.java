package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.exception.handler.CmsExceptionHandler;
import eu.isygoit.mapper.VCalendarMapper;
import eu.isygoit.model.VCalendar;
import eu.isygoit.service.impl.VCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The type V calendar controller.
 */
@Slf4j
@Validated
@RestController
@CtrlDef(handler = CmsExceptionHandler.class, mapper = VCalendarMapper.class, minMapper = VCalendarMapper.class, service = VCalendarService.class)
@RequestMapping(path = "/api/v1/private/calendar")
public class VCalendarController extends MappedCrudController<Long, VCalendar, VCalendarDto, VCalendarDto, VCalendarService> {

    private final VCalendarService vCalendarService;

    @Autowired
    public VCalendarController(VCalendarService vCalendarService) {
        this.vCalendarService = vCalendarService;
    }

    /**
     * Download response entity.
     *
     * @param requestContext the request context
     * @param domain         the domain
     * @param name           the name
     * @return the response entity
     * @throws IOException the io exception
     */
    @Operation(summary = "xxx Api",
            description = "xxx")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Resource.class))})
    })
    @GetMapping(path = "/ics/download", produces = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> download(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT) RequestContextDto requestContext,
                                             @RequestParam(name = RestApiConstants.DOMAIN_NAME) String domain,
                                             @RequestParam(name = RestApiConstants.NAME) String name) throws IOException {
        try {
            Resource resource = vCalendarService.download(domain, name);
            Path path = resource.getFile().toPath();
            log.info(resource.getFilename());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, Files.probeContentType(path))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Operation(summary = "Update locked calendar status Api",
            description = "Update locked calendar status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = VCalendarDto.class))})
    })
    @PutMapping(path = "/locked-status")
    public ResponseEntity<VCalendarDto> updateLockedCalendarStatus(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT) RequestContextDto requestContext,
                                                                   @RequestParam(name = RestApiConstants.ID) Long id,
                                                                   @RequestParam(name = RestApiConstants.IS_LOCKED) Boolean locked) {
        try {
            return ResponseFactory.ResponseOk(mapper().entityToDto(crudService().updateLockedStatus(id, locked)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}
