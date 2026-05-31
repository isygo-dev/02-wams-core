package eu.isygoit.ui.views.key.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos.RotateKeyResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.PinBaseActionDialog;
import eu.isygoit.ui.views.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class RotateKeyConfirmDialog extends PinBaseActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;
    private final String keyId;


    public RotateKeyConfirmDialog(KeyManagementView parentView,
                                  KmsApiService kmsApiService,
                                  String keyId,
                                  Runnable onSuccess) {
        super("Rotate Key Immediately",
                "Rotating the key will create a new cryptographic version.\n" +
                        "Previous versions remain usable for decryption/verification.\n" +
                        "This action is immediate and cannot be undone.",
                onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        setOkButtonText("Rotate");
        addThemeVariantsOkButton(com.vaadin.flow.component.button.ButtonVariant.LUMO_WARNING);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            ResponseEntity<RotateKeyResponse> response = kmsApiService.rotateKey(keyId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Rotation failed: " + response.getStatusCode();
                this.append(errorMsg);
                Notification.show(errorMsg, 6000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false;
            }


            Notification.show("Key rotated successfully", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
            Notification.show("Rotation error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            this.append(errorMsg);
            Notification.show("Error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        } finally {
            parentView.showLoading(false);
        }
    }
}