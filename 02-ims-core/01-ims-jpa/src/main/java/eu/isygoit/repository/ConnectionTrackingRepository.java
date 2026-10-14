package eu.isygoit.repository;

import eu.isygoit.model.ConnectionTracking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

/**
 * The interface Connection tracking repository.
 */
public interface ConnectionTrackingRepository extends JpaPagingAndSortingRepository<ConnectionTracking, Long> {

    /**
     * One page of an account's logins, newest first. The ordering is part of the query (and the pageable must be
     * unsorted): Spring Data would otherwise prefix a sort property with the wrong alias.
     */
    @Query(value = "select ct from Account a join a.connectionTracking ct where a.id = :accountId "
            + "order by ct.loginDate desc, ct.id desc",
            countQuery = "select count(ct) from Account a join a.connectionTracking ct where a.id = :accountId")
    Page<ConnectionTracking> findPageByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    /**
     * Login timestamps since a date, newest first; the pageable only caps the number of rows.
     */
    @Query("select ct.loginDate from Account a join a.connectionTracking ct "
            + "where a.id = :accountId and ct.loginDate >= :since order by ct.loginDate desc")
    List<Date> findLoginDatesSince(@Param("accountId") Long accountId, @Param("since") Date since, Pageable pageable);
}
