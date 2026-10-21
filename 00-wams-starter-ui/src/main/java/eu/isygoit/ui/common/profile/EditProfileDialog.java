package eu.isygoit.ui.common.profile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.StringLengthValidator;
import eu.isygoit.api.ProfileServiceApi;
import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.ContactDto;
import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.enums.IEnumContact;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.BidiLtr;
import eu.isygoit.ui.common.component.PhoneField;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.common.validation.CommonValidators;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modal editor for the connected user's own information, aligned with {@link AccountDto} and
 * {@link AccountDetailsDto}:
 * <ul>
 *   <li><b>Personal</b> — first and last name, email, phone number, country, language;</li>
 *   <li><b>Address</b> — street, zip code, city, state, additional information;</li>
 *   <li><b>Contacts</b> — any number of extra mobile / phone / fax / email entries.</li>
 * </ul>
 * Everything else on the account (tenant, role, status, authentication method, code...) is managed by an
 * administrator or by its own screen and is not editable here. The backend ({@code updateProfile}) applies only
 * these fields to the authenticated account.
 */
public class EditProfileDialog extends BaseActionDialog {

    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final transient ProfileServiceApi profileService;
    private final transient AccountDto account;
    private final transient Consumer<AccountDto> onSaved;
    private final transient Binder<AccountDto> binder = new Binder<>(AccountDto.class);
    private final transient List<ContactRow> contactRows = new ArrayList<>();
    private final Div contactsHost = new Div();

    private TextField streetField;
    private TextField zipCodeField;
    private TextField cityField;
    private TextField stateField;
    private TextField additionalInfoField;

