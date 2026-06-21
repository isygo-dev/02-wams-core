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
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.request.AuthenticationRequestDto;
import eu.isygoit.dto.response.AuthResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.remote.ims.PublicAuthService;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
@UIScope
@Route(value = "login/password")
@PageTitle("Password Login")
@PermitAll
public class PasswordView extends VerticalLayout implements BeforeEnterObserver {

    private final PasswordField passwordField = new PasswordField("Password");
    private final Button loginButton = new Button("Sign in", VaadinIcon.SIGN_IN.create());
    private final Div errorContainer = new Div();
    private boolean stylesInjected = false;

    private String tenant;
    private String username;
    private String redirectTarget;

    @Autowired
    private PublicAuthService authService;

    public PasswordView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("password-view");

        // Brand
        Div brand = new Div();
        brand.addClassName("brand");
        Avatar logo = new Avatar("KMS/IMS");
        logo.setColorIndex(1);
        logo.setWidth("56px");
        logo.setHeight("56px");
        H2 title = new H2("Enter Password");
        title.addClassName(LumoUtility.FontWeight.BOLD);
        title.addClassName(LumoUtility.Margin.NONE);
        brand.add(logo, title);

        // Password field
        passwordField.setWidthFull();
        passwordField.setPlaceholder("Your password");
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());

        // Login button
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        loginButton.addClickListener(e -> handlePasswordLogin());

        // Error container
        errorContainer.addClassName("error-container");
        errorContainer.setVisible(false);

        // Back link
        Anchor backLink = new Anchor("login", "← Back");
        backLink.addClassName("back-link");

        // Footer
        Paragraph footer = new Paragraph("© 2026 KMS/IMS Platform");
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

        addAttachListener(event -> {
            if (!stylesInjected) {
                injectResponsiveStyles();
                stylesInjected = true;
            }
        });
    }

    private void handlePasswordLogin() {
        String password = passwordField.getValue();
        if (password.isBlank()) {
            showError("Password is required");
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
                VaadinSession.getCurrent().setAttribute("user", username);
                VaadinSession.getCurrent().setAttribute("accessToken", authResponse.getAccessToken());

                String target = (redirectTarget != null) ? redirectTarget : "kms";
                UI.getCurrent().navigate(target);

                Notification.show("Welcome " + username + "!", 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                showError("Invalid credentials. Please try again.");
            }
        } catch (Exception ex) {
            showError("Authentication service error. Please try again.");
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
        // If already authenticated, redirect to target or default
        if (VaadinSession.getCurrent().getAttribute("user") != null) {
            String target = event.getLocation().getQueryParameters()
                    .getSingleParameter("redirect")
                    .filter(this::isSafeInternalPath)
                    .orElse("kms");
            event.forwardTo(target);
            return;
        }

        Optional<String> tenantOpt = event.getLocation().getQueryParameters().getSingleParameter("tenant");
        Optional<String> usernameOpt = event.getLocation().getQueryParameters().getSingleParameter("username");
        redirectTarget = event.getLocation().getQueryParameters()
                .getSingleParameter("redirect")
                .filter(this::isSafeInternalPath)
                .orElse(null);

        if (tenantOpt.isEmpty() || usernameOpt.isEmpty()) {
            event.forwardTo("login");
            return;
        }

        tenant = tenantOpt.get();
        username = usernameOpt.get();
        errorContainer.setVisible(false);
        passwordField.clear();
    }

    private boolean isSafeInternalPath(String path) {
        return StringUtils.hasText(path) && path.startsWith("/") && !path.contains("..") && !path.contains("//");
    }

    private void injectResponsiveStyles() {
        String css = """
                .password-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .password-wrapper {
                    background: var(--lumo-base-color);
                    border-radius: var(--lumo-border-radius-xl);
                    box-shadow: var(--lumo-box-shadow-m);
                    padding: var(--lumo-space-l);
                }
                .password-view .brand {
                    text-align: center;
                    margin-bottom: var(--lumo-space-m);
                }
                .password-view .brand h2 {
                    font-size: var(--lumo-font-size-xl);
                    letter-spacing: -0.5px;
                }
                .password-view vaadin-password-field {
                    width: 100%;
                }
                .password-view .error-container {
                    background: var(--lumo-error-color-10pct);
                    color: var(--lumo-error-text-color);
                    padding: var(--lumo-space-s);
                    border-radius: var(--lumo-border-radius-m);
                    font-size: var(--lumo-font-size-xs);
                    width: 100%;
                    text-align: center;
                }
                .password-view .back-link {
                    color: var(--lumo-primary-text-color);
                    font-size: var(--lumo-font-size-s);
                    margin-top: var(--lumo-space-m);
                }
                @media (max-width: 480px) {
                    .password-wrapper {
                        padding: var(--lumo-space-m);
                        border-radius: var(--lumo-border-radius-l);
                        margin: var(--lumo-space-m);
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}