package eu.isygoit.ui.views.keyTag.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.BaseActionDialog;
import org.springframework.util.StringUtils;

import java.util.List;

public class AddTagDialog extends BaseActionDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final Runnable onSuccess;

    private TextField keyField;
    private TextField valueField;

    public AddTagDialog(KmsApiService kmsApiService, String keyId, Runnable onSuccess) {
        super("Add tag", onSuccess);
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.onSuccess = onSuccess;

        setOkButtonText("Add");
        setWidth("400px");

        buildForm();
        add(keyField, valueField);
    }

    private void buildForm() {
        keyField = new TextField("Tag key");
        keyField.setRequired(true);
        keyField.setRequiredIndicatorVisible(true);
        keyField.setMaxLength(128);
        keyField.setPlaceholder("e.g., Environment");
        keyField.setHelperText("Tag key (max 128 characters)");

        valueField = new TextField("Tag value");
        valueField.setRequired(true);
        valueField.setRequiredIndicatorVisible(true);
        valueField.setMaxLength(256);
        valueField.setPlaceholder("e.g., Production");
        valueField.setHelperText("Tag value (max 256 characters)");
    }

    @Override
    protected boolean onOk() {
        String tagKey = keyField.getValue();
        String tagValue = valueField.getValue();

        if (!StringUtils.hasText(tagKey) || !StringUtils.hasText(tagValue)) {
            Notification.show("Both key and value are required", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        try {
            KmsDtos.TagResourceRequest request = KmsDtos.TagResourceRequest.builder()
                    .keyId(keyId)
                    .tags(List.of(KmsDtos.ListResourceTagsResponse.Tag.builder()
                            .tagKey(tagKey)
                            .tagValue(tagValue)
                            .build()))
                    .build();
            kmsApiService.tagResource(keyId, request);
            Notification.show("Tag added successfully", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (Exception e) {
            String errorMsg = "Failed to add tag: " + e.getMessage();
            showError(errorMsg);
            Notification.show(errorMsg, 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }
    }
}