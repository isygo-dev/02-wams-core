package eu.isygoit.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

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
    ResponseEntity<Long> getConfirmedResumeAccountsCount();

    @Operation(summary = "Count Confirmed employe account",
            description = "Count Confirmed employee account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Long.class))})
    })
    @GetMapping(path = "/connected/employee")
    ResponseEntity<Long> getConfirmedAccountNumberByEmployee();
}
