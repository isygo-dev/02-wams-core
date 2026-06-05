package eu.isygoit.ui.views.cryptography.key.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.KmsDtos.CreateKeyRequest;
import eu.isygoit.dto.KmsDtos.ListResourceTagsResponse;
import eu.isygoit.dto.KmsDtos.UpdateKeyDescriptionRequest;
import eu.isygoit.dto.KmsDtos.UpdateKeyDescriptionResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.cryptography.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

public class UpdateKeyDialog extends KeyDialogBase {

    private final ObjectMapper objectMapper;
    private final String keyId;
    private final List<ListResourceTagsResponse.Tag> currentTags;

    public UpdateKeyDialog(KeyManagementView parentView,
                           KmsApiService kmsApiService,
                           ObjectMapper objectMapper,
                           String keyId,
                           String currentAlias,
                           String currentDesc,
                           List<ListResourceTagsResponse.Tag> currentTags,
                           Boolean currentRotationEnabled,
                           Integer currentRotationPeriodInDays,
                           Runnable onSuccess) {
        super("Edit key", parentView, kmsApiService, onSuccess);
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.currentTags = currentTags != null ? currentTags : new ArrayList<>();
        setOkButtonText("Save");
        buildCommonForm();
        add(createCommonFormLayout());
        prefillData(currentAlias, currentDesc, currentRotationEnabled, currentRotationPeriodInDays);
    }

    private void prefillData(String currentAlias, String currentDesc,
                             Boolean currentRotationEnabled, Integer currentRotationPeriodInDays) {
        aliasField.setValue(currentAlias != null ? currentAlias : "");
        descriptionField.setValue(currentDesc != null ? currentDesc : "");
        rotationEnabledCheckbox.setValue(currentRotationEnabled != null ? currentRotationEnabled : false);
        if (currentRotationEnabled && currentRotationPeriodInDays != null) {
            rotationPeriodField.setValue(currentRotationPeriodInDays);
        }
        rotationPeriodField.setVisible(currentRotationEnabled);

        // Load existing tags
        for (ListResourceTagsResponse.Tag tag : currentTags) {
            addTagRow(tag.getTagKey(), tag.getTagValue());
        }
        // Ensure at least one empty row
        if (tagRows.isEmpty()) {
            addTagRow(null, null);
        }
    }

    @Override
    protected void prefillData() {
        // Not used; we have a custom prefill method.
    }

    @Override
    protected boolean onOk() {
        String newAlias = getAliasOrNull();
        if (newAlias == null && !aliasField.getValue().isBlank()) {
            parentView.showLoading(false);
            return false;
        }

        String newDescription = getDescriptionOrNull();

        Boolean newRotationEnabled = rotationEnabledCheckbox.getValue();
        Integer newRotationPeriod = getRotationPeriodOrNull();

        List<CreateKeyRequest.Tag> newTags = getTagsFromRows();

        parentView.showLoading(true);
        try {
            UpdateKeyDescriptionRequest request = UpdateKeyDescriptionRequest.builder()
                    .keyId(keyId)
                    .keyAlias(newAlias)
                    .description(newDescription)
                    .rotationEnabled(newRotationEnabled)
                    .rotationPeriodInDays(newRotationPeriod)
                    .tags(newTags.isEmpty() ? null : newTags)
                    .build();

            ResponseEntity<UpdateKeyDescriptionResponse> response = kmsApiService.updateKeyDescription(keyId, request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append("Update failed: " + (response.getBody() != null ? response.getBody() : "unknown error"));
                return false;
            }

            append("Key updated successfully");
            return true;
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed operation: " + e.getMessage();
            this.append(errorMsg);
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }
}