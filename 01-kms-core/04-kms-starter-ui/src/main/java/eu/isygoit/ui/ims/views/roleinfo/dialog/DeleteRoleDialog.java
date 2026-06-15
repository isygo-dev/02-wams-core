package eu.isygoit.ui.ims.views.roleinfo.dialog;

import eu.isygoit.remote.ims.RoleInfoService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.roleinfo.RoleManagementView;
import feign.FeignException;

public class DeleteRoleDialog extends PinBaseActionDialog {

    private final RoleManagementView parentView;
    private final RoleInfoService roleService;
    private final Long roleId;

    public DeleteRoleDialog(RoleManagementView parentView,
                            RoleInfoService roleService,
                            Long roleId,
                            Runnable onSuccess) {
        super("Delete Role",
                "This action is irreversible. The role and all its assignments will be permanently removed.",
                onSuccess);
        this.parentView = parentView;
        this.roleService = roleService;
        this.roleId = roleId;

        setOkButtonText("Delete permanently");
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            roleService.delete(roleId);
            append("Role deleted successfully");
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