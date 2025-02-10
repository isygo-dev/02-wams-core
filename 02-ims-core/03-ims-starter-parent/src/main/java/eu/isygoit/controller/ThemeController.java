package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.constants.RestApiConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.ThemeDto;
import eu.isygoit.dto.extendable.IdentifiableDto;
import eu.isygoit.exception.ThemeNotFoundException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * The type Theme controller.
 */
@Slf4j
@Validated
@RestController
@CtrlDef(handler = ImsExceptionHandler.class, mapper = ThemeMapper.class, minMapper = ThemeMapper.class, service = ThemeService.class)
@RequestMapping(path = "/api/v1/private/theme")
public class ThemeController extends MappedCrudController<Long, Theme, ThemeDto, ThemeDto, ThemeService> {

    private final ApplicationContextService applicationContextService;
    private final IThemeService themeService;
    private final ThemeMapper themeMapper;
    @Autowired
    public ThemeController(ApplicationContextService applicationContextService, IThemeService themeService, ThemeMapper themeMapper) {
        this.applicationContextService = applicationContextService;
        this.themeService = themeService;
        this.themeMapper = themeMapper;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    /**
     * Find theme by account code and domain code response entity.
     *
     * @param requestContext the request context
     * @param domainCode     the domain code
     * @param accountCode    the account code
     * @return the response entity
     */
    @Operation(summary = "findThemeByAccountCodeAndDomainCode Api",
            description = "findThemeByAccountCodeAndDomainCode")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdentifiableDto.class))})
    })
    @GetMapping(path = "/find/{domainCode}/{accountCode}")
    public ResponseEntity<ThemeDto> findThemeByAccountCodeAndDomainCode(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT) RequestContextDto requestContext,
                                                                        @PathVariable(name = RestApiConstants.DOMAIN_CODE) String domainCode,
                                                                        @PathVariable(name = RestApiConstants.ACCOUNT_CODE) String accountCode) {
        try {
            return ResponseFactory.ResponseOk(themeMapper.entityToDto(themeService.findThemeByAccountCodeAndDomainCode(accountCode, domainCode)
                    .orElseThrow(() -> new ThemeNotFoundException("for domain " + domainCode + " and account " + accountCode))));
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
                            schema = @Schema(implementation = IdentifiableDto.class))})
    })
    @PutMapping
    public ResponseEntity<ThemeDto> updateTheme(@RequestAttribute(value = JwtConstants.JWT_USER_CONTEXT) RequestContextDto requestContext,
                                                @Valid @RequestBody ThemeDto theme) {
        try {
            return ResponseFactory.ResponseOk(themeMapper.entityToDto(themeService.updateTheme(themeMapper.dtoToEntity(theme))
                    .orElseThrow(() -> new ThemeNotFoundException("for domain " + theme.getDomainCode() + " and account " + theme.getAccountCode()))));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}

