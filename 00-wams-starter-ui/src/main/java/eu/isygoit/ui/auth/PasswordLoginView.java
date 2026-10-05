package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
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
@Route(value = AuthRoutes.PASSWORD_LOGIN)
@PermitAll
public class PasswordLoginView extends BaseLoginView {

    private final AuthPasswordField passwordField = new AuthPasswordField();
    private final Button loginButton = new Button(
            I18n.t("auth.otp.button.signIn"), VaadinIcon.SIGN_IN.create());
    private final AuthServiceFacade authService;
    private final AuthSession authSession;
    private String tenant;
    private String username;
    private boolean submitting;

    public PasswordLoginView(AuthServiceFacade authService, AuthSession authSession) {
        super("auth.page.title.passwordLogin");
        this.authService = authService;
        this.authSession = authSession;

        describeErrors(passwordField.getField());
        passwordField.getField().addValueChangeListener(event -> clearError());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.addClickListener(event -> handlePasswordLogin());
        loginButton.addClickShortcut(com.vaadin.flow.component.Key.ENTER);

        Anchor backLink = new Anchor(AuthRoutes.LOGIN, I18n.t("auth.common.link.back"));
        addToCard(new BrandHeader(I18n.t("auth.password.title"), null),
                passwordField, loginButton, errorBanner, backLink, new AuthFooter());
    }

    private void handlePasswordLogin() {
        if (submitting) {
            return;
        }

        String password = passwordField.getField().getValue();
        if (password.isBlank()) {
            showError(I18n.t("auth.password.error.required"), Failure.INVALID_CREDENTIALS,
                    passwordField.getField());
            return;
        }

        clearError();
        setLoading(true);
        AuthenticationRequestDto request = AuthenticationRequestDto.builder()
                .tenant(tenant)
                .application(AuthConstants.DEFAULT_APPLICATION)
                .userName(username)
                .password(password)
                .authType(IEnumAuth.Types.PWD)
                .build();
        UI ui = UI.getCurrent();

        authService.authenticate(request).whenComplete((result, failure) ->
                ui.access(() -> {
                    setLoading(false);
                    if (failure != null) {
                        reportUnexpectedFailure(failure);
                        showError(I18n.t("auth.password.error.serviceError"), Failure.OFFLINE);
                    } else if (!result.succeeded()) {
                        showError(failureMessage(result.failure(),
                                "auth.password.error.invalidCredentials",
                                "auth.password.error.serviceError"), result.failure(),
                                passwordField.getField());
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
            showError(I18n.t("auth.password.error.serviceError"), Failure.SERVICE_ERROR);
        }
    }

    private void setLoading(boolean loading) {
        submitting = loading;
        passwordField.setEnabled(!loading);
        setButtonLoading(loginButton, loading);
    }

    @Override
    protected void onBeforeEnter(BeforeEnterEvent event) {
        clearError();
        Optional<String> tenantOpt = event.getLocation().getQueryParameters().getSingleParameter("tenant");
        Optional<String> usernameOpt = event.getLocation().getQueryParameters().getSingleParameter("username");
        if (tenantOpt.isEmpty() || usernameOpt.isEmpty()) {
            event.forwardTo(AuthRoutes.LOGIN);
            return;
        }

        tenant = tenantOpt.get();
        username = usernameOpt.get();
        passwordField.getField().clear();
    }
}
