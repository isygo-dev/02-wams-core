package eu.isygoit.ui.common.view;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import eu.isygoit.util.SecurityUtils;

public class ManagementVerticalView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public final void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.isUserLoggedIn()) {
            String currentPath = event.getLocation().getPath();
            SecurityUtils.storeRedirect(currentPath);
            event.forwardTo("login?redirect=" + currentPath);
        }
    }
}