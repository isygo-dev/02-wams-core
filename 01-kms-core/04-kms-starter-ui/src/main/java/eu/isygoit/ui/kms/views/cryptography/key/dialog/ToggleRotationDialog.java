package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import eu.isygoit.dto.KmsDtos.UpdateKeyRotationRequest;
import eu.isygoit.dto.KmsDtos.UpdateKeyRotationResponse;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Dialog to toggle key rotation with PIN confirmation.
 * When enabling rotation, a new key version will be created immediately.
 * When disabling rotation, future automatic rotations are stopped.
 */
public class ToggleRotationDialog extends PinBaseActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;
    private final String keyId;
    private final boolean currentlyEnabled;
    private final Integer currentPeriod;

    // Enable case – rotation period field
    private IntegerField periodField;

    public ToggleRotationDialog(KeyManagementView parentView,
                                KmsApiService kmsApiService,
                                String keyId,
                                boolean currentlyEnabled,
                                Integer currentPeriod,
                                Runnable onSuccess) {
        super(
                currentlyEnabled ? I18n.t("kms.key.dialog.rotation.toggle.title.disable") : I18n.t("kms.key.dialog.rotation.toggle.title.enable"),
                currentlyEnabled
                        ? I18n.t("kms.key.dialog.rotation.toggle.message.disable")
                        : I18n.t("kms.key.dialog.rotation.toggle.message.enable"),
                onSuccess,
                true  // always require PIN confirmation
        );
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.currentlyEnabled = currentlyEnabled;
        this.currentPeriod = currentPeriod;

        setOkButtonText(currentlyEnabled ? I18n.t("kms.key.dialog.rotation.toggle.button.disable") : I18n.t("kms.key.dialog.rotation.toggle.button.enable"));
        setWidth("450px");
        addClassName("toggle-rotation-dialog");

        if (!currentlyEnabled) {
            // For enable case, add the rotation period field before the PIN field
            buildEnableForm();
            // Insert period field into the layout before the PIN field
            VerticalLayout layout = (VerticalLayout) getChildren().findFirst().orElse(null);
            if (layout != null && layout.getComponentCount() >= 2) {
                // The PIN field is the last component; insert period field just before it
                layout.addComponentAtIndex(layout.getComponentCount() - 1, createPeriodLayout());
            }
        }
    }

    @Override
    protected boolean onOk() {
        // Validate PIN
        if (!validatePin()) {
            String errorMsg = I18n.t("kms.key.dialog.rotation.toggle.invalid.code");
            this.append(errorMsg);
            return false;
        }

        parentView.showLoading(true);

        try {
            UpdateKeyRotationRequest request;
            if (currentlyEnabled) {
                // Disable rotation
                request = UpdateKeyRotationRequest.builder()
                        .enableRotation(false)
                        .build();
            } else {
                // Enable rotation with chosen period (validate period)
                int period = periodField.getValue();
                if (period < 90 || period > 365) {
                    String errorMsg = I18n.t("kms.key.dialog.rotation.toggle.field.period.range");
                    this.append(errorMsg);
                    parentView.showLoading(false);
                    return false;
                }
                request = UpdateKeyRotationRequest.builder()
                        .enableRotation(true)
                        .rotationPeriodInDays(period)
                        .build();
            }

            ResponseEntity<UpdateKeyRotationResponse> response = kmsApiService.updateKeyRotation(keyId, request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = I18n.t("kms.key.dialog.rotation.toggle.failed",
                        (response.getBody() != null ? response.getBody().toString() : "unknown error"));
                this.append(errorMsg);
                return false;
            }

            String successMsg = currentlyEnabled
                    ? I18n.t("kms.key.dialog.rotation.toggle.success.disabled")
                    : I18n.t("kms.key.dialog.rotation.toggle.success.enabled", periodField.getValue());
            Notification.show(successMsg, 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            return true;

        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
        } catch (Exception e) {
            String errorMsg = I18n.t("kms.key.dialog.rotation.toggle.error", e.getMessage());
            this.append(errorMsg);
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildEnableForm() {
        periodField = new IntegerField(I18n.t("kms.key.dialog.rotation.toggle.field.period"));
        periodField.setMin(90);
        periodField.setMax(365);
        periodField.setValue(currentPeriod != null ? currentPeriod : 365);
        periodField.setStepButtonsVisible(true);
        periodField.setWidthFull();
        periodField.setTooltipText(I18n.t("kms.key.dialog.rotation.toggle.field.period.tooltip"));
        periodField.setRequiredIndicatorVisible(true);
    }

    private FormLayout createPeriodLayout() {
        FormLayout form = new FormLayout();
        form.add(periodField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.addClassName("period-form");
        return form;
    }
}