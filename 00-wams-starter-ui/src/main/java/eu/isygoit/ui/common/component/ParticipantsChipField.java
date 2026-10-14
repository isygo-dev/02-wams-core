package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.i18n.I18n;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Chip-based multi-value input for participants (emails or user codes).
 *
 * <p>Behaviour:
 * <ul>
 *   <li>Type a value, press <b>Enter</b> or click <b>+</b> to add it as a chip.</li>
 *   <li>Paste or type comma-separated values (e.g. {@code a@x.fr, b@y.fr})
 *       and commit — every token becomes its own chip.</li>
 *   <li>Leave the input to auto-commit (blur).</li>
 *   <li>Click the small <b>×</b> on a chip to remove it.</li>
 * </ul>
 *
 * <p>Extends {@link CustomField} so it plugs into a {@code FormLayout} and
 * participates in Vaadin's value/validation pipeline like any other field.
 */
public class ParticipantsChipField extends CustomField<List<String>> {

    private final HorizontalLayout chipsRow = new HorizontalLayout();
    private final VerticalLayout emptyHint = new VerticalLayout();
    private final TextField input = new TextField();
    private final Button addButton = new Button(VaadinIcon.PLUS.create());
    private final List<String> values = new ArrayList<>();

    public ParticipantsChipField(String label) {
        setLabel(label);
        getElement().getStyle().set("display", "block");

        chipsRow.setSpacing(true);
        chipsRow.setPadding(false);
        chipsRow.getStyle().set("flex-wrap", "wrap");

        input.setPlaceholder(I18n.t("calendar.participants.input.placeholder"));
        input.setWidthFull();
        input.setClearButtonVisible(true);
        input.addKeyDownListener(Key.ENTER, e -> addFromInput());
        input.addBlurListener(e -> addFromInput());

        addButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        addButton.setTooltipText(I18n.t("calendar.participants.add.tooltip"));
        addButton.addClickListener(e -> addFromInput());

        HorizontalLayout inputRow = new HorizontalLayout(input, addButton);
        inputRow.setWidthFull();
        inputRow.setPadding(false);
        inputRow.setSpacing(true);
        inputRow.setAlignItems(FlexComponent.Alignment.END);
        inputRow.expand(input);

        Span emptyMsg = new Span(I18n.t("calendar.participants.empty.hint"));
        emptyMsg.getStyle().set("color", "var(--lumo-secondary-text-color)");
        emptyMsg.getStyle().set("font-size", "var(--lumo-font-size-s)");
        emptyHint.add(emptyMsg);
        emptyHint.setPadding(false);
        emptyHint.setSpacing(false);

        VerticalLayout root = new VerticalLayout(chipsRow, emptyHint, inputRow);
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();
        add(root);

        updateEmptyHint();
    }

    /* ══════════════════════════════════════════════════════════════
     * CustomField plumbing
     * ══════════════════════════════════════════════════════════════ */

    @Override
    protected List<String> generateModelValue() {
        return new ArrayList<>(values);
    }

    @Override
    protected void setPresentationValue(List<String> value) {
        values.clear();
        if (value != null) {
            for (String s : value) {
                if (s != null && !s.isBlank()) {
                    String t = s.trim();
                    if (!values.contains(t)) values.add(t);
                }
            }
        }
        refreshChips();
    }

    @Override
    public List<String> getEmptyValue() {
        return Collections.emptyList();
    }

    /* ══════════════════════════════════════════════════════════════
     * Internals
     * ══════════════════════════════════════════════════════════════ */

    private void addFromInput() {
        String raw = input.getValue();
        if (raw == null || raw.isBlank()) return;

        Set<String> tokens = new LinkedHashSet<>();
        for (String token : raw.split("[,\\n]")) {
            String v = token.trim();
            if (!v.isEmpty()) tokens.add(v);
        }
        if (tokens.isEmpty()) return;

        boolean changed = false;
        for (String t : tokens) {
            if (!values.contains(t)) {
                values.add(t);
                changed = true;
            }
        }
        input.clear();
        if (changed) {
            refreshChips();
            updateValue();
        }
    }

    private void remove(String v) {
        if (values.remove(v)) {
            refreshChips();
            updateValue();
        }
    }

    private void refreshChips() {
        chipsRow.removeAll();
        for (String v : values) {
            chipsRow.add(buildChip(v));
        }
        updateEmptyHint();
    }

    private void updateEmptyHint() {
        emptyHint.setVisible(values.isEmpty());
    }

    private Span buildChip(String value) {
        Span chip = new Span();
        chip.getStyle()
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("gap", "0.35rem")
                .set("padding", "0.2rem 0.5rem")
                .set("border-radius", "999px")
                .set("background", "var(--lumo-primary-color-10pct)")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Span text = new Span(value);

        Icon remove = VaadinIcon.CLOSE_SMALL.create();
        remove.getStyle().set("cursor", "pointer");
        remove.addClickListener(e -> remove(value));

        chip.add(text, remove);
        return chip;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        input.setReadOnly(readOnly);
        addButton.setEnabled(!readOnly);
    }
}