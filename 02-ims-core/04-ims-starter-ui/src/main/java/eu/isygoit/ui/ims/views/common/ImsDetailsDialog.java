package eu.isygoit.ui.ims.views.common;

import eu.isygoit.ui.common.dialog.TabbedDetailsViewDialog;

/**
 * Read-only tabbed details dialog of the IMS module. Tabs, width, audit and
 * file-metadata helpers come from {@link TabbedDetailsViewDialog}.
 */
public abstract class ImsDetailsDialog extends TabbedDetailsViewDialog {

    protected ImsDetailsDialog(String title) {
        super(title);
        addClassName("ims-dialog");
    }
}