package eu.isygoit.repository;

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

    /**
     * Atomically marks a still-VALID one-time code as used (DEPRECATED). Returns 1 for the single caller that wins,
     * 0 if the code was already used, locked or replaced.
     */
    @Modifying
    @Query("update PasswordInfo pi set pi.status = 'DEPRECATED' where pi.id = :id and pi.status = 'VALID'")
    int consumeIfValid(@Param("id") Long id);

    /**
     * Atomically counts one failed attempt (no lost updates under concurrent guessing).
     */
    @Modifying
    @Query("update PasswordInfo pi set pi.failedAttempts = pi.failedAttempts + 1 where pi.id = :id")
    int incrementFailedAttempts(@Param("id") Long id);

    /**
     * Reads the stored attempt counter straight from the database (not from the persistence context).
     */
    @Query("select pi.failedAttempts from PasswordInfo pi where pi.id = :id")
    int findFailedAttempts(@Param("id") Long id);

    /**
     * Locks a still-VALID credential once its attempt budget is exhausted.
     */
    @Modifying
    @Query("update PasswordInfo pi set pi.status = 'LOCKED' where pi.id = :id and pi.status = 'VALID'")
    int lockIfValid(@Param("id") Long id);
}
