package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Base dialog that adds a 9‑digit PIN confirmation for destructive actions.
 * Displays a warning icon before the warning message in the content area.
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

    /**
     * Builds a warning message layout with an icon before the text.
     */
    private HorizontalLayout createWarningMessage(String message) {
        Icon warningIcon = VaadinIcon.WARNING.create();
        warningIcon.setSize("18px");
        warningIcon.addClassName("wams-dialog-warning-icon");

        Span messageSpan = new Span(message);
        messageSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        HorizontalLayout warningLayout = new HorizontalLayout(warningIcon, messageSpan);
        warningLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        warningLayout.setSpacing(true);
        warningLayout.setPadding(false);
        warningLayout.addClassName("wams-dialog-warning-row");
        return warningLayout;
    }

    private void buildContentWithPin(String warningMessage) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);

        layout.add(createWarningMessage(warningMessage));
        layout.add(new Span(I18n.t("common.dialog.pin.confirm.instruction")));

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
        layout.add(createWarningMessage(warningMessage));
        addContent(layout);
    }

    protected Div createCodeDisplay(String code) {
        Span codeSpan = new Span(code);
        codeSpan.addClassName(LumoUtility.FontWeight.BOLD);
        codeSpan.addClassName("wams-dialog-code-span");
        Div codeDiv = new Div(codeSpan);
        codeDiv.addClassName("wams-dialog-code-display");
        return codeDiv;
    }

    protected TextField createPinField() {
        TextField field = new TextField();
        field.setPlaceholder(I18n.t("common.dialog.pin.field.placeholder"));
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