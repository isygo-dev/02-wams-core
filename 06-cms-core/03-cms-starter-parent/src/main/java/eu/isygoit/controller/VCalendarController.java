package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.VCalendarServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.constants.RestApiConstants;
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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * The type V calendar controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = CmsExceptionHandler.class, mapper = VCalendarMapper.class, minMapper = VCalendarMapper.class, service = VCalendarService.class)
@RequestMapping(path = "/api/v1/private/calendar")
public class VCalendarController extends MappedCrudTenantController<Long, VCalendar, VCalendarDto, VCalendarDto, VCalendarService>
        implements VCalendarServiceApi {

    /**
     * Standard MIME type for iCalendar (.ics) files — RFC 5545.
     */
    private static final String ICS_CONTENT_TYPE = "text/calendar";

    @Autowired
    private VCalendarService vCalendarService;

    /**
     * Download response entity.
     *
     * @param tenant the tenant
     * @param name   the name
     * @return the response entity
     */
    @Operation(summary = "Download an ICS calendar file",
            description = "Streams the iCalendar (.ics) file for the given tenant and name as an attachment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = ICS_CONTENT_TYPE,
                            schema = @Schema(implementation = Resource.class))})
    })
    @GetMapping(path = "/ics/download", produces = ICS_CONTENT_TYPE)
    public ResponseEntity<Resource> download(
            @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
            @RequestParam(name = RestApiConstants.NAME) String name) {
        try {
            Resource resource = vCalendarService.download(tenant, name);
            if (resource == null) {
                log.warn("No calendar found for tenant {} and name {}", tenant, name);
                return ResponseEntity.notFound().build();
            }

            // The Resource may be an in-memory byte array (for example, when it comes from a
            // Feign call). In that case Resource#getFile() throws FileNotFoundException.
            // Use the known ICS MIME type and the resource's filename instead of probing
            // the filesystem with Files.probeContentType(...).
            String fileName = resource.getFilename() != null
                    ? resource.getFilename()
                    : name + ".ics";

            log.info("Serving calendar file '{}' for tenant {}", fileName, tenant);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, ICS_CONTENT_TYPE)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
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
    public ResponseEntity<VCalendarDto> updateLockedCalendarStatus(
            @RequestParam(name = RestApiConstants.ID) Long id,
            @RequestParam(name = RestApiConstants.IS_LOCKED) Boolean locked) {
        try {
            return ResponseFactory.responseOk(mapper().entityToDto(crudService().updateLockedStatus(id, locked)));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}