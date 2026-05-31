package eu.isygoit.ui.views.key.dialog;

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
 * Dialog for cancelling the scheduled deletion of a KMS key.
 */
public class CancelKeyDeletionDialog extends BaseActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;

    private final String keyId;
    private final String keyAliasOrId;

    public CancelKeyDeletionDialog(KeyManagementView parentView,
                                   KmsApiService kmsApiService,
                                   String keyId,
                                   String keyAliasOrId,
                                   Runnable onSuccess) {
        super("Cancel deletion", onSuccess);
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.keyAliasOrId = keyAliasOrId;
        this.parentView = parentView;

        setOkButtonText("Yes, cancel");
        setWidth("450px");

        buildContent();
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.CancelKeyDeletionResponse> response =
                    kmsApiService.cancelKeyDeletion(keyId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Failed to cancel deletion: " + response.getStatusCode();
                this.append(errorMsg);
                Notification.show(errorMsg, 6000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false;
            }


            Notification.show("Deletion cancelled", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
            Notification.show("Update error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            this.append(errorMsg);
            Notification.show("Error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.add(new Span("Are you sure you want to cancel the deletion of key " + keyAliasOrId + "?"));
        add(layout);
    }
}