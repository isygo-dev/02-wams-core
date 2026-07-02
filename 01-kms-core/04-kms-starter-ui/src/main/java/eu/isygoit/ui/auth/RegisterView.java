package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.remote.ims.PublicAuthService;
import eu.isygoit.util.SecurityUtils;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@UIScope
@Route(value = "register")
@PageTitle("Create Account")
@PermitAll
public class RegisterView extends VerticalLayout implements BeforeEnterObserver {

    private final TextField firstNameField = new TextField("First name");
    private final TextField lastNameField = new TextField("Last name");
    private final EmailField emailField = new EmailField("Email");
    private final TextField phoneField = new TextField("Phone number");
    private final TextField roleField = new TextField("Role (optional)");
    private final Button registerButton = new Button("Create account", VaadinIcon.USER_STAR.create());
    private final Div errorContainer = new Div();
    private final Binder<RegisteredUserDto> binder = new Binder<>(RegisteredUserDto.class);
    private boolean stylesInjected = false;

    @Autowired
    private PublicAuthService authService;

    public RegisterView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("register-view");

        // Brand
        Div brand = new Div();
        brand.addClassName("brand");
        Avatar logo = new Avatar("KMS/IMS");
        logo.setColorIndex(1);
        logo.setWidth("56px");
        logo.setHeight("56px");
        H2 title = new H2("Create Account");
        title.addClassName(LumoUtility.FontWeight.BOLD);
        title.addClassName(LumoUtility.Margin.NONE);
        brand.add(logo, title);

        // Form
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(firstNameField, lastNameField, emailField, phoneField, roleField);
        form.setWidthFull();
        form.addClassName("register-form");

        // Error container
        errorContainer.addClassName("error-container");
        errorContainer.setVisible(false);

        // Register button
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();
        registerButton.addClickListener(e -> handleRegistration());

        // Login link
        Anchor loginLink = new Anchor("login", "Already have an account? Sign in");
        loginLink.addClassName("login-link");

        // Footer
        Paragraph footer = new Paragraph("© 2026 KMS/IMS Platform");
        footer.addClassName(LumoUtility.TextColor.TERTIARY);
        footer.addClassName(LumoUtility.FontSize.XXSMALL);
        footer.addClassName(LumoUtility.Margin.Top.MEDIUM);

        // Validation
        binder.forField(firstNameField)
                .asRequired("First name is required")
                .withValidator(new StringLengthValidator("At least 2 characters", 2, 50))
                .bind(RegisteredUserDto::getFirstName, RegisteredUserDto::setFirstName);

        binder.forField(lastNameField)
                .asRequired("Last name is required")
                .withValidator(new StringLengthValidator("At least 2 characters", 2, 50))
                .bind(RegisteredUserDto::getLastName, RegisteredUserDto::setLastName);

        binder.forField(emailField)
                .asRequired("Email is required")
                .withValidator(new EmailValidator("Please enter a valid email address"))
                .bind(RegisteredUserDto::getEmail, RegisteredUserDto::setEmail);

        binder.forField(phoneField)
                .asRequired("Phone number is required")
                .withValidator(new StringLengthValidator("Enter a valid phone number", 5, 20))
                .bind(RegisteredUserDto::getPhoneNumber, RegisteredUserDto::setPhoneNumber);

        binder.forField(roleField)
                .bind(RegisteredUserDto::getFunctionRole, RegisteredUserDto::setFunctionRole);

        // Main wrapper
        VerticalLayout wrapper = new VerticalLayout(brand, form, errorContainer, registerButton, loginLink, footer);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.setMaxWidth("420px");
        wrapper.setWidthFull();
        wrapper.setPadding(true);
        wrapper.setSpacing(true);
        wrapper.addClassName("register-wrapper");

        add(wrapper);

        addAttachListener(event -> {
            if (!stylesInjected) {
                injectResponsiveStyles();
                stylesInjected = true;
            }
        });
    }

    private void handleRegistration() {
        if (!binder.validate().isOk()) {
            showError("Please fix the validation errors");
            return;
        }

        RegisteredUserDto dto = new RegisteredUserDto();
        binder.writeBeanIfValid(dto);
        dto.setTenant("default");

        try {
            ResponseEntity<Boolean> response = authService.registerUser(dto);
            if (response.getStatusCode().is2xxSuccessful() && Boolean.TRUE.equals(response.getBody())) {
                Notification.show("Account created successfully! Check your email for password setup.", 5000,
                        Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                UI.getCurrent().navigate("login");
            } else {
                showError("Registration failed. Please try again.");
            }
        } catch (Exception ex) {
            showError("Service error. Please try again later.");
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
            Optional<String> redirectOpt = event.getLocation()
                    .getQueryParameters()
                    .getSingleParameter("redirect")
                    .filter(SecurityUtils::isSafeInternalPath);
            String target = redirectOpt.orElse("landing");
            event.forwardTo(target);
        }
        errorContainer.setVisible(false);
    }

    private void injectResponsiveStyles() {
        String css = """
                .register-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .register-wrapper {
                    background: var(--lumo-base-color);
                    border-radius: var(--lumo-border-radius-xl);
                    box-shadow: var(--lumo-box-shadow-m);
                    padding: var(--lumo-space-l);
                }
                .register-view .brand {
                    text-align: center;
                    margin-bottom: var(--lumo-space-m);
                }
                .register-view .brand h2 {
                    font-size: var(--lumo-font-size-xl);
                    letter-spacing: -0.5px;
                }
                .register-view .register-form {
                    width: 100%;
                }
                .register-view .error-container {
                    background: var(--lumo-error-color-10pct);
                    color: var(--lumo-error-text-color);
                    padding: var(--lumo-space-s);
                    border-radius: var(--lumo-border-radius-m);
                    font-size: var(--lumo-font-size-xs);
                    width: 100%;
                    text-align: center;
                }
                .register-view .login-link {
                    color: var(--lumo-primary-text-color);
                    font-size: var(--lumo-font-size-s);
                    margin-top: var(--lumo-space-s);
                }
                @media (max-width: 480px) {
                    .register-wrapper {
                        padding: var(--lumo-space-m);
                        border-radius: var(--lumo-border-radius-l);
                        margin: var(--lumo-space-m);
                    }
                    .register-view .brand h2 {
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