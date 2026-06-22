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
import com.vaadin.flow.server.VaadinServletRequest;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.request.AuthenticationRequestDto;
import eu.isygoit.dto.response.AuthResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.remote.ims.PublicAuthService;
import eu.isygoit.util.SecurityUtils;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@UIScope
@Route(value = "login/otp")
@PageTitle("OTP Login")
@PermitAll
public class OtpLoginView extends VerticalLayout implements BeforeEnterObserver {

    private final TextField usernameField = new TextField("Username");
    private final HorizontalLayout otpFieldsLayout = new HorizontalLayout();
    private final Button requestOtpButton = new Button("Request OTP", new Icon(VaadinIcon.ENVELOPE));
    private final Button loginButton = new Button("Sign in", new Icon(VaadinIcon.SIGN_IN));
    private final Div errorContainer = new Div();
    private boolean stylesInjected = false;

    private String tenant;
    private String username;
    private int otpLength = 6;
    private String redirectTarget;
    private List<TextField> digitFields = new ArrayList<>();

    @Autowired
    private PublicAuthService authService;

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

        // Username field (prefilled, read-only)
        usernameField.setWidthFull();
        usernameField.setReadOnly(true);

        // OTP fields container
        otpFieldsLayout.setSpacing(true);
        otpFieldsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        otpFieldsLayout.setWidthFull();
        otpFieldsLayout.addClassName("otp-fields");

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
                otpFieldsLayout, loginButton, errorContainer, backToLogin, footer);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.setMaxWidth("400px");
        wrapper.setWidthFull();
        wrapper.setPadding(true);
        wrapper.setSpacing(true);
        wrapper.addClassName("otp-wrapper");

        add(wrapper);

        addAttachListener(event -> {
            if (!stylesInjected) {
                injectResponsiveStyles();
                stylesInjected = true;
            }
        });
    }

    private void buildOtpFields(int length) {
        otpFieldsLayout.removeAll();
        digitFields.clear();

        for (int i = 0; i < length; i++) {
            TextField field = new TextField();
            field.setMaxLength(1);
            field.setPattern("[0-9]");
            field.setPlaceholder("•");
            field.setValueChangeMode(ValueChangeMode.EAGER);
            final int index = i;
            field.addValueChangeListener(e -> {
                String val = e.getValue();
                if (!val.isEmpty() && index < digitFields.size() - 1) {
                    digitFields.get(index + 1).focus();
                }
                updateLoginButtonState();
            });
            field.addKeyDownListener(e -> {
                if (e.getKey().equals(com.vaadin.flow.component.Key.BACKSPACE) &&
                        field.getValue().isEmpty() && index > 0) {
                    digitFields.get(index - 1).focus();
                }
            });
            digitFields.add(field);
            otpFieldsLayout.add(field);
        }
        digitFields.forEach(f -> f.setEnabled(true));
        if (!digitFields.isEmpty()) {
            digitFields.get(0).focus();
        }
        digitFields.forEach(TextField::clear);
        loginButton.setEnabled(false);
    }

    private void updateLoginButtonState() {
        boolean allFilled = digitFields.stream().allMatch(f -> !f.getValue().isEmpty());
        loginButton.setEnabled(allFilled);
    }

    private String getOtpFromFields() {
        return digitFields.stream().map(TextField::getValue).collect(Collectors.joining());
    }

    private void requestOtp() {
        digitFields.forEach(f -> f.clear());
        if (!digitFields.isEmpty()) {
            digitFields.get(0).focus();
        }
        loginButton.setEnabled(false);
        errorContainer.setVisible(false);

        // In production, call the actual OTP service.
        // For demo, simulate success.
        Notification.show("OTP sent to your registered email.", 4000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void handleOtpLogin() {
        String otp = getOtpFromFields();
        if (otp.length() != otpLength) {
            showError("Please enter the complete OTP.");
            return;
        }

        AuthenticationRequestDto authRequest = AuthenticationRequestDto.builder()
                .tenant(tenant)
                .application("default")
                .userName(username)
                .password(otp)
                .authType(IEnumAuth.Types.OTP)
                .build();

        try {
            ResponseEntity<AuthResponseDto> response = authService.authenticate(authRequest);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AuthResponseDto authResponse = response.getBody();

                VaadinSession vaadinSession = VaadinSession.getCurrent();
                vaadinSession.setAttribute("user", username);
                vaadinSession.setAttribute("accessToken", authResponse.getAccessToken());

                // Also store in plain HTTP session as a fallback
                HttpSession httpSession = VaadinServletRequest.getCurrent().getSession(true);
                httpSession.setAttribute("user", username);
                httpSession.setAttribute("accessToken", authResponse.getAccessToken());

                log.info("✅ User logged in: {} (session id: {})", username, vaadinSession.getSession().getId());

                String target = (redirectTarget != null && SecurityUtils.isSafeInternalPath(redirectTarget))
                        ? redirectTarget
                        : "kms";
                UI.getCurrent().navigate(target);

                Notification.show("Welcome " + username + "!", 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                showError("Invalid OTP. Please try again.");
            }
        } catch (Exception ex) {
            showError("Authentication error. Please try again.");
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
        if (SecurityUtils.isUserLoggedIn()) {
            String target = SecurityUtils.consumeRedirect();
            if (target == null) {
                target = event.getLocation()
                        .getQueryParameters()
                        .getSingleParameter("redirect")
                        .filter(SecurityUtils::isSafeInternalPath)
                        .orElse("kms");
            }
            event.forwardTo(target);
            return;
        }

        // Capture redirect from session or query
        redirectTarget = SecurityUtils.consumeRedirect();
        if (redirectTarget == null) {
            redirectTarget = event.getLocation()
                    .getQueryParameters()
                    .getSingleParameter("redirect")
                    .filter(SecurityUtils::isSafeInternalPath)
                    .orElse(null);
        }

        Optional<String> tenantOpt = event.getLocation().getQueryParameters().getSingleParameter("tenant");
        Optional<String> usernameOpt = event.getLocation().getQueryParameters().getSingleParameter("username");
        Optional<String> otpLengthOpt = event.getLocation().getQueryParameters().getSingleParameter("otpLength");

        if (tenantOpt.isEmpty() || usernameOpt.isEmpty() || otpLengthOpt.isEmpty()) {
            event.forwardTo("login");
            return;
        }

        tenant = tenantOpt.get();
        username = usernameOpt.get();
        try {
            otpLength = Integer.parseInt(otpLengthOpt.get());
        } catch (NumberFormatException e) {
            otpLength = 6;
        }

        usernameField.setValue(username);
        errorContainer.setVisible(false);
        buildOtpFields(otpLength);
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
                .otp-login-view .otp-fields vaadin-text-field {
                    width: 2.8em !important;
                    height: 2.8em !important;
                    text-align: center;
                    font-size: 1.2em;
                    --lumo-text-field-size: 2.8em;
                }
                .otp-login-view .otp-fields vaadin-text-field input {
                    text-align: center;
                    padding: 0;
                    font-size: 1em;
                }
                .otp-login-view .otp-fields vaadin-text-field::placeholder {
                    font-size: 0.8em;
                    color: var(--lumo-tertiary-text-color);
                }
                .otp-login-view .otp-fields {
                    gap: 0.4em;
                    margin: var(--lumo-space-s) 0;
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
                    .otp-login-view .otp-fields vaadin-text-field {
                        width: 2.2em !important;
                        height: 2.2em !important;
                        font-size: 1em;
                    }
                    .otp-login-view .otp-fields {
                        gap: 0.3em;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}