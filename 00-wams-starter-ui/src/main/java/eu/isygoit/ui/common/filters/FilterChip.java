package eu.isygoit.ui.common.filters;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * A removable chip representing one active filter.
 * <p>
 * Renders as {@code Label: Value ✕}. The chip is a Jira-style lozenge —
 * primary-tinted background, compact typography, and a small ✕ button that
 * highlights on hover. All visuals live in {@code .filter-chip*} in the
 * stylesheet.
 */
public class FilterChip extends Div {

    public FilterChip(String label, String value, Runnable onRemove) {
        addClassName("filter-chip");
        getElement().setAttribute("title", label + ": " + value);

        Span labelEl = new Span(label);
        labelEl.addClassName("filter-chip__label");

        Span valueEl = new Span(value);
        valueEl.addClassName("filter-chip__value");

        Icon remove = VaadinIcon.CLOSE_SMALL.create();
        remove.addClassName("filter-chip__remove");
        remove.getElement().setAttribute("role", "button");
        remove.getElement().setAttribute("aria-label", "Remove filter");
        remove.getElement().addEventListener("click", e -> onRemove.run());

        add(labelEl, valueEl, remove);
    }
}