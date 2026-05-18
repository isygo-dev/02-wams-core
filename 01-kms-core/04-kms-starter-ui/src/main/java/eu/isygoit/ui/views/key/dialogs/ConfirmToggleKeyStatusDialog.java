package eu.isygoit.ui.views.key.dialogs;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.key.KeyManagementView;

/**
 * Dialog to this enabling or disabling a KMS key.
 */
public class ConfirmToggleKeyStatusDialog extends ConfirmDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final boolean currentlyEnabled;
    private final KeyManagementView parentView;

    public ConfirmToggleKeyStatusDialog(KmsApiService kmsApiService,
                                        String keyId,
                                        boolean currentlyEnabled,
                                        KeyManagementView parentView) {
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.currentlyEnabled = currentlyEnabled;
        this.parentView = parentView;
        show();
    }

    private void show() {
        this.setHeader(currentlyEnabled ? "Disable key" : "Enable key");
        this.setText(currentlyEnabled
                ? "Disabling the key prevents any cryptographic operations. Are you sure?"
                : "Enabling the key will restore its ability to perform cryptographic operations.");
        this.setCancelable(true);
        this.setConfirmText(currentlyEnabled ? "Disable" : "Enable");
        this.setConfirmButtonTheme(currentlyEnabled ? ButtonVariant.LUMO_ERROR.getVariantName() : ButtonVariant.LUMO_SUCCESS.getVariantName());

        this.addConfirmListener(event -> {
            try {
                if (currentlyEnabled) {
                    kmsApiService.disableKey(keyId);
                    Notification.show("Key disabled successfully", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    kmsApiService.enableKey(keyId);
                    Notification.show("Key enabled successfully", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
                parentView.loadKeys(); // refresh the view to reflect status change
            } catch (Exception ex) {
                Notification.show("Failed to toggle key status: " + ex.getMessage(), 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        this.open();
    }
}