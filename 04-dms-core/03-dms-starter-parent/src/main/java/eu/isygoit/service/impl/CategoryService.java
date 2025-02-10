package eu.isygoit.service.impl;

import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.service.impl.CrudService;
import eu.isygoit.model.Category;
import eu.isygoit.repository.CategoryRepository;
import eu.isygoit.service.ICategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Category service.
 */
@Service
@Transactional
@SrvRepo(value = CategoryRepository.class)
public class CategoryService extends CrudService<Long, Category, CategoryRepository> implements ICategoryService {

    private final ApplicationContextService applicationContextService;

    public CategoryService(ApplicationContextService applicationContextService) {
        this.applicationContextService = applicationContextService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }
}
