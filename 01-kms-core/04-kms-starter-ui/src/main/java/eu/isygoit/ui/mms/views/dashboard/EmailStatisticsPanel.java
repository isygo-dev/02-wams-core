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

import java.util.ArrayList;
import java.util.List;

@Component
@UIScope
public class EmailStatisticsPanel extends VerticalLayout {

    public EmailStatisticsPanel() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        addClassName("email-stats-panel");
        getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("padding", "var(--lumo-space-l)");

        H3 title = new H3(I18n.t("mms.dashboard.email.statistics"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        title.addClassName(LumoUtility.Margin.Bottom.MEDIUM);
        add(title);

        // Summary stats
        Div summaryStats = createSummaryStats();
        add(summaryStats);

        // Monthly data
        Div monthlyData = createMonthlyData();
        add(monthlyData);
    }

    private Div createSummaryStats() {
        Div grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(140px, 1fr))")
                .set("gap", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-l)");

        grid.add(createSummaryCard("Total Sent", "2,847", VaadinIcon.ENVELOPE, "var(--lumo-primary-color)"));
        grid.add(createSummaryCard("Opened", "1,925", VaadinIcon.EYE, "var(--lumo-success-color)"));
        grid.add(createSummaryCard("Bounced", "124", VaadinIcon.WARNING, "var(--lumo-error-color)"));
        grid.add(createSummaryCard("Open Rate", "67.6%", VaadinIcon.FILE_START, "var(--lumo-warning-color)"));

        return grid;
    }

    private Div createSummaryCard(String label, String value, VaadinIcon icon, String color) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("text-align", "center")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("transition", "all 0.3s ease");

        // Icon
        Icon iconComponent = icon.create();
        iconComponent.setSize("22px");
        iconComponent.setColor(color);
        iconComponent.getStyle()
                .set("display", "block")
                .set("margin", "0 auto var(--lumo-space-xs)");

        // Value
        Paragraph valueText = new Paragraph(value);
        valueText.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
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

    private Div createMonthlyData() {
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

        String[] monthHeaders = {"Month", "Sent", "Opened", "Bounced"};
        for (String h : monthHeaders) {
            Paragraph p = new Paragraph(h);
            p.addClassName(LumoUtility.FontWeight.BOLD);
            p.addClassName(LumoUtility.FontSize.XSMALL);
            p.getStyle().set("flex", "1").set("text-align", "center");
            header.add(p);
        }
        container.add(header);

        // Monthly data rows
        String[][] months = {
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

        for (String[] month : months) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setPadding(false);
            row.setSpacing(true);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.getStyle()
                    .set("padding", "var(--lumo-space-xs) 0")
                    .set("border-bottom", "1px solid var(--lumo-contrast-5pct)");

            for (int i = 0; i < month.length; i++) {
                Paragraph p = new Paragraph(month[i]);
                p.addClassName(LumoUtility.FontSize.SMALL);
                p.getStyle()
                        .set("flex", "1")
                        .set("text-align", "center")
                        .set("margin", "0");

                if (i == 1) p.getStyle().set("color", "var(--lumo-primary-color)");
                if (i == 2) p.getStyle().set("color", "var(--lumo-success-color)");
                if (i == 3) p.getStyle().set("color", "var(--lumo-error-color)");

                row.add(p);
            }
            container.add(row);
        }

        return container;
    }
}