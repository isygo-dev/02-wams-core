package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

@Route(value = "login")
@PageTitle("Sign In")
@PermitAll
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private static final String DEMO_USERNAME = "admin";
    private static final String DEMO_PASSWORD = "admin";

    private final LoginForm loginForm = new LoginForm();
    private final Div errorContainer = new Div();

    public LoginView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("login-view");

        // --- Brand / Logo ---
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

        // --- Login Form ---
        LoginI18n i18n = LoginI18n.createDefault();
        i18n.setHeader(new LoginI18n.Header());
        i18n.getHeader().setTitle("Welcome back");
        i18n.getHeader().setDescription("Sign in to your account");
        i18n.setAdditionalInformation(null);
        loginForm.setI18n(i18n);
        loginForm.setAction("");
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.addClassName("login-form");

        // Error container
        errorContainer.addClassName("error-container");
        errorContainer.setVisible(false);

        loginForm.addLoginListener(e -> handleLogin(e.getUsername(), e.getPassword()));

        // --- Alternative login methods ---
        HorizontalLayout altMethods = new HorizontalLayout();
        altMethods.addClassName("alt-methods");
        Anchor otpLink = new Anchor("login/otp", "OTP Login");
        otpLink.addClassName("alt-link");
        Anchor qrLink = new Anchor("login/qr", "QR Login");
        qrLink.addClassName("alt-link");
        altMethods.add(otpLink, qrLink);
        altMethods.setSpacing(true);
        altMethods.setJustifyContentMode(JustifyContentMode.CENTER);

        // --- Register link ---
        Anchor registerLink = new Anchor("register", "Create an account");
        registerLink.addClassName("register-link");

        // --- Footer ---
        Paragraph footer = new Paragraph("© 2026 KMS/IMS Platform");
        footer.addClassName(LumoUtility.TextColor.TERTIARY);
        footer.addClassName(LumoUtility.FontSize.XXSMALL);
        footer.addClassName(LumoUtility.Margin.Top.MEDIUM);

        // --- Main wrapper ---
        VerticalLayout wrapper = new VerticalLayout(brand, loginForm, errorContainer, altMethods, registerLink, footer);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.setMaxWidth("400px");
        wrapper.setWidthFull();
        wrapper.setPadding(true);
        wrapper.setSpacing(true);
        wrapper.addClassName("login-wrapper");

        add(wrapper);
        injectResponsiveStyles();
    }

    private void handleLogin(String username, String password) {
        if (DEMO_USERNAME.equals(username) && DEMO_PASSWORD.equals(password)) {
            VaadinSession.getCurrent().setAttribute("user", username);
            Notification.show("Welcome " + username + "!", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate("ims");
        } else {
            errorContainer.setText("Invalid username or password. Try admin/admin");
            errorContainer.setVisible(true);
            loginForm.setError(true);
            Notification.show("Invalid credentials", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("user") != null) {
            event.forwardTo("ims");
        }
        loginForm.setError(false);
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
                .login-view .login-form {
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
                .login-view .alt-methods {
                    gap: var(--lumo-space-s);
                    margin: var(--lumo-space-s) 0;
                }
                .login-view .alt-link {
                    color: var(--lumo-primary-text-color);
                    font-size: var(--lumo-font-size-xs);
                    text-decoration: none;
                    padding: var(--lumo-space-xs) var(--lumo-space-s);
                    border: 1px solid var(--lumo-contrast-20pct);
                    border-radius: var(--lumo-border-radius-m);
                    transition: all 0.2s;
                }
                .login-view .alt-link:hover {
                    background: var(--lumo-primary-color-10pct);
                    border-color: var(--lumo-primary-color);
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
                    .login-view .brand {
                        margin-bottom: var(--lumo-space-s);
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}