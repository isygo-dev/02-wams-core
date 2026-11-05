package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.VaadinSession;
import eu.isygoit.i18n.I18n;

import java.util.function.Consumer;

public final class ManagementViewModeSwitch extends Div {

    private final String sessionKey;
    private final Consumer<Boolean> onModeChanged;
    private final Button cardsButton;
    private final Button tableButton;
    private boolean tableMode;

    public ManagementViewModeSwitch(String viewId, Consumer<Boolean> onModeChanged) {
        this.sessionKey = "view-mode:" + viewId;
        this.onModeChanged = onModeChanged;
        this.tableMode = "table".equals(readMode());

        cardsButton = createButton(VaadinIcon.GRID_BIG_O, "common.view.cards", false);
        tableButton = createButton(VaadinIcon.LIST, "common.view.table", true);
        addClassName("view-mode-switch");
        add(cardsButton, tableButton);
        updateButtons();
    }

    public boolean isTableMode() {
        return tableMode;
    }

    private String readMode() {
        VaadinSession session = VaadinSession.getCurrent();
        Object mode = session == null ? null : session.getAttribute(sessionKey);
        return mode == null ? "cards" : mode.toString();
    }

    private Button createButton(VaadinIcon icon, String labelKey, boolean table) {
        String label = I18n.t(labelKey);
        Button button = new Button(icon.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        button.addClassName("view-mode-switch__button");
        button.setTooltipText(label);
        button.setAriaLabel(label);
        button.addClickListener(event -> setTableMode(table));
        return button;
    }

    private void setTableMode(boolean table) {
        if (tableMode == table) {
            return;
        }
        tableMode = table;
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(sessionKey, table ? "table" : "cards");
        }
        updateButtons();
        onModeChanged.accept(table);
    }

    private void updateButtons() {
        cardsButton.getElement().setAttribute("aria-pressed", String.valueOf(!tableMode));
        tableButton.getElement().setAttribute("aria-pressed", String.valueOf(tableMode));
        cardsButton.setClassName("view-mode-switch__button--active", !tableMode);
        tableButton.setClassName("view-mode-switch__button--active", tableMode);
    }
}
