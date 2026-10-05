package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.dto.request.AuthenticationContextRequest;
import eu.isygoit.dto.response.UserContext;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.auth.AuthServiceFacade.Failure;
import eu.isygoit.util.SecurityUtils;
import jakarta.annotation.security.PermitAll;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
@UIScope
@Route(value = AuthRoutes.LOGIN)
@PermitAll
public class LoginView extends BaseLoginView {

    private final TextField tenantField = new TextField(I18n.t("auth.login.field.tenant.label"));
    private final TextField usernameField = new TextField(I18n.t("auth.login.field.username.label"));
    private final Button continueButton = new Button(
            I18n.t("auth.login.button.continue"), VaadinIcon.ARROW_RIGHT.create());
    private final AuthServiceFacade authService;
    private boolean submitting;

    public LoginView(AuthServiceFacade authService) {
        super("auth.page.title.login");
        this.authService = authService;

        tenantField.setPlaceholder(I18n.t("auth.login.field.tenant.placeholder"));
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.getElement().setAttribute("autocomplete", "organization");
        usernameField.setPlaceholder(I18n.t("auth.login.field.username.placeholder"));
        usernameField.setRequiredIndicatorVisible(true);
        usernameField.getElement().setAttribute("autocomplete", "username");
        describeErrors(tenantField);
        describeErrors(usernameField);
        tenantField.addValueChangeListener(event -> clearError());
        usernameField.addValueChangeListener(event -> clearError());

        continueButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        continueButton.addClickListener(event -> handleContinue());
        continueButton.addClickShortcut(com.vaadin.flow.component.Key.ENTER);

        Anchor registerLink = new Anchor(AuthRoutes.REGISTER, I18n.t("auth.login.link.register"));
        addToCard(new BrandHeader(I18n.t("auth.common.brand.title"),
                        I18n.t("auth.common.brand.subtitle")),
                tenantField, usernameField, continueButton, errorBanner, registerLink, new AuthFooter());
    }

    private void handleContinue() {
        if (submitting) {
            return;
        }

        String tenant = tenantField.getValue().trim().toLowerCase(Locale.ROOT);
        String username = usernameField.getValue().trim().toLowerCase(Locale.ROOT);
        if (tenant.isEmpty() || username.isEmpty()) {
            showError(I18n.t("auth.login.error.requiredFields"), Failure.INVALID_CREDENTIALS,
                    tenant.isEmpty() ? tenantField : usernameField);
            return;
        }

        clearError();
        submitting = true;
        setLoading(true);
        AuthenticationContextRequest request = AuthenticationContextRequest.builder()
                .tenant(tenant)
                .userName(username)
                .build();
        UI ui = UI.getCurrent();

        authService.resolveAuthContext(request).whenComplete((result, failure) ->
                ui.access(() -> {
                    setLoading(false);
                    if (failure != null) {
                        reportUnexpectedFailure(failure);
                        showError(I18n.t("auth.login.error.serviceUnavailable"), Failure.OFFLINE);
                        return;
                    }
                    if (!result.succeeded()) {
                        showError(failureMessage(result.failure(), "auth.login.error.authMethodUnavailable",
                                "auth.login.error.serviceUnavailable"), result.failure());
                        return;
                    }
                    navigateToAuthentication(ui, result.value(), tenant, username);
                }));
    }

    private void navigateToAuthentication(UI ui, UserContext userContext, String tenant, String username) {
        IEnumAuth.Types authType = userContext.getAuthTypeMode();
        String targetView;
        Map<String, String> query = new LinkedHashMap<>();
        query.put("tenant", tenant);
        query.put("username", username);
        if (redirectTarget != null && SecurityUtils.isSafeInternalPath(redirectTarget)) {
            query.put("redirect", redirectTarget);
        }

        if (authType == IEnumAuth.Types.PWD) {
            targetView = AuthRoutes.PASSWORD_LOGIN;
        } else if (authType == IEnumAuth.Types.OTP) {
            Integer otpLength = userContext.getOtpLength();
            if (otpLength != null && (otpLength < 4 || otpLength > 8)) {
                showError(I18n.t("auth.login.error.authMethodUnavailable"), Failure.SERVICE_ERROR);
                return;
            }
            query.put("otpLength", Integer.toString(otpLength == null ? 6 : otpLength));
            targetView = AuthRoutes.OTP_LOGIN;
        } else if (authType == IEnumAuth.Types.QRC) {
            targetView = AuthRoutes.QR_LOGIN;
        } else if (authType == IEnumAuth.Types.TOKEN) {
            showError(I18n.t("auth.login.warning.tokenUnsupported"), Failure.SERVICE_ERROR);
            return;
        } else {
            showError(I18n.t("auth.login.error.unsupportedAuthType", authType), Failure.SERVICE_ERROR);
            return;
        }

        ui.navigate(targetView, QueryParameters.simple(query));
    }

    private void setLoading(boolean loading) {
        submitting = loading;
        tenantField.setEnabled(!loading);
        usernameField.setEnabled(!loading);
        setButtonLoading(continueButton, loading);
    }

    @Override
    protected void onBeforeEnter(BeforeEnterEvent event) {
        clearError();
    }
}
