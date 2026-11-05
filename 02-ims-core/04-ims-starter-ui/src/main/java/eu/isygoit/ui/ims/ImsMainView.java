package eu.isygoit.ui.ims;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.async.ContextAwareAsync;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.AccountGlobalStatDto;
import eu.isygoit.enums.IEnumSharedStatType;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.*;
import eu.isygoit.ui.common.component.DashboardBreakdownPanel;
import eu.isygoit.ui.common.component.DashboardShortcutsBar;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dashboard view for IMS with live entity totals, account-status metrics,
 * and quick shortcuts. Accessible at "/ims" and "/ims/home".
 */
@RouteAlias(value = "ims/home", layout = ImsMainLayout.class)
@SpringComponent
@RouteScope
@Route(value = "ims", layout = ImsMainLayout.class)
public class ImsMainView extends ManagementVerticalView {

    private static final Logger log = LoggerFactory.getLogger(ImsMainView.class);

    private final AccountService accountService;
    private final TenantService tenantService;
    private final ApplicationService applicationService;
    private final CustomerService customerService;
    private final RoleInfoService roleInfoService;
    private final RegisteredUserService registeredUserService;
    private final AccountStatisticsService accountStatisticsService;
    private final UI ui;
    private final AtomicLong loadGeneration = new AtomicLong();

    private final DashboardBreakdownPanel accountSnapshotPanel;
    private StatCard accountsCard;
    private StatCard tenantsCard;
    private StatCard applicationsCard;
    private StatCard customersCard;
    private StatCard rolesCard;
    private StatCard registeredUsersCard;

    @Override
    public String getPageTitle() {
        return I18n.t("ims.dashboard.title");
    }

    @Autowired
    public ImsMainView(AccountService accountService,
                       TenantService tenantService,
                       ApplicationService applicationService,
                       CustomerService customerService,
                       RoleInfoService roleInfoService,
                       RegisteredUserService registeredUserService,
                       AccountStatisticsService accountStatisticsService) {
        this.accountService = accountService;
        this.tenantService = tenantService;
        this.applicationService = applicationService;
        this.customerService = customerService;
        this.roleInfoService = roleInfoService;
        this.registeredUserService = registeredUserService;
        this.accountStatisticsService = accountStatisticsService;
        this.ui = UI.getCurrent();

        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("ims-dashboard");

        add(buildHeader());
        add(buildShortcutsBar());
        add(buildStatsGrid());
        accountSnapshotPanel = new DashboardBreakdownPanel(I18n.t("ims.dashboard.accounts.snapshot"));
        accountSnapshotPanel.setRefreshAction(this::loadStatistics);
        add(accountSnapshotPanel);

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
                        () -> UI.getCurrent().navigate("ims/roles")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.CLIPBOARD_USER,
                        I18n.t("ims.dashboard.quick.actions.add.registered.user"),
                        () -> UI.getCurrent().navigate("ims/registered-users"))
        );
        return new DashboardShortcutsBar(I18n.t("ims.dashboard.shortcuts.title"), shortcuts);
    }

    private H2 buildHeader() {
        H2 title = new H2(I18n.t("ims.dashboard.title"));
        title.addClassName("wams-dashboard-header");
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        return title;
    }

    /** The six live entity totals in a responsive shared grid. */
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
        registeredUsersCard = new StatCard(VaadinIcon.CLIPBOARD_USER, StatCard.Variant.PRIMARY,
                I18n.t("ims.dashboard.total.registeredUsers"), null)
                .withNavigation(() -> ui.navigate("ims/registered-users"));

        return new StatCardGrid(accountsCard, tenantsCard, applicationsCard, customersCard, rolesCard,
                registeredUsersCard);
    }

    private void loadStatistics() {
        long generation = loadGeneration.incrementAndGet();
        accountSnapshotPanel.setLoading(true);
        ContextAwareAsync.supplyAsync(() -> new DashboardCounts(
                count(accountService.findAll(0, 1).getBody()),
                count(tenantService.findAll(0, 1).getBody()),
                count(applicationService.findAll(0, 1).getBody()),
                count(customerService.findAll(0, 1).getBody()),
                count(roleInfoService.findAll(0, 1).getBody()),
                count(registeredUserService.findAll(0, 1).getBody())
        )).whenComplete((data, failure) -> ui.access(() -> {
            if (generation != loadGeneration.get()) {
                return;
            }
            if (failure != null) {
                log.error("Error fetching IMS dashboard statistics", failure);
                ui.push();
                return;
            }

            accountsCard.setValue(String.valueOf(data.accounts()));
            tenantsCard.setValue(String.valueOf(data.tenants()));
            applicationsCard.setValue(String.valueOf(data.applications()));
            customersCard.setValue(String.valueOf(data.customers()));
            rolesCard.setValue(String.valueOf(data.roles()));
            registeredUsersCard.setValue(String.valueOf(data.registeredUsers()));
            ui.push();
        }));

        ContextAwareAsync.supplyAsync(() ->
                        accountStatisticsService.getGlobalStatistics(IEnumSharedStatType.Types.TOTAL_COUNT))
                .whenComplete((accountStats, failure) -> ui.access(() -> {
                    if (generation != loadGeneration.get()) {
                        return;
                    }
                    if (failure != null) {
                        log.error("Error fetching IMS account overview statistics", failure);
                        accountSnapshotPanel.setError(I18n.t("common.stats.load.error"));
                    } else if (accountStats == null) {
                        accountSnapshotPanel.setError(I18n.t("common.stats.load.error"));
                    } else {
                        accountSnapshotPanel.setItems(List.of(
                                new DashboardBreakdownPanel.Item(I18n.t("ims.dashboard.accounts.active"),
                                        valueOrZero(accountStats.getActiveCount())),
                                new DashboardBreakdownPanel.Item(I18n.t("ims.dashboard.accounts.confirmed"),
                                        valueOrZero(accountStats.getConfirmedCount())),
                                new DashboardBreakdownPanel.Item(I18n.t("ims.dashboard.accounts.admins"),
                                        valueOrZero(accountStats.getAdminsCount()))
                        ));
                    }
                    ui.push();
                }));
    }

    private long count(PaginatedResponseDto<?> body) {
        if (body == null) {
            throw new IllegalStateException("IMS dashboard count response did not contain data");
        }
        return body.getTotalElements();
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0;
    }

    private record DashboardCounts(long accounts, long tenants, long applications,
                                   long customers, long roles, long registeredUsers) {
    }
}
