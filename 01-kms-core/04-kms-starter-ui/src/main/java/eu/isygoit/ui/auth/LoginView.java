package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.request.AccountAuthTypeRequest;
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
    private final Div errorContainer = new Div();

    @Autowired
    private PublicAuthService authService;

    public LoginView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("login-view");

        // Brand
        Div brand = new Div();
        brand.addClassName("brand");
        Avatar logo = new Avatar("KMS/IMS");
        logo.setColorIndex(0);
        logo.setWidth("64px");
        logo.setHeight("64px");
        H2 title = new H2(I18n.t("auth.common.brand.title"));
        title.addClassName(LumoUtility.FontWeight.BOLD);
        title.addClassName(LumoUtility.Margin.NONE);
        Paragraph subtitle = new Paragraph(I18n.t("auth.common.brand.subtitle"));
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.addClassName(LumoUtility.FontSize.SMALL);
        brand.add(logo, title, subtitle);

        // Tenant + Login fields
        tenantField.setWidthFull();
        tenantField.setPlaceholder(I18n.t("auth.login.field.tenant.placeholder"));
        tenantField.setPrefixComponent(VaadinIcon.BUILDING.create());
        usernameField.setWidthFull();
        usernameField.setPlaceholder(I18n.t("auth.login.field.username.placeholder"));
        usernameField.setPrefixComponent(VaadinIcon.USER.create());

        // Continue button
        continueButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        continueButton.setWidthFull();
        continueButton.addClickListener(e -> handleContinue());

        // Error container
        errorContainer.addClassName("error-container");
        errorContainer.setVisible(false);

        // Register link
        Anchor registerLink = new Anchor("register", I18n.t("auth.login.link.register"));
        registerLink.addClassName("register-link");

        // Footer
        Paragraph footer = new Paragraph(I18n.t("auth.common.footer"));
        footer.addClassName(LumoUtility.TextColor.TERTIARY);
        footer.addClassName(LumoUtility.FontSize.XXSMALL);
        footer.addClassName(LumoUtility.Margin.Top.MEDIUM);

        // Main wrapper
        VerticalLayout wrapper = new VerticalLayout(brand, tenantField, usernameField,
                continueButton, errorContainer, registerLink, footer);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.setMaxWidth("400px");
        wrapper.setWidthFull();
        wrapper.setPadding(true);
        wrapper.setSpacing(true);
        wrapper.addClassName("login-wrapper");

        add(wrapper);
    }

    private void handleContinue() {
        String tenant = tenantField.getValue().trim().toLowerCase();
        String username = usernameField.getValue().trim().toLowerCase();

        if (tenant.isEmpty() || username.isEmpty()) {
            String message = I18n.t("auth.login.error.requiredFields");
            showError(message);
            errorContainer.setText(message);
            errorContainer.setVisible(true);
            return;
        }

        AccountAuthTypeRequest request = AccountAuthTypeRequest.builder()
                .tenant(tenant)
                .userName(username)
                .build();

        try {
            ResponseEntity<UserContext> response = authService.getAuthenticationType(request);
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
                        String unsupportedMessage = I18n.t("auth.login.error.unsupportedAuthType", authType);
                        showError(unsupportedMessage);
                        errorContainer.setText(unsupportedMessage);
                        errorContainer.setVisible(true);
                        return;
                }

                if (targetView != null) {
                    UI.getCurrent().navigate(targetView + query.toString());
                }
            } else {
                String message = I18n.t("auth.login.error.authMethodUnavailable");
                showError(message);
                errorContainer.setText(message);
                errorContainer.setVisible(true);
            }
        } catch (Exception ex) {
            String message = I18n.t("auth.login.error.serviceUnavailable");
            showError(message);
            errorContainer.setText(message);
            errorContainer.setVisible(true);
        }
    }

    @Override
    protected void onBeforeEnter(BeforeEnterEvent event) {
        // Clear error on fresh login view
        errorContainer.setVisible(false);
        errorContainer.setText("");
    }
}