package eu.isygoit.ui.sms.views.common;

import eu.isygoit.ui.common.dialog.BaseActionDialog;

public abstract class SmsActionDialog extends BaseActionDialog {

    protected SmsActionDialog(String title) {
        super(title);
        addClassName("sms-dialog");
    }

    protected SmsActionDialog(String title, Runnable onSuccess) {
        super(title, onSuccess);
        addClassName("sms-dialog");
    }
}
