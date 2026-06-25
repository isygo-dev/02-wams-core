package eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.CreateAliasRequest;
import eu.isygoit.dto.KmsDtos.CreateAliasResponse;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.ListKeysResponse;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.AliasesView;
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

    private TextField aliasNameField;
    private ComboBox<String> targetKeyCombo;
    private Checkbox primaryCheckbox;

    public CreateAliasDialog(AliasesView parentView,
                             KmsApiService kmsApiService,
                             Runnable onSuccess) {
        super(I18n.t("alias.dialog.create.title"), onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;

        setOkButtonText(I18n.t("alias.dialog.create.button"));
        setWidth("500px");

        buildForm();
        add(createFormLayout());
    }

    @Override
    protected boolean onOk() {
        String aliasName = aliasNameField.getValue();
        String targetKeyId = targetKeyCombo.getValue();
        if (aliasName == null || aliasName.isBlank()) {
            append(I18n.t("alias.dialog.field.alias.name.required"));
            return false;
        }
        if (targetKeyId == null || targetKeyId.isBlank()) {
            append(I18n.t("alias.dialog.field.target.key.required"));
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
                append(I18n.t("alias.dialog.create.failed", response.getStatusCode()));
                return false;
            }

            append(I18n.t("alias.dialog.create.success"));
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("alias.dialog.create.failed", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildForm() {
        aliasNameField = new TextField(I18n.t("alias.dialog.field.alias.name"));
        aliasNameField.setPlaceholder(I18n.t("alias.dialog.field.alias.name.placeholder"));
        aliasNameField.setRequiredIndicatorVisible(true);

        targetKeyCombo = new ComboBox<>(I18n.t("alias.dialog.field.target.key"));
        targetKeyCombo.setRequiredIndicatorVisible(true);
        targetKeyCombo.setPlaceholder(I18n.t("alias.dialog.field.target.key.placeholder"));
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
                // fall through
            }
            return keyId;
        });

        primaryCheckbox = new Checkbox(I18n.t("alias.dialog.field.primary"));
        primaryCheckbox.setValue(false);
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.add(aliasNameField, targetKeyCombo, primaryCheckbox);
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
            Notification.show(I18n.t("aliases.view.load.keys.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        return keyIds;
    }
}