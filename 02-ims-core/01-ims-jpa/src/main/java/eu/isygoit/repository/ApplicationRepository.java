package eu.isygoit.repository;

import eu.isygoit.enums.IEnumBinaryStatus;
import eu.isygoit.model.Application;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * The interface Application repository.
 */
public interface ApplicationRepository extends JpaPagingAndSortingSAASCodifiableRepository<Application, Long> {

    @Modifying
    @Query("UPDATE Application app SET app.adminStatus = :newStatus WHERE app.id = :id")
    int updateAdminStatusById(@Param("id") Long id,
                              @Param("newStatus") IEnumBinaryStatus.Types newStatus);

    Optional<Application> findByNameIgnoreCase(String name);
}
