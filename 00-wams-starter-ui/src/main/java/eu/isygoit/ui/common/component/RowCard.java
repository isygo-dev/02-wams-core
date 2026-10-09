package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Compact horizontal card used instead of a table row, so that lists stay readable on
 * small screens: a title with an optional subtitle, a wrapping row of tags and facts, a few
 * emphasised figures and icon-only actions. On a narrow screen the figures and actions move
 * under the text. Styles live in {@code styles/scss/components/_row-card.scss}.
 *
 * <pre>{@code
 * RowCard.create()
 *     .title(item.getName())
 *     .tag(EnumTag.of(item.getStatus(), "status"))
 *     .fact("Due", date)
 *     .figure("Planned", amount)
 *     .action(VaadinIcon.EDIT, "Edit", () -> edit(item))
 *     .dangerAction(VaadinIcon.TRASH, "Delete", () -> delete(item));
 * }</pre>
 */
public class RowCard extends Div {

    private final Div lead = new Div();
    private final Div main = new Div();
    private final Span title = new Span();
    private final Span subtitle = new Span();
    private final Div meta = new Div();
    private final Div side = new Div();
    private final Div figures = new Div();
    private final Div actions = new Div();

    public RowCard() {
        addClassName("wams-row-card");
        getElement().setAttribute("role", "listitem");

        lead.addClassName("wams-row-card__lead");
        title.addClassName("wams-row-card__title");
        subtitle.addClassName("wams-row-card__subtitle");
        meta.addClassName("wams-row-card__meta");
        main.addClassName("wams-row-card__main");
        figures.addClassName("wams-row-card__figures");
        actions.addClassName("wams-row-card__actions");
        side.addClassName("wams-row-card__side");

        main.add(title);
        add(main);
        addClassName("wams-row-card--no-lead");
    }

    public static RowCard create() {
        return new RowCard();
    }

    /** Leading visual (icon, avatar, status dot). */
    public RowCard lead(Component component) {
        if (lead.getParent().isEmpty()) {
            getElement().insertChild(0, lead.getElement());
            removeClassName("wams-row-card--no-lead");
        }
        lead.add(component);
        return this;
    }

    public RowCard leadIcon(VaadinIcon icon) {
        Icon leadIcon = icon.create();
        leadIcon.addClassName("wams-row-card__lead-icon");
        return lead(leadIcon);
    }

    public RowCard title(String text) {
        title.setText(text == null || text.isBlank() ? "-" : text);
        return this;
    }

    public RowCard subtitle(String text) {
        if (text != null && !text.isBlank()) {
            subtitle.setText(text);
            if (subtitle.getParent().isEmpty()) {
                main.getElement().insertChild(1, subtitle.getElement());
            }
        }
        return this;
    }

    /** A tag (usually an enum tag) shown in the wrapping meta row. */
    public RowCard tag(Component tag) {
        if (tag != null) {
            addMeta(tag);
        }
        return this;
    }

    /** A small "label value" pair shown in the wrapping meta row; blank values are skipped. */
    public RowCard fact(String label, String value) {
        if (value == null || value.isBlank()) {
            return this;
        }
        Span fact = new Span();
        fact.addClassName("wams-row-card__fact");
        Span factLabel = new Span(label);
        factLabel.addClassName("wams-row-card__fact-label");
        Span factValue = new Span(value);
        factValue.addClassName("wams-row-card__fact-value");
        fact.add(factLabel, factValue);
        addMeta(fact);
        return this;
    }

    /** An emphasised figure (for example an amount) shown on the right, label above value. */
    public RowCard figure(String label, String value) {
        Div figure = new Div();
        figure.addClassName("wams-row-card__figure");
        Span figureLabel = new Span(label);
        figureLabel.addClassName("wams-row-card__figure-label");
        Span figureValue = new Span(value == null || value.isBlank() ? "-" : value);
        figureValue.addClassName("wams-row-card__figure-value");
        figure.add(figureLabel, figureValue);
        figures.add(figure);
        ensureSide();
        if (figures.getParent().isEmpty()) {
            side.getElement().insertChild(0, figures.getElement());
        }
        return this;
    }

    /** Any control shown on the right (for example a checkbox). */
    public RowCard control(Component control) {
        if (control != null) {
            ensureSide();
            Div wrapper = new Div(control);
            wrapper.addClassName("wams-row-card__control");
            side.add(wrapper);
        }
        return this;
    }

    /** Icon-only action. {@code label} is both the tooltip and the accessible name. */
    public RowCard action(VaadinIcon icon, String label, Runnable handler) {
        return addAction(icon, label, handler, false);
    }

    /** Destructive icon-only action (red). */
    public RowCard dangerAction(VaadinIcon icon, String label, Runnable handler) {
        return addAction(icon, label, handler, true);
    }

    private RowCard addAction(VaadinIcon icon, String label, Runnable handler, boolean danger) {
        Button button = new Button(icon.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        if (danger) {
            button.addThemeVariants(ButtonVariant.LUMO_ERROR);
        }
        button.setAriaLabel(label);
        button.setTooltipText(label);
        button.addClickListener(event -> handler.run());
        // A click on an action must not also activate the whole card.
        button.getElement().executeJs("this.addEventListener('click', e => e.stopPropagation())");
        actions.add(button);
        ensureSide();
        if (actions.getParent().isEmpty()) {
            side.add(actions);
        }
        return this;
    }

    /** Makes the whole card activatable (click, Enter or Space). */
    public RowCard onClick(Runnable handler) {
        addClassName("wams-row-card--clickable");
        getElement().setAttribute("tabindex", "0");
        addClickListener(event -> handler.run());
        getElement().addEventListener("keydown", event -> handler.run())
                .setFilter("event.key === 'Enter' || event.key === ' '");
        return this;
    }

    private void addMeta(Component component) {
        meta.add(component);
        if (meta.getParent().isEmpty()) {
            main.add(meta);
        }
    }

    private void ensureSide() {
        if (side.getParent().isEmpty()) {
            add(side);
        }
    }
}
