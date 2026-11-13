package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.VaadinSession;
import eu.isygoit.i18n.I18n;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Cards / Table switch of the management views, optionally with more modes (e.g. a Kanban board) added with
 * {@link #addMode}. The chosen mode is kept per view in the Vaadin session.
 *
 * <p>The first callback only tells whether the table is shown. An extra mode has its own callback, called with
 * {@code true} when it becomes the mode and {@code false} when another one replaces it; while it is active the
 * table flag is {@code false}.
 */
public final class ManagementViewModeSwitch extends Div {

    private static final String CARDS = "cards";
    private static final String TABLE = "table";

    private final String sessionKey;
    private final Consumer<Boolean> onModeChanged;
    private final Map<String, Button> buttons = new LinkedHashMap<>();
    private final Map<String, Consumer<Boolean>> extraModes = new LinkedHashMap<>();
    private String mode;

    public ManagementViewModeSwitch(String viewId, Consumer<Boolean> onModeChanged) {
        this.sessionKey = "view-mode:" + viewId;
        this.onModeChanged = onModeChanged;
        this.mode = readMode();

        addClassName("view-mode-switch");
        addButton(CARDS, VaadinIcon.GRID_BIG_O, "common.view.cards");
        addButton(TABLE, VaadinIcon.LIST, "common.view.table");
        updateButtons();
    }

    /**
     * Adds a mode after Cards and Table. If it was the mode last used in this session it is active right away; its
     * callback is called with {@code true} in that case, so the view can show it.
     */
    public void addMode(String id, VaadinIcon icon, String labelKey, Consumer<Boolean> onToggle) {
        extraModes.put(id, onToggle);
        addButton(id, icon, labelKey);
        if (id.equals(mode)) {
            onToggle.accept(true);
        } else if (!buttons.containsKey(mode)) {
            mode = CARDS;
        }
        updateButtons();
    }

    public boolean isTableMode() {
        return TABLE.equals(mode);
    }

    /** True when the extra mode {@code id} is the active one. */
    public boolean isMode(String id) {
        return id.equals(mode);
    }

    private void setMode(String newMode) {
        if (mode.equals(newMode)) {
            return;
        }
        String previous = mode;
        mode = newMode;
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(sessionKey, newMode);
        }
        updateButtons();
        Consumer<Boolean> left = extraModes.get(previous);
        if (left != null) {
            left.accept(false);
        }
        Consumer<Boolean> entered = extraModes.get(newMode);
        if (entered != null) {
            entered.accept(true);
        }
        if (TABLE.equals(previous) != TABLE.equals(newMode)) {
            onModeChanged.accept(TABLE.equals(newMode));
        }
    }

    private String readMode() {
        VaadinSession session = VaadinSession.getCurrent();
        Object stored = session == null ? null : session.getAttribute(sessionKey);
        return stored == null ? CARDS : stored.toString();
    }

    private void addButton(String id, VaadinIcon icon, String labelKey) {
        String label = I18n.t(labelKey);
        Button button = new Button(icon.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        button.addClassName("view-mode-switch__button");
        button.setTooltipText(label);
        button.setAriaLabel(label);
        button.addClickListener(event -> setMode(id));
        buttons.put(id, button);
        add(button);
    }

    private void updateButtons() {
        buttons.forEach((id, button) -> {
            boolean active = id.equals(mode);
            button.getElement().setAttribute("aria-pressed", String.valueOf(active));
            button.setClassName("view-mode-switch__button--active", active);
        });
    }
}
