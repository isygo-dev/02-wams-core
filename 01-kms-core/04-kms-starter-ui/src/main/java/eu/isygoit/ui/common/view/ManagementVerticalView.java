package eu.isygoit.ui.common.view;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import eu.isygoit.util.SecurityUtils;

import java.util.HashMap;
import java.util.Map;

public class ManagementVerticalView extends VerticalLayout implements BeforeEnterObserver {

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
}
