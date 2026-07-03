package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class StatCard extends VerticalLayout {

    public StatCard(String label, String value, VaadinIcon icon, String color, String tooltip) {
        setSpacing(false);
        setPadding(true);
        setWidthFull();
        addClassName("wams-stat-card");
        getElement().setAttribute("title", tooltip);

        Icon iconElement = icon.create();
        iconElement.setSize("32px");
        iconElement.addClassName("wams-stat-card__icon");
        // Per-instance icon color: driven by data (module/status specific), kept inline on purpose.
        iconElement.getStyle().set("color", color);

        Span valueSpan = new Span(value);
        valueSpan.addClassName("wams-stat-card__value");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("wams-stat-card__label");

        add(iconElement, valueSpan, labelSpan);
        setAlignItems(FlexComponent.Alignment.CENTER);
    }
}