package eu.isygoit.repository;

import eu.isygoit.model.QrLoginChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.Optional;

public interface QrLoginChallengeRepository extends JpaPagingAndSortingRepository<QrLoginChallenge, Long> {

    Optional<QrLoginChallenge> findByChallengeId(String challengeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select challenge from QrLoginChallenge challenge where challenge.challengeId = :challengeId")
    Optional<QrLoginChallenge> lockByChallengeId(@Param("challengeId") String challengeId);

    void deleteByExpiresAtBefore(Date expiresAt);
}
