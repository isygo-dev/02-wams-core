package eu.isygoit.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import eu.isygoit.util.SecurityUtils;
import jakarta.annotation.security.PermitAll;

@Route("")
@PermitAll
public class IndexView extends Div implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (SecurityUtils.isUserLoggedIn()) {
            String redirect = SecurityUtils.consumeRedirect();
            if (redirect == null) {
                redirect = event.getLocation()
                        .getQueryParameters()
                        .getSingleParameter("redirect")
                        .filter(SecurityUtils::isSafeInternalPath)
                        .orElse(null);
            }
            if (redirect != null) {
                event.forwardTo(redirect);
            } else {
                event.forwardTo("landing");
            }
        } else {
            event.forwardTo("login");
        }
    }
}