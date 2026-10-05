package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import eu.isygoit.ui.auth.AuthServiceFacade.Failure;

@Tag("auth-error-banner")
public class AuthErrorBanner extends Component {

    public static final String ID = "auth-error-banner";

    public AuthErrorBanner() {
        getElement().setAttribute("role", "status");
        getElement().setAttribute("aria-live", "polite");
        setVisible(false);
    }

    public void showError(String message, Failure failure) {
        getElement().setText(message);
        getElement().setAttribute("role", "alert");
        getElement().setAttribute("aria-live", "assertive");
        getElement().setAttribute("data-state", stateName(failure));
        addClassName("has-error");
        removeClassName("is-success");
        setVisible(true);
    }

    public void showSuccess(String message) {
        getElement().setText(message);
        getElement().setAttribute("role", "status");
        getElement().setAttribute("aria-live", "polite");
        getElement().removeAttribute("data-state");
        removeClassName("has-error");
        addClassName("is-success");
        setVisible(true);
    }

    public void clear() {
        getElement().setText("");
        getElement().removeAttribute("data-state");
        getElement().setAttribute("role", "status");
        getElement().setAttribute("aria-live", "polite");
        removeClassName("has-error");
        removeClassName("is-success");
        setVisible(false);
    }

    private static String stateName(Failure failure) {
        if (failure == null) {
            return "server-error";
        }
        return switch (failure) {
            case INVALID_CREDENTIALS -> "form-error";
            case INVALID_TOKEN -> "invalid-token";
            case SERVICE_ERROR -> "server-error";
            case OFFLINE -> "offline";
            case RATE_LIMITED -> "rate-limited";
            case LOCKED -> "locked";
            case EXPIRED -> "expired";
            case REGISTRATION_REJECTED -> "server-error";
        };
    }
}
