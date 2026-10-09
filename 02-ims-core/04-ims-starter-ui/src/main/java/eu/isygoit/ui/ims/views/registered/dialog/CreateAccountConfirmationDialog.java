package eu.isygoit.ui.ims.views.registered.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.PinConfirmActionDialog;
import eu.isygoit.ui.ims.views.registered.RegisteredManagementView;

/**
 * Confirmation dialog for creating an account from a NEW registration.
 * PIN confirmation is required before the confirmation callback is executed.
 */
public class CreateAccountConfirmationDialog extends PinConfirmActionDialog {

    public CreateAccountConfirmationDialog(RegisteredManagementView parentView,
                                           String userEmail,
                                           Runnable onConfirmAction,
                                           Runnable onSuccess) {
        super(new Texts(
                        I18n.t("ims.registered.dialog.confirm.title"),
                        I18n.t("ims.registered.dialog.confirm.message", userEmail),
                        I18n.t("ims.registered.dialog.confirm.proceed"),
                        I18n.t("common.dialog.pin.invalid"),
                        I18n.t("ims.registered.dialog.confirm.success"),
                        detail -> I18n.t("ims.registered.dialog.confirm.error", detail)),
                () -> {
                    // Execute the confirmation action
                    if (onConfirmAction != null) {
                        onConfirmAction.run();
                    }
                    return null;
                },
                onSuccess,
                parentView::showLoading,
                "ims-dialog",
                true,
                ButtonVariant.LUMO_PRIMARY);
        addClassName("wams-confirmation-dialog");
    }
}
