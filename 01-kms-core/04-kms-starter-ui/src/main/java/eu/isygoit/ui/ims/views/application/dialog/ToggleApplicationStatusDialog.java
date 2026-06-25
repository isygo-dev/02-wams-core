package eu.isygoit.ui.ims.views.application.dialog;

import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.application.ApplicationManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class ToggleApplicationStatusDialog extends PinBaseActionDialog {

    private final ApplicationManagementView parentView;
    private final ApplicationService applicationService;
    private final Long applicationId;
    private final IEnumEnabledBinaryStatus.Types currentStatus;

    public ToggleApplicationStatusDialog(ApplicationManagementView parentView,
                                         ApplicationService applicationService,
                                         Long applicationId,
                                         IEnumEnabledBinaryStatus.Types currentStatus,
                                         Runnable onSuccess) {
        super(
                currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("app.dialog.toggle.title.disable") : I18n.t("app.dialog.toggle.title.enable"),
                currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED
                        ? I18n.t("app.dialog.toggle.message.disable")
                        : I18n.t("app.dialog.toggle.message.enable"),
                onSuccess,
                false // requirePin = false (simple confirmation)
        );
        this.parentView = parentView;
        this.applicationService = applicationService;
        this.applicationId = applicationId;
        this.currentStatus = currentStatus;

        setOkButtonText(currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("app.dialog.toggle.button.disable") : I18n.t("app.dialog.toggle.button.enable"));
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            IEnumEnabledBinaryStatus.Types newStatus = currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED
                    ? IEnumEnabledBinaryStatus.Types.DISABLED
                    : IEnumEnabledBinaryStatus.Types.ENABLED;

            ResponseEntity<ApplicationDto> response = applicationService.updateStatus(applicationId, newStatus);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("app.dialog.toggle.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("app.dialog.toggle.success." + (newStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? "enable" : "disable")));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("app.dialog.toggle.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}