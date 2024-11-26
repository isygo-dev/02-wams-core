package eu.isygoit.repository;

import eu.isygoit.enums.IEnumAppToken;
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
     * Find by token and account code ignore case optional.
     *
     * @param token       the token
     * @param accountCode the account code
     * @return the optional
     */
    Optional<AccessToken> findByTokenAndAccountCodeIgnoreCase(String token, String accountCode);

    /**
     * Find first by application and account code ignore case and token and token type and deprecated false order by create date desc optional.
     *
     * @param application the application
     * @param accountCode the account code
     * @param token       the token
     * @param tokenType   the token type
     * @return the optional
     */
    Optional<AccessToken> findFirstByApplicationAndAccountCodeIgnoreCaseAndTokenAndTokenTypeAndDeprecatedFalseOrderByCreateDateDesc(String application, String accountCode, String token, IEnumAppToken.Types tokenType);

    /**
     * Find first by account code ignore case and token and token type and deprecated false order by create date desc optional.
     *
     * @param accountCode the account code
     * @param token       the token
     * @param tokenType   the token type
     * @return the optional
     */
    Optional<AccessToken> findFirstByAccountCodeIgnoreCaseAndTokenAndTokenTypeAndDeprecatedFalseOrderByCreateDateDesc(String accountCode, String token, IEnumAppToken.Types tokenType);

    /**
     * Delete by token type and account code ignore case.
     *
     * @param tokenType   the token type
     * @param accountCode the account code
     */
    @Modifying
    void deleteByTokenTypeAndAccountCodeIgnoreCase(IEnumAppToken.Types tokenType, String accountCode);

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
                            @Param("tokenType") IEnumAppToken.Types tokenType,
                            @Param("application") String application);
}
