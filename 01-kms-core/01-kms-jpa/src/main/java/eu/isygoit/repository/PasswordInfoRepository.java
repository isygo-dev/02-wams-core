package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.model.PasswordInfo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * The interface Password info repository.
 */
public interface PasswordInfoRepository extends JpaPagingAndSortingRepository<PasswordInfo, Long> {
    /**
     * Gets password infos by user id and valid status.
     *
     * @param userId the user id
     * @return the password infos by user id and valid status
     */
    @Query("SELECT p FROM PasswordInfo p WHERE p.userId = :userId AND p.status = 'VALID'")
    List<PasswordInfo> getPasswordInfosByUserIdAndValidStatus(@Param("userId") Long userId);

    /**
     * Find by status and auth type list.
     *
     * @param status   the status
     * @param authType the auth type
     * @return the list
     */
    List<PasswordInfo> findByStatusAndAuthType(IEnumPasswordStatus.Types status, IEnumAuth.Types authType);

    /**
     * Find by user id and auth type order by create date desc list.
     *
     * @param userId   the user id
     * @param authType the auth type
     * @return the list
     */
    List<PasswordInfo> findByUserIdAndAuthTypeOrderByCreateDateDesc(Long userId, IEnumAuth.Types authType);

    /**
     * Deactivate old passwords int.
     *
     * @param userId   the user id
     * @param authType the auth type
     * @return the int
     */
    @Modifying
    @Query("update PasswordInfo pi set pi.status = 'DEPRECATED' where pi.userId= :userId and pi.authType= :authType")
    int deactivateOldPasswords(@Param("userId") Long userId,
                               @Param("authType") IEnumAuth.Types authType);
}
