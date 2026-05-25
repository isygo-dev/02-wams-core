package eu.isygoit.ui.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Base dialog that provides a 9‑digit confirmation code, a PIN input field,
 * automatic validation, and a custom warning message.
 * <p>
 * Subclasses must implement {@link #onOk()} to perform the actual action.
 */
public abstract class PinBaseActionDialog extends BaseActionDialog {

    protected final String confirmationCode;
    private final String warning = "⚠️ ";
    protected TextField pinField;

    /**
     * Constructs a new PIN confirmation dialog.
     *
     * @param title          dialog header title
     * @param warningMessage the warning text to display (e.g., "This action is irreversible...")
     * @param onSuccess      callback executed after successful {@link #onOk()}
     */
    public PinBaseActionDialog(String title, String warningMessage, Runnable onSuccess) {
        super(title, onSuccess);
        this.confirmationCode = generateConfirmationCode();
        this.enableOkButton(false);
        buildContent(this.warning + warningMessage);
    }

    /**
     * Builds the dialog content: warning message(s), code display, and PIN field.
     *
     * @param warningMessage the warning text
     */
    private void buildContent(String warningMessage) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);

        // Warning message (can be multi-line by using multiple Spans or HTML)
        layout.add(new Span(warningMessage));
        layout.add(new Span("To confirm, enter the 9‑digit code below:"));

        // Code display
        layout.add(createCodeDisplay(confirmationCode));

        // PIN field with validation
        pinField = createPinField();
        layout.add(pinField);
        setupPinValidation(pinField);

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

    protected void setupPinValidation(TextField pinField) {
        pinField.addValueChangeListener(e -> validateAndEnableButton());
    }

    protected void validateAndEnableButton() {
        String value = pinField.getValue();
        boolean isValid = value != null && value.matches("\\d{9}") && value.equals(confirmationCode);
        enableOkButton(isValid);
        if (isValid) {
            clearError();
        }
    }

    private String generateConfirmationCode() {
        int code = ThreadLocalRandom.current().nextInt(100_000_000, 1_000_000_000);
        return String.valueOf(code);
    }
}