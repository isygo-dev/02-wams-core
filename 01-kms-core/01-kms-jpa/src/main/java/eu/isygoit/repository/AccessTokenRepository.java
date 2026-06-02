package eu.isygoit.repository;

import eu.isygoit.enums.IEnumToken;
import eu.isygoit.model.AccessToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * The interface Access token repository.
 */
public interface AccessTokenRepository extends JpaPagingAndSortingRepository<AccessToken, Long> {


    /**
     * Find first by application and account code ignore case and token and token type and deprecated false order by create date desc optional.
     *
     * @param application the application
     * @param accountCode the account code
     * @param crc16       the crc16
     * @param crc32       the crc32
     * @param tokenType   the token type
     * @return the optional
     */
    Optional<AccessToken> findFirstByApplicationAndAccountCodeIgnoreCaseAndCrc16AndCrc32AndTokenTypeAndDeprecatedFalseOrderByCreateDateDesc(String application, String accountCode, Long crc16, Long crc32, IEnumToken.Types tokenType);

    /**
     * Find first by account code ignore case and token and token type and deprecated false order by create date desc optional.
     *
     * @param accountCode the account code
     * @param crc16       the crc16
     * @param crc32       the crc32
     * @param tokenType   the token type
     * @return the optional
     */
    Optional<AccessToken> findFirstByAccountCodeIgnoreCaseAndCrc16AndCrc32AndTokenTypeAndDeprecatedFalseOrderByCreateDateDesc(String accountCode, Long crc16, Long crc32, IEnumToken.Types tokenType);

    /**
     * Deactivate old tokens int.
     *
     * @param accountCode the account code
     * @param tokenType   the token type
     * @param application the application
     * @return the int
     */
    @Modifying
    @Query("update AccessToken at set at.deprecated = true where at.accountCode= :accountCode and at.tokenType= :tokenType and at.application= :application")
    int deactivateOldTokens(@Param("accountCode") String accountCode,
                            @Param("tokenType") IEnumToken.Types tokenType,
                            @Param("application") String application);
}
