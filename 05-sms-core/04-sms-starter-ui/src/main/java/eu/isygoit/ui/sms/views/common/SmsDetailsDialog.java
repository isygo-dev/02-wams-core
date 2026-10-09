package eu.isygoit.ui.sms.views.common;

import eu.isygoit.ui.common.dialog.TabbedDetailsViewDialog;

/**
 * Read-only tabbed details dialog of the SMS module. Tabs, width, audit and
 * file-metadata helpers come from {@link TabbedDetailsViewDialog}.
 */
public abstract class SmsDetailsDialog extends TabbedDetailsViewDialog {

    protected SmsDetailsDialog(String title) {
        super(title);
        addClassName("sms-dialog");
    }
}