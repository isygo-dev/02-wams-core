package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.List;

/**
 * Compact "favorites/shortcuts" bar shown at the top of every module
 * dashboard: a handful of one-click shortcuts to the module's most common
 * actions (create X, jump to Y). Shared by every module so the pattern looks
 * and behaves identically everywhere — see {@code styles/card.css} for the
 * {@code wams-dashboard-shortcuts}/{@code wams-dashboard-shortcut} rules.
 */
public class DashboardShortcutsBar extends HorizontalLayout {

    public record Shortcut(VaadinIcon icon, String label, Runnable action) {
    }

    public DashboardShortcutsBar(String title, List<Shortcut> shortcuts) {
        addClassName("wams-dashboard-shortcuts");
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);

        if (title != null && !title.isBlank()) {
            var label = new com.vaadin.flow.component.html.Span(title);
            label.addClassName("wams-dashboard-shortcuts__title");
            add(label);
        }

        HorizontalLayout items = new HorizontalLayout();
        items.addClassName("wams-dashboard-shortcuts__items");
        items.setSpacing(true);

        for (Shortcut shortcut : shortcuts) {
            Button button = new Button(shortcut.label(), shortcut.icon().create());
            button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            button.addClassName("wams-dashboard-shortcut");
            button.addClickListener(e -> shortcut.action().run());
            items.add(button);
        }

        add(items);
        expand(items);
    }
}
