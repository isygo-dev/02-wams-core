package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;


import eu.isygoit.model.Property;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * The interface Property repository.
 */
@Repository
public interface PropertyRepository extends JpaPagingAndSortingRepository<Property, Long> {

    /**
     * Find by account and gui name and name optional.
     *
     * @param accountCode the account code
     * @param guiName     the gui name
     * @param name        the name
     * @return the optional
     */
    @Query(name = "property.findByAccountAndGuiNameAndName", nativeQuery = true)
    Optional<Property> findByAccountAndGuiNameAndName(@Param("accountCode") String accountCode,
                                                      @Param("guiName") String guiName,
                                                      @Param("name") String name);

    /**
     * Find by account and gui name list.
     *
     * @param accountCode the account code
     * @param guiName     the gui name
     * @return the list
     */
    @Query(name = "property.findByAccountAndGuiName", nativeQuery = true)
    List<Property> findByAccountAndGuiName(@Param("accountCode") String accountCode,
                                           @Param("guiName") String guiName);
}
