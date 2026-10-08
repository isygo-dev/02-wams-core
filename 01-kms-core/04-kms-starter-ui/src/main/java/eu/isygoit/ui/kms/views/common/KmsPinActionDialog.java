package eu.isygoit.ui.kms.views.common;

import eu.isygoit.ui.common.dialog.PinBaseActionDialog;

public abstract class KmsPinActionDialog extends PinBaseActionDialog {

    protected KmsPinActionDialog(
            String title,
            String warningMessage,
            Runnable onSuccess,
            boolean requirePin) {
        super(title, warningMessage, onSuccess, requirePin);
    }

    protected KmsPinActionDialog(String title, String warningMessage, Runnable onSuccess) {
        super(title, warningMessage, onSuccess);
    }
}
