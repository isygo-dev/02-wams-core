package eu.isygoit.ui.common.view;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import eu.isygoit.util.SecurityUtils;

import java.util.HashMap;
import java.util.Map;

public class ManagementCompositeVerticalView extends Composite<VerticalLayout> implements BeforeEnterObserver, AfterNavigationObserver {

    @Override
    public final void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.isUserLoggedIn()) {
            String currentPath = event.getLocation().getPath();
            SecurityUtils.storeRedirect(currentPath);
            Map<String, String> params = new HashMap<>();
            params.put("redirect", currentPath);
            event.forwardTo("login", QueryParameters.simple(params));
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String currentPath = event.getLocation().getPathWithQueryParameters(); // Better: includes query params if any
        SecurityUtils.storeRedirect(currentPath);
    }
}
