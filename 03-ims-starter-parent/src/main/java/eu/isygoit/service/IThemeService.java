package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.model.Theme;


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
    Theme findThemeByAccountCodeAndDomainCode(String accountCode, String domainCode);

    /**
     * Update theme theme.
     *
     * @param theme the theme
     * @return the theme
     */
    Theme updateTheme(Theme theme);
}
