package eu.isygoit.ui.views.key.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.PinBaseActionDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class EnableKeyVersionDialog extends PinBaseActionDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final String versionId;
    private final Runnable onSuccess;

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
        this.onSuccess = onSuccess;

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
                Notification.show("Error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false;
            }

            close();
            Notification.show("Key version enabled", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            if (onSuccess != null) {
                onSuccess.run();
            }
            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
            Notification.show("Enable error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            this.append(errorMsg);
            Notification.show("Error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        return false;
    }
}