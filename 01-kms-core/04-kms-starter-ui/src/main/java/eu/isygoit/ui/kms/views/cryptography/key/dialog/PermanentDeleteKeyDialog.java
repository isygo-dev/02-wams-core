package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class PermanentDeleteKeyDialog extends PinBaseActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;
    private final String keyId;

    public PermanentDeleteKeyDialog(KeyManagementView parentView,
                                    KmsApiService kmsApiService,
                                    String keyId,
                                    Runnable onSuccess) {
        super("Permanently delete key",
                "This action is irreversible. The key will be permanently removed.",
                onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;

        setOkButtonText("Delete permanently");
        addThemeVariantsOkButton(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append("Invalid confirmation code");
            return false;
        }

        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.DeleteKeyResponse> response = kmsApiService.deleteKey(keyId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append("Deletion failed: " + (response.getBody() != null ? response.getBody().toString() : "unknown error"));
                return false;
            }
            append("Key permanently deleted");
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
}