package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.model.Theme;

import java.util.Optional;


/**
 * The interface Theme service.
 */
public interface IThemeService extends ICrudServiceMethod<Long, Theme> {

    /**
     * Find theme by account code and domain code theme.
     *
     * @param accountCode the account code
     * @param domainCode  the domain code
     * @return the theme
     */
    Optional<Theme> findThemeByAccountCodeAndDomainCode(String accountCode, String domainCode);

    /**
     * Update theme theme.
     *
     * @param theme the theme
     * @return the theme
     */
    Optional<Theme> updateTheme(Theme theme);
}
