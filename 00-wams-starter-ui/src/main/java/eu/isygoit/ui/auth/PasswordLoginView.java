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
import com.vaadin.flow.component.textfield.PasswordField;
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

import java.util.Optional;

@Slf4j
@Component
@UIScope
@Route(value = "login/password")
@PageTitle("Password Login")
@PermitAll
public class PasswordLoginView extends BaseLoginView {

    private final PasswordField passwordField = new PasswordField(I18n.t("auth.password.field.password.label"));
    private final Button loginButton = new Button(I18n.t("auth.otp.button.signIn"), VaadinIcon.SIGN_IN.create());
    private final Div errorContainer = new Div();

    private String tenant;
    private String username;

    @Autowired
    private PublicAuthService authService;

    public PasswordLoginView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("password-view");

        // Brand
        Div brand = new Div();
        brand.addClassName("brand");
        Avatar logo = new Avatar("IsyGo");
        logo.setColorIndex(1);
        logo.setWidth("56px");
        logo.setHeight("56px");
        H2 title = new H2(I18n.t("auth.password.title"));
        title.addClassName(LumoUtility.FontWeight.BOLD);
        title.addClassName(LumoUtility.Margin.NONE);
        brand.add(logo, title);

        // Password field
        passwordField.setWidthFull();
        passwordField.setPlaceholder(I18n.t("auth.password.field.password.placeholder"));
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());

        // Login button
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        loginButton.addClickListener(e -> handlePasswordLogin());

        // Error container
        errorContainer.addClassName("error-container");
        errorContainer.setVisible(false);

        // Back link
        Anchor backLink = new Anchor("login", I18n.t("auth.common.link.back"));
        backLink.addClassName("back-link");

        // Footer
        Paragraph footer = new Paragraph(I18n.t("auth.common.footer"));
        footer.addClassName(LumoUtility.TextColor.TERTIARY);
        footer.addClassName(LumoUtility.FontSize.XXSMALL);
        footer.addClassName(LumoUtility.Margin.Top.MEDIUM);

        VerticalLayout wrapper = new VerticalLayout(brand, passwordField, loginButton,
                errorContainer, backLink, footer);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.setMaxWidth("400px");
        wrapper.setWidthFull();
        wrapper.setPadding(true);
        wrapper.setSpacing(true);
        wrapper.addClassName("password-wrapper");

        add(wrapper);
    }

    private void handlePasswordLogin() {
        String password = passwordField.getValue();
        if (password.isBlank()) {
            String message = I18n.t("auth.password.error.required");
            showError(message);
            errorContainer.setText(message);
            errorContainer.setVisible(true);
            return;
        }

        AuthenticationRequestDto request = AuthenticationRequestDto.builder()
                .tenant(tenant)
                .application("default")
                .userName(username)
                .password(password)
                .authType(IEnumAuth.Types.PWD)
                .build();

        try {
            ResponseEntity<AuthResponseDto> response = authService.authenticate(request);
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
                UI.getCurrent().navigate(target);

                Notification.show(I18n.t("auth.common.notification.welcome", username), 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                String message = I18n.t("auth.password.error.invalidCredentials");
                showError(message);
                errorContainer.setText(message);
                errorContainer.setVisible(true);
            }
        } catch (Exception ex) {
            String message = I18n.t("auth.password.error.serviceError");
            showError(message);
            errorContainer.setText(message);
            errorContainer.setVisible(true);
        }
    }

    @Override
    protected void onBeforeEnter(BeforeEnterEvent event) {
        errorContainer.setVisible(false);
        errorContainer.setText("");

        Optional<String> tenantOpt = event.getLocation().getQueryParameters().getSingleParameter("tenant");
        Optional<String> usernameOpt = event.getLocation().getQueryParameters().getSingleParameter("username");

        if (tenantOpt.isEmpty() || usernameOpt.isEmpty()) {
            event.forwardTo("login");
            return;
        }

        tenant = tenantOpt.get();
        username = usernameOpt.get();
        passwordField.clear();
    }
}