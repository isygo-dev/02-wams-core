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
public class EmailStatisticsPanel extends VerticalLayout {

    public EmailStatisticsPanel() {
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("email-stats-panel");

        // Main container with glass effect
        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.addClassName("wams-panel-container");

        // Compact Header
        HorizontalLayout header = createCompactHeader();
        mainContainer.add(header);

        // Mini Stats Grid - 2x2 compact
        Div statsGrid = createMiniStats();
        mainContainer.add(statsGrid);

        // Sparkline style monthly trend
        Div trendSection = createTrendSection();
        mainContainer.add(trendSection);

        // Compact monthly table
        Div monthlyCompact = createMonthlyCompact();
        mainContainer.add(monthlyCompact);

        add(mainContainer);
    }

    private HorizontalLayout createCompactHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.addClassName("wams-panel-header");

        H3 title = new H3(I18n.t("mms.dashboard.email.statistics"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        title.addClassName(LumoUtility.Margin.NONE);
        title.addClassName("wams-panel-title");

        Span period = new Span(I18n.t("mms.dashboard.email.stats.period"));
        period.addClassName(LumoUtility.FontSize.XSMALL);
        period.addClassName(LumoUtility.TextColor.SECONDARY);
        period.addClassName("wams-panel-period-badge");

        HorizontalLayout rightGroup = new HorizontalLayout(period);
        rightGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        rightGroup.setSpacing(true);

        header.add(title, rightGroup);
        return header;
    }

    private Div createMiniStats() {
        Div grid = new Div();
        grid.addClassName("wams-mini-stats-grid");

        grid.add(createMiniStatCard(I18n.t("mms.dashboard.email.stats.sent"), "2.8K", VaadinIcon.ENVELOPE_O, "#4F46E5", "+12%"));
        grid.add(createMiniStatCard(I18n.t("mms.dashboard.email.stats.opened"), "1.9K", VaadinIcon.EYE, "#10B981", "+8%"));
        grid.add(createMiniStatCard(I18n.t("mms.dashboard.email.stats.bounced"), "124", VaadinIcon.EXCLAMATION_CIRCLE_O, "#EF4444", "-3%"));
        grid.add(createMiniStatCard(I18n.t("mms.dashboard.email.stats.rate"), "67.6%", VaadinIcon.TRENDING_UP, "#F59E0B", "+5%"));

        return grid;
    }

    private Div createMiniStatCard(String label, String value, VaadinIcon icon, String color, String change) {
        Div card = new Div();
        card.addClassName("wams-mini-stat-card");
        card.addClassName("stat-card");

        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();

        // Icon with circle background
        Div iconWrapper = new Div();
        iconWrapper.addClassName("wams-mini-stat-icon-wrapper");
        iconWrapper.getStyle().set("--wams-mini-stat-color", color + "20");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.setColor(color);
        iconWrapper.add(iconComponent);

        // Value and label
        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);
        info.addClassName("wams-mini-stat-info");

        Span valueSpan = new Span(value);
        valueSpan.addClassName("wams-mini-stat-value");

        Span labelSpan = new Span(label);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName(LumoUtility.TextColor.SECONDARY);

        info.add(valueSpan, labelSpan);

        // Change indicator
        Span changeSpan = new Span(change);
        changeSpan.addClassName(LumoUtility.FontSize.XSMALL);
        boolean isPositive = change.startsWith("+");
        changeSpan.addClassName("wams-mini-stat-change");
        changeSpan.addClassName(isPositive ? "wams-mini-stat-change--positive" : "wams-mini-stat-change--negative");

        row.add(iconWrapper, info, changeSpan);
        card.add(row);
        return card;
    }

    private Div createTrendSection() {
        Div container = new Div();
        container.addClassName("wams-panel-section");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName("wams-panel-section-header");

        Span trendLabel = new Span(I18n.t("mms.dashboard.email.stats.monthly.trend"));
        trendLabel.addClassName(LumoUtility.FontSize.XSMALL);
        trendLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        Span viewAll = new Span(I18n.t("mms.dashboard.viewAll"));
        viewAll.addClassName(LumoUtility.FontSize.XSMALL);
        viewAll.addClassName(LumoUtility.TextColor.PRIMARY);
        viewAll.addClassName("wams-panel-viewall-link");

        header.add(trendLabel, viewAll);
        container.add(header);

        // Mini bar chart - using divs as bars
        Div chartBars = new Div();
        chartBars.addClassName("wams-chart-bars");

        int[] values = {35, 42, 50, 45, 55, 62, 58, 70, 75, 68, 80, 85};
        int max = 85;

        for (int i = 0; i < values.length; i++) {
            Div bar = new Div();
            int height = (int) ((values[i] / (double) max) * 45) + 5;
            boolean isLast = (i == values.length - 1);
            bar.addClassName("wams-chart-bar");
            if (isLast) {
                bar.addClassName("wams-chart-bar--last");
            }
            bar.getStyle().set("height", height + "px");
            chartBars.add(bar);
        }

        // Month labels
        HorizontalLayout labels = new HorizontalLayout();
        labels.setWidthFull();
        labels.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        labels.addClassName("wams-chart-labels");

        String[] months = {"J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"};
        for (String m : months) {
            Span label = new Span(m);
            label.addClassName(LumoUtility.FontSize.XSMALL);
            label.addClassName(LumoUtility.TextColor.TERTIARY);
            label.addClassName("wams-chart-label");
            labels.add(label);
        }

        container.add(chartBars, labels);
        return container;
    }

    private Div createMonthlyCompact() {
        Div container = new Div();
        container.addClassName("wams-compact-table");

        // Table header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(false);
        header.setSpacing(true);
        header.addClassName("wams-compact-table-header");

        String[] headers = {
                I18n.t("mms.dashboard.email.stats.table.month"),
                I18n.t("mms.dashboard.email.stats.sent"),
                I18n.t("mms.dashboard.email.stats.table.open"),
                I18n.t("mms.dashboard.email.stats.table.bounce")
        };
        for (String h : headers) {
            Span span = new Span(h);
            span.addClassName(LumoUtility.FontSize.XSMALL);
            span.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            span.addClassName("wams-compact-table-header-cell");
            header.add(span);
        }
        container.add(header);

        // Data rows with compact styling
        String[][] data = {
                {"Jan", "120", "80", "5"},
                {"Feb", "145", "95", "8"},
                {"Mar", "180", "120", "12"},
                {"Apr", "210", "140", "10"},
                {"May", "190", "130", "15"},
                {"Jun", "220", "150", "10"},
                {"Jul", "250", "170", "8"},
                {"Aug", "230", "160", "12"},
                {"Sep", "270", "180", "9"},
                {"Oct", "300", "200", "14"},
                {"Nov", "280", "190", "11"},
                {"Dec", "320", "210", "10"}
        };

        for (String[] rowData : data) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setPadding(false);
            row.setSpacing(true);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.addClassName("wams-compact-table-row");

            for (int i = 0; i < rowData.length; i++) {
                Span span = new Span(rowData[i]);
                span.addClassName(LumoUtility.FontSize.XSMALL);
                span.addClassName("wams-compact-table-cell");
                if (i == 0) {
                    span.addClassName("wams-compact-table-cell--label");
                }
                if (i == 1) span.addClassName("wams-compact-table-cell--sent");
                if (i == 2) span.addClassName("wams-compact-table-cell--open");
                if (i == 3) span.addClassName("wams-compact-table-cell--bounce");

                row.add(span);
            }
            container.add(row);
        }

        return container;
    }
}
