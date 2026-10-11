package eu.isygoit.ui.ims.views.account.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.constants.AccountTypeConstants;
import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.ContactDto;
import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.common.dialog.ProfilePhotoSection;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import eu.isygoit.ui.ims.views.common.AbstractImsFormDialog;
import eu.isygoit.ui.ims.views.common.ImsDialogSupport;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared form of the account create/update dialogs: every editable
 * {@link AccountDto} field is declared here once. {@code id} is never shown and
 * {@code code} is always read-only (assigned by the server). Server-managed
 * values (full name, origin, authentication type, system status, ...) are only
 * shown in the details dialog. Subclasses only decide how the DTO is persisted.
 *
 * <p>The profile photo is <b>downloaded</b> through {@link AccountImageService}
 * (see {@link #downloadProfileImage(Long)}); the loading is triggered from
 * {@link #fillFrom(AccountDto)} via
 * {@link #showExistingPhoto(ProfilePhotoSection, Long)}.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
@Slf4j
abstract class AbstractAccountFormDialog extends AbstractImsFormDialog {

    final AccountManagementView parentView;
    final AccountService accountService;
    final AccountImageService accountImageService;
    private final TenantService tenantService;
    private final String messagePrefix;

    ProfilePhotoSection photoSection;

    ComboBox<String> tenantCombo;
    ComboBox<String> accountTypeCombo;
    TextField codeField;
    EmailField emailField;
    TextField firstNameField;
    TextField lastNameField;
    TextField phoneNumberField;
    ComboBox<IEnumLanguage.Types> languageCombo;
    TextField functionRoleField;
    Checkbox isAdminCheckbox;
    ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;

    TextField countryField;
    TextField streetField;
    TextField zipCodeField;
    TextField cityField;
    TextField stateField;
    TextField addressCountryField;
    TextField additionalInfoField;
    AccountContactsEditor contactsEditor;

    private List<TenantDto> tenants = new ArrayList<>();

    AbstractAccountFormDialog(String title,
                              Runnable onSuccess,
                              String messagePrefix,
                              AccountManagementView parentView,
                              AccountService accountService,
                              AccountImageService accountImageService,
                              TenantService tenantService) {
        super(title, onSuccess);
        this.messagePrefix = messagePrefix;
        this.parentView = parentView;
        this.accountService = accountService;
        this.accountImageService = accountImageService;
        this.tenantService = tenantService;
    }

    /** The DTO that receives the form values on save. */
    abstract AccountDto target();

    /** Persists the DTO; returns the account id, or null after appending an error message. */
    abstract Long persist(AccountDto dto);

    /** i18n key of the photo upload button. */
    abstract String uploadLabelKey();

    /** True when a picture must be selected before saving. */
    abstract boolean imageRequired();

    /* ------------------------------------------------------------------
     * Photo download hook
     * ------------------------------------------------------------------ */

    @Override
    protected byte[] downloadProfileImage(Long id) {
        if (accountImageService == null || id == null) return null;
        try {
            ResponseEntity<Resource> response = accountImageService.downloadImage(id);
            if (response != null
                    && response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                return response.getBody().getContentAsByteArray();
            }
        } catch (Exception e) {
            log.warn("Failed to download avatar for account {}", id, e);
        }
        return null;
    }

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_L);

        photoSection = new ProfilePhotoSection(
                I18n.t("ims.account.dialog.photo.title"),
                I18n.t("ims.account.dialog.photo.help"),
                I18n.t(uploadLabelKey()),
                I18n.t("ims.account.dialog.photo.remove"));

        VerticalLayout root = DialogLayout.stack();
        root.add(photoSection, buildTabs());
        add(root);

        loadTenants();
    }

    private Component buildTabs() {
        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add(new Tab(I18n.t("ims.account.dialog.tab.identity")), buildIdentityTab());
        tabs.add(new Tab(I18n.t("ims.account.dialog.tab.account")), buildAccountTab());
        tabs.add(new Tab(I18n.t("ims.account.dialog.tab.contact")), buildContactTab());
        return tabs;
    }

    private Component buildIdentityTab() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.account.dialog.section.identity"), VaadinIcon.USER);
        FormLayout form = DialogLayout.responsiveForm();

        codeField = new TextField(I18n.t("ims.account.dialog.field.code"));
        codeField.setReadOnly(true);

        emailField = new EmailField(I18n.t("ims.account.dialog.field.email"));
        emailField.setRequiredIndicatorVisible(true);
        emailField.setPlaceholder(I18n.t("ims.account.dialog.field.email.placeholder"));

        firstNameField = new TextField(I18n.t("ims.account.dialog.field.first.name"));
        firstNameField.setPlaceholder(I18n.t("ims.account.dialog.field.first.name.placeholder"));

        lastNameField = new TextField(I18n.t("ims.account.dialog.field.last.name"));
        lastNameField.setPlaceholder(I18n.t("ims.account.dialog.field.last.name.placeholder"));

        phoneNumberField = new TextField(I18n.t("ims.account.dialog.field.phone"));
        phoneNumberField.setPlaceholder(I18n.t("ims.account.dialog.field.phone.placeholder"));

        languageCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.language"));
        languageCombo.setItems(IEnumLanguage.Types.values());
        languageCombo.setItemLabelGenerator(language -> ImsEnumTag.label(language, "ims.enum.language"));
        languageCombo.setRenderer(ImsEnumTag.renderer("ims.enum.language"));

        form.add(codeField, emailField, firstNameField, lastNameField, phoneNumberField, languageCombo);
        section.add(form);
        return section;
    }

    private Component buildAccountTab() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.account.dialog.section.account"), VaadinIcon.SHIELD);
        FormLayout form = DialogLayout.responsiveForm();

        tenantCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.tenant"));
        tenantCombo.setRequiredIndicatorVisible(true);
        tenantCombo.setPlaceholder(I18n.t("ims.account.dialog.field.tenant.placeholder"));
        tenantCombo.setItemLabelGenerator(item -> {
            TenantDto tenant = findTenantByCode(item);
            return tenant != null ? tenant.getName() + " (" + tenant.getCode() + ")" : item;
        });
        tenantCombo.setAllowCustomValue(false);
        tenantCombo.setReadOnly(isTenantReadOnly());

        accountTypeCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.account.type"));
        accountTypeCombo.setRequiredIndicatorVisible(true);
        accountTypeCombo.setItems(
                AccountTypeConstants.SUPER_ADMIN,
                AccountTypeConstants.TENANT_ADMIN,
                AccountTypeConstants.TENANT_USER
        );
        accountTypeCombo.setAllowCustomValue(true);
        accountTypeCombo.setPlaceholder(I18n.t("ims.account.dialog.field.account.type.placeholder"));

        functionRoleField = new TextField(I18n.t("ims.account.dialog.field.function.role"));
        functionRoleField.setPlaceholder(I18n.t("ims.account.dialog.field.function.role.placeholder"));

        adminStatusCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.admin.status"));
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setItemLabelGenerator(status -> ImsEnumTag.label(status, null));
        adminStatusCombo.setRenderer(ImsEnumTag.renderer(null));

        isAdminCheckbox = new Checkbox(I18n.t("ims.account.dialog.field.is.admin"));

        form.add(tenantCombo, accountTypeCombo, functionRoleField, adminStatusCombo, isAdminCheckbox);
        section.add(form);
        return section;
    }

    private Component buildContactTab() {
        VerticalLayout addressSection = DialogLayout.section(
                I18n.t("ims.account.dialog.section.address"), VaadinIcon.MAP_MARKER);
        FormLayout addressForm = DialogLayout.responsiveForm();

        countryField = new TextField(I18n.t("ims.account.dialog.field.country"));

        streetField = new TextField(I18n.t("ims.account.dialog.field.street"));

        zipCodeField = new TextField(I18n.t("ims.account.dialog.field.zip.code"));

        cityField = new TextField(I18n.t("ims.account.dialog.field.city"));

        stateField = new TextField(I18n.t("ims.account.dialog.field.state"));

        addressCountryField = new TextField(I18n.t("ims.account.dialog.field.address.country"));

        additionalInfoField = new TextField(I18n.t("ims.account.dialog.field.additional.info"));

        addressForm.add(countryField, streetField, zipCodeField, cityField, stateField,
                addressCountryField, additionalInfoField);
        addressForm.setColspan(streetField, 2);
        addressForm.setColspan(additionalInfoField, 2);
        addressSection.add(addressForm);

        VerticalLayout contactsSection = DialogLayout.section(
                I18n.t("ims.account.dialog.section.contacts"), VaadinIcon.PHONE);
        contactsEditor = new AccountContactsEditor();
        contactsSection.add(contactsEditor);

        VerticalLayout tab = DialogLayout.stack();
        tab.add(addressSection, contactsSection);
        return tab;
    }

    /** True when the tenant cannot be changed (update). */
    abstract boolean isTenantReadOnly();

    /** Loads the tenant choices. */
    private void loadTenants() {
        parentView.showLoading(true);
        try {
            ResponseEntity<List<TenantDto>> response = tenantService.findAllList();
            if (response.getBody() != null) {
                tenants = response.getBody();
                tenantCombo.setItems(tenants.stream().map(TenantDto::getCode).collect(Collectors.toList()));
            }
        } catch (FeignException ex) {
            append(I18n.t("ims.account.dialog.load.tenants.error", ImsDialogSupport.extractErrorMessage(ex)));
        } catch (Exception e) {
            append(I18n.t("ims.account.dialog.load.tenants.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
    }

    private TenantDto findTenantByCode(String code) {
        return tenants.stream()
                .filter(t -> t.getCode() != null && t.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    /** Fills the form from an existing account. */
    final void fillFrom(AccountDto dto) {
        tenantCombo.setValue(dto.getTenant());
        accountTypeCombo.setValue(dto.getAccountType());
        codeField.setValue(dto.getCode() != null ? dto.getCode() : "");
        emailField.setValue(dto.getEmail() != null ? dto.getEmail() : "");
        phoneNumberField.setValue(dto.getPhoneNumber() != null ? dto.getPhoneNumber() : "");
        languageCombo.setValue(dto.getLanguage());
        functionRoleField.setValue(dto.getFunctionRole() != null ? dto.getFunctionRole() : "");
        isAdminCheckbox.setValue(Boolean.TRUE.equals(dto.getIsAdmin()));
        adminStatusCombo.setValue(dto.getAdminStatus());

        AccountDetailsDto details = dto.getAccountDetails();
        if (details != null) {
            firstNameField.setValue(details.getFirstName() != null ? details.getFirstName() : "");
            lastNameField.setValue(details.getLastName() != null ? details.getLastName() : "");
            countryField.setValue(details.getCountry() != null ? details.getCountry() : "");
            contactsEditor.setContacts(details.getContacts());

            AddressDto address = details.getAddress();
            if (address != null) {
                streetField.setValue(address.getStreet() != null ? address.getStreet() : "");
                zipCodeField.setValue(address.getZipCode() != null ? address.getZipCode() : "");
                cityField.setValue(address.getCity() != null ? address.getCity() : "");
                stateField.setValue(address.getState() != null ? address.getState() : "");
                addressCountryField.setValue(address.getCountry() != null ? address.getCountry() : "");
                additionalInfoField.setValue(address.getAdditionalInfo() != null ? address.getAdditionalInfo() : "");
            }
        }

        // Download the profile photo through accountImageService; no static
        // fallback path exists on AccountDto.
        showExistingPhoto(photoSection, dto.getId());
    }

    /** Copies the form values into the DTO ({@code id} and {@code code} are left untouched). */
    private void applyTo(AccountDto dto) {
        dto.setTenant(tenantCombo.getValue());
        dto.setAccountType(accountTypeCombo.getValue());
        dto.setEmail(emailField.getValue());
        dto.setPhoneNumber(phoneNumberField.getValue());
        dto.setLanguage(languageCombo.getValue());
        dto.setFunctionRole(functionRoleField.getValue());
        dto.setIsAdmin(isAdminCheckbox.getValue());
        dto.setAdminStatus(adminStatusCombo.getValue());

        AccountDetailsDto details = dto.getAccountDetails() != null ? dto.getAccountDetails() : new AccountDetailsDto();
        details.setFirstName(firstNameField.getValue());
        details.setLastName(lastNameField.getValue());
        details.setCountry(blankToNull(countryField.getValue()));

        List<ContactDto> contacts = contactsEditor.getContacts();
        if (details.getContacts() != null || !contacts.isEmpty()) {
            details.setContacts(contacts);
        }

        AddressDto address = details.getAddress();
        boolean hasAddressData = !isBlank(streetField.getValue()) || !isBlank(zipCodeField.getValue())
                || !isBlank(cityField.getValue()) || !isBlank(stateField.getValue())
                || !isBlank(addressCountryField.getValue()) || !isBlank(additionalInfoField.getValue());
        if (address != null || hasAddressData) {
            if (address == null) {
                address = new AddressDto();
            }
            address.setStreet(blankToNull(streetField.getValue()));
            address.setZipCode(blankToNull(zipCodeField.getValue()));
            address.setCity(blankToNull(cityField.getValue()));
            address.setState(blankToNull(stateField.getValue()));
            address.setCountry(blankToNull(addressCountryField.getValue()));
            address.setAdditionalInfo(blankToNull(additionalInfoField.getValue()));
            details.setAddress(address);
        }
        dto.setAccountDetails(details);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private boolean isValid() {
        if (tenantCombo.getValue() == null || tenantCombo.getValue().isBlank()) {
            append(I18n.t("ims.account.dialog.tenant.required"));
            return false;
        }
        if (accountTypeCombo.getValue() == null || accountTypeCombo.getValue().isBlank()) {
            append(I18n.t("ims.account.dialog.account.type.required"));
            return false;
        }
        if (emailField.getValue().isBlank()) {
            append(I18n.t("ims.account.dialog.email.required"));
            return false;
        }
        if (imageRequired() && photoSection.getSelectedFile() == null) {
            append(I18n.t("ims.account.dialog.create.image.required"));
            return false;
        }
        return true;
    }

    /* ------------------------------------------------------------------
     * Save
     * ------------------------------------------------------------------ */

    @Override
    protected final boolean onOk() {
        if (!isValid()) {
            return false;
        }

        parentView.showLoading(true);
        try {
            AccountDto dto = target();
            applyTo(dto);

            Long accountId = persist(dto);
            if (accountId == null) {
                return false;
            }

            int failures = uploadAttachments(accountId);
            if (failures == 0) {
                append(I18n.t(messagePrefix + ".success"));
            } else {
                append(I18n.t(messagePrefix + ".success.with.warnings", failures));
            }
            return true;
        } catch (Exception e) {
            log.error("Error saving account", e);
            append(I18n.t(messagePrefix + ".failed", e.getMessage()));
            return false;
        } finally {
            parentView.showLoading(false);
        }
    }

    /** Uploads the profile photo; returns the number of failures. */
    private int uploadAttachments(Long accountId) {
        int failures = 0;

        MultipartFile image = photoSection.getSelectedFile();
        if (image != null) {
            try {
                ResponseEntity<AccountDto> response =
                        accountImageService.uploadImage(accountId, image);
                if (response == null || !response.getStatusCode().is2xxSuccessful()) {
                    failures++;
                    append(I18n.t("ims.account.image.upload.error",
                            response == null ? "no response" : "HTTP " + response.getStatusCode()));
                }
            } catch (Exception ex) {
                failures++;
                log.error("Image upload failed for account {}", accountId, ex);
                append(I18n.t("ims.account.image.upload.error", ex.getMessage()));
            }
        }
        return failures;
    }
}