    public EditProfileDialog(ProfileServiceApi profileService, AccountDto account, Consumer<AccountDto> onSaved) {
        super(I18n.t("profile.edit"));
        this.profileService = profileService;
        this.account = account;
        this.onSaved = onSaved;

        DialogLayout.size(this, DialogLayout.WIDTH_M);
        setOkButtonText(I18n.t("profile.save"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout content = DialogLayout.stack();
        content.add(buildPersonalSection(), buildAddressSection(), buildContactsSection());
        addContent(content);
    }

    private static AccountDetailsDto detailsOf(AccountDto dto) {
        return dto.getAccountDetails();
    }

    private static AccountDetailsDto ensureAccountDetails(AccountDto dto) {
        AccountDetailsDto details = dto.getAccountDetails();
        if (details == null) {
            details = new AccountDetailsDto();
            dto.setAccountDetails(details);
        }
        return details;
    }

    private static AddressDto ensureAddress(AccountDto dto) {
        AccountDetailsDto details = ensureAccountDetails(dto);
        if (details.getAddress() == null) {
            details.setAddress(new AddressDto());
        }
        return details.getAddress();
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }

    private static TextField text(String labelKey, int maxLength) {
        TextField field = new TextField(I18n.t(labelKey));
        field.setWidthFull();
        field.setMaxLength(maxLength);
        field.setClearButtonVisible(true);
        return field;
    }

    /* ══════════════════════════════════════════════════════════════
     * Personal information
     * ══════════════════════════════════════════════════════════════ */

    private Component buildPersonalSection() {
        VerticalLayout section = DialogLayout.section(I18n.t("profile.section.personal"), VaadinIcon.USER);
        FormLayout form = DialogLayout.responsiveForm();

        TextField firstNameField = text("profile.field.firstName", 50);
        firstNameField.setRequiredIndicatorVisible(true);
        TextField lastNameField = text("profile.field.lastName", 50);
        lastNameField.setRequiredIndicatorVisible(true);

        EmailField emailField = new EmailField(I18n.t("profile.field.email"));
        emailField.setWidthFull();
        emailField.setRequiredIndicatorVisible(true);

        PhoneField phoneField = new PhoneField(I18n.t("profile.field.phone"));
        phoneField.setWidthFull();
        TextField countryField = text("profile.field.country", 100);

        ComboBox<IEnumLanguage.Types> languageCombo = new ComboBox<>(I18n.t("profile.field.language"));
        languageCombo.setWidthFull();
        languageCombo.setItems(IEnumLanguage.Types.values());
        languageCombo.setItemLabelGenerator(I18n::enumLabel);

        binder.forField(firstNameField)
                .withValidator(new StringLengthValidator(I18n.t("profile.validation.firstName.length"), 2, 50))
                .bind(dto -> detailsOf(dto) != null ? detailsOf(dto).getFirstName() : null,
                        (dto, value) -> ensureAccountDetails(dto).setFirstName(value));

        binder.forField(lastNameField)
                .withValidator(new StringLengthValidator(I18n.t("profile.validation.lastName.length"), 2, 50))
                .bind(dto -> detailsOf(dto) != null ? detailsOf(dto).getLastName() : null,
                        (dto, value) -> ensureAccountDetails(dto).setLastName(value));

        binder.forField(emailField)
                .asRequired(I18n.t("profile.validation.email.invalid"))
                .withValidator(CommonValidators.email(I18n.t("profile.validation.email.invalid")))
                .bind(AccountDto::getEmail, AccountDto::setEmail);

        binder.forField(phoneField)
                .withNullRepresentation("")
                .withValidator(CommonValidators.phone(I18n.t("profile.validation.phone.invalid")))
                .bind(AccountDto::getPhoneNumber, AccountDto::setPhoneNumber);

        binder.forField(countryField)
                .bind(dto -> detailsOf(dto) != null ? detailsOf(dto).getCountry() : null,
                        (dto, value) -> ensureAccountDetails(dto).setCountry(value));

        binder.forField(languageCombo).bind(AccountDto::getLanguage, AccountDto::setLanguage);

        binder.readBean(account);

        form.add(firstNameField, lastNameField);
        form.add(emailField, phoneField);
        form.add(countryField, languageCombo);
        section.add(form);
        return section;
    }

    /* ══════════════════════════════════════════════════════════════
     * Address
     * ══════════════════════════════════════════════════════════════ */

    private Component buildAddressSection() {
        VerticalLayout section = DialogLayout.section(I18n.t("profile.section.address"), VaadinIcon.HOME);
        FormLayout form = DialogLayout.responsiveForm();

        AddressDto address = detailsOf(account) != null ? detailsOf(account).getAddress() : null;

        streetField = text("profile.field.street", 255);
        zipCodeField = text("profile.field.zipCode", 20);
        cityField = text("profile.field.city", 100);
        stateField = text("profile.field.state", 100);
        additionalInfoField = text("profile.field.additionalInfo", 255);

        if (address != null) {
            streetField.setValue(nvl(address.getStreet()));
            zipCodeField.setValue(nvl(address.getZipCode()));
            cityField.setValue(nvl(address.getCity()));
            stateField.setValue(nvl(address.getState()));
            additionalInfoField.setValue(nvl(address.getAdditionalInfo()));
        }

        form.add(streetField, 2);
        form.add(zipCodeField, cityField);
        form.add(stateField, additionalInfoField);
        section.add(form);
        return section;
    }

    /* ══════════════════════════════════════════════════════════════
     * Contacts
     * ══════════════════════════════════════════════════════════════ */

    private Component buildContactsSection() {
        VerticalLayout section = DialogLayout.section(I18n.t("profile.section.contacts"), VaadinIcon.PHONE);
        contactsHost.addClassName("profile-contacts");
        contactsHost.setWidthFull();

        List<ContactDto> existing = detailsOf(account) != null ? detailsOf(account).getContacts() : null;
        if (existing != null) {
            existing.forEach(contact -> addContactRow(contact.getType(), contact.getValue()));
        }

        Button add = new Button(I18n.t("profile.contacts.add"), VaadinIcon.PLUS.create());
        add.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        add.addClickListener(e -> addContactRow(IEnumContact.Types.MOBILE, ""));

        section.add(DialogLayout.help(I18n.t("profile.contacts.help")), contactsHost, add);
        refreshContactsEmptyState();
        return section;
    }

    private void addContactRow(IEnumContact.Types type, String value) {
        ContactRow row = new ContactRow(type, value);
        contactRows.add(row);
        contactsHost.add(row.layout);
        refreshContactsEmptyState();
    }

    private void removeContactRow(ContactRow row) {
        contactRows.remove(row);
        contactsHost.remove(row.layout);
        refreshContactsEmptyState();
    }

    private void refreshContactsEmptyState() {
        contactsHost.getElement().setAttribute("data-empty", String.valueOf(contactRows.isEmpty()));
    }

    @Override
    protected boolean onOk() {
        boolean contactsValid = contactRows.stream().map(ContactRow::validate).reduce(true, Boolean::logicalAnd);
        if (!binder.validate().isOk() || !contactsValid) {
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

        // address and contacts are sent as a whole: what is empty here is cleared on the account
        AddressDto address = ensureAddress(updated);
        address.setStreet(streetField.getValue());
        address.setZipCode(zipCodeField.getValue());
        address.setCity(cityField.getValue());
        address.setState(stateField.getValue());
        address.setAdditionalInfo(additionalInfoField.getValue());
        AddressDto previous = detailsOf(account) != null ? detailsOf(account).getAddress() : null;
        if (previous != null) {
            address.setCountry(previous.getCountry());   // not editable here, keep it as it is
        }

        List<ContactDto> contacts = new ArrayList<>();
        for (ContactRow row : contactRows) {
            ContactDto dto = row.toDto();
            if (dto != null) {
                contacts.add(dto);
            }
        }
        ensureAccountDetails(updated).setContacts(contacts);

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

    /* ══════════════════════════════════════════════════════════════
     * Save
     * ══════════════════════════════════════════════════════════════ */

    /**
     * One contact: a type, a value and a remove button.
     */
    private final class ContactRow {
        private final ComboBox<IEnumContact.Types> typeCombo = new ComboBox<>(I18n.t("profile.contacts.type"));
        private final TextField valueField = new TextField(I18n.t("profile.contacts.value"));
        private final com.vaadin.flow.component.orderedlayout.HorizontalLayout layout;

        ContactRow(IEnumContact.Types type, String value) {
            typeCombo.setItems(IEnumContact.Types.values());
            typeCombo.setItemLabelGenerator(t -> I18n.t("profile.contact.type." + t.name()));
            typeCombo.setValue(type);
            typeCombo.setAllowCustomValue(false);
            typeCombo.setWidth("11rem");

            valueField.setValue(nvl(value));
            valueField.setMaxLength(100);
            valueField.setWidthFull();
            applyMaskFor(type);
            typeCombo.addValueChangeListener(e -> applyMaskFor(e.getValue()));

            Button remove = new Button(VaadinIcon.TRASH.create());
            remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            remove.setTooltipText(I18n.t("profile.contacts.remove"));
            remove.setAriaLabel(I18n.t("profile.contacts.remove"));
            remove.addClickListener(e -> removeContactRow(this));

            layout = new com.vaadin.flow.component.orderedlayout.HorizontalLayout(typeCombo, valueField, remove);
            layout.setWidthFull();
            layout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
            layout.expand(valueField);
        }

        /**
         * Restricts the characters the user can type to match the selected contact type:
         * a phone-like mask for mobile/phone/fax, unrestricted for email.
         */
        private void applyMaskFor(IEnumContact.Types type) {
            if (type == IEnumContact.Types.EMAIL) {
                valueField.setAllowedCharPattern(null);
                BidiLtr.clear(valueField.getElement());
            } else {
                valueField.setAllowedCharPattern(PhoneField.ALLOWED_CHAR_PATTERN);
                BidiLtr.apply(valueField.getElement());
            }
        }

        /**
         * Valid when empty (the entry is dropped) or when the value fits its type.
         */
        boolean validate() {
            String value = valueField.getValue() == null ? "" : valueField.getValue().trim();
            boolean ok = value.isEmpty()
                    || (typeCombo.getValue() == IEnumContact.Types.EMAIL
                    ? value.matches(EMAIL_PATTERN)
                    : value.matches(PhoneField.PATTERN));
            if (typeCombo.getValue() == null && !value.isEmpty()) {
                ok = false;
            }
            valueField.setInvalid(!ok);
            valueField.setErrorMessage(ok ? null : I18n.t("profile.validation.contact.invalid"));
            return ok;
        }

        ContactDto toDto() {
            String value = valueField.getValue() == null ? "" : valueField.getValue().trim();
            if (value.isEmpty() || typeCombo.getValue() == null) {
                return null;
            }
            ContactDto dto = new ContactDto();
            dto.setType(typeCombo.getValue());
            dto.setValue(value);
            return dto;
        }
    }
}
