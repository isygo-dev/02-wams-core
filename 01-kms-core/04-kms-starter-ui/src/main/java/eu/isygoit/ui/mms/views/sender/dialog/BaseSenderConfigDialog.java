package eu.isygoit.ui.mms.views.sender.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.mms.views.sender.SenderConfigManagementView;
import lombok.extern.slf4j.Slf4j;

/**
 * Base dialog for Sender Configuration operations.
 * Provides common structure with header, content, and footer sections.
 */
@Slf4j
public abstract class BaseSenderConfigDialog extends Dialog {

    protected final SenderConfigManagementView parentView;
    protected final SenderConfigService senderConfigService;
    protected final Runnable onSuccess;

    protected final Div contentArea = new Div();
    protected final Div errorArea = new Div();
    protected Button okButton;
    protected Button cancelButton;

    public BaseSenderConfigDialog(String title,
                                  SenderConfigManagementView parentView,
                                  SenderConfigService senderConfigService,
                                  Runnable onSuccess) {
        this.parentView = parentView;
        this.senderConfigService = senderConfigService;
        this.onSuccess = onSuccess;

        setHeaderTitle(title);
        setWidth("600px");
        setMaxWidth("95vw");
        setModal(true);
        setDraggable(true);
        setResizable(true);

        buildLayout();

        // Close on ESC
        addDialogCloseActionListener(e -> close());
    }

    private void buildLayout() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(true);
        mainLayout.setSpacing(true);
        mainLayout.setWidthFull();

        // Content area
        contentArea.addClassName("wams-dialog-content-area");
        mainLayout.add(contentArea);

        add(mainLayout);

        // Same footer contract as BaseActionDialog/NoActionDialog: an error-message
        // slot above the action button row, inside Vaadin's dedicated footer slot
        // (not just the last row of scrollable content).
        errorArea.setVisible(false);
        errorArea.addClassName("wams-dialog-error-area");
        errorArea.addClassName("wams-dialog-error-span");
        errorArea.setWidthFull();

        HorizontalLayout buttonRow = buildFooter();

        VerticalLayout footerLayout = new VerticalLayout(errorArea, buttonRow);
        footerLayout.setWidthFull();
        footerLayout.setSpacing(true);
        footerLayout.setPadding(false);

        getFooter().removeAll();
        getFooter().add(footerLayout);
    }

    private HorizontalLayout buildFooter() {
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        footer.setSpacing(true);

        cancelButton = new Button(I18n.t("common.dialog.cancel"), e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        okButton = new Button(getOkButtonText(), e -> {
            if (onOk()) {
                close();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        footer.add(cancelButton, okButton);
        return footer;
    }

    /**
     * Override to provide custom OK button text.
     */
    protected String getOkButtonText() {
        return I18n.t("common.dialog.ok");
    }

    /**
     * Sets the OK button text.
     */
    protected void setOkButtonText(String text) {
        okButton.setText(text);
    }

    /**
     * Override to add theme variants to OK button.
     */
    protected void addThemeVariantsOkButton(ButtonVariant... variants) {
        okButton.addThemeVariants(variants);
    }

    /**
     * Appends an error message to the error area.
     */
    protected void append(String errorMessage) {
        errorArea.removeAll();
        errorArea.add(new Span(errorMessage));
        errorArea.setVisible(true);
    }

    /**
     * Clears the error area.
     */
    protected void clearErrors() {
        errorArea.removeAll();
        errorArea.setVisible(false);
    }

    /**
     * Shows a success notification.
     */
    protected void showSuccess(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    /**
     * Shows an error notification.
     */
    protected void showError(String message) {
        Notification.show(message, 5000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /**
     * Abstract method to be implemented by subclasses for the OK action.
     *
     * @return true if successful, false otherwise
     */
    protected abstract boolean onOk();

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