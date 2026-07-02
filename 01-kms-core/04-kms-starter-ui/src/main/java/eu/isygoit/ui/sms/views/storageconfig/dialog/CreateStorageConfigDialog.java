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

public class CreateStorageConfigDialog extends BaseActionDialog {

    private final StorageConfigManagementView parentView;
    private final StorageConfigService storageConfigService;
    private final Runnable onSuccess;

    private TextField tenantField;
    private ComboBox<IEnumStorage.Types> typeCombo;
    private TextField userNameField;
    private PasswordField passwordField;
    private TextField urlField;

    public CreateStorageConfigDialog(StorageConfigManagementView parentView,
                                     StorageConfigService storageConfigService,
                                     Runnable onSuccess) {
        super(I18n.t("storageconfig.dialog.create.title"), onSuccess);
        this.parentView = parentView;
        this.storageConfigService = storageConfigService;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("storageconfig.dialog.create.button"));
        setWidth("600px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
    }

    private void buildForm() {
        tenantField = new TextField(I18n.t("storageconfig.dialog.field.tenant"));
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setPlaceholder(I18n.t("storageconfig.dialog.field.tenant.placeholder"));
        tenantField.setWidthFull();

        typeCombo = new ComboBox<>(I18n.t("storageconfig.dialog.field.type"));
        typeCombo.setItems(IEnumStorage.Types.values());
        typeCombo.setItemLabelGenerator(type -> type.name());
        typeCombo.setRequiredIndicatorVisible(true);
        typeCombo.setPlaceholder(I18n.t("storageconfig.dialog.field.type.placeholder"));
        typeCombo.setWidthFull();

        userNameField = new TextField(I18n.t("storageconfig.dialog.field.username"));
        userNameField.setRequiredIndicatorVisible(true);
        userNameField.setPlaceholder(I18n.t("storageconfig.dialog.field.username.placeholder"));
        userNameField.setWidthFull();

        passwordField = new PasswordField(I18n.t("storageconfig.dialog.field.password"));
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setPlaceholder(I18n.t("storageconfig.dialog.field.password.placeholder"));
        passwordField.setWidthFull();

        urlField = new TextField(I18n.t("storageconfig.dialog.field.url"));
        urlField.setRequiredIndicatorVisible(true);
        urlField.setPlaceholder(I18n.t("storageconfig.dialog.field.url.placeholder"));
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
        if (passwordField.getValue().isBlank()) {
            append(I18n.t("storageconfig.dialog.field.password.required"));
            return false;
        }
        if (urlField.getValue().isBlank()) {
            append(I18n.t("storageconfig.dialog.field.url.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            StorageConfigDto newConfig = StorageConfigDto.builder()
                    .tenant(tenantField.getValue().trim())
                    .type(typeCombo.getValue())
                    .userName(userNameField.getValue().trim())
                    .password(passwordField.getValue())
                    .url(urlField.getValue().trim())
                    .build();

            ResponseEntity<StorageConfigDto> response = storageConfigService.create(newConfig);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("storageconfig.dialog.create.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("storageconfig.dialog.create.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("storageconfig.dialog.create.error", e.getMessage()));
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