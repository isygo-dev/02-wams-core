package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Dialog for scheduling deletion of a KMS key.
 */
public class ScheduleKeyDeletionDialog extends BaseActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;


    private final String keyId;
    private final int days;


    private IntegerField daysField;

    public ScheduleKeyDeletionDialog(KeyManagementView parentView,
                                     KmsApiService kmsApiService,
                                     String keyId,
                                     Integer days,
                                     Runnable onSuccess) {
        super(I18n.t("kms.key.dialog.schedule.title"), onSuccess);
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.parentView = parentView;
        this.days = days != null ? days : 30; // default to 30 if not provided

        setOkButtonText(I18n.t("kms.key.dialog.schedule.button"));
        setWidth("400px");

        buildContent();
    }

    @Override
    protected boolean onOk() {
        int days = daysField.getValue();
        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.ScheduleKeyDeletionResponse> response = kmsApiService.scheduleKeyDeletion(keyId, days);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = I18n.t("kms.key.dialog.schedule.failed", response.getStatusCode());
                this.append(errorMsg);
                return false;
            }

            Notification.show(I18n.t("kms.key.dialog.schedule.success", days), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            return true;
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
        } catch (Exception e) {
            String errorMsg = I18n.t("kms.key.dialog.schedule.error", e.getMessage());
            this.append(errorMsg);
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);

        daysField = new IntegerField(I18n.t("kms.key.dialog.schedule.field.pending.window"));
        daysField.setMin(7);
        daysField.setMax(30);
        daysField.setValue(days);
        daysField.setStepButtonsVisible(true);
        daysField.setWidthFull();

        layout.add(daysField);
        add(layout);
    }
}