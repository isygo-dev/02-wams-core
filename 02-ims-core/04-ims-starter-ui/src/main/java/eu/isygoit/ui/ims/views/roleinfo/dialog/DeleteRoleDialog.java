package eu.isygoit.ui.ims.views.roleinfo.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RoleInfoService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.ims.views.roleinfo.RoleManagementView;

public class DeleteRoleDialog extends DeleteActionDialog {

    public DeleteRoleDialog(RoleManagementView parentView,
                            RoleInfoService roleService,
                            Long roleId,
                            Runnable onSuccess) {
        super(new Texts(
                        I18n.t("ims.role.dialog.delete.title"),
                        I18n.t("ims.role.dialog.delete.message"),
                        I18n.t("ims.role.dialog.delete.button"),
                        I18n.t("ims.role.dialog.delete.invalid.code"),
                        I18n.t("ims.role.dialog.delete.success"),
                        detail -> I18n.t("ims.role.dialog.delete.error", detail)),
                () -> roleService.delete(roleId),
                onSuccess,
                parentView::showLoading,
                "ims-dialog");
    }
}
