package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.dto.request.AuthenticationContextRequest;
import eu.isygoit.dto.request.AuthenticationRequestDto;
import eu.isygoit.constants.AuthConstants;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.auth.AuthServiceFacade.Failure;
import jakarta.annotation.security.PermitAll;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@UIScope
@Route(value = AuthRoutes.OTP_LOGIN)
@PermitAll
public class OtpLoginView extends BaseLoginView {

    private final TextField usernameField = new TextField(I18n.t("auth.otp.field.username.label"));
    private final OtpInput otpInput = new OtpInput();
    private final Button requestOtpButton = new Button(
            I18n.t("auth.otp.button.requestOtp"), VaadinIcon.ENVELOPE.create());
    private final Button loginButton = new Button(
            I18n.t("auth.otp.button.signIn"), VaadinIcon.SIGN_IN.create());
    private final AuthServiceFacade authService;
    private final AuthSession authSession;
    private String tenant;
    private String username;
    private int otpLength = 6;
    private boolean requestingOtp;
    private boolean submitting;
    private boolean otpConfigurationValid = true;

    public OtpLoginView(AuthServiceFacade authService, AuthSession authSession) {
        super("auth.page.title.otpLogin");
        this.authService = authService;
        this.authSession = authSession;

        describeErrors(usernameField);
        otpInput.setErrorDescriptionId(AuthErrorBanner.ID);
        usernameField.setReadOnly(true);
        usernameField.getElement().setAttribute("autocomplete", "username");
        requestOtpButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        requestOtpButton.addClickListener(event -> requestOtp());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setEnabled(false);
        loginButton.addClickListener(event -> handleOtpLogin());
        loginButton.addClickShortcut(com.vaadin.flow.component.Key.ENTER);

        otpInput.setValueChangeListener(complete -> {
            clearError();
            updateLoginButtonState();
        });
        otpInput.setCompleteListener(ignored -> handleOtpLogin());

        Anchor backToLogin = new Anchor(AuthRoutes.LOGIN, I18n.t("auth.common.link.backToSignIn"));
        addToCard(new BrandHeader(I18n.t("auth.otp.title"), null),
                usernameField, requestOtpButton, otpInput, loginButton, errorBanner,
                backToLogin, new AuthFooter());
    }

    private void requestOtp() {
        if (requestingOtp || submitting || !otpConfigurationValid) {
            return;
        }
        clearError();
        otpInput.clear();
        otpInput.focusFirst();
        otpInput.setEnabled(false);
        updateLoginButtonState();
        setRequestLoading(true);

        AuthenticationContextRequest request = AuthenticationContextRequest.builder()
                .tenant(tenant)
                .userName(username)
                .build();
        UI ui = UI.getCurrent();
        authService.resolveAuthContext(request).whenComplete((result, failure) ->
                ui.access(() -> {
                    setRequestLoading(false);
                    otpInput.setEnabled(otpConfigurationValid);
                    updateLoginButtonState();
                    if (failure != null) {
                        reportUnexpectedFailure(failure);
                        showError(I18n.t("auth.otp.error.requestFailed"), Failure.OFFLINE);
                    } else if (!result.succeeded()) {
                        showError(failureMessage(result.failure(), "auth.otp.error.requestFailed",
                                "auth.otp.error.requestFailed"), result.failure());
                    } else {
                        errorBanner.showSuccess(I18n.t("auth.otp.notification.otpSent"));
                        otpInput.focusFirst();
                    }
                }));
    }

    private void handleOtpLogin() {
        if (submitting || !otpConfigurationValid) {
            return;
        }
        if (!otpInput.isComplete()) {
            showError(I18n.t("auth.otp.error.incomplete"), Failure.INVALID_CREDENTIALS,
                    otpInput);
            otpInput.focusFirstIncomplete();
            return;
        }

        clearError();
        setLoginLoading(true);
        AuthenticationRequestDto request = AuthenticationRequestDto.builder()
                .tenant(tenant)
                .application(AuthConstants.DEFAULT_APPLICATION)
                .userName(username)
                .password(otpInput.getValue())
                .authType(IEnumAuth.Types.OTP)
                .build();
        UI ui = UI.getCurrent();

        authService.authenticate(request).whenComplete((result, failure) ->
                ui.access(() -> {
                    setLoginLoading(false);
                    if (failure != null) {
                        reportUnexpectedFailure(failure);
                        showError(I18n.t("auth.common.error.authenticationError"), Failure.OFFLINE);
                    } else if (!result.succeeded()) {
                        showError(failureMessage(result.failure(), "auth.otp.error.invalidOtp",
                                "auth.common.error.authenticationError"), result.failure(),
                                otpInput);
                    } else {
                        completeLogin(ui, result.value().getAccessToken());
                    }
                }));
    }

    private void completeLogin(UI ui, String accessToken) {
        try {
            authSession.write(username, accessToken);
            authSession.redirect(ui, redirectTarget);
            showWelcome(username);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            reportUnexpectedFailure(exception);
            showError(I18n.t("auth.common.error.authenticationError"), Failure.SERVICE_ERROR);
        }
    }

    private void updateLoginButtonState() {
        loginButton.setEnabled(otpConfigurationValid && otpInput.isComplete() && !submitting);
    }

    private void setRequestLoading(boolean loading) {
        requestingOtp = loading;
        setButtonLoading(requestOtpButton, loading);
        if (!loading && !otpConfigurationValid) {
            requestOtpButton.setEnabled(false);
        }
    }

    private void setLoginLoading(boolean loading) {
        submitting = loading;
        otpInput.setEnabled(!loading);
        setButtonLoading(loginButton, loading);
        updateLoginButtonState();
    }

    @Override
    protected void onBeforeEnter(BeforeEnterEvent event) {
        clearError();
        otpConfigurationValid = true;
        otpLength = 6;
        Optional<String> tenantOpt = event.getLocation().getQueryParameters().getSingleParameter("tenant");
        Optional<String> usernameOpt = event.getLocation().getQueryParameters().getSingleParameter("username");
        Optional<String> otpLengthOpt = event.getLocation().getQueryParameters().getSingleParameter("otpLength");

        if (tenantOpt.isEmpty() || usernameOpt.isEmpty()) {
            event.forwardTo(AuthRoutes.LOGIN);
            return;
        }

        tenant = tenantOpt.get();
        username = usernameOpt.get();
        try {
            otpLength = Integer.parseInt(otpLengthOpt.orElse("6"));
        } catch (NumberFormatException exception) {
            otpConfigurationValid = false;
        }

        otpConfigurationValid = otpConfigurationValid && otpInput.setLength(otpLength);
        usernameField.setValue(username);
        if (!otpConfigurationValid) {
            requestOtpButton.setEnabled(false);
            loginButton.setEnabled(false);
            showError(I18n.t("auth.otp.error.invalidLength"), Failure.SERVICE_ERROR);
            return;
        }

        requestOtpButton.setEnabled(true);
        otpInput.clear();
        otpInput.focusFirst();
        updateLoginButtonState();
    }
}
