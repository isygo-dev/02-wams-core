package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.data.ThemeDto;
import eu.isygoit.dto.extendable.IdAssignableDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.ThemeMapper;
import eu.isygoit.model.Theme;
import eu.isygoit.service.IThemeService;
import eu.isygoit.service.impl.ThemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * The type Theme controller.
 */
@Slf4j
@Validated
@RestController
@InjectMapperAndService(handler = ImsExceptionHandler.class, mapper = ThemeMapper.class, minMapper = ThemeMapper.class, service = ThemeService.class)
@RequestMapping(path = "/api/v1/private/theme")
public class ThemeController extends MappedCrudController<Long, Theme, ThemeDto, ThemeDto, ThemeService> {

    @Autowired
    private IThemeService themeService;
    @Autowired
    private ThemeMapper themeMapper;

    /**
     * Find theme by account code and tenant code response entity.
     *
     * @param requestContext the request context
     * @param tenantCode     the tenant code
     * @param accountCode    the account code
     * @return the response entity
     */
    @Operation(summary = "findThemeByAccountCodeAndDomainCode Api",
            description = "findThemeByAccountCodeAndDomainCode")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @GetMapping(path = "/find/{tenantCode}/{accountCode}")
    public ResponseEntity<ThemeDto> findThemeByAccountCodeAndDomainCode(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT) ContextRequestDto requestContext,
                                                                        @PathVariable(name = RestApiConstants.TENANT_CODE) String tenantCode,
                                                                        @PathVariable(name = RestApiConstants.ACCOUNT_CODE) String accountCode) {
        try {
            Theme theme = themeService.findThemeByAccountCodeAndDomainCode(accountCode, tenantCode);

            if (theme != null) {
                ThemeDto themeDto = themeMapper.entityToDto(theme);
                return new ResponseEntity<>(themeDto, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    /**
     * Update theme response entity.
     *
     * @param requestContext the request context
     * @param theme          the theme
     * @return the response entity
     */
    @Operation(summary = "updateTheme Api",
            description = "updateTheme")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @PutMapping
    public ResponseEntity<ThemeDto> updateTheme(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT) ContextRequestDto requestContext,
                                                @Valid @RequestBody ThemeDto theme) {
        try {
            Theme themeResult = themeService.updateTheme(themeMapper.dtoToEntity(theme));
            return new ResponseEntity<>(themeMapper.entityToDto(themeResult), HttpStatus.OK);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

