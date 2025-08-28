package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.PublicControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.config.AppProperties;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.TenantMapper;
import eu.isygoit.service.ITenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Public controller.
 */
@Slf4j
@Validated
@RestController
@InjectExceptionHandler(ImsExceptionHandler.class)
@RequestMapping(path = "/api/v1/public")
public class PublicController extends ControllerExceptionHandler implements PublicControllerApi {

    private final AppProperties appProperties;

    @Autowired
    private ITenantService tenantService;
    @Autowired
    private TenantMapper tenantMapper;

    /**
     * Instantiates a new Public controller.
     *
     * @param appProperties the app properties
     */
    public PublicController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public ResponseEntity<TenantDto> getTenantByName(String tenant) {
        log.info("get tenant by name {}", tenant);
        try {
            return ResponseFactory.responseOk(tenantMapper.entityToDto(tenantService.findByName(tenant)));
        } catch (Throwable e) {
            log.error("<Error>: get by name : {} ", e);
            return getBackExceptionResponse(e);
        }
    }
}
