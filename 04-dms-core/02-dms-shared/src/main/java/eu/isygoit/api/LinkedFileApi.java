package eu.isygoit.api;

import eu.isygoit.com.rest.api.ILinkedFileApi;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.LinkedFileRequestDto;
import eu.isygoit.dto.common.LinkedFileResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;


/**
 * The interface Linked file api.
 */
public interface LinkedFileApi extends ILinkedFileApi<LinkedFileRequestDto> {

    /**
     * Search by tags response entity.
     *
     * @param tags   the tags
     * @return the response entity
     */
    @Operation(summary = "searchByTags Api",
            description = "searchByTags")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = LinkedFileResponseDto.class))})
    })
    @GetMapping(path = "/searchByTags")
    ResponseEntity<List<LinkedFileResponseDto>> searchByTags(
            @RequestParam(name = RestApiConstants.TAGS) List<String> tags);


    /**
     * Search by original name response entity.
     *
     * @param originalFileName the original file name
     * @return the response entity
     */
    @Operation(summary = "Search file by original name Api",
            description = "Search file by original name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = LinkedFileResponseDto.class))})
    })
    @GetMapping(path = "/searchByOriginalName")
    ResponseEntity<LinkedFileResponseDto> searchByOriginalName(
            @RequestParam(name = RestApiConstants.ORIGINAL_FILE_NAME) String originalFileName);

    /**
     * Rename file response entity.
     *
     * @param code    the code
     * @param newName the new name
     * @return the response entity
     */
    @Operation(summary = "Rename file Api",
            description = "Rename file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = LinkedFileResponseDto.class))})
    })
    @GetMapping(path = "/renameFile")
    ResponseEntity<LinkedFileResponseDto> renameFile(
            @RequestParam(name = RestApiConstants.CODE) String code,
            @RequestParam(name = RestApiConstants.NEW_NAME) String newName);

    /**
     * Search by categories response entity.
     *
     * @param categories the categories
     * @return the response entity
     */
    @Operation(summary = "Search files by category Api",
            description = "Search files by category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = LinkedFileResponseDto.class))})
    })
    @GetMapping(path = "/searchByCategories")
    ResponseEntity<List<LinkedFileResponseDto>> searchByCategories(
            @RequestParam(name = RestApiConstants.CATEGORIES) List<String> categories);
}
