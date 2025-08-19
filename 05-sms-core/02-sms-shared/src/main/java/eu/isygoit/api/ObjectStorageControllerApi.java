package eu.isygoit.api;

import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.dto.data.FileTagsDto;
import eu.isygoit.dto.extendable.IdAssignableDto;
import eu.isygoit.enums.IEnumLogicalOperator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


/**
 * The interface Object storage controller api.
 */
public interface ObjectStorageControllerApi {

    /**
     * Upload response entity.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param bucketName     the bucket name
     * @param path           the path
     * @param fileName       the file name
     * @param tags           the tags
     * @param file           the file
     * @return the response entity
     */
    @Operation(summary = "Upload file Api",
            description = "Upload file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Object> upload(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                  @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                  @RequestParam(name = RestApiConstants.BUCKET_NAME) String bucketName,
                                  @RequestParam(name = RestApiConstants.PATH) String path,
                                  @RequestParam(name = RestApiConstants.FILE_NAME) String fileName,
                                  @RequestParam(name = RestApiConstants.TAGS) List<String> tags,
                                  @RequestPart(name = RestApiConstants.FILE) MultipartFile file);

    /**
     * Download resource.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param bucketName     the bucket name
     * @param path           the path
     * @param fileName       the file name
     * @param versionID      the version id
     * @return the resource
     */
    @Operation(summary = "Download file Api",
            description = "Download file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Resource.class))})
    })
    @GetMapping(path = "/download")
    ResponseEntity<Resource> download(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                      @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                      @RequestParam(name = RestApiConstants.BUCKET_NAME) String bucketName,
                                      @RequestParam(name = RestApiConstants.PATH) String path,
                                      @RequestParam(name = RestApiConstants.FILE_NAME) String fileName,
                                      @RequestParam(name = RestApiConstants.VERSION_ID) String versionID);

    /**
     * Delete response entity.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param bucketName     the bucket name
     * @param path           the path
     * @param fileName       the file name
     * @return the response entity
     */
    @Operation(summary = "xxx Api",
            description = "xxx")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @DeleteMapping(path = "/delete")
    ResponseEntity<Object> delete(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                  @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                  @RequestParam(name = RestApiConstants.BUCKET_NAME) String bucketName,
                                  @RequestParam(name = RestApiConstants.PATH) String path,
                                  @RequestParam(name = RestApiConstants.FILE_NAME) String fileName);


    /**
     * Gets objects.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param bucketName     the bucket name
     * @return the objects
     */
    @Operation(summary = "xxx Api",
            description = "xxx")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @GetMapping(path = "/getObjects")
    ResponseEntity<Object> getObjects(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                      @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                      @RequestParam(name = RestApiConstants.BUCKET_NAME) String bucketName);


    /**
     * Filter objects response entity.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param bucketName     the bucket name
     * @param tags           the tags
     * @param condition      the condition
     * @return the response entity
     */
    @Operation(summary = "xxx Api",
            description = "xxx")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @GetMapping(path = "/filterObjects")
    ResponseEntity<Object> filterObjects(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                         @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                         @RequestParam(name = RestApiConstants.BUCKET_NAME) String bucketName,
                                         @RequestParam(name = RestApiConstants.TAGS) String tags,
                                         @RequestParam(name = RestApiConstants.CONDITION) IEnumLogicalOperator.Types condition);

    /**
     * Update tags response entity.
     *
     * @param fileTags the file tags
     * @return the response entity
     */
    @Operation(summary = "xxx Api",
            description = "xxx")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @PutMapping(path = "/updateTags")
    ResponseEntity<Object> updateTags(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                      @Valid @RequestBody FileTagsDto fileTags);

    /**
     * Delete objects response entity.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param bucketName     the bucket name
     * @param files          the files
     * @return the response entity
     */
    @Operation(summary = "xxx Api",
            description = "xxx")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @DeleteMapping(path = "/deleteObjects")
    ResponseEntity<Object> deleteObjects(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                         @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                         @RequestParam(name = RestApiConstants.BUCKET_NAME) String bucketName,
                                         @RequestParam(name = RestApiConstants.FILES) String files);

    /**
     * Save bucket response entity.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param bucketName     the bucket name
     * @return the response entity
     */
    @Operation(summary = "xxx Api",
            description = "xxx")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @PostMapping(path = "/saveBucket")
    ResponseEntity<Object> saveBucket(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                      @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                      @RequestParam(name = RestApiConstants.BUCKET_NAME) String bucketName);

    /**
     * Sets versioning bucket.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param bucketName     the bucket name
     * @param status         the status
     * @return the versioning bucket
     */
    @Operation(summary = "xxx Api",
            description = "xxx")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @PostMapping(path = "/setVersioningBucket")
    ResponseEntity<Object> setVersioningBucket(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                               @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                               @RequestParam(name = RestApiConstants.BUCKET_NAME) String bucketName,
                                               @RequestParam(name = RestApiConstants.STATUS) boolean status);


    /**
     * Gets buckets.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @return the buckets
     */
    @Operation(summary = "xxx Api",
            description = "xxx")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = BucketDto.class))})
    })
    @GetMapping(path = "/getBuckets")
    ResponseEntity<List<BucketDto>> getBuckets(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                               @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant);


    /**
     * Delete bucket response entity.
     *
     * @param requestContext the request context
     * @param tenant         the tenant
     * @param bucketName     the bucket name
     * @return the response entity
     */
    @Operation(summary = "xxx Api",
            description = "xxx")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @DeleteMapping(path = "/deleteBucket")
    ResponseEntity<Object> deleteBucket(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext,
                                        @RequestParam(name = RestApiConstants.TENANT_NAME) String tenant,
                                        @RequestParam(name = RestApiConstants.BUCKET_NAME) String bucketName);
}