package eu.isygoit.ui.mms.views.dashboard;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
public class SenderConfigPanel extends VerticalLayout {

    public SenderConfigPanel() {
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        addClassName("sender-config-panel");
        getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("padding", "var(--lumo-space-l)");

        // Header with action
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName(LumoUtility.Margin.Bottom.MEDIUM);

        H3 title = new H3(I18n.t("mms.dashboard.sender.configurations"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        title.addClassName(LumoUtility.Margin.NONE);

        Button addBtn = new Button(I18n.t("mms.dashboard.sender.add"), VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        header.add(title, addBtn);
        add(header);

        // Sender configurations
        add(createSenderEntry(
                "SMTP - Production",
                "smtp.prod.company.com",
                "Active",
                "var(--lumo-success-color)",
                "mail@company.com",
                "587"
        ));

        add(createSenderEntry(
                "SMTP - Staging",
                "smtp.staging.company.com",
                "Inactive",
                "var(--lumo-error-color)",
                "staging@company.com",
                "587"
        ));

        add(createSenderEntry(
                "SendGrid - Production",
                "api.sendgrid.com",
                "Active",
                "var(--lumo-success-color)",
                "sendgrid@company.com",
                "443"
        ));

        // Summary
        Div summary = createSummary();
        add(summary);
    }

    private Div createSenderEntry(String name, String host, String status, String statusColor, String email, String port) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("transition", "all 0.3s ease");

        // Main row
        HorizontalLayout mainRow = new HorizontalLayout();
        mainRow.setWidthFull();
        mainRow.setAlignItems(FlexComponent.Alignment.CENTER);
        mainRow.setSpacing(true);

        // Icon
        Icon icon = VaadinIcon.MAILBOX.create();
        icon.setColor("var(--lumo-primary-color)");
        icon.setSize("24px");
        icon.getStyle().set("flex-shrink", "0");

        // Info
        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);
        info.getStyle().set("flex", "1");

        Paragraph nameLabel = new Paragraph(name);
        nameLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        nameLabel.addClassName(LumoUtility.Margin.NONE);
        nameLabel.getStyle().set("font-size", "var(--lumo-font-size-m)");

        HorizontalLayout details = new HorizontalLayout();
        details.setSpacing(true);
        details.setPadding(false);
        details.getStyle().set("gap", "var(--lumo-space-m)");

        Paragraph hostLabel = new Paragraph(host);
        hostLabel.addClassName(LumoUtility.TextColor.SECONDARY);
        hostLabel.addClassName(LumoUtility.FontSize.XSMALL);
        hostLabel.addClassName(LumoUtility.Margin.NONE);

        Paragraph emailLabel = new Paragraph(email);
        emailLabel.addClassName(LumoUtility.TextColor.SECONDARY);
        emailLabel.addClassName(LumoUtility.FontSize.XSMALL);
        emailLabel.addClassName(LumoUtility.Margin.NONE);

        Paragraph portLabel = new Paragraph("Port: " + port);
        portLabel.addClassName(LumoUtility.TextColor.SECONDARY);
        portLabel.addClassName(LumoUtility.FontSize.XSMALL);
        portLabel.addClassName(LumoUtility.Margin.NONE);

        details.add(hostLabel, emailLabel, portLabel);
        info.add(nameLabel, details);

        // Status with dot
        HorizontalLayout statusLayout = new HorizontalLayout();
        statusLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        statusLayout.setSpacing(true);

        Div dot = new Div();
        dot.getStyle()
                .set("width", "10px")
                .set("height", "10px")
                .set("border-radius", "50%")
                .set("background", statusColor)
                .set("animation", status.equals("Active") ? "pulse 2s ease-in-out infinite" : "none");

        Paragraph statusLabel = new Paragraph(status);
        statusLabel.getStyle()
                .set("color", statusColor)
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-sm)")
                .set("margin", "0");

        statusLayout.add(dot, statusLabel);

        // Actions
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setPadding(false);

        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        editBtn.setTooltipText(I18n.t("mms.dashboard.sender.edit"));

        Button testBtn = new Button(VaadinIcon.PLAY.create());
        testBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        testBtn.setTooltipText(I18n.t("mms.dashboard.sender.test"));

        Button deleteBtn = new Button(VaadinIcon.TRASH.create());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        deleteBtn.setTooltipText(I18n.t("mms.dashboard.sender.delete"));

        actions.add(editBtn, testBtn, deleteBtn);

        mainRow.add(icon, info, statusLayout, actions);
        mainRow.expand(info);

        card.add(mainRow);
        return card;
    }

    private Div createSummary() {
        Div container = new Div();
        container.getStyle()
                .set("margin-top", "var(--lumo-space-m)")
                .set("padding-top", "var(--lumo-space-m)")
                .set("border-top", "1px solid var(--lumo-contrast-10pct)")
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(150px, 1fr))")
                .set("gap", "var(--lumo-space-m)");

        // Total senders
        Div total = createSummaryItem(
                "Total Senders",
                "3",
                VaadinIcon.MAILBOX,
                "var(--lumo-primary-color)"
        );

        // Active
        Div active = createSummaryItem(
                "Active",
                "2",
                VaadinIcon.CHECK_CIRCLE,
                "var(--lumo-success-color)"
        );

        // Inactive
        Div inactive = createSummaryItem(
                "Inactive",
                "1",
                VaadinIcon.WARNING,
                "var(--lumo-error-color)"
        );

        container.add(total, active, inactive);
        return container;
    }

    private Div createSummaryItem(String label, String value, VaadinIcon icon, String color) {
        Div item = new Div();
        item.getStyle()
                .set("text-align", "center")
                .set("padding", "var(--lumo-space-s)");

        Icon iconComponent = icon.create();
        iconComponent.setSize("20px");
        iconComponent.setColor(color);
        iconComponent.getStyle()
                .set("display", "block")
                .set("margin", "0 auto var(--lumo-space-xs)");

        Paragraph valueText = new Paragraph(value);
        valueText.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "700")
                .set("margin", "0")
                .set("color", "var(--lumo-header-text-color)");

        Paragraph labelText = new Paragraph(label);
        labelText.addClassName(LumoUtility.TextColor.SECONDARY);
        labelText.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("margin", "0");

        item.add(iconComponent, valueText, labelText);
        return item;
    }
}