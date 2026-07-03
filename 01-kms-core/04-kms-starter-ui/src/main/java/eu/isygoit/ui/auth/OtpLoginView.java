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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.request.AuthenticationRequestDto;
import eu.isygoit.dto.response.AuthResponseDto;
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
    private final Div errorContainer = new Div();

    private String tenant;
    private String username;
    private int otpLength = 6;
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
        H2 title = new H2(I18n.t("auth.otp.title"));
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
        Anchor backToLogin = new Anchor("login", I18n.t("auth.common.link.backToSignIn"));
        backToLogin.addClassName("back-link");

        // Footer
        Paragraph footer = new Paragraph(I18n.t("auth.common.footer"));
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
        errorContainer.setVisible(false);

        // In production, call the actual OTP service.
        // For demo, simulate success.
        Notification.show(I18n.t("auth.otp.notification.otpSent"), 4000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void handleOtpLogin() {
        String otp = getOtpFromFields();
        if (otp.length() != otpLength) {
            String message = I18n.t("auth.otp.error.incomplete");
            showError(message);
            errorContainer.setText(message);
            errorContainer.setVisible(true);
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
                        : "landing";

                log.info("Redirecting after login to: {}", target);
                UI.getCurrent().navigate(target);

                Notification.show(I18n.t("auth.common.notification.welcome", username), 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                String message = I18n.t("auth.otp.error.invalidOtp");
                showError(message);
                errorContainer.setText(message);
                errorContainer.setVisible(true);
            }
        } catch (Exception ex) {
            String message = I18n.t("auth.common.error.authenticationError");
            showError(message);
            errorContainer.setText(message);
            errorContainer.setVisible(true);
        }
    }

    // ─── Override only onBeforeEnter – base beforeEnter will call this ────
    @Override
    protected void onBeforeEnter(BeforeEnterEvent event) {
        // Clear error container
        errorContainer.setVisible(false);
        errorContainer.setText("");

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