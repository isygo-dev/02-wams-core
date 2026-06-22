package eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.UpdateAliasRequest;
import eu.isygoit.dto.KmsDtos.UpdateAliasResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.AliasesView;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

/**
 * Dialog for reassigning an alias to a different KMS key.
 */
@Slf4j
public class UpdateAliasDialog extends BaseActionDialog {

    private final AliasesView parentView;
    private final KmsApiService kmsApiService;

    private final String aliasName;
    private final String currentTargetKeyId;
    private ComboBox<String> targetKeyCombo;

    public UpdateAliasDialog(AliasesView parentView,
                             KmsApiService kmsApiService,
                             Runnable onSuccess,
                             String aliasName,
                             String currentTargetKeyId) {
        super("Reassign alias", onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.aliasName = aliasName;
        this.currentTargetKeyId = currentTargetKeyId;

        setOkButtonText("Update");
        setWidth("500px");

        buildForm();
        add(createFormLayout());
    }

    @Override
    protected boolean onOk() {
        String newTargetId = targetKeyCombo.getValue();
        if (newTargetId == null || newTargetId.isBlank()) {
            append("Please select a target key");
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
                append("Update failed: " + response.getStatusCode());
                return false;
            }

            append("Alias reassigned");
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append("Failed operation: " + e.getMessage());
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildForm() {
        targetKeyCombo = new ComboBox<>("New target KMS key");
        targetKeyCombo.setRequiredIndicatorVisible(true);
        targetKeyCombo.setPlaceholder("Select a key...");
        targetKeyCombo.setItems(parentView.fetchKeyIds());
        targetKeyCombo.setItemLabelGenerator(keyId -> {
            try {
                ResponseEntity<DescribeKeyResponse> desc = kmsApiService.describeKey(keyId);
                DescribeKeyResponse descBody = desc.getBody();
                if (descBody != null && descBody.getKeyMetadata() != null) {
                    String alias = descBody.getKeyMetadata().getKeyAlias();
                    if (alias != null && !alias.isEmpty()) return alias + " (" + keyId + ")";
                }
            } catch (Exception ignored) {
                log.error("Failed to fetch key metadata for keyId: {}", keyId, ignored);
            }
            return keyId;
        });
        targetKeyCombo.setValue(currentTargetKeyId);
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.add(targetKeyCombo);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }
}