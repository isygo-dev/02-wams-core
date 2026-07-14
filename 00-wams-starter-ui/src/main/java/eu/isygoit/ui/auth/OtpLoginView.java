package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.dto.request.AuthenticationContextRequest;
import eu.isygoit.dto.request.AuthenticationRequestDto;
import eu.isygoit.dto.response.AuthResponseDto;
import eu.isygoit.dto.response.UserContext;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.i18n.I18n;
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
public class OtpLoginView extends BaseLoginView {

    private final TextField usernameField = new TextField(I18n.t("auth.otp.field.username.label"));
    private final HorizontalLayout otpFieldsLayout = new HorizontalLayout();
    private final Button requestOtpButton = new Button(I18n.t("auth.otp.button.requestOtp"), new Icon(VaadinIcon.ENVELOPE));
    private final Button loginButton = new Button(I18n.t("auth.otp.button.signIn"), new Icon(VaadinIcon.SIGN_IN));
    private final Div errorBanner = createErrorBanner();

    private String tenant;
    private String username;
    private int otpLength = 6;
    private final List<TextField> digitFields = new ArrayList<>();

    @Autowired
    private PublicAuthService authService;

    public OtpLoginView() {
        configureAsAuthPage("otp-login-view");

        usernameField.setWidthFull();
        usernameField.setReadOnly(true);

        otpFieldsLayout.setSpacing(true);
        otpFieldsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        otpFieldsLayout.setWidthFull();
        otpFieldsLayout.addClassName("wams-otp-fields");

        requestOtpButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        requestOtpButton.setWidthFull();
        requestOtpButton.addClickListener(e -> requestOtp());

        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.addClassName("wams-auth-primary-btn");
        loginButton.setEnabled(false);
        loginButton.addClickListener(e -> handleOtpLogin());

        Anchor backToLogin = createLink("login", I18n.t("auth.common.link.backToSignIn"));

        var card = createCard();
        card.add(createBrand(I18n.t("auth.otp.title"), null),
                usernameField, requestOtpButton, otpFieldsLayout, loginButton,
                errorBanner, backToLogin, createFooter());

        add(card);
    }

    private void buildOtpFields(int length) {
        otpFieldsLayout.removeAll();
        digitFields.clear();

        for (int i = 0; i < length; i++) {
            TextField field = new TextField();
            field.setMaxLength(1);
            field.setPattern("[0-9]");
            field.setPlaceholder(I18n.t("auth.otp.field.digit.placeholder"));
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
        errorBanner.setVisible(false);

        AuthenticationContextRequest request = AuthenticationContextRequest.builder()
                .tenant(tenant)
                .userName(username)
                .build();

        try {
            ResponseEntity<UserContext> response = authService.resolveAuthContext(request);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Notification.show(I18n.t("auth.otp.notification.otpSent"), 4000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                showError(errorBanner, I18n.t("auth.otp.error.requestFailed"));
            }
        } catch (Exception ex) {
            showError(errorBanner, I18n.t("auth.otp.error.requestFailed"));
        }
    }

    private void handleOtpLogin() {
        String otp = getOtpFromFields();
        if (otp.length() != otpLength) {
            showError(errorBanner, I18n.t("auth.otp.error.incomplete"));
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

                log.info("User logged in: {} (session id: {})", username, vaadinSession.getSession().getId());

                String target = (redirectTarget != null && SecurityUtils.isSafeInternalPath(redirectTarget))
                        ? redirectTarget
                        : "landing";

                log.info("Redirecting after login to: {}", target);
                UI.getCurrent().navigate(target);

                Notification.show(I18n.t("auth.common.notification.welcome", username), 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                showError(errorBanner, I18n.t("auth.otp.error.invalidOtp"));
            }
        } catch (Exception ex) {
            showError(errorBanner, I18n.t("auth.common.error.authenticationError"));
        }
    }

    // ─── Override only onBeforeEnter – base beforeEnter will call this ────
    @Override
    protected void onBeforeEnter(BeforeEnterEvent event) {
        // Clear error container
        errorBanner.setVisible(false);
        errorBanner.setText("");

        // DO NOT overwrite redirectTarget here — BaseLoginView already did it
        // Only extract tenant/username/otpLength

        Optional<String> tenantOpt = event.getLocation().getQueryParameters().getSingleParameter("tenant");
        Optional<String> usernameOpt = event.getLocation().getQueryParameters().getSingleParameter("username");
        Optional<String> otpLengthOpt = event.getLocation().getQueryParameters().getSingleParameter("otpLength");

        if (tenantOpt.isEmpty() || usernameOpt.isEmpty()) {
            event.forwardTo("login");
            return;
        }

        tenant = tenantOpt.get();
        username = usernameOpt.get();
        try {
            otpLength = Integer.parseInt(otpLengthOpt.orElse("6"));
        } catch (NumberFormatException e) {
            otpLength = 6;
        }

        usernameField.setValue(username);
        buildOtpFields(otpLength);
    }
}
