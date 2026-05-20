package eu.isygoit.ui.views.alias.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.CreateAliasRequest;
import eu.isygoit.dto.KmsDtos.CreateAliasResponse;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.ListKeysResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.BaseActionDialog;
import eu.isygoit.ui.views.alias.AliasesView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog for creating a new alias.
 */
public class CreateAliasDialog extends BaseActionDialog {

    private final AliasesView parentView;
    private final KmsApiService kmsApiService;
    private final Runnable onSuccess;

    private TextField aliasNameField;
    private ComboBox<String> targetKeyCombo;

    public CreateAliasDialog(AliasesView parentView,
                             KmsApiService kmsApiService,
                             Runnable onSuccess) {
        super("Create alias", onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.onSuccess = onSuccess;

        setOkButtonText("Create");
        setWidth("500px");

        buildForm();
        add(createFormLayout());
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);

        String aliasName = aliasNameField.getValue();
        String targetKeyId = targetKeyCombo.getValue();
        if (aliasName == null || aliasName.isBlank()) {
            String errorMsg = "Alias name is required";
            showError(errorMsg);
            Notification.show(errorMsg, 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }
        if (targetKeyId == null || targetKeyId.isBlank()) {
            String errorMsg = "Target key is required";
            showError(errorMsg);
            Notification.show(errorMsg, 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        try {
            CreateAliasRequest request = CreateAliasRequest.builder()
                    .aliasName(aliasName)
                    .targetKeyId(targetKeyId)
                    .build();
            ResponseEntity<CreateAliasResponse> response = kmsApiService.createAlias(request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Creation failed: " + response.getStatusCode();
                showError(errorMsg);
                Notification.show(errorMsg, 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            close();
            Notification.show("Alias created successfully", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            showError(errorMsg);
            Notification.show("Creation error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            showError(errorMsg);
            Notification.show("Error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildForm() {
        aliasNameField = new TextField("Alias name");
        aliasNameField.setPlaceholder("alias:my-key-alias");
        aliasNameField.setRequiredIndicatorVisible(true);

        targetKeyCombo = new ComboBox<>("Target KMS key");
        targetKeyCombo.setRequiredIndicatorVisible(true);
        targetKeyCombo.setPlaceholder("Select a key...");
        targetKeyCombo.setItems(fetchKeyIds());
        targetKeyCombo.setItemLabelGenerator(keyId -> {
            try {
                ResponseEntity<DescribeKeyResponse> desc = kmsApiService.describeKey(keyId);
                DescribeKeyResponse descBody = desc.getBody();
                if (descBody != null && descBody.getKeyMetadata() != null) {
                    String alias = descBody.getKeyMetadata().getKeyAlias();
                    if (alias != null && !alias.isEmpty()) return alias + " (" + keyId + ")";
                }
            } catch (Exception ignored) {
            }
            return keyId;
        });
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.add(aliasNameField, targetKeyCombo);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }

    private List<String> fetchKeyIds() {
        List<String> keyIds = new ArrayList<>();
        try {
            ResponseEntity<ListKeysResponse> response = kmsApiService.listKeys(100, null);
            ListKeysResponse keys = response.getBody();
            if (keys != null && keys.getKeys() != null) {
                keyIds = keys.getKeys().stream()
                        .map(ListKeysResponse.KeyEntry::getKeyId)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            Notification.show("Could not load keys: " + e.getMessage(), 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        return keyIds;
    }
}