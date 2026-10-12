package eu.isygoit.ui.common.filters;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tri-state boolean toggle switch.
 *
 * <ul>
 *   <li>{@code null}          — no filter (grey pill, thumb centred)</li>
 *   <li>{@code Boolean.TRUE}  — filter for {@code true}  (green pill, thumb right)</li>
 *   <li>{@code Boolean.FALSE} — filter for {@code false} (red pill,   thumb left)</li>
 * </ul>
 *
 * <p>A click cycles {@code unset → true → false → unset}. {@code Space}
 * and {@code Enter} do the same.</p>
 *
 * <p>This component carries no styling of its own — every visual aspect
 * lives in {@code components/_filter.scss} under {@code .toggle-switch*}.
 * The class names emitted here are the entire contract between the two:</p>
 *
 * <pre>
 * .toggle-switch
 *   .toggle-switch__label      ← the text on the left
 *   .toggle-switch__track      ← the pill background
 *     .toggle-switch__thumb    ← the sliding knob
 *
 * state modifiers applied on the root:
 *   .toggle-switch--unset
 *   .toggle-switch--on
 *   .toggle-switch--off
 * </pre>
 */
public class ToggleSwitch extends Div {

    /** {@code null} = unset (no filter). */
    private Boolean value;

    private Consumer<Boolean> onChange;

    private final Span labelSpan = new Span();
    private final Span track = new Span();
    private final Span thumb = new Span();

    public ToggleSwitch(String label) {
        addClassName("toggle-switch");

        labelSpan.setText(label);
        labelSpan.addClassName("toggle-switch__label");

        track.addClassName("toggle-switch__track");
        thumb.addClassName("toggle-switch__thumb");
        track.add(thumb);

        add(labelSpan, track);

        // Accessibility: behaves like a switch, is keyboard-focusable.
        getElement().setAttribute("role", "switch");
        getElement().setAttribute("tabindex", "0");
        getElement().setAttribute("aria-label", label);

        // Mouse / touch
        addClickListener(e -> cycle());

        // Keyboard: Space and Enter flip the switch
        getElement().addEventListener("keydown", e -> {
            String key = e.getEventData().getString("event.key");
            if (" ".equals(key) || "Enter".equals(key)) {
                cycle();
            }
        }).addEventData("event.key");

        updateVisual();
    }

    // ══════════════════════════════════════════════════════════════
    //  Public API
    // ══════════════════════════════════════════════════════════════

    public Boolean getValue() { return value; }

    public void setValue(Boolean v) {
        if (Objects.equals(this.value, v)) return;
        this.value = v;
        updateVisual();
        if (onChange != null) onChange.accept(v);
    }

    /** Resets to the "no filter" state without firing {@code onChange}. */
    public void clear() {
        this.value = null;
        updateVisual();
    }

    public void setOnChange(Consumer<Boolean> onChange) {
        this.onChange = onChange;
    }

    // ══════════════════════════════════════════════════════════════
    //  Internal
    // ══════════════════════════════════════════════════════════════

    private void cycle() {
        if (value == null)                    value = Boolean.TRUE;
        else if (Boolean.TRUE.equals(value))  value = Boolean.FALSE;
        else                                  value = null;

        updateVisual();
        if (onChange != null) onChange.accept(value);
    }

    private void updateVisual() {
        // ARIA
        getElement().setAttribute("aria-checked",
                String.valueOf(Boolean.TRUE.equals(value)));

        // State modifier classes — all styling hangs off these in SCSS.
        getElement().getClassList().set("toggle-switch--on",    Boolean.TRUE.equals(value));
        getElement().getClassList().set("toggle-switch--off",   Boolean.FALSE.equals(value));
        getElement().getClassList().set("toggle-switch--unset", value == null);
    }
}