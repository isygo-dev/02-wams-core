package eu.isygoit.ui.views.key.dialogs;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.BaseActionDialog;
import eu.isygoit.ui.views.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Dialog for scheduling deletion of a KMS key.
 */
public class ScheduleKeyDeletionDialog extends BaseActionDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final KeyManagementView parentView;

    private IntegerField daysField;

    public ScheduleKeyDeletionDialog(KmsApiService kmsApiService, String keyId, KeyManagementView parentView) {
        super("Schedule key deletion");
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.parentView = parentView;

        setOkButtonText("Schedule");
        setWidth("400px");

        buildContent();
    }

    @Override
    protected void onOk() {
        clearError();
        int days = daysField.getValue();
        try {
            ResponseEntity<KmsDtos.ScheduleKeyDeletionResponse> response =
                    kmsApiService.scheduleKeyDeletion(keyId, days);
            if (response.getStatusCode().is2xxSuccessful()) {
                close();
                Notification.show("Deletion scheduled in " + days + " days", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                parentView.loadKeys();
            } else {
                String errorMsg = "Failed to schedule deletion: " + response.getStatusCode();
                showError(errorMsg);
                Notification.show(errorMsg, 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
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

        daysField = new IntegerField("Pending window (days)");
        daysField.setMin(7);
        daysField.setMax(30);
        daysField.setValue(30);
        daysField.setStepButtonsVisible(true);
        daysField.setWidthFull();

        layout.add(daysField);
        add(layout);
    }
}