package eu.isygoit.ui.ims;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
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
import eu.isygoit.remote.ims.*;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Dashboard view for IMS, displaying key statistics and quick actions.
 * Accessible at "/ims" and "/ims/home".
 */
@RouteAlias(value = "ims/home", layout = ImsMainLayout.class)
@UIScope
@Route(value = "ims", layout = ImsMainLayout.class)
@PageTitle("Identity management Dashboard")
public class ImsMainView extends ManagementVerticalView {

    private final AccountService accountService;
    private final TenantService tenantService;
    private final ApplicationService applicationService;
    private final CustomerService customerService;
    private final RoleInfoService roleInfoService;
    private final UI ui;

    @Autowired
    public ImsMainView(AccountService accountService,
                       TenantService tenantService,
                       ApplicationService applicationService,
                       CustomerService customerService,
                       RoleInfoService roleInfoService) {
        this.accountService = accountService;
        this.tenantService = tenantService;
        this.applicationService = applicationService;
        this.customerService = customerService;
        this.roleInfoService = roleInfoService;
        this.ui = UI.getCurrent();

        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("ims-dashboard");

        add(buildHeader());
        add(buildStatisticsRow());
        add(buildRecentActivityPanel());
        add(buildQuickActions());
    }

    private H2 buildHeader() {
        H2 title = new H2(I18n.t("ims.dashboard.title"));
        title.addClassName("dashboard-title");
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

        row.add(createStatCard(I18n.t("ims.dashboard.total.accounts"), "0", VaadinIcon.USER, "accounts-link"));
        row.add(createStatCard(I18n.t("ims.dashboard.total.tenants"), "0", VaadinIcon.BUILDING, "tenants-link"));
        row.add(createStatCard(I18n.t("ims.dashboard.total.applications"), "0", VaadinIcon.PAPERCLIP, "apps-link"));
        row.add(createStatCard(I18n.t("ims.dashboard.total.customers"), "0", VaadinIcon.GROUP, "customers-link"));
        row.add(createStatCard(I18n.t("ims.dashboard.total.roles"), "0", VaadinIcon.SHIELD, "roles-link"));

        return row;
    }

    private Div createStatCard(String title, String value, VaadinIcon icon, String navigateTo) {
        Div card = new Div();
        card.addClassName("stat-card");

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
        panel.addClassName("activity-panel");
        H2 title = new H2(I18n.t("ims.dashboard.recent.activity"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        Div content = new Div();
        content.setText(I18n.t("ims.dashboard.recent.activity.description"));
        content.addClassName("activity-panel-content");
        panel.add(title, content);
        return panel;
    }

    private VerticalLayout buildQuickActions() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.addClassName("quick-actions");
        H2 title = new H2(I18n.t("ims.dashboard.quick.actions"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        Div actions = new Div();
        StringBuilder sb = new StringBuilder();
        sb.append("• ").append(I18n.t("ims.dashboard.quick.actions.create.account")).append("\n");
        sb.append("• ").append(I18n.t("ims.dashboard.quick.actions.add.tenant")).append("\n");
        sb.append("• ").append(I18n.t("ims.dashboard.quick.actions.register.application")).append("\n");
        sb.append("• ").append(I18n.t("ims.dashboard.quick.actions.add.customer")).append("\n");
        sb.append("• ").append(I18n.t("ims.dashboard.quick.actions.assign.role"));
        actions.add(new Span(sb.toString()));
        actions.addClassName("quick-actions-list");
        layout.add(title, actions);
        return layout;
    }

    // Helper Span
    private static class Span extends com.vaadin.flow.component.html.Span {
        public Span(String text) {
            super(text);
        }
    }
}