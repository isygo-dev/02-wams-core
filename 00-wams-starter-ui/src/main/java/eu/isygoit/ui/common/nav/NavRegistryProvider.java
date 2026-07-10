package eu.isygoit.ui.common.nav;

import eu.isygoit.ui.common.component.INavRegistry;

/**
 * Simple provider interface for navigation registry.
 */
public interface NavRegistryProvider {

    /**
     * Get the navigation registry.
     *
     * @return the NavRegistry class
     */
    INavRegistry getNavRegistry();
}