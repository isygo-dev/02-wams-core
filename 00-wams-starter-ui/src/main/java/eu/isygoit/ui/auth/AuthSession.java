package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import eu.isygoit.util.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AuthSession {

    public void write(String username, String accessToken) {
        Objects.requireNonNull(username, "username");
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("A non-empty access token is required");
        }

        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null) {
            throw new IllegalStateException("No active Vaadin session");
        }

        vaadinSession.setAttribute("user", username);
        vaadinSession.setAttribute("accessToken", accessToken);
        vaadinSession.getSession().setAttribute("user", username);
        vaadinSession.getSession().setAttribute("accessToken", accessToken);
    }

    public void redirect(UI ui, String target) {
        String destination = target != null && SecurityUtils.isSafeInternalPath(target)
                ? target
                : AuthRoutes.LANDING;
        ui.navigate(destination);
    }
}
