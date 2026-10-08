package eu.isygoit.ui.kms.views.common;

import eu.isygoit.ui.common.dialog.BaseActionDialog;

public abstract class KmsActionDialog extends BaseActionDialog {

    protected KmsActionDialog(String title) {
        super(title);
    }

    protected KmsActionDialog(String title, Runnable onSuccess) {
        super(title, onSuccess);
    }
}
