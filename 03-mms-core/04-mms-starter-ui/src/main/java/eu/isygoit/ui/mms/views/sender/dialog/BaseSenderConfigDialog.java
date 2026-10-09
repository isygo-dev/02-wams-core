package eu.isygoit.ui.mms.views.sender.dialog;

import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.mms.views.sender.SenderConfigManagementView;
import lombok.extern.slf4j.Slf4j;

/**
 * Base dialog for Sender Configuration operations.
 * Provides the shared services and the dialog width.
 */
@Slf4j
public abstract class BaseSenderConfigDialog extends BaseActionDialog {

    protected final SenderConfigManagementView parentView;
    protected final SenderConfigService senderConfigService;

    public BaseSenderConfigDialog(String title,
                                  SenderConfigManagementView parentView,
                                  SenderConfigService senderConfigService,
                                  Runnable onSuccess) {
        super(title, onSuccess);
        addClassName("mms-dialog");
        this.parentView = parentView;
        this.senderConfigService = senderConfigService;

        DialogLayout.size(this, DialogLayout.WIDTH_L);
        setDraggable(true);
        setResizable(true);
    }
}
