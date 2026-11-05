package eu.isygoit.ui.common.view;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import eu.isygoit.util.SecurityUtils;

public class ManagementCompositeVerticalView extends Composite<VerticalLayout> implements BeforeEnterObserver, HasDynamicTitle {

    protected ManagementCompositeVerticalView() {
        getContent().addClassName("management-view");
    }

    @Override
    public String getPageTitle() {
        return ViewTitleUtils.humanize(getClass());
    }

    @Override
    public final void beforeEnter(BeforeEnterEvent event) {
        String currentPath = event.getLocation().getPathWithQueryParameters(); // Better: includes query params if any
        if (!SecurityUtils.isUserLoggedIn()) {
            UI.getCurrent().getPage().setLocation("login?redirect=" + java.net.URLEncoder.encode(currentPath, java.nio.charset.StandardCharsets.UTF_8));
        } else {
            SecurityUtils.storeRedirect(currentPath);
        }
    }
}
