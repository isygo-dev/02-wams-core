package eu.isygoit.api;

import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.data.VCalendarEventDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The interface Calendar event controller api.
 */
public interface CalendarEventControllerAPI {

    /**
     * Event by tenant and calendar and code response entity.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param calendar       the calendar
     * @param code           the code
     * @return the response entity
     */
    @Operation(summary = "eventByTenantAndCalendarAndCode Api",
            description = "eventByTenantAndCalendarAndCode")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = VCalendarEventDto.class))})
    })
    @GetMapping(path = "/byDomainAndCalendarAndCode/{tenant}/{calendar}/{code}")
    ResponseEntity<VCalendarEventDto> eventByTenantAndCalendarAndCode(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                                                      @PathVariable(name = RestApiConstants.TENANT_NAME) String tenant,
                                                                      @PathVariable(name = RestApiConstants.CALENDAR) String calendar,
                                                                      @PathVariable(name = RestApiConstants.CODE) String code);

    /**
     * Gets all by tenant and calendar name.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param calendar       the calendar
     * @return the all by tenant and calendar name
     */
    @Operation(summary = "getAllByTenantAndCalendarName Api",
            description = "getAllByTenantAndCalendarName")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = VCalendarEventDto.class))})
    })
    @GetMapping(path = "/byDomainAndCalendarName/{tenant}/{calendar}")
    ResponseEntity<List<VCalendarEventDto>> getAllByTenantAndCalendarName(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                                                          @PathVariable(name = RestApiConstants.TENANT_NAME) String tenant,
                                                                          @PathVariable(name = RestApiConstants.CALENDAR) String calendar);

    /**
     * Save event response entity.
     *
     * @param event the event
     * @return the response entity
     */
    @Operation(summary = "saveEvent Api",
            description = "saveEvent")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = VCalendarEventDto.class))})
    })
    @PostMapping()
    ResponseEntity<VCalendarEventDto> saveEvent(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                                @Valid @RequestBody VCalendarEventDto event);

    /**
     * Update event response entity.
     *
     * @param id    the id
     * @param event the event
     * @return the response entity
     */
    @Operation(summary = "updateEvent Api",
            description = "updateEvent")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = VCalendarEventDto.class))})
    })
    @PutMapping(path = "/{id}")
    ResponseEntity<VCalendarEventDto> updateEvent(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                                  @PathVariable(name = RestApiConstants.ID) Long id,
                                                  @Valid @RequestBody VCalendarEventDto event);
}
