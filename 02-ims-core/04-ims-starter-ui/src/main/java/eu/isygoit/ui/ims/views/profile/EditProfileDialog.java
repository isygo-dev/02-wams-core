package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ProfileService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import org.springframework.http.ResponseEntity;

import java.util.function.Consumer;

/**
 * Modal editor for the user's own personal details (first/last name, email, phone).
 * Read-only account attributes (tenant, role, status, ...) are shown in the Overview
 * tab and are not editable here.
 */
public class EditProfileDialog extends BaseActionDialog {

    private final transient ProfileService profileService;
    private final transient AccountDto account;
    private final transient Consumer<AccountDto> onSaved;
    private final transient Binder<AccountDto> binder = new Binder<>(AccountDto.class);

    public EditProfileDialog(ProfileService profileService, AccountDto account, Consumer<AccountDto> onSaved) {
        super(I18n.t("profile.edit"));
        this.profileService = profileService;
        this.account = account;
        this.onSaved = onSaved;

        setOkButtonText(I18n.t("profile.save"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_PRIMARY);
        addContent(buildForm());
    }

    private FormLayout buildForm() {
        FormLayout form = new FormLayout();
        form.addClassName("profile-form");

        TextField firstNameField = new TextField(I18n.t("profile.field.firstName"));
        firstNameField.setWidthFull();
        firstNameField.setRequiredIndicatorVisible(true);

        TextField lastNameField = new TextField(I18n.t("profile.field.lastName"));
        lastNameField.setWidthFull();
        lastNameField.setRequiredIndicatorVisible(true);

        EmailField emailField = new EmailField(I18n.t("profile.field.email"));
        emailField.setWidthFull();
        emailField.setRequiredIndicatorVisible(true);

        TextField phoneField = new TextField(I18n.t("profile.field.phone"));
        phoneField.setWidthFull();

        binder.readBean(account);

        AccountDetailsDto details = account.getAccountDetails();
        firstNameField.setValue(details != null && details.getFirstName() != null ? details.getFirstName() : "");
        lastNameField.setValue(details != null && details.getLastName() != null ? details.getLastName() : "");
        emailField.setValue(account.getEmail() != null ? account.getEmail() : "");
        phoneField.setValue(account.getPhoneNumber() != null ? account.getPhoneNumber() : "");

        binder.forField(firstNameField)
                .withValidator(new StringLengthValidator(I18n.t("profile.validation.firstName.length"), 2, 50))
                .bind(dto -> dto.getAccountDetails() != null ? dto.getAccountDetails().getFirstName() : null,
                        (dto, value) -> ensureAccountDetails(dto).setFirstName(value));

        binder.forField(lastNameField)
                .withValidator(new StringLengthValidator(I18n.t("profile.validation.lastName.length"), 2, 50))
                .bind(dto -> dto.getAccountDetails() != null ? dto.getAccountDetails().getLastName() : null,
                        (dto, value) -> ensureAccountDetails(dto).setLastName(value));

        binder.forField(emailField)
                .withValidator(new EmailValidator(I18n.t("profile.validation.email.invalid")))
                .bind(AccountDto::getEmail, AccountDto::setEmail);

        binder.forField(phoneField)
                .bind(AccountDto::getPhoneNumber, AccountDto::setPhoneNumber);

        form.add(firstNameField, lastNameField, emailField, phoneField);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("420px", 2));
        return form;
    }

    @Override
    protected boolean onOk() {
        if (!binder.isValid()) {
            append(I18n.t("profile.validation.error"));
            return false;
        }

        AccountDto updated = new AccountDto();
        try {
            binder.writeBean(updated);
        } catch (Exception e) {
            append(I18n.t("profile.validation.error"));
            return false;
        }

        try {
            ResponseEntity<AccountDto> response = profileService.updateProfile(updated);
            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                if (onSaved != null) {
                    onSaved.accept(response.getBody());
                }
                append(I18n.t("profile.update.success"));
                return true;
            }
            append(I18n.t("profile.update.error"));
            return false;
        } catch (Exception e) {
            append(I18n.t("profile.update.error"));
            return false;
        }
    }

    private static AccountDetailsDto ensureAccountDetails(AccountDto dto) {
        AccountDetailsDto details = dto.getAccountDetails();
        if (details == null) {
            details = new AccountDetailsDto();
            dto.setAccountDetails(details);
        }
        return details;
    }
}
