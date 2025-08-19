package eu.isygoit.api;

import eu.isygoit.constants.JwtConstants;
import eu.isygoit.dto.common.ContextRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;

/**
 * The interface Public controller api.
 */
public interface StatisticControllerApi {

    @Operation(summary = "Count Confirmed resumes account",
            description = "Count Confirmed resumes account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Long.class))})
    })
    @GetMapping(path = "/connected/resumes")
    ResponseEntity<Long> getConfirmedResumeAccountsCount(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext);

    @Operation(summary = "Count Confirmed employe account",
            description = "Count Confirmed employee account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Long.class))})
    })
    @GetMapping(path = "/connected/employee")
    ResponseEntity<Long> getConfirmedAccountNumberByEmployee(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT, required = false) ContextRequestDto requestContext);
}
