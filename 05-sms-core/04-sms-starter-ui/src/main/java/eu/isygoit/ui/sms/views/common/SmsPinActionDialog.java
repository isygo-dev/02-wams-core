package eu.isygoit.ui.sms.views.common;

import eu.isygoit.ui.common.dialog.PinBaseActionDialog;

public abstract class SmsPinActionDialog extends PinBaseActionDialog {

    protected SmsPinActionDialog(String title, String warningMessage, Runnable onSuccess) {
        super(title, warningMessage, onSuccess);
    }

    protected SmsPinActionDialog(
            String title,
            String warningMessage,
            Runnable onSuccess,
            boolean requirePin) {
        super(title, warningMessage, onSuccess, requirePin);
    }
}
