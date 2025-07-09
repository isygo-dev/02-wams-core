package eu.isygoit.repository;


import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;


import eu.isygoit.model.LinkedFile;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * The interface Linked file repository.
 */
@Repository
public interface LinkedFileRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<LinkedFile, Long> {

    /**
     * Find by tenant ignore case and original file name and check cancel false list.
     *
     * @param tenant           the tenant
     * @param originalFileName the origin file name
     * @return the list
     */
    List<LinkedFile> findByTenantIgnoreCaseAndOriginalFileNameOrderByCreateDateDesc(String tenant, String originalFileName);

    /**
     * Find by tenant ignore case and original file name and check cancel false and version optional.
     *
     * @param tenant           the tenant
     * @param originalFileName the origin file name
     * @param version          the version
     * @return the optional
     */
    Optional<LinkedFile> findByTenantIgnoreCaseAndOriginalFileNameAndVersionOrderByCreateDateDesc(String tenant, String originalFileName, Long version);

    /**
     * Find by tenant ignore case and tags containing and check cancel false list.
     *
     * @param tenant the tenant
     * @param tags   the tags
     * @return the list
     */
    List<LinkedFile> findByTenantIgnoreCaseAndTagsContaining(String tenant, String tags);

    /**
     * Find by tenant ignore case and categories in and check cancel false list.
     *
     * @param tenant     the tenant
     * @param categories the categories
     * @return the list
     */
    List<LinkedFile> findByTenantIgnoreCaseAndCategoriesIn(String tenant, List<String> categories);
}
