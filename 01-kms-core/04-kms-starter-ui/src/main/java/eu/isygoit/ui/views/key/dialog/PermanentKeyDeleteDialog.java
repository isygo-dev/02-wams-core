package eu.isygoit.ui.views.key.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.PinBaseActionDialog;
import eu.isygoit.ui.views.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class PermanentKeyDeleteDialog extends PinBaseActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;
    private final String keyId;

    public PermanentKeyDeleteDialog(KeyManagementView parentView,
                                    KmsApiService kmsApiService,
                                    Runnable onSuccess,
                                    String keyId) {
        super("Permanently delete key",
                "This action is irreversible. The key will be permanently removed.",
                onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;

        setOkButtonText("Delete permanently");
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.DeleteKeyResponse> response = kmsApiService.deleteKey(keyId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Deletion failed: " + response.getStatusCode();
                this.append(errorMsg);
                Notification.show(errorMsg, 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false;
            }

            close();
            Notification.show("Key permanently deleted", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
            Notification.show("Update error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            this.append(errorMsg);
            Notification.show("Error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        } finally {
            parentView.showLoading(false);
        }
    }
}