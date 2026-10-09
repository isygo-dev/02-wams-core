package eu.isygoit.ui.sms.views.storageconfig.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.enums.IEnumStorage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.sms.views.common.SmsActionDialog;
import eu.isygoit.ui.sms.views.common.SmsDialogSupport;
import eu.isygoit.ui.sms.views.common.SmsEnumTag;
import eu.isygoit.ui.sms.views.storageconfig.StorageConfigManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Shared form of the storage configuration create/update dialogs: every editable
 * {@link StorageConfigDto} field (tenant, type, userName, password, url) is
 * declared here once. {@code id} is never shown; audit fields are only displayed
 * in the details dialog. Subclasses decide how the DTO is persisted and whether
 * the password is mandatory (create) or left empty to keep the current one (update).
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
abstract class AbstractStorageConfigFormDialog extends SmsActionDialog {

    final StorageConfigService service;
    private final StorageConfigManagementView parentView;
    private final String messagePrefix;
    private final boolean passwordRequired;

    TextField tenantField;
    ComboBox<IEnumStorage.Types> typeCombo;
    TextField userNameField;
    PasswordField passwordField;
    TextField urlField;

    AbstractStorageConfigFormDialog(String title,
                                    Runnable onSuccess,
                                    String messagePrefix,
                                    boolean passwordRequired,
                                    StorageConfigManagementView parentView,
                                    StorageConfigService service) {
        super(title, onSuccess);
        this.messagePrefix = messagePrefix;
        this.passwordRequired = passwordRequired;
        this.parentView = parentView;
        this.service = service;
    }

    /** The DTO that receives the form values on save. */
    abstract StorageConfigDto target();

    /** Persists the DTO and returns the service response. */
    abstract ResponseEntity<StorageConfigDto> persist(StorageConfigDto dto);

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_M);

        tenantField = new TextField(I18n.t("sms.storageconfig.dialog.field.tenant"));
        tenantField.setRequired(true);
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setPlaceholder(I18n.t("sms.storageconfig.dialog.field.tenant.placeholder"));
        tenantField.setWidthFull();

        typeCombo = new ComboBox<>(I18n.t("sms.storageconfig.dialog.field.type"));
        typeCombo.setItems(IEnumStorage.Types.values());
        SmsEnumTag.useTagRenderer(typeCombo, "sms.enum.storage");
        typeCombo.setRequired(true);
        typeCombo.setRequiredIndicatorVisible(true);
        typeCombo.setPlaceholder(I18n.t("sms.storageconfig.dialog.field.type.placeholder"));
        typeCombo.setWidthFull();

        userNameField = new TextField(I18n.t("sms.storageconfig.dialog.field.username"));
        userNameField.setRequired(true);
        userNameField.setRequiredIndicatorVisible(true);
        userNameField.setPlaceholder(I18n.t("sms.storageconfig.dialog.field.username.placeholder"));
        userNameField.setWidthFull();

        passwordField = new PasswordField(I18n.t("sms.storageconfig.dialog.field.password"));
        passwordField.setRequired(passwordRequired);
        passwordField.setRequiredIndicatorVisible(passwordRequired);
        passwordField.setPlaceholder(I18n.t(passwordRequired
                ? "sms.storageconfig.dialog.field.password.placeholder"
                : "sms.storageconfig.dialog.field.password.update.placeholder"));
        passwordField.setWidthFull();

        urlField = new TextField(I18n.t("sms.storageconfig.dialog.field.url"));
        urlField.setRequired(true);
        urlField.setRequiredIndicatorVisible(true);
        urlField.setPlaceholder(I18n.t("sms.storageconfig.dialog.field.url.placeholder"));
        urlField.setWidthFull();

        FormLayout form = DialogLayout.responsiveForm();
        form.add(tenantField, typeCombo, userNameField, passwordField, urlField);
        form.setColspan(urlField, 2);
        addContent(form);
    }

    /** Fills the form from an existing configuration; the password is never loaded. */
    final void fillFrom(StorageConfigDto dto) {
        tenantField.setValue(dto.getTenant() != null ? dto.getTenant() : "");
        typeCombo.setValue(dto.getType());
        userNameField.setValue(dto.getUserName() != null ? dto.getUserName() : "");
        urlField.setValue(dto.getUrl() != null ? dto.getUrl() : "");
    }

    private boolean isValid() {
        if (tenantField.getValue() == null || tenantField.getValue().isBlank()) {
            append(I18n.t("sms.storageconfig.dialog.field.tenant.required"));
            return false;
        }
        if (typeCombo.getValue() == null) {
            append(I18n.t("sms.storageconfig.dialog.field.type.required"));
            return false;
        }
        if (userNameField.getValue() == null || userNameField.getValue().isBlank()) {
            append(I18n.t("sms.storageconfig.dialog.field.username.required"));
            return false;
        }
        if (passwordRequired && (passwordField.getValue() == null || passwordField.getValue().isBlank())) {
            append(I18n.t("sms.storageconfig.dialog.field.password.required"));
            return false;
        }
        if (urlField.getValue() == null || urlField.getValue().isBlank()) {
            append(I18n.t("sms.storageconfig.dialog.field.url.required"));
            return false;
        }
        return true;
    }

    @Override
    protected final boolean onOk() {
        if (!isValid()) {
            return false;
        }

        parentView.showLoading(true);
        try {
            StorageConfigDto dto = target();
            dto.setTenant(tenantField.getValue().trim());
            dto.setType(typeCombo.getValue());
            dto.setUserName(userNameField.getValue().trim());
            // An empty password keeps the current one (update only; mandatory on create).
            if (passwordField.getValue() != null && !passwordField.getValue().isBlank()) {
                dto.setPassword(passwordField.getValue());
            }
            dto.setUrl(urlField.getValue().trim());

            ResponseEntity<StorageConfigDto> response = persist(dto);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t(messagePrefix + ".failed", response.getStatusCodeValue()));
                return false;
            }
            append(I18n.t(messagePrefix + ".success"));
            return true;
        } catch (FeignException ex) {
            append(SmsDialogSupport.extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t(messagePrefix + ".error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}
