package eu.isygoit.ui.ims.views.common;

import eu.isygoit.ui.common.dialog.PinBaseActionDialog;

public abstract class ImsPinActionDialog extends PinBaseActionDialog {

    protected ImsPinActionDialog(String title, String warningMessage, Runnable onSuccess, boolean requirePin) {
        super(title, warningMessage, onSuccess, requirePin);
    }

    protected ImsPinActionDialog(String title, String warningMessage, Runnable onSuccess) {
        super(title, warningMessage, onSuccess);
    }
}
