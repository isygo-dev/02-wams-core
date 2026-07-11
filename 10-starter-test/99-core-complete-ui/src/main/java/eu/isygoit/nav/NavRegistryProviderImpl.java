package eu.isygoit.nav;

import eu.isygoit.ui.common.component.INavRegistry;
import eu.isygoit.ui.common.nav.NavRegistryProvider;
import org.springframework.stereotype.Component;

/**
 * Simple implementation of NavRegistryProvider.
 */
@Component
public class NavRegistryProviderImpl implements NavRegistryProvider {

    @Override
    public INavRegistry getNavRegistry() {
        return new NavRegistry();
    }
}