package eu.isygoit.ui.views.kms.dashbord;

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
        getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("align-items", "center")
                .set("flex", "1 1 180px")
                .set("background-color", "var(--lumo-base-color)")
                .set("text-align", "center")
                .set("transition", "all 0.2s ease");
        addClassName("stat-card");
        getElement().setAttribute("title", tooltip);

        Icon iconElement = icon.create();
        iconElement.setSize("32px");
        iconElement.getStyle().set("color", color);

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "bold")
                .set("margin-top", "8px");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        add(iconElement, valueSpan, labelSpan);
        setAlignItems(FlexComponent.Alignment.CENTER);
    }
}