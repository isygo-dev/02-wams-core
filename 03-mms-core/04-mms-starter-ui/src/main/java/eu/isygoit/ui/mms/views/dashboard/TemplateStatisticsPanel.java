package eu.isygoit.ui.mms.views.dashboard;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
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
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("template-stats-panel");

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.addClassName("wams-panel-container");

        // Compact Header
        HorizontalLayout header = createCompactHeader();
        mainContainer.add(header);

        // Mini Stats - 4 compact cards
        Div statsGrid = createMiniStats();
        mainContainer.add(statsGrid);

        // Template usage with mini progress bars
        Div usageSection = createCompactUsage();
        mainContainer.add(usageSection);

        add(mainContainer);
    }

    private HorizontalLayout createCompactHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.addClassName("wams-panel-header");

        H3 title = new H3(I18n.t("mms.dashboard.template.statistics"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        title.addClassName(LumoUtility.Margin.NONE);
        title.addClassName("wams-panel-title");

        Span badge = new Span(I18n.t("mms.dashboard.template.stats.live"));
        badge.addClassName("wams-panel-live-badge");

        header.add(title, badge);
        return header;
    }

    private Div createMiniStats() {
        Div grid = new Div();
        grid.addClassName("wams-mini-stats-grid");

        grid.add(createMiniStatCard(I18n.t("mms.dashboard.template.stats.total"), "120", VaadinIcon.FILE_TEXT, "#4F46E5"));
        grid.add(createMiniStatCard(I18n.t("mms.dashboard.template.stats.active"), "85", VaadinIcon.CHECK_CIRCLE, "#10B981"));
        grid.add(createMiniStatCard(I18n.t("mms.dashboard.template.stats.draft"), "25", VaadinIcon.PENCIL, "#F59E0B"));
        grid.add(createMiniStatCard(I18n.t("mms.dashboard.template.stats.archived"), "10", VaadinIcon.ARCHIVE, "#6B7280"));

        return grid;
    }

    private Div createMiniStatCard(String label, String value, VaadinIcon icon, String color) {
        Div card = new Div();
        card.addClassName("wams-mini-stat-card");
        card.addClassName("template-stat-card");

        Icon iconComponent = icon.create();
        iconComponent.setSize("18px");
        iconComponent.setColor(color);
        iconComponent.addClassName("template-stat-icon");

        Span valueSpan = new Span(value);
        valueSpan.addClassName("wams-mini-stat-value");

        Span labelSpan = new Span(label);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName(LumoUtility.TextColor.SECONDARY);

        card.add(iconComponent, valueSpan, labelSpan);
        return card;
    }

    private Div createCompactUsage() {
        Div container = new Div();
        container.addClassName("wams-panel-section");

        // Header with title
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName("wams-panel-section-header");

        Span usageLabel = new Span(I18n.t("mms.dashboard.template.stats.top.templates"));
        usageLabel.addClassName(LumoUtility.FontSize.XSMALL);
        usageLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        Span viewAll = new Span(I18n.t("mms.dashboard.viewAll"));
        viewAll.addClassName(LumoUtility.FontSize.XSMALL);
        viewAll.addClassName(LumoUtility.TextColor.PRIMARY);
        viewAll.addClassName("wams-panel-viewall-link");

        header.add(usageLabel, viewAll);
        container.add(header);

        // Usage items with mini progress bars
        container.add(createUsageItem(I18n.t("mms.dashboard.template.stats.sample.welcome"), 45, "#4F46E5"));
        container.add(createUsageItem(I18n.t("mms.dashboard.template.stats.sample.password.reset"), 30, "#10B981"));
        container.add(createUsageItem(I18n.t("mms.dashboard.template.stats.sample.newsletter"), 15, "#F59E0B"));
        container.add(createUsageItem(I18n.t("mms.dashboard.template.stats.sample.invoice"), 8, "#EF4444"));

        return container;
    }

    private HorizontalLayout createUsageItem(String name, int usage, String color) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setPadding(false);
        row.setSpacing(true);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.addClassName("wams-usage-item");

        // Name with icon
        HorizontalLayout nameLayout = new HorizontalLayout();
        nameLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        nameLayout.setSpacing(true);
        nameLayout.addClassName("wams-usage-name-layout");

        Icon fileIcon = VaadinIcon.FILE_O.create();
        fileIcon.setSize("14px");
        fileIcon.setColor(color);

        Span nameSpan = new Span(name);
        nameSpan.addClassName(LumoUtility.FontSize.SMALL);
        nameSpan.addClassName("wams-usage-name");

        nameLayout.add(fileIcon, nameSpan);

        // Progress bar
        Div barContainer = new Div();
        barContainer.addClassName("wams-usage-bar-container");

        Div bar = new Div();
        bar.addClassName("wams-usage-bar-track");

        Div fill = new Div();
        fill.addClassName("wams-usage-bar-fill");
        fill.getStyle()
                .set("width", usage + "%")
                .set("--wams-usage-color", color);
        bar.add(fill);
        barContainer.add(bar);

        // Percentage
        Span percentSpan = new Span(usage + "%");
        percentSpan.addClassName(LumoUtility.FontSize.XSMALL);
        percentSpan.addClassName("wams-usage-percent");
        percentSpan.getStyle().set("--wams-usage-color", color);

        row.add(nameLayout, barContainer, percentSpan);
        row.expand(nameLayout);
        return row;
    }
}
