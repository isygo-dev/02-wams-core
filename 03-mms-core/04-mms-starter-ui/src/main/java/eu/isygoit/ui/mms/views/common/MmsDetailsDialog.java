package eu.isygoit.ui.mms.views.common;

import eu.isygoit.ui.common.dialog.TabbedDetailsViewDialog;

/**
 * Read-only tabbed details dialog of the MMS module. Tabs, width, audit and
 * file-metadata helpers come from {@link TabbedDetailsViewDialog}.
 */
public abstract class MmsDetailsDialog extends TabbedDetailsViewDialog {

    protected MmsDetailsDialog(String title) {
        super(title);
        addClassName("mms-dialog");
    }
}