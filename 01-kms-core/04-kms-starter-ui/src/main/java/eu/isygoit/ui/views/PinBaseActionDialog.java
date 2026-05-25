package eu.isygoit.ui.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Base dialog that optionally provides a 9‑digit confirmation code, a PIN input field,
 * automatic validation, and a custom warning message.
 * <p>
 * Subclasses must implement {@link #onOk()} to perform the actual action.
 */
public abstract class PinBaseActionDialog extends BaseActionDialog {

    protected final String confirmationCode;
    private final String warning = "⚠️ ";
    protected TextField pinField;
    private final boolean requirePin;

    /**
     * Constructs a new PIN confirmation dialog.
     *
     * @param title          dialog header title
     * @param warningMessage the warning text to display (e.g., "This action is irreversible...")
     * @param onSuccess      callback executed after successful {@link #onOk()}
     * @param requirePin     if true, a 9‑digit code is required; if false, the dialog behaves like a simple BaseActionDialog
     */
    public PinBaseActionDialog(String title, String warningMessage, Runnable onSuccess, boolean requirePin) {
        super(title, onSuccess);
        this.requirePin = requirePin;
        if (requirePin) {
            this.confirmationCode = generateConfirmationCode();
            this.enableOkButton(false);
            buildContent(this.warning + warningMessage);
        } else {
            this.confirmationCode = null;
            buildContentSimple(warningMessage);
        }
    }

    /**
     * Legacy constructor – defaults to requirePin = true.
     */
    public PinBaseActionDialog(String title, String warningMessage, Runnable onSuccess) {
        this(title, warningMessage, onSuccess, true);
    }

    /**
     * Builds the dialog content with PIN challenge.
     */
    private void buildContent(String warningMessage) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);

        layout.add(new Span(warningMessage));
        layout.add(new Span("To confirm, enter the 9‑digit code below:"));

        layout.add(createCodeDisplay(confirmationCode));

        pinField = createPinField();
        layout.add(pinField);
        setupPinValidation();

        add(layout);
    }

    /**
     * Builds simple content without PIN (just the warning).
     */
    private void buildContentSimple(String warningMessage) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.add(new Span(warningMessage));
        add(layout);
    }

    protected Div createCodeDisplay(String code) {
        Span codeSpan = new Span(code);
        codeSpan.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "28px")
                .set("font-family", "monospace")
                .set("background-color", "#f0f0f0")
                .set("padding", "12px 20px")
                .set("border-radius", "8px")
                .set("text-align", "center")
                .set("letter-spacing", "4px");

        Div codeDiv = new Div(codeSpan);
        codeDiv.getStyle().set("text-align", "center");
        return codeDiv;
    }

    protected TextField createPinField() {
        TextField field = new TextField();
        field.setPlaceholder("Enter 9‑digit code");
        field.setWidthFull();
        field.setPattern("[0-9]*");
        field.setMaxLength(9);
        field.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        field.setAllowedCharPattern("[0-9]");
        return field;
    }

    protected void setupPinValidation() {
        pinField.addValueChangeListener(e -> {
            String value = pinField.getValue();
            boolean isValid = value != null && value.matches("\\d{9}") && value.equals(confirmationCode);
            enableOkButton(isValid);
            if (isValid) clearError();
        });
    }

    private String generateConfirmationCode() {
        int code = ThreadLocalRandom.current().nextInt(100_000_000, 1_000_000_000);
        return String.valueOf(code);
    }

    /**
     * Override this to perform additional validation if needed.
     * The base implementation does nothing.
     */
    protected boolean validatePin() {
        if (!requirePin) return true;
        String enteredPin = pinField.getValue();
        return enteredPin != null && enteredPin.equals(confirmationCode);
    }
}