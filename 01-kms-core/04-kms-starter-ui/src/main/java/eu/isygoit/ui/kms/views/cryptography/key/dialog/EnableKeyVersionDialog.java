package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class EnableKeyVersionDialog extends PinBaseActionDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final String versionId;


    public EnableKeyVersionDialog(KmsApiService kmsApiService,
                                  String keyId,
                                  String versionId,
                                  Runnable onSuccess) {
        super("Enable Key Version",
                "Enabling a key version makes it available for cryptographic operations again.",
                onSuccess);
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.versionId = versionId;

        setOkButtonText("Enable");
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        try {
            ResponseEntity<KmsDtos.EnableKeyVersionResponse> response =
                    kmsApiService.enableKeyVersion(keyId, versionId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Enable failed: " + response.getStatusCode();
                this.append(errorMsg);
                return false;
            }

            Notification.show("Key version enabled", 6000, Notification.Position.BOTTOM_END)
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