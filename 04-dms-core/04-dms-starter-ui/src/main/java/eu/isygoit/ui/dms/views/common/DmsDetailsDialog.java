package eu.isygoit.ui.dms.views.common;

import eu.isygoit.ui.common.dialog.TabbedDetailsViewDialog;

/**
 * Read-only tabbed details dialog of the DMS module. Tabs, width, audit and
 * file-metadata helpers come from {@link TabbedDetailsViewDialog}.
 */
public abstract class DmsDetailsDialog extends TabbedDetailsViewDialog {

    protected DmsDetailsDialog(String title) {
        super(title);
        addClassName("dms-dialog");
    }
}