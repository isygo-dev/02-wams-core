package eu.isygoit.ui.kms.views.cryptography.keyTag.dialog;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import feign.FeignException;
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
        addContent(keyField, valueField);
    }

    private void buildForm() {
        keyField = new TextField("Tag key");
        keyField.setRequired(true);
        keyField.setRequiredIndicatorVisible(true);
        keyField.setMaxLength(128);
        keyField.setPlaceholder("e.g., Environment");
        keyField.setHelperText("Tag key (max 128 characters)");
        keyField.setWidthFull();

        valueField = new TextField("Tag value");
        valueField.setRequired(true);
        valueField.setRequiredIndicatorVisible(true);
        valueField.setMaxLength(256);
        valueField.setPlaceholder("e.g., Production");
        valueField.setHelperText("Tag value (max 256 characters)");
        valueField.setWidthFull();
    }

    @Override
    protected boolean onOk() {
        String tagKey = keyField.getValue();
        String tagValue = valueField.getValue();

        if (!StringUtils.hasText(tagKey) || !StringUtils.hasText(tagValue)) {
            append("Both key and value are required");
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
            append("Tag added successfully");
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append("Failed to add tag: " + e.getMessage());
        }

        return false;
    }
}