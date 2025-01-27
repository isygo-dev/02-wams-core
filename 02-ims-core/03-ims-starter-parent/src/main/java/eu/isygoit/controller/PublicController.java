package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlHandler;
import eu.isygoit.api.PublicControllerApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.config.AppProperties;
import eu.isygoit.dto.data.DomainDto;
import eu.isygoit.exception.DomainNotFoundException;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.mapper.DomainMapper;
import eu.isygoit.service.IDomainService;
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
@CtrlHandler(ImsExceptionHandler.class)
@RequestMapping(path = "/api/v1/public")
public class PublicController extends ControllerExceptionHandler implements PublicControllerApi {

    private final AppProperties appProperties;

    @Autowired
    private IDomainService domainService;
    @Autowired
    private DomainMapper domainMapper;

    /**
     * Instantiates a new Public controller.
     *
     * @param appProperties the app properties
     */
    public PublicController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public ResponseEntity<DomainDto> getDomainByName(String domain) {
        log.info("get domain by name {}", domain);
        try {
            return ResponseFactory.ResponseOk(domainMapper.entityToDto(domainService.findByName(domain)
                    .orElseThrow(() -> new DomainNotFoundException("with domain " + domain))));
        } catch (Throwable e) {
            log.error("<Error>: get by name : {} ", e);
            return getBackExceptionResponse(e);
        }
    }
}
