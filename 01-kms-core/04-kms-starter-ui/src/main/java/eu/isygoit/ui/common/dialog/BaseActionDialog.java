package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;

/**
 * Modern, compact base dialog with a header, a content area, and a footer
 * with Ok/Cancel buttons. Subclasses must implement {@link #onOk()}.
 */
public abstract class BaseActionDialog extends Dialog {

    private final Span errorSpan;
    private final Button okButton;
    private final Button cancelButton;
    private final StringBuilder msgBuilder = new StringBuilder();
    private Runnable onSuccess;

    public BaseActionDialog(String title, Runnable onSuccess) {
        this(title);
        this.onSuccess = onSuccess;
    }

    public BaseActionDialog(String title) {
        setHeaderTitle(title);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
        setWidth("500px");
        setMaxWidth("90%");
        setResizable(false);

        this.errorSpan = createErrorSpan();
        this.okButton = createOkButton();
        this.cancelButton = createCancelButton();

        buildFooter();
    }

    public void append(String error) {
        msgBuilder.append(error).append("\n");
    }

    public void enableOkButton(boolean enable) {
        okButton.setEnabled(enable);
    }

    public void addThemeVariantsOkButton(ButtonVariant... variants) {
        okButton.addThemeVariants(variants);
    }

    /**
     * Implement this method to perform the action. Return {@code true} on success,
     * {@code false} on failure. Use {@link #append(String)} to collect error messages.
     */
    protected abstract boolean onOk();

    protected void setOkButtonText(String text) {
        okButton.setText(text);
    }

    protected void setCancelButtonClickListener(
            ComponentEventListener<ClickEvent<Button>> listener) {
        cancelButton.addClickListener(listener);
    }

    protected void showError(String message) {
        errorSpan.setText(message);
        errorSpan.setVisible(true);
        Notification.show(message, 5000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    protected void showSuccess(String message) {
        errorSpan.setText(null);
        errorSpan.setVisible(false);
        Notification.show(message, 5000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    protected void clearError() {
        msgBuilder.setLength(0);
        errorSpan.setText("");
        errorSpan.setVisible(false);
    }

    // ------------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------------

    private Span createErrorSpan() {
        Span span = new Span();
        span.addClassName(LumoUtility.TextColor.ERROR);
        span.addClassName(LumoUtility.FontSize.SMALL);
        span.addClassName("wams-dialog-error-span");
        span.setVisible(false);
        return span;
    }

    private Button createOkButton() {
        Button button = new Button(I18n.t("common.dialog.base.ok"));
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(e -> {
            clearError();
            boolean ok = onOk();
            if (ok) {
                close();
                if (onSuccess != null) onSuccess.run();
                showSuccess(msgBuilder.toString());
            } else {
                showError(msgBuilder.toString());
            }
        });
        return button;
    }

    private Button createCancelButton() {
        Button button = new Button(I18n.t("common.dialog.base.cancel"));
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        button.addClickListener(e -> close());
        return button;
    }

    private void buildFooter() {
        VerticalLayout footerLayout = new VerticalLayout();
        footerLayout.setWidthFull();
        footerLayout.setSpacing(true);
        footerLayout.setPadding(false);

        errorSpan.setWidthFull();
        footerLayout.add(errorSpan);

        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, okButton);
        buttonLayout.setSpacing(true);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        footerLayout.add(buttonLayout);

        getFooter().removeAll();
        getFooter().add(footerLayout);
    }

    /**
     * Convenience method to add content with consistent padding.
     */
    protected void addContent(com.vaadin.flow.component.Component... components) {
        VerticalLayout content = new VerticalLayout(components);
        content.setPadding(true);
        content.setSpacing(true);
        content.setWidthFull();
        add(content);
    }
}