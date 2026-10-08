package eu.isygoit.ui.dms.views.common;

import eu.isygoit.ui.common.dialog.BaseActionDialog;

public abstract class DmsActionDialog extends BaseActionDialog {

    protected DmsActionDialog(String title) {
        super(title);
        addClassName("dms-dialog");
    }

    protected DmsActionDialog(String title, Runnable onSuccess) {
        super(title, onSuccess);
        addClassName("dms-dialog");
    }
}
