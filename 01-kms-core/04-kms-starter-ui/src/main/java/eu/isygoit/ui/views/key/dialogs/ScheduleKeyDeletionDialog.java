package eu.isygoit.ui.views.key.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.key.KeyManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Dialog for scheduling deletion of a KMS key.
 */
public class ScheduleKeyDeletionDialog extends Dialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final KeyManagementView parentView;

    public ScheduleKeyDeletionDialog(KmsApiService kmsApiService, String keyId, KeyManagementView parentView) {
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.parentView = parentView;

        setHeaderTitle("Schedule key deletion");
        setWidth("400px");
        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);

        IntegerField daysField = new IntegerField("Pending window (days)");
        daysField.setMin(7);
        daysField.setMax(30);
        daysField.setValue(30);
        daysField.setStepButtonsVisible(true);
        daysField.setWidthFull();

        Button confirmBtn = new Button("Schedule", e -> {
            int days = daysField.getValue();
            close();
            try {
                ResponseEntity<KmsDtos.ScheduleKeyDeletionResponse> response =
                        kmsApiService.scheduleKeyDeletion(keyId, days);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Notification.show("Deletion scheduled in " + days + " days", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    parentView.loadKeys();
                } else {
                    Notification.show("Failed to schedule deletion", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> close());

        HorizontalLayout buttonLayout = new HorizontalLayout(cancelBtn, confirmBtn);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        getFooter().add(buttonLayout);
        add(daysField);
        this.open();
    }
}