package eu.isygoit.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import eu.isygoit.util.SecurityUtils;
import jakarta.annotation.security.PermitAll;

import java.util.Optional;

@VaadinSessionScope //(or UIScope)
@Route("")
@PermitAll
public class IndexView extends Div implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("user") != null) {
            // Try to get the redirect target from query parameters.
            Optional<String> redirectOpt = event.getLocation()
                    .getQueryParameters()
                    .getSingleParameter("redirect");

            // If a redirect target exists and is a valid internal path, use it.
            // Otherwise, fall back to a default view.
            String target = redirectOpt
                    .filter(SecurityUtils::isSafeInternalPath)
                    .orElse("kms");

            event.forwardTo(target);
        } else {
            event.forwardTo("login");
        }
    }
}