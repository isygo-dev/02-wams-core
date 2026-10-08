package eu.isygoit.ui.ims.views.common;

import eu.isygoit.ui.common.dialog.BaseActionDialog;

public abstract class ImsActionDialog extends BaseActionDialog {

    protected ImsActionDialog(String title) {
        super(title);
        addClassName("ims-dialog");
    }

    protected ImsActionDialog(String title, Runnable onSuccess) {
        super(title, onSuccess);
        addClassName("ims-dialog");
    }
}
