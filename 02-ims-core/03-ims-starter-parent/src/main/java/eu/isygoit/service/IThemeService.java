package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethods;
import eu.isygoit.model.Theme;


/**
 * The interface Theme service.
 */
public interface IThemeService extends ICrudServiceMethods<Long, Theme> {

    /**
     * Find theme by account code and tenant code theme.
     *
     * @param accountCode the account code
     * @param tenantCode  the tenant code
     * @return the theme
     */
    Theme findThemeByAccountCodeAndDomainCode(String accountCode, String tenantCode);

    /**
     * Update theme theme.
     *
     * @param theme the theme
     * @return the theme
     */
    Theme updateTheme(Theme theme);
}
