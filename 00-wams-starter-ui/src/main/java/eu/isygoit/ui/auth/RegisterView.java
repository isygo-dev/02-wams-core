package eu.isygoit.ui.auth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.enums.IEnumAccountOrigin;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.auth.AuthServiceFacade.Failure;
import jakarta.annotation.security.PermitAll;
import org.springframework.stereotype.Component;

@Component
@UIScope
@Route(value = AuthRoutes.REGISTER)
@PermitAll
public class RegisterView extends BaseLoginView {

    private final TextField firstNameField = new TextField(I18n.t("auth.register.field.firstName.label"));
    private final TextField lastNameField = new TextField(I18n.t("auth.register.field.lastName.label"));
    private final EmailField emailField = new EmailField(I18n.t("auth.register.field.email.label"));
    private final TextField phoneField = new TextField(I18n.t("auth.register.field.phone.label"));
    private final TextField organisationField = new TextField(I18n.t("auth.register.field.organisation.label"));
    private final TextField roleField = new TextField(I18n.t("auth.register.field.role.label"));
    private final Button registerButton = new Button(
            I18n.t("auth.register.button.createAccount"), VaadinIcon.USER_STAR.create());
    private final Binder<RegisteredUserDto> binder = new Binder<>(RegisteredUserDto.class);
    private final AuthServiceFacade authService;
    private boolean submitting;

    public RegisterView(AuthServiceFacade authService) {
        super("auth.page.title.register");
        this.authService = authService;
        setWideLayout(true);

        setAutocomplete(firstNameField, "given-name");
        setAutocomplete(lastNameField, "family-name");
        setAutocomplete(emailField, "email");
        setAutocomplete(phoneField, "tel");
        setAutocomplete(organisationField, "organization");
        setAutocomplete(roleField, "organization-title");
        phoneField.getElement().setAttribute("inputmode", "tel");
        describeErrors(firstNameField);
        describeErrors(lastNameField);
        describeErrors(emailField);
        describeErrors(phoneField);
        describeErrors(organisationField);
        describeErrors(roleField);
        setRequired(firstNameField);
        setRequired(lastNameField);
        setRequired(emailField);
        setRequired(phoneField);
        setRequired(organisationField);

        firstNameField.addValueChangeListener(event -> clearError());
        lastNameField.addValueChangeListener(event -> clearError());
        emailField.addValueChangeListener(event -> clearError());
        phoneField.addValueChangeListener(event -> clearError());
        organisationField.addValueChangeListener(event -> clearError());
        roleField.addValueChangeListener(event -> clearError());

        AuthFormGrid form = new AuthFormGrid();
        form.add(firstNameField, lastNameField, emailField, phoneField, organisationField, roleField);
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.addClickListener(event -> handleRegistration());

        configureBinder();
        binder.setBean(new RegisteredUserDto());

        com.vaadin.flow.component.html.Anchor loginLink =
                new com.vaadin.flow.component.html.Anchor(AuthRoutes.LOGIN,
                        I18n.t("auth.register.link.signIn"));
        addToCard(new BrandHeader(I18n.t("auth.register.title"), null),
                form, errorBanner, registerButton, loginLink, new AuthFooter());
    }

    private void configureBinder() {
        binder.forField(firstNameField)
                .asRequired(I18n.t("auth.register.validation.firstName.required"))
                .withValidator(AuthValidators.length(
                        I18n.t("auth.register.validation.minLength2"), 2, 50))
                .bind(RegisteredUserDto::getFirstName, RegisteredUserDto::setFirstName);
        binder.forField(lastNameField)
                .asRequired(I18n.t("auth.register.validation.lastName.required"))
                .withValidator(AuthValidators.length(
                        I18n.t("auth.register.validation.minLength2"), 2, 50))
                .bind(RegisteredUserDto::getLastName, RegisteredUserDto::setLastName);
        binder.forField(emailField)
                .asRequired(I18n.t("auth.register.validation.email.required"))
                .withValidator(AuthValidators.email(I18n.t("auth.register.validation.email.invalid")))
                .bind(RegisteredUserDto::getEmail, RegisteredUserDto::setEmail);
        binder.forField(phoneField)
                .asRequired(I18n.t("auth.register.validation.phone.required"))
                .withValidator(AuthValidators.length(
                        I18n.t("auth.register.validation.phone.invalid"), 5, 20))
                .bind(RegisteredUserDto::getPhoneNumber, RegisteredUserDto::setPhoneNumber);
        binder.forField(organisationField)
                .asRequired(I18n.t("auth.register.validation.organisation.required"))
                .withValidator(AuthValidators.length(
                        I18n.t("auth.register.validation.organisation.minLength"), 2, 100))
                .bind(RegisteredUserDto::getOrganisation, RegisteredUserDto::setOrganisation);
        binder.forField(roleField)
                .bind(RegisteredUserDto::getFunctionRole, RegisteredUserDto::setFunctionRole);
    }

    private static void setAutocomplete(com.vaadin.flow.component.Component field, String value) {
        field.getElement().setAttribute("autocomplete", value);
    }

    private static void setRequired(TextField field) {
        field.setRequiredIndicatorVisible(true);
    }

    private static void setRequired(EmailField field) {
        field.setRequiredIndicatorVisible(true);
    }

    private void handleRegistration() {
        if (submitting) {
            return;
        }

        RegisteredUserDto request = new RegisteredUserDto();
        if (!binder.writeBeanIfValid(request)) {
            showError(I18n.t("auth.register.error.validationErrors"),
                    Failure.REGISTRATION_REJECTED, firstInvalidField());
            return;
        }
        clearError();
        request.setOrigin(IEnumAccountOrigin.Types.SIGNUP);
        request.setTenant(TenantConstants.DEFAULT_TENANT_NAME);
        setLoading(true);
        UI ui = UI.getCurrent();

        authService.registerUser(request).whenComplete((result, failure) ->
                ui.access(() -> {
                    setLoading(false);
                    if (failure != null) {
                        reportUnexpectedFailure(failure);
                        showError(I18n.t("auth.register.error.serviceError"), Failure.SERVICE_ERROR);
                    } else if (!result.succeeded()) {
                        showError(failureMessage(result.failure(), "auth.register.error.failed",
                                "auth.register.error.serviceError"), result.failure());
                    } else {
                        ui.navigate(AuthRoutes.REGISTRATION_CONFIRMATION);
                    }
                }));
    }

    private com.vaadin.flow.component.Component firstInvalidField() {
        if (firstNameField.isInvalid()) {
            return firstNameField;
        }
        if (lastNameField.isInvalid()) {
            return lastNameField;
        }
        if (emailField.isInvalid()) {
            return emailField;
        }
        if (phoneField.isInvalid()) {
            return phoneField;
        }
        return organisationField;
    }

    private void setLoading(boolean loading) {
        submitting = loading;
        firstNameField.setEnabled(!loading);
        lastNameField.setEnabled(!loading);
        emailField.setEnabled(!loading);
        phoneField.setEnabled(!loading);
        organisationField.setEnabled(!loading);
        roleField.setEnabled(!loading);
        setButtonLoading(registerButton, loading);
    }

    @Override
    protected void onBeforeEnter(BeforeEnterEvent event) {
        clearError();
    }
}
