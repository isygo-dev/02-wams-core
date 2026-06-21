package eu.isygoit.ui.ims.views.tenant.dialog;

import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.tenant.TenantManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class ToggleTenantStatusDialog extends PinBaseActionDialog {

    private final TenantManagementView parentView;
    private final TenantService tenantService;
    private final Long tenantId;
    private final IEnumEnabledBinaryStatus.Types currentStatus;

    public ToggleTenantStatusDialog(TenantManagementView parentView,
                                    TenantService tenantService,
                                    Long tenantId,
                                    IEnumEnabledBinaryStatus.Types currentStatus,
                                    Runnable onSuccess) {
        super(
                currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? "Disable Tenant" : "Enable Tenant",
                currentStatus == IEnumEnabledBinaryStatus.Types.ENABLED
                        ? "This will deactivate the tenant. All associated users will lose access. Are you sure?"
                        : "This will reactivate the tenant and restore full access. Are you sure?",
                onSuccess,
                false // simple confirmation, no PIN
        );
        this.parentView = parentView;
        this.tenantService = tenantService;
        this.tenantId = tenantId;
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

            ResponseEntity<TenantDto> response = tenantService.updateAdminStatus(tenantId, newStatus);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append("Failed to update tenant status: HTTP " + response.getStatusCodeValue());
                return false;
            }

            append("Tenant " + (newStatus == IEnumEnabledBinaryStatus.Types.ENABLED ? "enabled" : "disabled") + " successfully");
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