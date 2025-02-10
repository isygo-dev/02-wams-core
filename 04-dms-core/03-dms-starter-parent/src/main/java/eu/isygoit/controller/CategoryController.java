package eu.isygoit.controller;

import eu.isygoit.annotation.CtrlDef;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.controller.impl.MappedCrudController;
import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.exception.handler.DmsExceptionHandler;
import eu.isygoit.mapper.CategoryMapper;
import eu.isygoit.model.Category;
import eu.isygoit.service.impl.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Category controller.
 */
@Slf4j
@Validated
@RestController
@CtrlDef(handler = DmsExceptionHandler.class, mapper = CategoryMapper.class, minMapper = CategoryMapper.class, service = CategoryService.class)
@RequestMapping(path = "/api/v1/private/category")
public class CategoryController extends MappedCrudController<Long, Category, CategoryDto, CategoryDto, CategoryService> {

    private final ApplicationContextService applicationContextService;

    @Autowired
    public CategoryController(ApplicationContextService applicationContextService) {
        this.applicationContextService = applicationContextService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }
}
