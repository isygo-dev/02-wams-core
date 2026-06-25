package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.KmsDtos.CreateKeyRequest;
import eu.isygoit.dto.KmsDtos.ListResourceTagsResponse;
import eu.isygoit.dto.KmsDtos.UpdateKeyDescriptionRequest;
import eu.isygoit.dto.KmsDtos.UpdateKeyDescriptionResponse;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;
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
        super(I18n.t("key.dialog.update.title"), parentView, kmsApiService, onSuccess);
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.currentTags = currentTags != null ? currentTags : new ArrayList<>();
        setOkButtonText(I18n.t("key.dialog.update.button"));
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

        for (ListResourceTagsResponse.Tag tag : currentTags) {
            addTagRow(tag.getTagKey(), tag.getTagValue());
        }
        if (tagRows.isEmpty()) {
            addTagRow(null, null);
        }
    }

    @Override
    protected void prefillData() {
        // not used
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
                append(I18n.t("key.dialog.update.failed", (response.getBody() != null ? response.getBody() : "unknown error")));
                return false;
            }

            append(I18n.t("key.dialog.update.success"));
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("key.dialog.update.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}