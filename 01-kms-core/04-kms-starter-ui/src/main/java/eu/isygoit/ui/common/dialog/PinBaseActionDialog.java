package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Base dialog that adds a 9‑digit PIN confirmation for destructive actions.
 */
public abstract class PinBaseActionDialog extends BaseActionDialog {

    protected final String confirmationCode;
    private final boolean requirePin;
    protected TextField pinField;

    public PinBaseActionDialog(String title, String warningMessage, Runnable onSuccess, boolean requirePin) {
        super(title, onSuccess);
        this.requirePin = requirePin;
        if (requirePin) {
            this.confirmationCode = generateConfirmationCode();
            enableOkButton(false);
            buildContentWithPin(warningMessage);
        } else {
            this.confirmationCode = null;
            buildContentSimple(warningMessage);
        }
    }

    public PinBaseActionDialog(String title, String warningMessage, Runnable onSuccess) {
        this(title, warningMessage, onSuccess, true);
    }

    private void buildContentWithPin(String warningMessage) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);

        layout.add(new Span(warningMessage));
        layout.add(new Span("To confirm, enter the 9‑digit code below:"));

        layout.add(createCodeDisplay(confirmationCode));

        pinField = createPinField();
        layout.add(pinField);
        setupPinValidation();

        addContent(layout);
    }

    private void buildContentSimple(String warningMessage) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.add(new Span(warningMessage));
        addContent(layout);
    }

    protected Div createCodeDisplay(String code) {
        Span codeSpan = new Span(code);
        codeSpan.addClassName(LumoUtility.FontWeight.BOLD);
        codeSpan.getStyle()
                .set("font-size", "28px")
                .set("font-family", "monospace")
                .set("background-color", "var(--lumo-contrast-5pct)")
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

    protected boolean validatePin() {
        if (!requirePin) return true;
        String entered = pinField.getValue();
        return entered != null && entered.equals(confirmationCode);
    }
}