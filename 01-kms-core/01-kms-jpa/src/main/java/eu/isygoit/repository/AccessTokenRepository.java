package eu.isygoit.repository;

import eu.isygoit.enums.IEnumToken;
import eu.isygoit.model.AccessToken;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * The interface Access token repository.
 */
public interface AccessTokenRepository extends JpaPagingAndSortingTenantAssignableRepository<AccessToken, Long> {


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
    Optional<AccessToken> findFirstByTenantAndApplicationAndAccountCodeIgnoreCaseAndCrc16AndCrc32AndTokenTypeAndDeprecatedFalseOrderByCreateDateDesc(String tenant, String application, String accountCode, Long crc16, Long crc32, IEnumToken.Types tokenType);

    /**
     * Find first by account code ignore case and token and token type and deprecated false order by create date desc optional.
     *
     * @param accountCode the account code
     * @param crc16       the crc16
     * @param crc32       the crc32
     * @param tokenType   the token type
     * @return the optional
     */
    Optional<AccessToken> findFirstByTenantAndAccountCodeIgnoreCaseAndCrc16AndCrc32AndTokenTypeAndDeprecatedFalseOrderByCreateDateDesc(String tenant, String accountCode, Long crc16, Long crc32, IEnumToken.Types tokenType);

    /**
     * Deactivate old tokens int.
     *
     * @param accountCode the account code
     * @param tokenType   the token type
     * @param application the application
     * @return the int
     */
    @Modifying
    @Query("update AccessToken at set at.deprecated = true where at.tenant= :tenant and at.accountCode= :accountCode and at.tokenType= :tokenType and at.application= :application")
    int deactivateOldTokens(@Param("tenant") String tenant,
                            @Param("accountCode") String accountCode,
                            @Param("tokenType") IEnumToken.Types tokenType,
                            @Param("application") String application);
}
