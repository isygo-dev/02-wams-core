package eu.isygoit.ui.kms.views.common;

import eu.isygoit.ui.common.dialog.BaseActionDialog;

public abstract class KmsActionDialog extends BaseActionDialog {

    protected KmsActionDialog(String title) {
        super(title);
        addClassName("kms-dialog");
    }

    protected KmsActionDialog(String title, Runnable onSuccess) {
        super(title, onSuccess);
        addClassName("kms-dialog");
    }
}
