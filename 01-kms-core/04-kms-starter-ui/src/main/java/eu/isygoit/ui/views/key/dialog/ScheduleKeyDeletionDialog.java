package eu.isygoit.ui.views.key.dialog;

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

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;
    private final Runnable onSuccess;

    private final String keyId;


    private IntegerField daysField;

    public ScheduleKeyDeletionDialog(KeyManagementView parentView,
                                     KmsApiService kmsApiService,
                                     Runnable onSuccess,
                                     String keyId) {
        super("Schedule key deletion", onSuccess);
        this.onSuccess = onSuccess;
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.parentView = parentView;

        setOkButtonText("Schedule");
        setWidth("400px");

        buildContent();
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        int days = daysField.getValue();
        try {
            ResponseEntity<KmsDtos.ScheduleKeyDeletionResponse> response = kmsApiService.scheduleKeyDeletion(keyId, days);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Schedule deletion error: " + response.getStatusCode();
                showError(errorMsg);
                Notification.show(errorMsg, 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false;
            }

            close();
            Notification.show("Deletion scheduled in " + days + " days", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            showError(errorMsg);
            Notification.show("Schedule deletion error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            showError(errorMsg);
            Notification.show("Schedule deletion error: " + errorMsg, 5000, Notification.Position.TOP_END)
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