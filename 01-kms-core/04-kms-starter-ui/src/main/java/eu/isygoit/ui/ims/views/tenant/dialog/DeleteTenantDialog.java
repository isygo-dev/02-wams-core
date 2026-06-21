package eu.isygoit.ui.ims.views.tenant.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.tenant.TenantManagementView;
import feign.FeignException;

public class DeleteTenantDialog extends PinBaseActionDialog {

    private final TenantManagementView parentView;
    private final TenantService tenantService;
    private final Long tenantId;

    public DeleteTenantDialog(TenantManagementView parentView,
                              TenantService tenantService,
                              Long tenantId,
                              Runnable onSuccess) {
        super("Delete Tenant",
                "This action is irreversible. The tenant and all associated data will be permanently removed.",
                onSuccess);
        this.parentView = parentView;
        this.tenantService = tenantService;
        this.tenantId = tenantId;

        setOkButtonText("Delete permanently");
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append("Invalid confirmation code");
            return false;
        }

        parentView.showLoading(true);
        try {
            tenantService.delete(tenantId);
            append("Tenant deleted successfully");
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append("Failed operation: " + e.getMessage());
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