package eu.isygoit.ui.kms.views.common;

import eu.isygoit.ui.common.dialog.TabbedDetailsViewDialog;

/**
 * Read-only tabbed details dialog of the KMS module. Tabs, width, audit and
 * file-metadata helpers come from {@link TabbedDetailsViewDialog}.
 */
public abstract class KmsDetailsDialog extends TabbedDetailsViewDialog {

    protected KmsDetailsDialog(String title) {
        super(title);
        addClassName("kms-dialog");
    }
}