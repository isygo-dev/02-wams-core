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
import eu.isygoit.ui.common.view.ManagementVerticalView;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.request.AccountAuthTypeRequest;
import eu.isygoit.dto.response.UserContext;
import eu.isygoit.enums.IEnumAuth;
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
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final TextField tenantField = new TextField("Organisation");
    private final TextField usernameField = new TextField("Login");
    private final Button continueButton = new Button("Continue", VaadinIcon.ARROW_RIGHT.create());
    private final Div errorContainer = new Div();
    private boolean stylesInjected = false;

    private String redirectTarget;

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
        H2 title = new H2("KMS · IMS");
        title.addClassName(LumoUtility.FontWeight.BOLD);
        title.addClassName(LumoUtility.Margin.NONE);
        Paragraph subtitle = new Paragraph("Key & Identity Management");
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.addClassName(LumoUtility.FontSize.SMALL);
        brand.add(logo, title, subtitle);

        // Tenant + Login fields
        tenantField.setWidthFull();
        tenantField.setPlaceholder("Your organisation");
        tenantField.setPrefixComponent(VaadinIcon.BUILDING.create());
        usernameField.setWidthFull();
        usernameField.setPlaceholder("Username");
        usernameField.setPrefixComponent(VaadinIcon.USER.create());

        // Continue button
        continueButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        continueButton.setWidthFull();
        continueButton.addClickListener(e -> handleContinue());

        // Error container
        errorContainer.addClassName("error-container");
        errorContainer.setVisible(false);

        // Register link
        Anchor registerLink = new Anchor("register", "Create an account");
        registerLink.addClassName("register-link");

        // Footer
        Paragraph footer = new Paragraph("© 2026 KMS/IMS Platform");
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

        addAttachListener(event -> {
            if (!stylesInjected) {
                injectResponsiveStyles();
                stylesInjected = true;
            }
        });
    }

    private void handleContinue() {
        String tenant = tenantField.getValue().trim().toLowerCase();
        String username = usernameField.getValue().trim().toLowerCase();

        if (tenant.isEmpty() || username.isEmpty()) {
            showError("Organisation and Login are required");
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
                        Notification.show("Token-based login is not supported in UI yet.", 3000,
                                Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                        return;
                    default:
                        showError("Unsupported authentication type: " + authType);
                        return;
                }

                if (targetView != null) {
                    UI.getCurrent().navigate(targetView + query.toString());
                }
            } else {
                showError("Unable to determine authentication method. Please check your credentials.");
            }
        } catch (Exception ex) {
            showError("Service unavailable. Please try again later.");
        }
    }

    private void showError(String message) {
        errorContainer.setText(message);
        errorContainer.setVisible(true);
        Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // 1. Try session first
        redirectTarget = SecurityUtils.consumeRedirect();

        // 2. Fallback to query parameter
        if (redirectTarget == null) {
            redirectTarget = event.getLocation()
                    .getQueryParameters()
                    .getSingleParameter("redirect")
                    .filter(SecurityUtils::isSafeInternalPath)
                    .orElse(null);
        }

        log.info("🔐 LoginView redirectTarget: {}", redirectTarget);

        // 3. If already authenticated, forward directly
        if (SecurityUtils.isUserLoggedIn()) {
            String target = (redirectTarget != null && SecurityUtils.isSafeInternalPath(redirectTarget))
                    ? redirectTarget
                    : "kms";
            log.info("✅ User already logged in – forwarding to {}", target);
            event.forwardTo(target);
            return;
        }

        errorContainer.setVisible(false);
    }

    private void injectResponsiveStyles() {
        String css = """
                .login-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .login-wrapper {
                    background: var(--lumo-base-color);
                    border-radius: var(--lumo-border-radius-xl);
                    box-shadow: var(--lumo-box-shadow-m);
                    padding: var(--lumo-space-l);
                }
                .login-view .brand {
                    text-align: center;
                    margin-bottom: var(--lumo-space-m);
                }
                .login-view .brand h2 {
                    font-size: var(--lumo-font-size-xxl);
                    letter-spacing: -0.5px;
                }
                .login-view vaadin-text-field {
                    width: 100%;
                }
                .login-view .error-container {
                    background: var(--lumo-error-color-10pct);
                    color: var(--lumo-error-text-color);
                    padding: var(--lumo-space-s);
                    border-radius: var(--lumo-border-radius-m);
                    font-size: var(--lumo-font-size-xs);
                    width: 100%;
                    text-align: center;
                }
                .login-view .register-link {
                    color: var(--lumo-primary-text-color);
                    font-size: var(--lumo-font-size-s);
                    margin-top: var(--lumo-space-s);
                }
                @media (max-width: 480px) {
                    .login-wrapper {
                        padding: var(--lumo-space-m);
                        border-radius: var(--lumo-border-radius-l);
                        margin: var(--lumo-space-m);
                    }
                    .login-view .brand h2 {
                        font-size: var(--lumo-font-size-xl);
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}