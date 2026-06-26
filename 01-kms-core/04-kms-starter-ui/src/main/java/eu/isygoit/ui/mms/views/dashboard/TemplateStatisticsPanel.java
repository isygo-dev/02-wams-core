package eu.isygoit.ui.mms.views.dashboard;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import org.springframework.stereotype.Component;

@Component
@UIScope
public class TemplateStatisticsPanel extends VerticalLayout {

    public TemplateStatisticsPanel() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        addClassName("template-stats-panel");
        getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("padding", "var(--lumo-space-l)");

        H3 title = new H3(I18n.t("mms.dashboard.template.statistics"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        title.addClassName(LumoUtility.Margin.Bottom.MEDIUM);
        add(title);

        // Stats grid
        Div statsGrid = createStatsGrid();
        add(statsGrid);

        // Template usage list
        Div usageList = createUsageList();
        add(usageList);
    }

    private Div createStatsGrid() {
        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(150px, 1fr))")
                .set("gap", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-l)");

        grid.add(createStatCard("Total Templates", "120", VaadinIcon.FILE_TEXT, "var(--lumo-primary-color)"));
        grid.add(createStatCard("Active", "85", VaadinIcon.CHECK_CIRCLE, "var(--lumo-success-color)"));
        grid.add(createStatCard("Draft", "25", VaadinIcon.PENCIL, "var(--lumo-warning-color)"));
        grid.add(createStatCard("Archived", "10", VaadinIcon.ARCHIVE, "var(--lumo-secondary-text-color)"));

        return grid;
    }

    private Div createStatCard(String label, String value, VaadinIcon icon, String color) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("text-align", "center")
                .set("transition", "all 0.3s ease")
                .set("border", "1px solid var(--lumo-contrast-10pct)");

        // Icon
        Icon iconComponent = icon.create();
        iconComponent.setSize("24px");
        iconComponent.setColor(color);
        iconComponent.getStyle()
                .set("display", "block")
                .set("margin", "0 auto var(--lumo-space-xs)");

        // Value
        Paragraph valueText = new Paragraph(value);
        valueText.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "700")
                .set("margin", "0")
                .set("color", "var(--lumo-header-text-color)");

        // Label
        Paragraph labelText = new Paragraph(label);
        labelText.addClassName(LumoUtility.TextColor.SECONDARY);
        labelText.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("margin", "var(--lumo-space-xs) 0 0");

        card.add(iconComponent, valueText, labelText);
        return card;
    }

    private Div createUsageList() {
        Div container = new Div();
        container.getStyle()
                .set("margin-top", "var(--lumo-space-m)");

        // Header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(false);
        header.setSpacing(true);
        header.getStyle()
                .set("border-bottom", "2px solid var(--lumo-contrast-20pct)")
                .set("padding-bottom", "var(--lumo-space-xs)")
                .set("margin-bottom", "var(--lumo-space-s)");

        Paragraph nameHeader = new Paragraph(I18n.t("mms.dashboard.template.name"));
        nameHeader.addClassName(LumoUtility.FontWeight.BOLD);
        nameHeader.addClassName(LumoUtility.FontSize.XSMALL);
        nameHeader.getStyle().set("flex", "1");

        Paragraph usageHeader = new Paragraph(I18n.t("mms.dashboard.template.usage"));
        usageHeader.addClassName(LumoUtility.FontWeight.BOLD);
        usageHeader.addClassName(LumoUtility.FontSize.XSMALL);
        usageHeader.getStyle().set("width", "80px").set("text-align", "right");

        header.add(nameHeader, usageHeader);
        container.add(header);

        // Template items with usage bars
        container.add(createUsageItem("Welcome Email", 45, "var(--lumo-primary-color)"));
        container.add(createUsageItem("Password Reset", 30, "var(--lumo-success-color)"));
        container.add(createUsageItem("Newsletter", 15, "var(--lumo-warning-color)"));
        container.add(createUsageItem("Invoice", 8, "var(--lumo-error-color)"));
        container.add(createUsageItem("Other", 2, "var(--lumo-secondary-text-color)"));

        return container;
    }

    private HorizontalLayout createUsageItem(String name, int usage, String color) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setPadding(false);
        row.setSpacing(true);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.getStyle()
                .set("padding", "var(--lumo-space-xs) 0")
                .set("border-bottom", "1px solid var(--lumo-contrast-5pct)");

        // Name
        Paragraph nameText = new Paragraph(name);
        nameText.addClassName(LumoUtility.FontSize.SMALL);
        nameText.getStyle()
                .set("flex", "1")
                .set("margin", "0");

        // Usage bar container
        Div barContainer = new Div();
        barContainer.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "var(--lumo-space-s)")
                .set("width", "120px");

        // Progress bar (custom)
        Div bar = new Div();
        bar.getStyle()
                .set("height", "6px")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("flex", "1")
                .set("overflow", "hidden");

        Div fill = new Div();
        fill.getStyle()
                .set("height", "100%")
                .set("width", usage + "%")
                .set("background", color)
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("transition", "width 0.6s ease");
        bar.add(fill);

        // Percentage
        Paragraph percentText = new Paragraph(usage + "%");
        percentText.addClassName(LumoUtility.FontSize.XSMALL);
        percentText.getStyle()
                .set("color", color)
                .set("font-weight", "600")
                .set("margin", "0")
                .set("min-width", "40px")
                .set("text-align", "right");

        barContainer.add(bar, percentText);
        row.add(nameText, barContainer);

        return row;
    }
}