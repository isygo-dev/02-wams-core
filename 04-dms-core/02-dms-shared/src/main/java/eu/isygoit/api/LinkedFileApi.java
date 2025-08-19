package eu.isygoit.api;

import eu.isygoit.com.rest.api.ILinkedFileApi;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.common.LinkedFileRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


/**
 * The interface Linked file api.
 */
public interface LinkedFileApi extends ILinkedFileApi<LinkedFileRequestDto> {

    /**
     * Search by tags response entity.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param tags           the tags
     * @return the response entity
     */
    @Operation(summary = "searchByTags Api",
            description = "searchByTags")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = LinkedFileRequestDto.class))})
    })
    @GetMapping(path = "/searchByTags")
    ResponseEntity<List<LinkedFileRequestDto>> searchByTags(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                                            @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                                            @RequestParam(name = RestApiConstants.TAGS) String tags);


    /**
     * Search by original name response entity.
     *
     * @param requestContext   the request context
     * @param tenant           the tenant
     * @param originalFileName the original file name
     * @return the response entity
     */
    @Operation(summary = "Search file by original name Api",
            description = "Search file by original name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = LinkedFileRequestDto.class))})
    })
    @GetMapping(path = "/searchByOriginalName")
    ResponseEntity<LinkedFileRequestDto> searchByOriginalName(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                                              @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                                              @RequestParam(name = RestApiConstants.ORIGINAL_FILE_NAME) String originalFileName);

    /**
     * Rename file response entity.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param code           the code
     * @param newName        the new name
     * @return the response entity
     */
    @Operation(summary = "Rename file Api",
            description = "Rename file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = LinkedFileRequestDto.class))})
    })
    @GetMapping(path = "/renameFile")
    ResponseEntity<LinkedFileRequestDto> renameFile(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                                    @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                                    @RequestParam(name = RestApiConstants.CODE) String code,
                                                    @RequestParam(name = RestApiConstants.NEW_NAME) String newName);

    /**
     * Search by categories response entity.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param categories     the categories
     * @return the response entity
     */
    @Operation(summary = "Search files by category Api",
            description = "Search files by category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = LinkedFileRequestDto.class))})
    })
    @GetMapping(path = "/searchByCategories")
    ResponseEntity<List<LinkedFileRequestDto>> searchByCategories(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                                                  @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                                                  @RequestParam(name = RestApiConstants.CATEGORIES) String categories);
}
