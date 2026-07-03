package eu.isygoit.ui.sms;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.sms.layout.SmsMainLayout;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Dashboard view for SMS, displaying key statistics and quick actions.
 * Accessible at "/sms" and "/sms/home".
 */
@RouteAlias(value = "sms/home", layout = SmsMainLayout.class)
@UIScope
@Route(value = "sms", layout = SmsMainLayout.class)
@PageTitle("Storage Management Dashboard")
public class SmsMainView extends ManagementVerticalView {

    private final StorageConfigService storageConfigService;
    private final UI ui;

    @Autowired
    public SmsMainView(StorageConfigService storageConfigService) {
        this.storageConfigService = storageConfigService;
        this.ui = UI.getCurrent();

        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("sms-dashboard");

        add(buildHeader());
        add(buildStatisticsRow());
        add(buildRecentActivityPanel());
        add(buildQuickActions());

        injectResponsiveStyles();
    }

    private H2 buildHeader() {
        H2 title = new H2(I18n.t("sms.dashboard.title"));
        title.getStyle().set("margin-bottom", "10px");
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        return title;
    }

    private HorizontalLayout buildStatisticsRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(true);
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);
        row.addClassName("stats-row");

        row.add(createStatCard(I18n.t("sms.dashboard.total.configs"), "0", VaadinIcon.DATABASE, "storageconfigs-link"));
        row.add(createStatCard(I18n.t("sms.dashboard.total.buckets"), "0", VaadinIcon.FOLDER_O, "buckets-link"));
        row.add(createStatCard(I18n.t("sms.dashboard.total.storage.used"), "0", VaadinIcon.HARDDRIVE, "storage-stats-link"));

        return row;
    }

    private Div createStatCard(String title, String value, VaadinIcon icon, String navigateTo) {
        Div card = new Div();
        card.addClassName("stat-card");
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("padding", "var(--lumo-space-m)")
                .set("text-align", "center")
                .set("flex", "1")
                .set("cursor", "pointer");

        Icon iconComponent = icon.create();
        iconComponent.setSize("32px");
        iconComponent.setColor("var(--lumo-primary-color)");

        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontSize.SMALL);
        titleSpan.addClassName(LumoUtility.TextColor.SECONDARY);

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.XXXLARGE);
        valueSpan.addClassName(LumoUtility.FontWeight.BOLD);
        valueSpan.setId(title.toLowerCase().replace(" ", "-") + "-value");

        card.add(iconComponent, titleSpan, valueSpan);
        card.addClickListener(e -> ui.navigate(navigateTo));
        return card;
    }

    private VerticalLayout buildRecentActivityPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.getStyle().set("margin-top", "24px");
        H2 title = new H2(I18n.t("sms.dashboard.recent.activity"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        Div content = new Div();
        content.setText(I18n.t("sms.dashboard.recent.activity.description"));
        content.getStyle().set("padding", "var(--lumo-space-m)")
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)");
        panel.add(title, content);
        return panel;
    }

    private VerticalLayout buildQuickActions() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.getStyle().set("gap", "10px").set("margin-top", "24px");
        H2 title = new H2(I18n.t("sms.dashboard.quick.actions"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        Div actions = new Div();
        StringBuilder sb = new StringBuilder();
        sb.append("• ").append(I18n.t("sms.dashboard.quick.actions.add.storage")).append("\n");
        sb.append("• ").append(I18n.t("sms.dashboard.quick.actions.create.bucket")).append("\n");
        sb.append("• ").append(I18n.t("sms.dashboard.quick.actions.upload.file")).append("\n");
        sb.append("• ").append(I18n.t("sms.dashboard.quick.actions.view.usage"));
        actions.add(new Span(sb.toString()));
        actions.getStyle().set("white-space", "pre-line");
        layout.add(title, actions);
        return layout;
    }

    private void injectResponsiveStyles() {
        String css = """
                .sms-dashboard .stat-card {
                    transition: transform 0.2s ease, box-shadow 0.2s ease;
                }
                .sms-dashboard .stat-card:hover {
                    transform: translateY(-4px);
                    box-shadow: var(--lumo-box-shadow-m);
                }
                .sms-dashboard .stats-row {
                    flex-wrap: wrap;
                }
                @media (max-width: 768px) {
                    .sms-dashboard .stat-card {
                        flex-basis: calc(50% - 16px);
                        margin-bottom: 16px;
                    }
                    .sms-dashboard .stats-row {
                        gap: 16px;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}