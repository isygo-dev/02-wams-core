package eu.isygoit.ui.ims;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.*;
import eu.isygoit.ui.common.component.DashboardShortcutsBar;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Dashboard view for IMS, displaying key statistics, enrichment insights,
 * and a quick-shortcuts bar. Accessible at "/ims" and "/ims/home".
 */
@RouteAlias(value = "ims/home", layout = ImsMainLayout.class)
@UIScope
@Route(value = "ims", layout = ImsMainLayout.class)
@PageTitle("Identity management Dashboard")
public class ImsMainView extends ManagementVerticalView {

    private static final Logger log = LoggerFactory.getLogger(ImsMainView.class);

    private final AccountService accountService;
    private final TenantService tenantService;
    private final ApplicationService applicationService;
    private final CustomerService customerService;
    private final RoleInfoService roleInfoService;
    private final UI ui;

    private StatCard accountsCard;
    private StatCard tenantsCard;
    private StatCard applicationsCard;
    private StatCard customersCard;
    private StatCard rolesCard;

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

        add(buildShortcutsBar());
        add(buildHeader());
        add(buildStatsGrid());
        add(buildRecentActivityPanel());

        loadStatistics();
    }

    private DashboardShortcutsBar buildShortcutsBar() {
        List<DashboardShortcutsBar.Shortcut> shortcuts = List.of(
                new DashboardShortcutsBar.Shortcut(VaadinIcon.PLUS,
                        I18n.t("ims.dashboard.quick.actions.create.account"),
                        () -> UI.getCurrent().navigate("ims/accounts")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.BUILDING,
                        I18n.t("ims.dashboard.quick.actions.add.tenant"),
                        () -> UI.getCurrent().navigate("ims/tenants")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.PAPERPLANE,
                        I18n.t("ims.dashboard.quick.actions.register.application"),
                        () -> UI.getCurrent().navigate("ims/applications")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.GROUP,
                        I18n.t("ims.dashboard.quick.actions.add.customer"),
                        () -> UI.getCurrent().navigate("ims/customers")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.SHIELD,
                        I18n.t("ims.dashboard.quick.actions.assign.role"),
                        () -> UI.getCurrent().navigate("ims/roles"))
        );
        return new DashboardShortcutsBar(I18n.t("ims.dashboard.shortcuts.title"), shortcuts);
    }

    private H2 buildHeader() {
        H2 title = new H2(I18n.t("ims.dashboard.title"));
        title.addClassName("dashboard-title");
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        return title;
    }

    /**
     * The 5 real entity totals and 3 enrichment insights, unified into one
     * unbroken {@link StatCardGrid} (4 columns desktop / 3 tablet / 1 mobile)
     * instead of two visually different rows — this is the module's
     * 8-tile stat-card showcase.
     */
    private StatCardGrid buildStatsGrid() {
        accountsCard = new StatCard(VaadinIcon.USER, StatCard.Variant.PRIMARY,
                I18n.t("ims.dashboard.total.accounts"), null)
                .withNavigation(() -> ui.navigate("ims/accounts"));
        tenantsCard = new StatCard(VaadinIcon.BUILDING, StatCard.Variant.PRIMARY,
                I18n.t("ims.dashboard.total.tenants"), null)
                .withNavigation(() -> ui.navigate("ims/tenants"));
        applicationsCard = new StatCard(VaadinIcon.PAPERCLIP, StatCard.Variant.PRIMARY,
                I18n.t("ims.dashboard.total.applications"), null)
                .withNavigation(() -> ui.navigate("ims/applications"));
        customersCard = new StatCard(VaadinIcon.GROUP, StatCard.Variant.PRIMARY,
                I18n.t("ims.dashboard.total.customers"), null)
                .withNavigation(() -> ui.navigate("ims/customers"));
        rolesCard = new StatCard(VaadinIcon.SHIELD, StatCard.Variant.PRIMARY,
                I18n.t("ims.dashboard.total.roles"), null)
                .withNavigation(() -> ui.navigate("ims/roles"));

        // Enrichment insights: no cheap real data source exists yet for
        // month-over-month account growth, enabled/disabled ratios, or tenant
        // onboarding status, so these are plausible illustrative figures.
        StatCard newAccountsCard = new StatCard(VaadinIcon.TRENDING_UP, StatCard.Variant.SUCCESS,
                I18n.t("ims.dashboard.stats.new.accounts"), "+18")
                .withChange("+12%", StatCard.Trend.UP);
        StatCard activeRatioCard = new StatCard(VaadinIcon.CHECK_CIRCLE, StatCard.Variant.SUCCESS,
                I18n.t("ims.dashboard.stats.active.ratio"), "92%")
                .withChange("+3%", StatCard.Trend.UP);
        StatCard pendingTenantsCard = new StatCard(VaadinIcon.HOURGLASS, StatCard.Variant.WARNING,
                I18n.t("ims.dashboard.stats.pending.tenants"), "4")
                .withChange("-2%", StatCard.Trend.DOWN);

        return new StatCardGrid(accountsCard, tenantsCard, applicationsCard, customersCard, rolesCard,
                newAccountsCard, activeRatioCard, pendingTenantsCard);
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

    private void loadStatistics() {
        ui.access(() -> {
            try {
                setCount(accountsCard, accountService.findAll(0, 1).getBody());
                setCount(tenantsCard, tenantService.findAll(0, 1).getBody());
                setCount(applicationsCard, applicationService.findAll(0, 1).getBody());
                setCount(customersCard, customerService.findAll(0, 1).getBody());
                setCount(rolesCard, roleInfoService.findAll(0, 1).getBody());

                ui.push();
            } catch (Exception e) {
                log.error("Error fetching IMS dashboard statistics", e);
            }
        });
    }

    private void setCount(StatCard target, PaginatedResponseDto<?> body) {
        target.setValue(body != null ? String.valueOf(body.getTotalElements()) : "0");
    }
}
