package eu.isygoit.ui.mms.views.dashboard;

import com.vaadin.flow.component.UI;
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
        mainContainer.getStyle()
                .set("background", "linear-gradient(145deg, rgba(255,255,255,0.95), rgba(249,250,251,0.98))")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("padding", "var(--lumo-space-m)")
                .set("border", "1px solid rgba(255,255,255,0.3)")
                .set("backdrop-filter", "blur(10px)")
                .set("-webkit-backdrop-filter", "blur(10px)");

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
        injectResponsiveStyles();
    }

    private HorizontalLayout createCompactHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.getStyle()
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-xs)");

        H3 title = new H3(I18n.t("mms.dashboard.email.statistics"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        title.addClassName(LumoUtility.Margin.NONE);
        title.getStyle().set("font-weight", "600");

        Span period = new Span("Last 12 months");
        period.addClassName(LumoUtility.FontSize.XSMALL);
        period.addClassName(LumoUtility.TextColor.SECONDARY);
        period.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-s)");

        HorizontalLayout rightGroup = new HorizontalLayout(period);
        rightGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        rightGroup.setSpacing(true);

        header.add(title, rightGroup);
        return header;
    }

    private Div createMiniStats() {
        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(4, 1fr)")
                .set("gap", "var(--lumo-space-xs)")
                .set("margin-bottom", "var(--lumo-space-s)");

        grid.add(createMiniStatCard("Sent", "2.8K", VaadinIcon.ENVELOPE_O, "#4F46E5", "+12%"));
        grid.add(createMiniStatCard("Opened", "1.9K", VaadinIcon.EYE, "#10B981", "+8%"));
        grid.add(createMiniStatCard("Bounced", "124", VaadinIcon.EXCLAMATION_CIRCLE_O, "#EF4444", "-3%"));
        grid.add(createMiniStatCard("Rate", "67.6%", VaadinIcon.TRENDING_UP, "#F59E0B", "+5%"));

        return grid;
    }

    private Div createMiniStatCard(String label, String value, VaadinIcon icon, String color, String change) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-s)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("transition", "all 0.2s ease")
                .set("cursor", "pointer");

        // Hover effect
        card.addClassName("stat-card");

        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();

        // Icon with circle background
        Div iconWrapper = new Div();
        iconWrapper.getStyle()
                .set("background", color + "20")
                .set("border-radius", "50%")
                .set("padding", "var(--lumo-space-xs)")
                .set("width", "32px")
                .set("height", "32px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("flex-shrink", "0");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.setColor(color);
        iconWrapper.add(iconComponent);

        // Value and label
        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);
        info.getStyle().set("flex", "1");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "700")
                .set("color", "var(--lumo-header-text-color)")
                .set("line-height", "1.2");

        Span labelSpan = new Span(label);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName(LumoUtility.TextColor.SECONDARY);

        info.add(valueSpan, labelSpan);

        // Change indicator
        Span changeSpan = new Span(change);
        changeSpan.addClassName(LumoUtility.FontSize.XSMALL);
        boolean isPositive = change.startsWith("+");
        changeSpan.getStyle()
                .set("color", isPositive ? "#10B981" : "#EF4444")
                .set("font-weight", "600")
                .set("background", (isPositive ? "#10B981" : "#EF4444") + "15")
                .set("padding", "0 var(--lumo-space-xs)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("white-space", "nowrap");

        row.add(iconWrapper, info, changeSpan);
        card.add(row);
        return card;
    }

    private Div createTrendSection() {
        Div container = new Div();
        container.getStyle()
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-s)");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        Span trendLabel = new Span("Monthly Trend");
        trendLabel.addClassName(LumoUtility.FontSize.XSMALL);
        trendLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        Span viewAll = new Span("View all →");
        viewAll.addClassName(LumoUtility.FontSize.XSMALL);
        viewAll.addClassName(LumoUtility.TextColor.PRIMARY);
        viewAll.getStyle().set("cursor", "pointer");

        header.add(trendLabel, viewAll);
        container.add(header);

        // Mini bar chart - using divs as bars
        Div chartBars = new Div();
        chartBars.getStyle()
                .set("display", "flex")
                .set("align-items", "flex-end")
                .set("justify-content", "space-between")
                .set("height", "60px")
                .set("gap", "2px")
                .set("padding", "var(--lumo-space-xs) 0");

        int[] values = {35, 42, 50, 45, 55, 62, 58, 70, 75, 68, 80, 85};
        int max = 85;

        for (int i = 0; i < values.length; i++) {
            Div bar = new Div();
            int height = (int) ((values[i] / (double) max) * 45) + 5;
            boolean isLast = (i == values.length - 1);
            bar.getStyle()
                    .set("height", height + "px")
                    .set("width", "100%")
                    .set("background", isLast ? "#4F46E5" : "var(--lumo-primary-color-60pct)")
                    .set("border-radius", "3px 3px 0 0")
                    .set("min-height", "4px")
                    .set("transition", "height 0.3s ease");
            chartBars.add(bar);
        }

        // Month labels
        HorizontalLayout labels = new HorizontalLayout();
        labels.setWidthFull();
        labels.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        labels.getStyle().set("gap", "2px");

        String[] months = {"J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"};
        for (String m : months) {
            Span label = new Span(m);
            label.addClassName(LumoUtility.FontSize.XSMALL);
            label.addClassName(LumoUtility.TextColor.TERTIARY);
            label.getStyle().set("text-align", "center").set("flex", "1");
            labels.add(label);
        }

        container.add(chartBars, labels);
        return container;
    }

    private Div createMonthlyCompact() {
        Div container = new Div();
        container.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-s)")
                .set("max-height", "180px")
                .set("overflow-y", "auto");

        // Table header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(false);
        header.setSpacing(true);
        header.getStyle()
                .set("border-bottom", "2px solid var(--lumo-contrast-20pct)")
                .set("padding-bottom", "var(--lumo-space-xs)")
                .set("margin-bottom", "var(--lumo-space-xs)")
                .set("position", "sticky")
                .set("top", "0")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("z-index", "1");

        String[] headers = {"Month", "Sent", "Open", "Bounce"};
        for (String h : headers) {
            Span span = new Span(h);
            span.addClassName(LumoUtility.FontSize.XSMALL);
            span.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            span.getStyle().set("flex", "1").set("text-align", "center");
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
            row.getStyle()
                    .set("padding", "var(--lumo-space-xs) 0")
                    .set("border-bottom", "1px solid var(--lumo-contrast-5pct)");

            for (int i = 0; i < rowData.length; i++) {
                Span span = new Span(rowData[i]);
                span.addClassName(LumoUtility.FontSize.XSMALL);
                span.getStyle()
                        .set("flex", "1")
                        .set("text-align", "center")
                        .set("font-weight", i == 0 ? "500" : "400");

                if (i == 1) span.getStyle().set("color", "#4F46E5");
                if (i == 2) span.getStyle().set("color", "#10B981");
                if (i == 3) span.getStyle().set("color", "#EF4444");

                row.add(span);
            }
            container.add(row);
        }

        return container;
    }

    private void injectResponsiveStyles() {
        String css = """
                .email-stats-panel {
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(10px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .stat-card:hover {
                    transform: translateY(-2px);
                    box-shadow: var(--lumo-box-shadow-xs);
                    border-color: var(--lumo-primary-color-50pct);
                }
                @media (max-width: 640px) {
                    .email-stats-panel [class*="stat-card"] {
                        padding: var(--lumo-space-xs);
                    }
                    .email-stats-panel [class*="stat-card"] span:first-child {
                        font-size: var(--lumo-font-size-m);
                    }
                }
                @media (max-width: 480px) {
                    .email-stats-panel [style*="grid-template-columns: repeat(4, 1fr)"] {
                        grid-template-columns: repeat(2, 1fr) !important;
                    }
                }
                /* Custom scrollbar */
                .email-stats-panel [style*="max-height: 180px"]::-webkit-scrollbar {
                    width: 4px;
                }
                .email-stats-panel [style*="max-height: 180px"]::-webkit-scrollbar-track {
                    background: var(--lumo-contrast-5pct);
                    border-radius: 10px;
                }
                .email-stats-panel [style*="max-height: 180px"]::-webkit-scrollbar-thumb {
                    background: var(--lumo-contrast-30pct);
                    border-radius: 10px;
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}