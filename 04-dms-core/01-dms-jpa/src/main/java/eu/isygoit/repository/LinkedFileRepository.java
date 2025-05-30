package eu.isygoit.repository;


import eu.isygoit.model.LinkedFile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * The interface Linked file repository.
 */
@Repository
public interface LinkedFileRepository extends JpaPagingAndSortingDomainAndCodeAssignableRepository<LinkedFile, Long> {

    /**
     * Find by domain ignore case and original file name and check cancel false list.
     *
     * @param domain           the domain
     * @param originalFileName the origin file name
     * @return the list
     */
    List<LinkedFile> findByDomainIgnoreCaseAndOriginalFileNameOrderByCreateDateDesc(String domain, String originalFileName);

    /**
     * Find by domain ignore case and original file name and check cancel false and version optional.
     *
     * @param domain           the domain
     * @param originalFileName the origin file name
     * @param version          the version
     * @return the optional
     */
    Optional<LinkedFile> findByDomainIgnoreCaseAndOriginalFileNameAndVersionOrderByCreateDateDesc(String domain, String originalFileName, Long version);

    /**
     * Find by domain ignore case and tags containing and check cancel false list.
     *
     * @param domain the domain
     * @param tags   the tags
     * @return the list
     */
    List<LinkedFile> findByDomainIgnoreCaseAndTagsContaining(String domain, String tags);

    /**
     * Find by domain ignore case and categories in and check cancel false list.
     *
     * @param domain     the domain
     * @param categories the categories
     * @return the list
     */
    List<LinkedFile> findByDomainIgnoreCaseAndCategoriesIn(String domain, List<String> categories);
}
