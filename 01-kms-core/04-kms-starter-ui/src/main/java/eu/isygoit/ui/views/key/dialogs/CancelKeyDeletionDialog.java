package eu.isygoit.ui.views.key.dialogs;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.key.KeyManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Dialog for cancelling the scheduled deletion of a KMS key.
 */
public class CancelKeyDeletionDialog extends ConfirmDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final String keyAliasOrId;
    private final KeyManagementView parentView;

    public CancelKeyDeletionDialog(KmsApiService kmsApiService, String keyId, String keyAliasOrId, KeyManagementView parentView) {
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.keyAliasOrId = keyAliasOrId;
        this.parentView = parentView;
        show();
    }

    private void show() {
        this.setHeader("Cancel deletion");
        this.setText("Are you sure you want to cancel the deletion of key " + keyAliasOrId + "?");
        this.setCancelable(true);
        this.setConfirmText("Yes, cancel");
        this.addConfirmListener(event -> {
            try {
                ResponseEntity<KmsDtos.CancelKeyDeletionResponse> response =
                        kmsApiService.cancelKeyDeletion(keyId);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Notification.show("Deletion cancelled", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    parentView.loadKeys();
                } else {
                    Notification.show("Failed to cancel deletion", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (Exception e) {
                Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        this.open();
    }
}