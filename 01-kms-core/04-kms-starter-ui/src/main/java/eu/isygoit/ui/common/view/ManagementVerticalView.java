package eu.isygoit.ui.common.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import eu.isygoit.util.SecurityUtils;

public class ManagementVerticalView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public final void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.isUserLoggedIn()) {
            String currentPath = event.getLocation().getPathWithQueryParameters(); // Better: includes query params if any
            SecurityUtils.storeRedirect(currentPath);
            UI.getCurrent().getPage().setLocation("login?redirect=" + java.net.URLEncoder.encode(currentPath, java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}