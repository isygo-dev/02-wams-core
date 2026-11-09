package eu.isygoit.ui.nav;

import eu.isygoit.ui.common.component.INavRegistry;
import eu.isygoit.ui.common.nav.NavRegistryProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Simple implementation of NavRegistryProvider.
 */
@Component
@Primary
public class NavRegistryProviderImpl implements NavRegistryProvider {

    @Override
    public INavRegistry getNavRegistry() {
        return new NavRegistry();
    }
}