package eu.isygoit.ui.ims.views.roleinfo.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
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
        super(I18n.t("ims.role.dialog.delete.title"),
                I18n.t("ims.role.dialog.delete.message"),
                onSuccess);
        this.parentView = parentView;
        this.roleService = roleService;
        this.roleId = roleId;

        setOkButtonText(I18n.t("ims.role.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("ims.role.dialog.delete.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            roleService.delete(roleId);
            append(I18n.t("ims.role.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.role.dialog.delete.error", e.getMessage()));
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