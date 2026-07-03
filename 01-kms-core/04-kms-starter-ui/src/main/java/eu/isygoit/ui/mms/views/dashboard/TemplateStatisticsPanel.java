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
public class TemplateStatisticsPanel extends VerticalLayout {

    public TemplateStatisticsPanel() {
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("template-stats-panel");

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

        // Mini Stats - 4 compact cards
        Div statsGrid = createMiniStats();
        mainContainer.add(statsGrid);

        // Template usage with mini progress bars
        Div usageSection = createCompactUsage();
        mainContainer.add(usageSection);

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

        H3 title = new H3(I18n.t("mms.dashboard.template.statistics"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        title.addClassName(LumoUtility.Margin.NONE);
        title.getStyle().set("font-weight", "600");

        Span badge = new Span(I18n.t("mms.dashboard.template.stats.live"));
        badge.getStyle()
                .set("background", "#10B98120")
                .set("color", "#10B981")
                .set("padding", "2px 12px")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "600");

        header.add(title, badge);
        return header;
    }

    private Div createMiniStats() {
        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(4, 1fr)")
                .set("gap", "var(--lumo-space-xs)")
                .set("margin-bottom", "var(--lumo-space-s)");

        grid.add(createMiniStatCard(I18n.t("mms.dashboard.template.stats.total"), "120", VaadinIcon.FILE_TEXT, "#4F46E5"));
        grid.add(createMiniStatCard(I18n.t("mms.dashboard.template.stats.active"), "85", VaadinIcon.CHECK_CIRCLE, "#10B981"));
        grid.add(createMiniStatCard(I18n.t("mms.dashboard.template.stats.draft"), "25", VaadinIcon.PENCIL, "#F59E0B"));
        grid.add(createMiniStatCard(I18n.t("mms.dashboard.template.stats.archived"), "10", VaadinIcon.ARCHIVE, "#6B7280"));

        return grid;
    }

    private Div createMiniStatCard(String label, String value, VaadinIcon icon, String color) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-s)")
                .set("text-align", "center")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("transition", "all 0.2s ease")
                .set("cursor", "pointer");

        card.addClassName("template-stat-card");

        Icon iconComponent = icon.create();
        iconComponent.setSize("18px");
        iconComponent.setColor(color);
        iconComponent.getStyle()
                .set("display", "block")
                .set("margin", "0 auto var(--lumo-space-xs)");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "700")
                .set("color", "var(--lumo-header-text-color)")
                .set("display", "block");

        Span labelSpan = new Span(label);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName(LumoUtility.TextColor.SECONDARY);

        card.add(iconComponent, valueSpan, labelSpan);
        return card;
    }

    private Div createCompactUsage() {
        Div container = new Div();
        container.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-s)");

        // Header with title
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "var(--lumo-space-s)");

        Span usageLabel = new Span(I18n.t("mms.dashboard.template.stats.top.templates"));
        usageLabel.addClassName(LumoUtility.FontSize.XSMALL);
        usageLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        Span viewAll = new Span(I18n.t("mms.dashboard.viewAll"));
        viewAll.addClassName(LumoUtility.FontSize.XSMALL);
        viewAll.addClassName(LumoUtility.TextColor.PRIMARY);
        viewAll.getStyle().set("cursor", "pointer");

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
        row.getStyle()
                .set("padding", "var(--lumo-space-xs) 0")
                .set("border-bottom", "1px solid var(--lumo-contrast-5pct)")
                .set("gap", "var(--lumo-space-s)");

        // Name with icon
        HorizontalLayout nameLayout = new HorizontalLayout();
        nameLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        nameLayout.setSpacing(true);
        nameLayout.getStyle().set("flex", "1").set("min-width", "0");

        Icon fileIcon = VaadinIcon.FILE_O.create();
        fileIcon.setSize("14px");
        fileIcon.setColor(color);

        Span nameSpan = new Span(name);
        nameSpan.addClassName(LumoUtility.FontSize.SMALL);
        nameSpan.getStyle()
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");

        nameLayout.add(fileIcon, nameSpan);

        // Progress bar
        Div barContainer = new Div();
        barContainer.getStyle()
                .set("flex", "2")
                .set("min-width", "60px");

        Div bar = new Div();
        bar.getStyle()
                .set("height", "5px")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("overflow", "hidden");

        Div fill = new Div();
        fill.getStyle()
                .set("height", "100%")
                .set("width", usage + "%")
                .set("background", color)
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("transition", "width 0.6s ease");
        bar.add(fill);
        barContainer.add(bar);

        // Percentage
        Span percentSpan = new Span(usage + "%");
        percentSpan.addClassName(LumoUtility.FontSize.XSMALL);
        percentSpan.getStyle()
                .set("color", color)
                .set("font-weight", "600")
                .set("min-width", "36px")
                .set("text-align", "right");

        row.add(nameLayout, barContainer, percentSpan);
        row.expand(nameLayout);
        return row;
    }

    private void injectResponsiveStyles() {
        String css = """
                .template-stats-panel {
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(10px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .template-stat-card:hover {
                    transform: translateY(-2px);
                    box-shadow: var(--lumo-box-shadow-xs);
                    border-color: var(--lumo-primary-color-50pct);
                }
                @media (max-width: 480px) {
                    .template-stats-panel [style*="grid-template-columns: repeat(4, 1fr)"] {
                        grid-template-columns: repeat(2, 1fr) !important;
                    }
                    .template-stats-panel [class*="usage-item"] {
                        flex-wrap: wrap;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}