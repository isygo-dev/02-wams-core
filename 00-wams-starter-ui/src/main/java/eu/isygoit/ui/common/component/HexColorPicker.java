package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.HasHelper;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 * A minimal color field: a native browser {@code <input type="color">} swatch
 * kept in sync with a validated hex {@link TextField}.
 * <p>
 * This exists because {@code com.vaadin.flow.component.colorpicker.ColorPicker}
 * is NOT a real class in vaadin-core — there is no built-in Vaadin Flow color
 * picker component. This class fills that gap using only core Vaadin classes,
 * so it needs no add-on dependency (e.g. com.github.juchar:color-picker-flow).
 * <p>
 * Usage is a drop-in replacement for the imagined {@code ColorPicker}:
 * <pre>
 *     HexColorPicker colorField = new HexColorPicker("Color");
 *     colorField.setHelperText("Pick a color for this item");
 *     colorField.setValue("#3366FF");
 *     String hex = colorField.getValue();
 * </pre>
 */
public class HexColorPicker extends CustomField<String> implements HasHelper {

    private static final String HEX_PATTERN = "^#([A-Fa-f0-9]{6})$";
    private static final String DEFAULT_COLOR = "#000000";

    private final Input swatch = new Input();
    private final TextField hexField = new TextField();

    public HexColorPicker() {
        this(null);
    }

    public HexColorPicker(String label) {
        if (label != null) {
            setLabel(label);
        }

        swatch.getElement().setAttribute("type", "color");
        swatch.getElement().getStyle().set("width", "40px");
        swatch.getElement().getStyle().set("height", "40px");
        swatch.getElement().getStyle().set("padding", "0");
        swatch.getElement().getStyle().set("border", "none");
        swatch.getElement().getStyle().set("cursor", "pointer");
        swatch.getElement().addEventListener("input", e -> {
            String value = e.getEventData().getString("element.value");
            hexField.setValue(value != null ? value : "");
        }).addEventData("element.value");

        hexField.setPlaceholder("#RRGGBB");
        hexField.setPattern(HEX_PATTERN);
        hexField.setAllowedCharPattern("[A-Fa-f0-9#]");
        hexField.setClearButtonVisible(true);
        hexField.setWidth("120px");
        hexField.addValueChangeListener(e -> {
            String v = e.getValue();
            if (v != null && v.matches(HEX_PATTERN)) {
                swatch.getElement().setProperty("value", v);
            }
            // Required so this field's own getValue() (and any Binder bound
            // to it) picks up edits typed directly into hexField, not just
            // changes driven by the swatch.
            updateValue();
        });

        HorizontalLayout layout = new HorizontalLayout(swatch, hexField);
        layout.setSpacing(true);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        add(layout);
    }

    @Override
    protected String generateModelValue() {
        String v = hexField.getValue();
        return (v == null || v.isBlank()) ? null : v;
    }

    @Override
    protected void setPresentationValue(String value) {
        String v = (value != null && !value.isBlank()) ? value : DEFAULT_COLOR;
        hexField.setValue(v);
        swatch.getElement().setProperty("value", v);
    }
}