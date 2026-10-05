package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.shared.communication.PushMode;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.auth.AuthServiceFacade.Failure;
import eu.isygoit.util.SecurityUtils;

@CssImport("./themes/isygo/auth/auth.scss")
public abstract class BaseLoginView extends Div implements BeforeEnterObserver, HasDynamicTitle {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(BaseLoginView.class);

    protected final AuthErrorBanner errorBanner = new AuthErrorBanner();
    protected String redirectTarget;

    private final String pageTitleKey;
    private final AuthLayout authLayout = new AuthLayout();

    protected BaseLoginView(String pageTitleKey) {
        this.pageTitleKey = pageTitleKey;
        errorBanner.setId(AuthErrorBanner.ID);
        getElement().appendChild(authLayout.getElement());
    }

    @Override
    public String getPageTitle() {
        return I18n.t(pageTitleKey);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getUI().getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        redirectTarget = SecurityUtils.consumeRedirect();
        if (redirectTarget == null) {
            redirectTarget = event.getLocation()
                    .getQueryParameters()
                    .getSingleParameter("redirect")
                    .filter(SecurityUtils::isSafeInternalPath)
                    .orElse(null);
        }

        if (SecurityUtils.isUserLoggedIn()) {
            String destination = redirectTarget != null
                    && SecurityUtils.isSafeInternalPath(redirectTarget)
                    ? redirectTarget
                    : AuthRoutes.LANDING;
            event.forwardTo(destination);
            return;
        }

        onBeforeEnter(event);
        getElement().executeJs(
                "this.querySelector('auth-brand h1')?.focus({preventScroll: true})");
    }

    protected void onBeforeEnter(BeforeEnterEvent event) {
    }

    protected void showError(String message, Failure failure) {
        errorBanner.showError(message, failure);
    }

    protected void showError(String message, Failure failure, Component focusTarget) {
        showError(message, failure);
        if (focusTarget != null) {
            focusTarget.getElement().executeJs("this.focus()");
        }
    }

    protected void clearError() {
        errorBanner.clear();
    }

    protected void describeErrors(Component field) {
        field.getElement().setAttribute("aria-describedby", AuthErrorBanner.ID);
    }

    protected final void setButtonLoading(Button button, boolean loading) {
        button.setEnabled(!loading);
        button.getElement().setAttribute("aria-busy", Boolean.toString(loading));
        if (loading) {
            button.addClassName("is-loading");
        } else {
            button.removeClassName("is-loading");
        }
    }

    protected void addToCard(Component... components) {
        authLayout.addToCard(components);
    }

    protected void setWideLayout(boolean wide) {
        authLayout.setWideLayout(wide);
    }

    protected String failureMessage(Failure failure, String invalidCredentialsKey, String fallbackKey) {
        if (failure == null) {
            return I18n.t(fallbackKey);
        }
        return switch (failure) {
            case INVALID_CREDENTIALS -> I18n.t(invalidCredentialsKey);
            case INVALID_TOKEN -> I18n.t("auth.qrcode.status.authFailed");
            case RATE_LIMITED -> I18n.t("auth.common.error.rateLimited");
            case LOCKED -> I18n.t("auth.common.error.locked");
            case EXPIRED -> I18n.t("auth.common.error.expired");
            case OFFLINE -> I18n.t("auth.common.error.offline");
            case SERVICE_ERROR -> I18n.t(fallbackKey);
            case REGISTRATION_REJECTED -> I18n.t(fallbackKey);
        };
    }

    protected void showWelcome(String username) {
        Notification.show(I18n.t("auth.common.notification.welcome", username), 2000,
                        Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    protected void reportUnexpectedFailure(Throwable failure) {
        LOG.warn("Authentication service request failed ({})",
                failure.getClass().getSimpleName());
    }
}
