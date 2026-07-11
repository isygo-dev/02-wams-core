package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Canonical dashboard stat tile, used by every module's dashboard/statistics
 * panel: a circular colored icon, a bold metric value, an uppercase label,
 * and an optional colored up/down change indicator. A thin accent bar along
 * the top edge signals the metric's {@link Variant}. See {@code styles/card.css}
 * for the full {@code wams-stat-card} rule set (hover-lift, skeleton-loading,
 * responsive grid via {@link StatCardGrid}).
 */
public class StatCard extends VerticalLayout {

    private final Div iconCircle;
    private final Span valueSpan;
    private final Span changeSpan;
    private final Icon changeArrow;
    private final Span changeText;

    public StatCard(VaadinIcon icon, Variant variant, String label, String value) {
        this(icon, variant, label, value, null);
    }

    public StatCard(VaadinIcon icon, Variant variant, String label, String value, String tooltip) {
        setSpacing(false);
        setPadding(true);
        setWidthFull();
        addClassName("wams-stat-card");
        addClassName("wams-stat-card--" + variant.name().toLowerCase());
        if (tooltip != null && !tooltip.isBlank()) {
            getElement().setAttribute("title", tooltip);
        }

        iconCircle = new Div();
        iconCircle.addClassName("wams-stat-card__icon-circle");
        Icon iconElement = icon.create();
        iconElement.addClassName("wams-stat-card__icon");
        iconCircle.add(iconElement);

        changeArrow = VaadinIcon.ARROW_UP.create();
        changeArrow.addClassName("wams-stat-card__change-arrow");
        changeText = new Span();
        changeSpan = new Span(changeArrow, changeText);
        changeSpan.addClassName("wams-stat-card__change");
        changeSpan.setVisible(false);

        HorizontalLayout headerRow = new HorizontalLayout(iconCircle, changeSpan);
        headerRow.setWidthFull();
        headerRow.setSpacing(true);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerRow.addClassName("wams-stat-card__header");

        valueSpan = new Span();
        valueSpan.addClassName("wams-stat-card__value");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("wams-stat-card__label");

        add(headerRow, valueSpan, labelSpan);

        setValue(value);
    }

    /**
     * Adds a colored "+N%"/"-N" style change indicator with a directional
     * arrow. Returns {@code this} for fluent construction, e.g.
     * {@code new StatCard(...).withChange("+12%", Trend.UP)}.
     */
    public StatCard withChange(String changeLabel, Trend trend) {
        setChange(changeLabel, trend);
        return this;
    }

    /**
     * Makes the whole card clickable (e.g. navigate to the entity's list
     * view). Returns {@code this} for fluent construction.
     */
    public StatCard withNavigation(Runnable onClick) {
        addClassName("wams-stat-card--clickable");
        addClickListener(e -> onClick.run());
        return this;
    }

    /**
     * Updates the metric value. Passing {@code null}/blank puts the card in
     * its skeleton-loading state (used while an async count is in flight).
     */
    public void setValue(String value) {
        boolean loading = value == null || value.isBlank();
        valueSpan.setText(loading ? "" : value);
        valueSpan.getElement().getClassList().set("wams-skeleton", loading);
    }

    /**
     * Updates or hides the change indicator. Pass {@code null}/{@link Trend#NONE}
     * to hide it entirely (e.g. for a metric with no meaningful trend).
     */
    public void setChange(String changeLabel, Trend trend) {
        if (changeLabel == null || changeLabel.isBlank() || trend == null || trend == Trend.NONE) {
            changeSpan.setVisible(false);
            return;
        }
        changeSpan.setVisible(true);
        changeSpan.removeClassName("wams-stat-card__change--up");
        changeSpan.removeClassName("wams-stat-card__change--down");
        changeSpan.addClassName(trend == Trend.UP ? "wams-stat-card__change--up" : "wams-stat-card__change--down");
        changeArrow.getElement().removeAttribute("icon");
        changeArrow.getElement().setAttribute("icon",
                trend == Trend.UP ? "vaadin:arrow-up" : "vaadin:arrow-down");
        changeText.setText(changeLabel);
    }

    /**
     * Semantic color coding, mapped to Lumo theme colors.
     */
    public enum Variant {
        PRIMARY, SUCCESS, WARNING, NEUTRAL
    }

    /**
     * Direction of the optional change indicator (e.g. "+12%", "-3").
     */
    public enum Trend {
        UP, DOWN, NONE
    }
}
