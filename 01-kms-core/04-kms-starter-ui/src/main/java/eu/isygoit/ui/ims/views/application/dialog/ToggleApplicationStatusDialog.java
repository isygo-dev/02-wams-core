package eu.isygoit.ui.ims.views.application.dialog;

import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
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
                currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? "Disable Application" : "Enable Application",
                currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED
                        ? "This will deactivate the application. Users will not see it in the store. Are you sure?"
                        : "This will reactivate the application and make it visible in the store. Are you sure?",
                onSuccess
        );
        this.parentView = parentView;
        this.applicationService = applicationService;
        this.applicationId = applicationId;
        this.currentStatus = currentStatus;

        setOkButtonText(currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? "Disable" : "Enable");
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
                append("Failed to update application status: HTTP " + response.getStatusCodeValue());
                return false;
            }

            append("Application " + (newStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? "enabled" : "disabled") + " successfully");
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append("Operation failed: " + e.getMessage());
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