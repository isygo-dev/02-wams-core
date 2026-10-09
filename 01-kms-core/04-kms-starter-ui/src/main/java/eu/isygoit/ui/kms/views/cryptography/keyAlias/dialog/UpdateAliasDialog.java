package eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.UpdateAliasRequest;
import eu.isygoit.dto.KmsDtos.UpdateAliasResponse;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsActionDialog;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.AliasesView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Dialog for reassigning an alias to a different KMS key.
 */
public class UpdateAliasDialog extends KmsActionDialog {

    private final AliasesView parentView;
    private final KmsApiService kmsApiService;

    private final String aliasName;
    private final String currentTargetKeyId;
    private TextField aliasNameField;
    private ComboBox<String> targetKeyCombo;

    public UpdateAliasDialog(AliasesView parentView,
                             KmsApiService kmsApiService,
                             Runnable onSuccess,
                             String aliasName,
                             String currentTargetKeyId) {
        super(I18n.t("kms.alias.dialog.update.title"), onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.aliasName = aliasName;
        this.currentTargetKeyId = currentTargetKeyId;

        setOkButtonText(I18n.t("kms.alias.dialog.update.button"));
        DialogLayout.size(this, DialogLayout.WIDTH_S);

        buildForm();
        add(createFormLayout());
    }

    @Override
    protected boolean onOk() {
        String newTargetId = targetKeyCombo.getValue();
        if (newTargetId == null || newTargetId.isBlank()) {
            append(I18n.t("kms.alias.dialog.field.target.key.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            UpdateAliasRequest request = UpdateAliasRequest.builder()
                    .aliasName(aliasName)
                    .targetKeyId(newTargetId)
                    .build();
            ResponseEntity<UpdateAliasResponse> response = kmsApiService.updateAlias(aliasName, request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("kms.alias.dialog.update.failed", response.getStatusCode()));
                return false;
            }

            append(I18n.t("kms.alias.dialog.update.success"));
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("kms.alias.dialog.update.failed.operation", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildForm() {
        // Alias name identifies the alias being reassigned: shown read-only (UpdateAliasRequest.aliasName)
        aliasNameField = new TextField(I18n.t("kms.alias.dialog.field.alias.name"));
        aliasNameField.setValue(aliasName != null ? aliasName : "");
        aliasNameField.setReadOnly(true);
        aliasNameField.setWidthFull();

        targetKeyCombo = AliasDialogSupport.createTargetKeyCombo(kmsApiService, AliasDialogSupport.fetchKeyIds(kmsApiService));
        targetKeyCombo.setValue(currentTargetKeyId);
    }

    private FormLayout createFormLayout() {
        FormLayout form = DialogLayout.responsiveForm();
        form.add(aliasNameField, targetKeyCombo);
        form.setColspan(targetKeyCombo, 2);
        return form;
    }
}
