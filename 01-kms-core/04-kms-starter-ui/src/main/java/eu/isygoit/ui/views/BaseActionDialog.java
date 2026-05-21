package eu.isygoit.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Abstract base dialog that provides a standard header, footer with error span,
 * and Ok/Cancel buttons. Subclasses must implement {@link #onOk()} to definee
 * the ok action. The Cancel button closes the dialog by default.
 */
public abstract class BaseActionDialog extends Dialog {

    private final Runnable onSuccess;

    private final Span errorSpan;
    private final Button okButton;
    private final Button cancelButton;

    private StringBuilder errorMsgBuilder = new StringBuilder();

    /**
     * Constructs a new dialog with the given header title.
     *
     * @param title the dialog header title
     */
    public BaseActionDialog(String title, Runnable onSuccess) {
        this.onSuccess = onSuccess;
        setHeaderTitle(title);
        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);

        this.errorSpan = createErrorSpan();
        this.okButton = createOkButton();
        this.cancelButton = createCancelButton();

        buildFooter();
    }

    public void append(String error) {
        errorMsgBuilder.append(error).append("\n");
    }

    public void enableOkButton(boolean enable) {
        okButton.setEnabled(enable);
    }

    public void addThemeVariantsOkButton(ButtonVariant... variants) {
        okButton.addThemeVariants(variants);
    }

    /**
     * Hook method for the ok action. Called when the Ok button is clicked.
     * Implementations should perform the actual ok/creation logic and may call
     * {@link #showError(String)} to display an error.
     */
    protected abstract boolean onOk();

    /**
     * Sets the text of the Ok button.
     *
     * @param text the button label (e.g., "Create", "Update")
     */
    protected void setOkButtonText(String text) {
        okButton.setText(text);
    }

    /**
     * Optionally overrides the Cancel button's click listener. The default behaviour
     * is to close the dialog.
     *
     * @param listener the new click listener for the Cancel button
     */
    protected void setCancelButtonClickListener(com.vaadin.flow.component.ComponentEventListener<
            com.vaadin.flow.component.ClickEvent<Button>> listener) {
        cancelButton.addClickListener(listener);
    }

    /**
     * Displays an error message in the footer.
     *
     * @param message the error text (will be visible and styled in red)
     */
    protected void showError(String message) {
        errorSpan.setText(message);
        errorSpan.setVisible(true);
    }

    /**
     * Clears any displayed error message.
     */
    protected void clearError() {
        errorMsgBuilder = new StringBuilder();
        errorSpan.setText("");
        errorSpan.setVisible(false);
    }

    // ------------------------------------------------------------------------
    //  Private helper methods
    // ------------------------------------------------------------------------

    private Span createErrorSpan() {
        Span span = new Span();
        span.getStyle().set("color", "var(--lumo-error-text-color)");
        span.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        span.getStyle().set("margin-right", "auto");
        span.setVisible(false);
        return span;
    }

    private Button createOkButton() {
        Button button = new Button("Ok");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(e -> {
            clearError();
            boolean ok = onOk();
            if (ok && onSuccess != null) {
                onSuccess.run();
            } else {
                showError(errorMsgBuilder.toString());
            }
        });
        return button;
    }

    private Button createCancelButton() {
        Button button = new Button("Cancel");
        button.addClickListener(e -> close());
        return button;
    }

    private void buildFooter() {
        VerticalLayout footerLayout = new VerticalLayout();
        footerLayout.setWidthFull();
        footerLayout.setSpacing(true);
        footerLayout.setPadding(false);

        // Error span takes full width
        errorSpan.setWidthFull();
        footerLayout.add(errorSpan);

        // Buttons aligned to the right
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, okButton);
        buttonLayout.setSpacing(true);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        footerLayout.add(buttonLayout);

        getFooter().removeAll();
        getFooter().add(footerLayout);
    }
}