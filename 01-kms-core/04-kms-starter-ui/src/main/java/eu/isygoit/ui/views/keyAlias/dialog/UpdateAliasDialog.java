package eu.isygoit.ui.views.keyAlias.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.UpdateAliasRequest;
import eu.isygoit.dto.KmsDtos.UpdateAliasResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.BaseActionDialog;
import eu.isygoit.ui.views.keyAlias.AliasesView;
import feign.FeignException;
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
        parentView.showLoading(true);
        String newTargetId = targetKeyCombo.getValue();
        if (newTargetId == null || newTargetId.isBlank()) {
            String errorMsg = "Please select a target key";
            this.append(errorMsg);
            Notification.show(errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        try {
            UpdateAliasRequest request = UpdateAliasRequest.builder()
                    .aliasName(aliasName)
                    .targetKeyId(newTargetId)
                    .build();
            ResponseEntity<UpdateAliasResponse> response = kmsApiService.updateAlias(aliasName, request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Update failed: " + response.getStatusCode();
                this.append(errorMsg);
                Notification.show(errorMsg, 6000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false;
            }


            Notification.show("Alias reassigned", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
            Notification.show("Update error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            this.append(errorMsg);
            Notification.show("Error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
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