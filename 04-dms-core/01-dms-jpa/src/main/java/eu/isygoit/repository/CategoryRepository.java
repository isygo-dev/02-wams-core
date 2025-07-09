package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;


import eu.isygoit.model.Category;

import java.util.Optional;

/**
 * The interface Category repository.
 */
public interface CategoryRepository extends JpaPagingAndSortingRepository<Category, Long> {
    /**
     * Find by name optional.
     *
     * @param categoryName the category name
     * @return the optional
     */
    Optional<Category> findByName(String categoryName);
}
