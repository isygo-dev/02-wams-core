package eu.isygoit.ui.mms.views.dashboard;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
public class SenderConfigPanel extends VerticalLayout {

    public SenderConfigPanel() {
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("sender-config-panel");

        Div mainContainer = new Div();
        mainContainer.setWidthFull();
        mainContainer.addClassName("wams-panel-container");

        // Compact Header with actions
        HorizontalLayout header = createCompactHeader();
        mainContainer.add(header);

        // Sender list compact
        Div senderList = createCompactSenderList();
        mainContainer.add(senderList);

        // Mini summary at bottom
        Div summary = createMiniSummary();
        mainContainer.add(summary);

        add(mainContainer);
    }

    private HorizontalLayout createCompactHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.addClassName("wams-panel-header");

        H3 title = new H3(I18n.t("mms.dashboard.sender.configurations"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        title.addClassName(LumoUtility.Margin.NONE);
        title.addClassName("wams-panel-title");

        Span sampleBadge = new Span(I18n.t("mms.dashboard.sample.data.badge"));
        sampleBadge.addClassName("wams-panel-period-badge");
        sampleBadge.addClassName(LumoUtility.FontSize.XSMALL);
        sampleBadge.addClassName(LumoUtility.TextColor.SECONDARY);

        HorizontalLayout titleGroup = new HorizontalLayout(title, sampleBadge);
        titleGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        titleGroup.setSpacing(true);

        Button addBtn = new Button(VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        addBtn.setTooltipText(I18n.t("mms.dashboard.sender.add"));

        header.add(titleGroup, addBtn);
        return header;
    }

    private Div createCompactSenderList() {
        Div container = new Div();
        container.addClassName("wams-scroll-list");

        container.add(createSenderItem(
                I18n.t("mms.dashboard.sender.sample.production.name"),
                "smtp.prod.company.com",
                true,
                "#4F46E5",
                "mail@company.com",
                "587"
        ));

        container.add(createSenderItem(
                I18n.t("mms.dashboard.sender.sample.staging.name"),
                "smtp.staging.company.com",
                false,
                "#6B7280",
                "staging@company.com",
                "587"
        ));

        container.add(createSenderItem(
                I18n.t("mms.dashboard.sender.sample.sendgrid.name"),
                "api.sendgrid.com",
                true,
                "#10B981",
                "sendgrid@company.com",
                "443"
        ));

        return container;
    }

    private Div createSenderItem(String name, String host, boolean active, String color, String email, String port) {
        Div item = new Div();
        item.addClassName("sender-item");

        // Main row
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.addClassName("sender-item-row");

        // Status dot
        Div dot = new Div();
        dot.addClassName("sender-item-dot");
        if (active) {
            dot.addClassName("sender-item-dot--active");
        }

        // Icon
        Icon mailIcon = VaadinIcon.MAILBOX.create();
        mailIcon.setSize("16px");
        mailIcon.setColor(color);

        // Info
        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);
        info.addClassName("sender-item-info");

        Span nameSpan = new Span(name);
        nameSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        nameSpan.addClassName(LumoUtility.FontSize.SMALL);
        nameSpan.addClassName("sender-item-name");

        Span hostSpan = new Span(host);
        hostSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        hostSpan.addClassName(LumoUtility.FontSize.XSMALL);
        hostSpan.addClassName("sender-item-host");

        // Host and port in one line
        HorizontalLayout details = new HorizontalLayout();
        details.setPadding(false);
        details.setSpacing(true);
        details.addClassName("sender-item-details");

        Span portSpan = new Span(I18n.t("mms.dashboard.sender.port", port));
        portSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        portSpan.addClassName(LumoUtility.FontSize.XSMALL);

        details.add(hostSpan, portSpan);

        info.add(nameSpan, details);

        // Status label
        Span statusSpan = new Span(active ? I18n.t("mms.dashboard.sender.status.active") : I18n.t("mms.dashboard.sender.status.inactive"));
        statusSpan.addClassName(LumoUtility.FontSize.XSMALL);
        statusSpan.addClassName("sender-item-status");
        if (active) {
            statusSpan.addClassName("sender-item-status--active");
        }

        // Actions
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setPadding(false);
        actions.addClassName("sender-item-actions");

        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        editBtn.setTooltipText(I18n.t("mms.dashboard.sender.edit"));

        Button testBtn = new Button(VaadinIcon.PLAY.create());
        testBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        testBtn.setTooltipText(I18n.t("mms.dashboard.sender.test"));

        actions.add(editBtn, testBtn);

        row.add(dot, mailIcon, info, statusSpan, actions);
        row.expand(info);
        item.add(row);

        return item;
    }

    private Div createMiniSummary() {
        Div container = new Div();
        container.addClassName("wams-summary-grid");

        container.add(createSummaryItem(I18n.t("mms.dashboard.sender.summary.total"), "3", VaadinIcon.MAILBOX, "#4F46E5"));
        container.add(createSummaryItem(I18n.t("mms.dashboard.sender.status.active"), "2", VaadinIcon.CHECK, "#10B981"));
        container.add(createSummaryItem(I18n.t("mms.dashboard.sender.status.inactive"), "1", VaadinIcon.CLOSE, "#EF4444"));

        return container;
    }

    private Div createSummaryItem(String label, String value, VaadinIcon icon, String color) {
        Div item = new Div();
        item.addClassName("wams-summary-item");

        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        row.setSpacing(true);

        Icon iconComponent = icon.create();
        iconComponent.setSize("14px");
        iconComponent.setColor(color);

        Span valueSpan = new Span(value);
        valueSpan.addClassName("wams-summary-value");

        Span labelSpan = new Span(label);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName(LumoUtility.TextColor.SECONDARY);

        row.add(iconComponent, valueSpan, labelSpan);
        item.add(row);
        return item;
    }
}
