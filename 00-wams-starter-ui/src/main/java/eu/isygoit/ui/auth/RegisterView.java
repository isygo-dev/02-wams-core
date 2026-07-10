package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
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
import eu.isygoit.i18n.I18n;
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
@CssImport("./styles/auth.css")
public class RegisterView extends VerticalLayout implements BeforeEnterObserver {

    private final TextField firstNameField = new TextField(I18n.t("auth.register.field.firstName.label"));
    private final TextField lastNameField = new TextField(I18n.t("auth.register.field.lastName.label"));
    private final EmailField emailField = new EmailField(I18n.t("auth.register.field.email.label"));
    private final TextField phoneField = new TextField(I18n.t("auth.register.field.phone.label"));
    private final TextField roleField = new TextField(I18n.t("auth.register.field.role.label"));
    private final Button registerButton = new Button(I18n.t("auth.register.button.createAccount"), VaadinIcon.USER_STAR.create());
    private final Div errorContainer = new Div();
    private final Binder<RegisteredUserDto> binder = new Binder<>(RegisteredUserDto.class);

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
        H2 title = new H2(I18n.t("auth.register.title"));
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
        Anchor loginLink = new Anchor("login", I18n.t("auth.register.link.signIn"));
        loginLink.addClassName("login-link");

        // Footer
        Paragraph footer = new Paragraph(I18n.t("auth.common.footer"));
        footer.addClassName(LumoUtility.TextColor.TERTIARY);
        footer.addClassName(LumoUtility.FontSize.XXSMALL);
        footer.addClassName(LumoUtility.Margin.Top.MEDIUM);

        // Validation
        binder.forField(firstNameField)
                .asRequired(I18n.t("auth.register.validation.firstName.required"))
                .withValidator(new StringLengthValidator(I18n.t("auth.register.validation.minLength2"), 2, 50))
                .bind(RegisteredUserDto::getFirstName, RegisteredUserDto::setFirstName);

        binder.forField(lastNameField)
                .asRequired(I18n.t("auth.register.validation.lastName.required"))
                .withValidator(new StringLengthValidator(I18n.t("auth.register.validation.minLength2"), 2, 50))
                .bind(RegisteredUserDto::getLastName, RegisteredUserDto::setLastName);

        binder.forField(emailField)
                .asRequired(I18n.t("auth.register.validation.email.required"))
                .withValidator(new EmailValidator(I18n.t("auth.register.validation.email.invalid")))
                .bind(RegisteredUserDto::getEmail, RegisteredUserDto::setEmail);

        binder.forField(phoneField)
                .asRequired(I18n.t("auth.register.validation.phone.required"))
                .withValidator(new StringLengthValidator(I18n.t("auth.register.validation.phone.invalid"), 5, 20))
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
    }

    private void handleRegistration() {
        if (!binder.validate().isOk()) {
            showError(I18n.t("auth.register.error.validationErrors"));
            return;
        }

        RegisteredUserDto dto = new RegisteredUserDto();
        binder.writeBeanIfValid(dto);
        dto.setTenant("default");

        try {
            ResponseEntity<Boolean> response = authService.registerUser(dto);
            if (response.getStatusCode().is2xxSuccessful() && Boolean.TRUE.equals(response.getBody())) {
                Notification.show(I18n.t("auth.register.notification.success"), 5000,
                        Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                UI.getCurrent().navigate("login");
            } else {
                showError(I18n.t("auth.register.error.failed"));
            }
        } catch (Exception ex) {
            showError(I18n.t("auth.register.error.serviceError"));
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
}