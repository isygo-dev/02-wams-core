package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

@Route(value = "login/otp")
@PageTitle("OTP Login")
@PermitAll
public class OtpLoginView extends VerticalLayout implements BeforeEnterObserver {

    private final TextField usernameField = new TextField("Username");
    private final TextField otpField = new TextField("One-Time Password");
    private final Button requestOtpButton = new Button("Request OTP", new Icon(VaadinIcon.ENVELOPE));
    private final Button loginButton = new Button("Sign in", new Icon(VaadinIcon.SIGN_IN));
    private final Div errorContainer = new Div();

    private String generatedOtp = null;

    public OtpLoginView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("otp-login-view");

        // Brand
        Div brand = new Div();
        brand.addClassName("brand");
        Avatar logo = new Avatar("KMS/IMS");
        logo.setColorIndex(2);
        logo.setWidth("56px");
        logo.setHeight("56px");
        H2 title = new H2("OTP Login");
        title.addClassName(LumoUtility.FontWeight.BOLD);
        title.addClassName(LumoUtility.Margin.NONE);
        brand.add(logo, title);

        // Username field
        usernameField.setWidthFull();
        usernameField.setPlaceholder("Enter your username");

        // OTP field
        otpField.setWidthFull();
        otpField.setPlaceholder("6-digit code");
        otpField.setEnabled(false);
        otpField.setPrefixComponent(new Icon(VaadinIcon.CODE));

        // Buttons
        requestOtpButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        requestOtpButton.setWidthFull();
        requestOtpButton.addClickListener(e -> requestOtp());

        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        loginButton.setEnabled(false);
        loginButton.addClickListener(e -> handleOtpLogin());

        // Error container
        errorContainer.addClassName("error-container");
        errorContainer.setVisible(false);

        // Back link
        Anchor backToLogin = new Anchor("login", "← Back to sign in");
        backToLogin.addClassName("back-link");

        // Footer
        Paragraph footer = new Paragraph("© 2026 KMS/IMS Platform");
        footer.addClassName(LumoUtility.TextColor.TERTIARY);
        footer.addClassName(LumoUtility.FontSize.XXSMALL);
        footer.addClassName(LumoUtility.Margin.Top.MEDIUM);

        VerticalLayout wrapper = new VerticalLayout(brand, usernameField, requestOtpButton,
                otpField, loginButton, errorContainer, backToLogin, footer);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.setMaxWidth("400px");
        wrapper.setWidthFull();
        wrapper.setPadding(true);
        wrapper.setSpacing(true);
        wrapper.addClassName("otp-wrapper");

        add(wrapper);
        injectResponsiveStyles();
    }

    private void requestOtp() {
        String username = usernameField.getValue();
        if (username.isBlank()) {
            showError("Please enter your username");
            return;
        }

        generatedOtp = String.format("%06d", (int) (Math.random() * 1000000));
        Notification.show("OTP sent to your registered email: " + generatedOtp,
                        5000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        otpField.setEnabled(true);
        loginButton.setEnabled(true);
        requestOtpButton.setEnabled(false);
        errorContainer.setVisible(false);
    }

    private void handleOtpLogin() {
        String otp = otpField.getValue();
        if (otp.isBlank()) {
            showError("Please enter the OTP");
            return;
        }
        if (!otp.equals(generatedOtp)) {
            showError("Invalid OTP. Please try again.");
            return;
        }

        VaadinSession.getCurrent().setAttribute("user", usernameField.getValue());
        Notification.show("Welcome " + usernameField.getValue() + "!", 2000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate("ims");
    }

    private void showError(String message) {
        errorContainer.setText(message);
        errorContainer.setVisible(true);
        Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("user") != null) {
            event.forwardTo("ims");
        }
        errorContainer.setVisible(false);
        generatedOtp = null;
        otpField.setEnabled(false);
        loginButton.setEnabled(false);
        requestOtpButton.setEnabled(true);
        otpField.clear();
    }

    private void injectResponsiveStyles() {
        String css = """
                .otp-login-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .otp-wrapper {
                    background: var(--lumo-base-color);
                    border-radius: var(--lumo-border-radius-xl);
                    box-shadow: var(--lumo-box-shadow-m);
                    padding: var(--lumo-space-l);
                }
                .otp-login-view .brand {
                    text-align: center;
                    margin-bottom: var(--lumo-space-m);
                }
                .otp-login-view .brand h2 {
                    font-size: var(--lumo-font-size-xl);
                    letter-spacing: -0.5px;
                }
                .otp-login-view vaadin-text-field {
                    width: 100%;
                }
                .otp-login-view .error-container {
                    background: var(--lumo-error-color-10pct);
                    color: var(--lumo-error-text-color);
                    padding: var(--lumo-space-s);
                    border-radius: var(--lumo-border-radius-m);
                    font-size: var(--lumo-font-size-xs);
                    width: 100%;
                    text-align: center;
                }
                .otp-login-view .back-link {
                    color: var(--lumo-primary-text-color);
                    font-size: var(--lumo-font-size-s);
                    margin-top: var(--lumo-space-m);
                }
                @media (max-width: 480px) {
                    .otp-wrapper {
                        padding: var(--lumo-space-m);
                        border-radius: var(--lumo-border-radius-l);
                        margin: var(--lumo-space-m);
                    }
                    .otp-login-view .brand h2 {
                        font-size: var(--lumo-font-size-l);
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}