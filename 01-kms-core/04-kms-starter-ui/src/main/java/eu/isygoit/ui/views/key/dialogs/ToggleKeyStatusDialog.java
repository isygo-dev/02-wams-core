package eu.isygoit.ui.views.key.dialogs;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.BaseActionDialog;
import eu.isygoit.ui.views.key.KeyManagementView;
import feign.FeignException;

/**
 * Dialog to confirm enabling or disabling a KMS key.
 */
public class ToggleKeyStatusDialog extends BaseActionDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final boolean currentlyEnabled;
    private final KeyManagementView parentView;

    public ToggleKeyStatusDialog(KmsApiService kmsApiService,
                                 String keyId,
                                 boolean currentlyEnabled,
                                 KeyManagementView parentView) {
        super(currentlyEnabled ? "Disable key" : "Enable key");
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.currentlyEnabled = currentlyEnabled;
        this.parentView = parentView;

        setOkButtonText(currentlyEnabled ? "Disable" : "Enable");
        if (currentlyEnabled) {
            okButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        } else {
            okButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        }
        setWidth("450px");

        buildContent();
    }

    @Override
    protected void onOk() {
        clearError();
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
            close();
            parentView.loadKeys();
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            showError(errorMsg);
            Notification.show("Update error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            showError(errorMsg);
            Notification.show("Error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void buildContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.add(new Span(currentlyEnabled
                ? "Disabling the key prevents any cryptographic operations. Are you sure?"
                : "Enabling the key will restore its ability to perform cryptographic operations."));
        add(layout);
    }
}