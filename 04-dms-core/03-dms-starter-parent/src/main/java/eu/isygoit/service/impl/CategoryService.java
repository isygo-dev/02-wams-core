package eu.isygoit.service.impl;

import eu.isygoit.annotation.ServRepo;
import eu.isygoit.com.rest.service.CrudService;
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
@ServRepo(value = CategoryRepository.class)
public class CategoryService extends CrudService<Long, Category, CategoryRepository> implements ICategoryService {

}
