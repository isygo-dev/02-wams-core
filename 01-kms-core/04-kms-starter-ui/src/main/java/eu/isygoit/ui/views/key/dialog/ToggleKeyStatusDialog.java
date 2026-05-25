package eu.isygoit.ui.views.key.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.BaseActionDialog;
import eu.isygoit.ui.views.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Dialog to confirm enabling or disabling a KMS key.
 */
public class ToggleKeyStatusDialog extends BaseActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;
    private final Runnable onSuccess;

    private final String keyId;
    private final boolean currentlyEnabled;


    public ToggleKeyStatusDialog(KeyManagementView parentView,
                                 KmsApiService kmsApiService,
                                 Runnable onSuccess,
                                 String keyId,
                                 boolean currentlyEnabled) {
        super(currentlyEnabled ? "Disable key" : "Enable key", onSuccess);
        this.kmsApiService = kmsApiService;
        this.onSuccess = onSuccess;
        this.keyId = keyId;
        this.currentlyEnabled = currentlyEnabled;
        this.parentView = parentView;

        setOkButtonText(currentlyEnabled ? "Disable" : "Enable");
        if (currentlyEnabled) {
            addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        } else {
            addThemeVariantsOkButton(ButtonVariant.LUMO_SUCCESS);
        }
        setWidth("450px");

        buildContent();
    }

    @Override
    protected boolean onOk() {
        try {
            if (currentlyEnabled) {
                ResponseEntity<KmsDtos.DisableKeyResponse> response = kmsApiService.disableKey(keyId);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    String errorMsg = "Disable key failed: " + (response.getBody() != null ? response.getBody().toString() : "unknown error");
                    this.append(errorMsg);
                    Notification.show(errorMsg, 8000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return false;
                }

                Notification.show("Key disabled successfully", 8000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                ResponseEntity<KmsDtos.EnableKeyResponse> response = kmsApiService.enableKey(keyId);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    String errorMsg = "Enable key failed: " + (response.getBody() != null ? response.getBody().toString() : "unknown error");
                    this.append(errorMsg);
                    Notification.show(errorMsg, 8000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return false;
                }

                Notification.show("Key enabled successfully", 8000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }

            close();

            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
            Notification.show("Update error: " + errorMsg, 8000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            this.append(errorMsg);
            Notification.show("Error: " + errorMsg, 8000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }

        return false;
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