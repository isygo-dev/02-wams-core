package eu.isygoit.ui.sms.views.storageconfig.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.enums.IEnumStorage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.sms.views.storageconfig.StorageConfigManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class UpdateStorageConfigDialog extends BaseActionDialog {

    private final StorageConfigManagementView parentView;
    private final StorageConfigService storageConfigService;
    private final StorageConfigDto config;
    private final Runnable onSuccess;

    private TextField tenantField;
    private ComboBox<IEnumStorage.Types> typeCombo;
    private TextField userNameField;
    private PasswordField passwordField;
    private TextField urlField;

    public UpdateStorageConfigDialog(StorageConfigManagementView parentView,
                                     StorageConfigService storageConfigService,
                                     StorageConfigDto config,
                                     Runnable onSuccess) {
        super(I18n.t("storageconfig.dialog.update.title"), onSuccess);
        this.parentView = parentView;
        this.storageConfigService = storageConfigService;
        this.config = config;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("storageconfig.dialog.update.button"));
        setWidth("600px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
        populateFields();
    }

    private void buildForm() {
        tenantField = new TextField(I18n.t("storageconfig.dialog.field.tenant"));
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setWidthFull();

        typeCombo = new ComboBox<>(I18n.t("storageconfig.dialog.field.type"));
        typeCombo.setItems(IEnumStorage.Types.values());
        typeCombo.setItemLabelGenerator(type -> type.name());
        typeCombo.setRequiredIndicatorVisible(true);
        typeCombo.setWidthFull();

        userNameField = new TextField(I18n.t("storageconfig.dialog.field.username"));
        userNameField.setRequiredIndicatorVisible(true);
        userNameField.setWidthFull();

        passwordField = new PasswordField(I18n.t("storageconfig.dialog.field.password"));
        passwordField.setPlaceholder(I18n.t("storageconfig.dialog.field.password.update.placeholder"));
        passwordField.setWidthFull();

        urlField = new TextField(I18n.t("storageconfig.dialog.field.url"));
        urlField.setRequiredIndicatorVisible(true);
        urlField.setWidthFull();
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        form.add(tenantField, typeCombo, userNameField, passwordField, urlField);
        form.setColspan(urlField, 2);
        return form;
    }

    private void populateFields() {
        tenantField.setValue(config.getTenant() != null ? config.getTenant() : "");
        typeCombo.setValue(config.getType());
        userNameField.setValue(config.getUserName() != null ? config.getUserName() : "");
        urlField.setValue(config.getUrl() != null ? config.getUrl() : "");
        // Password field is left empty for security, only update if changed
    }

    @Override
    protected boolean onOk() {
        if (tenantField.getValue().isBlank()) {
            append(I18n.t("storageconfig.dialog.field.tenant.required"));
            return false;
        }
        if (typeCombo.getValue() == null) {
            append(I18n.t("storageconfig.dialog.field.type.required"));
            return false;
        }
        if (userNameField.getValue().isBlank()) {
            append(I18n.t("storageconfig.dialog.field.username.required"));
            return false;
        }
        if (urlField.getValue().isBlank()) {
            append(I18n.t("storageconfig.dialog.field.url.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            config.setTenant(tenantField.getValue().trim());
            config.setType(typeCombo.getValue());
            config.setUserName(userNameField.getValue().trim());

            // Only update password if a new one is provided
            if (passwordField.getValue() != null && !passwordField.getValue().isBlank()) {
                config.setPassword(passwordField.getValue());
            }

            config.setUrl(urlField.getValue().trim());

            ResponseEntity<StorageConfigDto> response = storageConfigService.update(config.getId(), config);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("storageconfig.dialog.update.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("storageconfig.dialog.update.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("storageconfig.dialog.update.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}