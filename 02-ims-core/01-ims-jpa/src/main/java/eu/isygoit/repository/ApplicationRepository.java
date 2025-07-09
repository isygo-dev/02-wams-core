package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.model.Application;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The interface Application repository.
 */
public interface ApplicationRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<Application, Long> {

    @Modifying
    @Query("UPDATE Application app SET app.adminStatus = :newStatus WHERE app.id = :id")
    int updateAdminStatusById(@Param("id") Long id,
                              @Param("newStatus") IEnumEnabledBinaryStatus.Types newStatus);

    Application findByNameIgnoreCase(String name);
}
