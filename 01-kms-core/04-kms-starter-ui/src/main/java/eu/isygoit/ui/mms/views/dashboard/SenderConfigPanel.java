package eu.isygoit.ui.mms.views.dashboard;

import com.vaadin.flow.component.UI;
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
        mainContainer.getStyle()
                .set("background", "linear-gradient(145deg, rgba(255,255,255,0.95), rgba(249,250,251,0.98))")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("padding", "var(--lumo-space-m)")
                .set("border", "1px solid rgba(255,255,255,0.3)")
                .set("backdrop-filter", "blur(10px)")
                .set("-webkit-backdrop-filter", "blur(10px)");

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

        H3 title = new H3(I18n.t("mms.dashboard.sender.configurations"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        title.addClassName(LumoUtility.Margin.NONE);
        title.getStyle().set("font-weight", "600");

        Button addBtn = new Button(VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        addBtn.setTooltipText(I18n.t("mms.dashboard.sender.add"));

        header.add(title, addBtn);
        return header;
    }

    private Div createCompactSenderList() {
        Div container = new Div();
        container.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-xs)")
                .set("max-height", "220px")
                .set("overflow-y", "auto")
                .set("margin-bottom", "var(--lumo-space-s)");

        container.add(createSenderItem(
                "Production SMTP",
                "smtp.prod.company.com",
                true,
                "#4F46E5",
                "mail@company.com",
                "587"
        ));

        container.add(createSenderItem(
                "Staging SMTP",
                "smtp.staging.company.com",
                false,
                "#6B7280",
                "staging@company.com",
                "587"
        ));

        container.add(createSenderItem(
                "SendGrid",
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
        item.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("padding", "var(--lumo-space-s)")
                .set("margin-bottom", "var(--lumo-space-xs)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("transition", "all 0.2s ease")
                .set("cursor", "pointer");

        item.addClassName("sender-item");

        // Main row
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.getStyle().set("gap", "var(--lumo-space-xs)");

        // Status dot
        Div dot = new Div();
        dot.getStyle()
                .set("width", "8px")
                .set("height", "8px")
                .set("border-radius", "50%")
                .set("background", active ? "#10B981" : "#6B7280")
                .set("flex-shrink", "0")
                .set("animation", active ? "pulse-dot 2s ease-in-out infinite" : "none");

        // Icon
        Icon mailIcon = VaadinIcon.MAILBOX.create();
        mailIcon.setSize("16px");
        mailIcon.setColor(color);

        // Info
        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);
        info.getStyle().set("flex", "1").set("min-width", "0");

        Span nameSpan = new Span(name);
        nameSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        nameSpan.addClassName(LumoUtility.FontSize.SMALL);
        nameSpan.getStyle()
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");

        Span hostSpan = new Span(host);
        hostSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        hostSpan.addClassName(LumoUtility.FontSize.XSMALL);
        hostSpan.getStyle()
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");

        // Host and port in one line
        HorizontalLayout details = new HorizontalLayout();
        details.setPadding(false);
        details.setSpacing(true);
        details.getStyle().set("gap", "var(--lumo-space-s)");

        Span portSpan = new Span("Port: " + port);
        portSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        portSpan.addClassName(LumoUtility.FontSize.XSMALL);

        details.add(hostSpan, portSpan);

        info.add(nameSpan, details);

        // Status label
        Span statusSpan = new Span(active ? "Active" : "Inactive");
        statusSpan.addClassName(LumoUtility.FontSize.XSMALL);
        statusSpan.getStyle()
                .set("color", active ? "#10B981" : "#6B7280")
                .set("font-weight", "600")
                .set("white-space", "nowrap");

        // Actions
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setPadding(false);
        actions.getStyle().set("gap", "2px");

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
        container.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(3, 1fr)")
                .set("gap", "var(--lumo-space-xs)")
                .set("padding", "var(--lumo-space-xs) 0");

        container.add(createSummaryItem("Total", "3", VaadinIcon.MAILBOX, "#4F46E5"));
        container.add(createSummaryItem("Active", "2", VaadinIcon.CHECK, "#10B981"));
        container.add(createSummaryItem("Inactive", "1", VaadinIcon.CLOSE, "#EF4444"));

        return container;
    }

    private Div createSummaryItem(String label, String value, VaadinIcon icon, String color) {
        Div item = new Div();
        item.getStyle()
                .set("text-align", "center")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("padding", "var(--lumo-space-xs)");

        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        row.setSpacing(true);

        Icon iconComponent = icon.create();
        iconComponent.setSize("14px");
        iconComponent.setColor(color);

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-m)")
                .set("font-weight", "700")
                .set("color", "var(--lumo-header-text-color)");

        Span labelSpan = new Span(label);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName(LumoUtility.TextColor.SECONDARY);

        row.add(iconComponent, valueSpan, labelSpan);
        item.add(row);
        return item;
    }

    private void injectResponsiveStyles() {
        String css = """
                .sender-config-panel {
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(10px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                @keyframes pulse-dot {
                    0%, 100% { opacity: 1; }
                    50% { opacity: 0.4; }
                }
                .sender-item:hover {
                    border-color: var(--lumo-primary-color-50pct);
                    box-shadow: var(--lumo-box-shadow-xs);
                }
                .sender-item:hover [class*="actions"] vaadin-button {
                    opacity: 1;
                }
                .sender-item [class*="actions"] vaadin-button {
                    opacity: 0.6;
                }
                @media (max-width: 480px) {
                    .sender-config-panel [style*="grid-template-columns: repeat(3, 1fr)"] {
                        grid-template-columns: repeat(3, 1fr) !important;
                    }
                    .sender-item [class*="details"] {
                        flex-wrap: wrap;
                    }
                }
                /* Custom scrollbar */
                .sender-config-panel [style*="max-height: 220px"]::-webkit-scrollbar {
                    width: 4px;
                }
                .sender-config-panel [style*="max-height: 220px"]::-webkit-scrollbar-track {
                    background: var(--lumo-contrast-5pct);
                    border-radius: 10px;
                }
                .sender-config-panel [style*="max-height: 220px"]::-webkit-scrollbar-thumb {
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