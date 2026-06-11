package eu.isygoit.ui.views.kms.cryptography.key.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.common.dialog.PinBaseActionDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class DisableKeyVersionDialog extends PinBaseActionDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final String versionId;


    public DisableKeyVersionDialog(KmsApiService kmsApiService,
                                   String keyId,
                                   String versionId,
                                   Runnable onSuccess) {
        super("Disable Key Version",
                "Disabling a key version makes it unusable for cryptographic operations. This action is reversible only by enabling the version (if supported).",
                onSuccess);
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.versionId = versionId;

        setOkButtonText("Disable permanently");
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        try {
            // Correct API call: two path variables - keyId and keyVersionId
            ResponseEntity<KmsDtos.DisableKeyVersionResponse> response =
                    kmsApiService.disableKeyVersion(keyId, versionId);  // versionId maps to keyVersionId
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Disable failed: " + response.getStatusCode();
                this.append(errorMsg);

                return false;
            }


            Notification.show("Key version disabled", 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed operation: " + e.getMessage();
            this.append(errorMsg);
        }

        return false;
    }
}