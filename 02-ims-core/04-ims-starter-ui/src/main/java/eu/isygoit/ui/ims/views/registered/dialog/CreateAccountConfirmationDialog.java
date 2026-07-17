package eu.isygoit.ui.ims.views.registered.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.registered.RegisteredManagementView;
import feign.FeignException;

/**
 * Confirmation dialog for creating an account from a NEW registration.
 * Extends PinBaseActionDialog to require PIN confirmation before proceeding.
 */
public class CreateAccountConfirmationDialog extends PinBaseActionDialog {

    private final RegisteredManagementView parentView;
    private final Runnable onConfirmAction;

    public CreateAccountConfirmationDialog(RegisteredManagementView parentView,
                                           String userEmail,
                                           Runnable onConfirmAction,
                                           Runnable onSuccess) {
        super(I18n.t("ims.registered.dialog.confirm.title"),
                I18n.t("ims.registered.dialog.confirm.message", userEmail),
                onSuccess);
        this.parentView = parentView;
        this.onConfirmAction = onConfirmAction;

        setOkButtonText(I18n.t("ims.registered.dialog.confirm.proceed"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_PRIMARY);
        setWidth("480px");
        addClassName("wams-confirmation-dialog");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("common.dialog.pin.invalid"));
            return false;
        }

        parentView.showLoading(true);
        try {
            // Execute the confirmation action
            if (onConfirmAction != null) {
                onConfirmAction.run();
            }
            append(I18n.t("ims.registered.dialog.confirm.success"));
            return true;
        } catch (Exception e) {
            append(I18n.t("ims.registered.dialog.confirm.error", e.getMessage()));
            return false;
        } finally {
            parentView.showLoading(false);
        }
    }
}