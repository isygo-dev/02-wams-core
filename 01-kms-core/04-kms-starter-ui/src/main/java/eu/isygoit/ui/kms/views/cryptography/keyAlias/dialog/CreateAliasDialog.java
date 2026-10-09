package eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.CreateAliasRequest;
import eu.isygoit.dto.KmsDtos.CreateAliasResponse;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsActionDialog;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.AliasesView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;


/**
 * Dialog for creating a new alias.
 */
public class CreateAliasDialog extends KmsActionDialog {

    private final AliasesView parentView;
    private final KmsApiService kmsApiService;

    private TextField aliasNameField;
    private ComboBox<String> targetKeyCombo;
    private Checkbox primaryCheckbox;

    public CreateAliasDialog(AliasesView parentView,
                             KmsApiService kmsApiService,
                             Runnable onSuccess) {
        super(I18n.t("kms.alias.dialog.create.title"), onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;

        setOkButtonText(I18n.t("kms.alias.dialog.create.button"));
        DialogLayout.size(this, DialogLayout.WIDTH_S);

        buildForm();
        add(createFormLayout());
    }

    @Override
    protected boolean onOk() {
        String aliasName = aliasNameField.getValue();
        String targetKeyId = targetKeyCombo.getValue();
        if (aliasName == null || aliasName.isBlank()) {
            append(I18n.t("kms.alias.dialog.field.alias.name.required"));
            return false;
        }
        if (targetKeyId == null || targetKeyId.isBlank()) {
            append(I18n.t("kms.alias.dialog.field.target.key.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            CreateAliasRequest request = CreateAliasRequest.builder()
                    .aliasName(aliasName)
                    .targetKeyId(targetKeyId)
                    .primary(primaryCheckbox.getValue())
                    .build();
            ResponseEntity<CreateAliasResponse> response = kmsApiService.createAlias(request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("kms.alias.dialog.create.failed", response.getStatusCode()));
                return false;
            }

            append(I18n.t("kms.alias.dialog.create.success"));
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("kms.alias.dialog.create.failed", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildForm() {
        aliasNameField = new TextField(I18n.t("kms.alias.dialog.field.alias.name"));
        aliasNameField.setPlaceholder(I18n.t("kms.alias.dialog.field.alias.name.placeholder"));
        aliasNameField.setRequiredIndicatorVisible(true);
        aliasNameField.setWidthFull();

        targetKeyCombo = AliasDialogSupport.createTargetKeyCombo(kmsApiService, AliasDialogSupport.fetchKeyIds(kmsApiService));

        primaryCheckbox = new Checkbox(I18n.t("kms.alias.dialog.field.primary"));
        primaryCheckbox.setValue(false);
    }

    private FormLayout createFormLayout() {
        FormLayout form = DialogLayout.responsiveForm();
        form.add(aliasNameField, targetKeyCombo, primaryCheckbox);
        form.setColspan(targetKeyCombo, 2);
        form.setColspan(primaryCheckbox, 2);
        return form;
    }
}
