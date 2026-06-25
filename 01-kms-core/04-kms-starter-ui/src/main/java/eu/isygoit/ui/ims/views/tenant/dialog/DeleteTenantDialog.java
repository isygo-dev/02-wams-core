package eu.isygoit.ui.ims.views.tenant.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
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
        super(I18n.t("tenant.dialog.delete.title"),
                I18n.t("tenant.dialog.delete.message"),
                onSuccess);
        this.parentView = parentView;
        this.tenantService = tenantService;
        this.tenantId = tenantId;

        setOkButtonText(I18n.t("tenant.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("tenant.dialog.delete.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            tenantService.delete(tenantId);
            append(I18n.t("tenant.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("tenant.dialog.delete.error", e.getMessage()));
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