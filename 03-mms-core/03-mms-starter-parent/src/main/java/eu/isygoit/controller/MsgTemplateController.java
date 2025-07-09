package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.dto.extendable.IdAssignableDto;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.exception.handler.MmsExceptionHandler;
import eu.isygoit.mapper.MsgTemplateMapper;
import eu.isygoit.model.MsgTemplate;
import eu.isygoit.service.impl.MsgTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * The type Template controller.
 */
@Slf4j
@Validated
@RestController
@RequestMapping(path = "/api/v1/private/mail/template")
@InjectMapperAndService(handler = MmsExceptionHandler.class, mapper = MsgTemplateMapper.class, minMapper = MsgTemplateMapper.class, service = MsgTemplateService.class)
public class MsgTemplateController extends MappedCrudTenantController<Long, MsgTemplate, MsgTemplateDto, MsgTemplateDto, MsgTemplateService> {

    /**
     * Get template names response entity.
     *
     * @return the response entity
     */
    @Operation(summary = "get Template Names Api",
            description = "get Template Names")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Api executed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IdAssignableDto.class))})
    })
    @GetMapping(path = "/names")
    public ResponseEntity<List<String>> getTemplateNames() {
        try {
            return ResponseFactory.responseOk(Arrays.stream(Arrays.stream(IEnumEmailTemplate.Types.class.getEnumConstants()).map(Enum::name)
                            .toArray(String[]::new))
                    .toList());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}
