package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.dto.request.AuthenticationContextRequest;
import eu.isygoit.dto.response.UserContext;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.PublicAuthService;
import eu.isygoit.util.SecurityUtils;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@UIScope
@Route(value = "login")
@PageTitle("Sign In")
@PermitAll
public class LoginView extends BaseLoginView {

    private final TextField tenantField = new TextField(I18n.t("auth.login.field.tenant.label"));
    private final TextField usernameField = new TextField(I18n.t("auth.login.field.username.label"));
    private final Button continueButton = new Button(I18n.t("auth.login.button.continue"), VaadinIcon.ARROW_RIGHT.create());
    private final Div errorBanner = createErrorBanner();

    @Autowired
    private PublicAuthService authService;

    public LoginView() {
        configureAsAuthPage("login-view");

        tenantField.setWidthFull();
        tenantField.setPlaceholder(I18n.t("auth.login.field.tenant.placeholder"));
        tenantField.setPrefixComponent(VaadinIcon.BUILDING.create());

        usernameField.setWidthFull();
        usernameField.setPlaceholder(I18n.t("auth.login.field.username.placeholder"));
        usernameField.setPrefixComponent(VaadinIcon.USER.create());

        continueButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        continueButton.addClassName("wams-auth-primary-btn");
        continueButton.addClickListener(e -> handleContinue());
        continueButton.addClickShortcut(com.vaadin.flow.component.Key.ENTER);

        Anchor registerLink = createLink("register", I18n.t("auth.login.link.register"));

        var card = createCard();
        card.add(createBrand(I18n.t("auth.common.brand.title"), I18n.t("auth.common.brand.subtitle")),
                tenantField, usernameField, continueButton, errorBanner, registerLink, createFooter());

        add(card);
    }

    private void handleContinue() {
        String tenant = tenantField.getValue().trim().toLowerCase();
        String username = usernameField.getValue().trim().toLowerCase();

        if (tenant.isEmpty() || username.isEmpty()) {
            showError(errorBanner, I18n.t("auth.login.error.requiredFields"));
            return;
        }

        AuthenticationContextRequest request = AuthenticationContextRequest.builder()
                .tenant(tenant)
                .userName(username)
                .build();

        try {
            ResponseEntity<UserContext> response = authService.resolveAuthContext(request);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                UserContext userContext = response.getBody();
                IEnumAuth.Types authType = userContext.getAuthTypeMode();

                // Ensure redirectTarget is set (fallback to current query param)
                if (redirectTarget == null) {
                    redirectTarget = UI.getCurrent().getInternals()
                            .getActiveViewLocation()
                            .getQueryParameters()
                            .getSingleParameter("redirect")
                            .filter(SecurityUtils::isSafeInternalPath)
                            .orElse(null);
                }

                // Build base query string with tenant and username
                StringBuilder query = new StringBuilder("?tenant=" + tenant + "&username=" + username);
                // Append redirect if present
                if (redirectTarget != null && SecurityUtils.isSafeInternalPath(redirectTarget)) {
                    query.append("&redirect=").append(redirectTarget);
                }

                Integer otpLength = null;
                String targetView = null;
                switch (authType) {
                    case PWD:
                        targetView = "login/password";
                        break;
                    case OTP:
                        otpLength = userContext.getOtpLength();
                        if (otpLength != null) {
                            query.append("&otpLength=").append(otpLength);
                        }
                        targetView = "login/otp";
                        break;
                    case QRC:
                        targetView = "login/qr";
                        break;
                    case TOKEN:
                        Notification.show(I18n.t("auth.login.warning.tokenUnsupported"), 3000,
                                Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                        return;
                    default:
                        showError(errorBanner, I18n.t("auth.login.error.unsupportedAuthType", authType));
                        return;
                }

                if (targetView != null) {
                    UI.getCurrent().navigate(targetView + query.toString());
                }
            } else {
                showError(errorBanner, I18n.t("auth.login.error.authMethodUnavailable"));
            }
        } catch (Exception ex) {
            showError(errorBanner, I18n.t("auth.login.error.serviceUnavailable"));
        }
    }

    @Override
    protected void onBeforeEnter(BeforeEnterEvent event) {
        // Clear error on fresh login view
        errorBanner.setVisible(false);
        errorBanner.setText("");
    }
}
