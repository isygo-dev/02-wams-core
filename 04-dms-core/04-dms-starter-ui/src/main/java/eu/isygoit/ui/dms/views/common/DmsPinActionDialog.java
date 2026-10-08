package eu.isygoit.ui.dms.views.common;

import eu.isygoit.ui.common.dialog.PinBaseActionDialog;

public abstract class DmsPinActionDialog extends PinBaseActionDialog {

    protected DmsPinActionDialog(String title, String warningMessage, Runnable onSuccess) {
        super(title, warningMessage, onSuccess);
    }

    protected DmsPinActionDialog(String title, String warningMessage, Runnable onSuccess, boolean requirePin) {
        super(title, warningMessage, onSuccess, requirePin);
    }
}
