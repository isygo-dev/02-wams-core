package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.PublicAuthService;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@UIScope
@Route(value = "register")
@PageTitle("Create Account")
@PermitAll
public class RegisterView extends BaseLoginView {

    private final TextField firstNameField = new TextField(I18n.t("auth.register.field.firstName.label"));
    private final TextField lastNameField = new TextField(I18n.t("auth.register.field.lastName.label"));
    private final EmailField emailField = new EmailField(I18n.t("auth.register.field.email.label"));
    private final TextField phoneField = new TextField(I18n.t("auth.register.field.phone.label"));
    private final TextField roleField = new TextField(I18n.t("auth.register.field.role.label"));
    private final Button registerButton = new Button(I18n.t("auth.register.button.createAccount"), VaadinIcon.USER_STAR.create());
    private final Div errorBanner = createErrorBanner();
    private final Binder<RegisteredUserDto> binder = new Binder<>(RegisteredUserDto.class);

    @Autowired
    private PublicAuthService authService;

    public RegisterView() {
        configureAsAuthPage("register-view");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(firstNameField, lastNameField, emailField, phoneField, roleField);
        form.setWidthFull();

        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.addClassName("wams-auth-primary-btn");
        registerButton.addClickListener(e -> handleRegistration());

        Anchor loginLink = createLink("login", I18n.t("auth.register.link.signIn"));

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

        var card = createCard();
        card.addClassName("wams-auth-card--wide");
        card.add(createBrand(I18n.t("auth.register.title"), null),
                form, errorBanner, registerButton, loginLink, createFooter());

        add(card);
    }

    private void handleRegistration() {
        if (!binder.validate().isOk()) {
            showError(errorBanner, I18n.t("auth.register.error.validationErrors"));
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
                showError(errorBanner, I18n.t("auth.register.error.failed"));
            }
        } catch (Exception ex) {
            showError(errorBanner, I18n.t("auth.register.error.serviceError"));
        }
    }

    @Override
    protected void onBeforeEnter(BeforeEnterEvent event) {
        errorBanner.setVisible(false);
        errorBanner.setText("");
    }
}
