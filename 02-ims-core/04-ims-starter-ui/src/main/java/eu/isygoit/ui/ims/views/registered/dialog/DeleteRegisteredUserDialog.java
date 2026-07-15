package eu.isygoit.ui.ims.views.registered.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.registered.RegisteredManagementView;
import feign.FeignException;

public class DeleteRegisteredUserDialog extends PinBaseActionDialog {

    private final RegisteredManagementView parentView;
    private final RegisteredUserService registeredUserService;
    private final Long registeredUserId;

    public DeleteRegisteredUserDialog(RegisteredManagementView parentView,
                                      RegisteredUserService registeredUserService,
                                      Long registeredUserId,
                                      Runnable onSuccess) {
        super(I18n.t("ims.registered.dialog.delete.title"),
                I18n.t("ims.registered.dialog.delete.message"),
                onSuccess);
        this.parentView = parentView;
        this.registeredUserService = registeredUserService;
        this.registeredUserId = registeredUserId;

        setOkButtonText(I18n.t("ims.registered.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("ims.registered.dialog.delete.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            registeredUserService.delete(registeredUserId);
            append(I18n.t("ims.registered.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.registered.dialog.delete.error", e.getMessage()));
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
