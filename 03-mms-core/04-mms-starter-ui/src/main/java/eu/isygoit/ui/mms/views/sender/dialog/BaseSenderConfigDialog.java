package eu.isygoit.ui.mms.views.sender.dialog;

import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.mms.views.sender.SenderConfigManagementView;
import lombok.extern.slf4j.Slf4j;

/**
 * Base dialog for Sender Configuration operations.
 * Provides common structure with header, content, and footer sections.
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
        this.parentView = parentView;
        this.senderConfigService = senderConfigService;

        setWidth("600px");
        setMaxWidth("95vw");
        setDraggable(true);
        setResizable(true);
    }

    /**
     * Builds the content area for the dialog.
     * Subclasses should override this to add their specific content.
     */
    protected void buildContent() {
        // Subclasses override this
    }

    protected boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return true; // Empty is allowed (optional field)
        }
        // Simple email validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}
