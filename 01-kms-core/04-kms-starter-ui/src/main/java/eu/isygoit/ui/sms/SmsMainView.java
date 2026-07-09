package eu.isygoit.ui.sms;

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
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.component.DashboardShortcutsBar;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.sms.layout.SmsMainLayout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Dashboard view for SMS, displaying key statistics, a favorites/shortcuts
 * quick-action bar and recent activity.
 * Accessible at "/sms" and "/sms/home".
 */
@Slf4j
@RouteAlias(value = "sms/home", layout = SmsMainLayout.class)
@UIScope
@Route(value = "sms", layout = SmsMainLayout.class)
@PageTitle("Storage Management Dashboard")
public class SmsMainView extends ManagementVerticalView {

    private final StorageConfigService storageConfigService;
    private final UI ui;

    private StatCard totalConfigsCard;

    @Autowired
    public SmsMainView(StorageConfigService storageConfigService) {
        this.storageConfigService = storageConfigService;
        this.ui = UI.getCurrent();

        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("sms-dashboard");

        add(buildShortcutsBar());
        add(buildHeader());
        add(buildStatsGrid());
        add(buildRecentActivityPanel());

        loadStatistics();
    }

    private DashboardShortcutsBar buildShortcutsBar() {
        List<DashboardShortcutsBar.Shortcut> shortcuts = List.of(
                new DashboardShortcutsBar.Shortcut(VaadinIcon.PLUS_CIRCLE,
                        I18n.t("sms.dashboard.quick.actions.add.storage"),
                        () -> ui.navigate("sms/storageconfigs")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.FOLDER_ADD,
                        I18n.t("sms.dashboard.quick.actions.create.bucket"),
                        () -> ui.navigate("sms/storageconfigs")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.UPLOAD,
                        I18n.t("sms.dashboard.quick.actions.upload.file"),
                        () -> ui.navigate("sms/storageconfigs")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.BAR_CHART,
                        I18n.t("sms.dashboard.quick.actions.view.usage"),
                        () -> ui.navigate("sms/storageconfigs"))
        );
        return new DashboardShortcutsBar(I18n.t("sms.dashboard.quick.actions"), shortcuts);
    }

    private H2 buildHeader() {
        H2 title = new H2(I18n.t("sms.dashboard.title"));
        title.addClassName("wams-dashboard-header");
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        return title;
    }

    /**
     * The 3 core totals and 3 enrichment insights, unified into one
     * {@link StatCardGrid} (4 columns desktop / 3 tablet / 1 mobile).
     */
    private StatCardGrid buildStatsGrid() {
        totalConfigsCard = new StatCard(VaadinIcon.DATABASE, StatCard.Variant.PRIMARY,
                I18n.t("sms.dashboard.total.configs"), null)
                .withNavigation(() -> ui.navigate("sms/storageconfigs"));

        // "Total Buckets" / "Total Storage Used": no bucket/file entity exists
        // yet in this codebase (storage config is the only implemented entity),
        // so these remain illustrative placeholder figures.
        StatCard bucketsCard = new StatCard(VaadinIcon.FOLDER_O, StatCard.Variant.PRIMARY,
                I18n.t("sms.dashboard.total.buckets"), "18")
                .withNavigation(() -> ui.navigate("sms/storageconfigs"));
        StatCard storageUsedCard = new StatCard(VaadinIcon.HARDDRIVE, StatCard.Variant.PRIMARY,
                I18n.t("sms.dashboard.total.storage.used"), "2.4 TB")
                .withNavigation(() -> ui.navigate("sms/storageconfigs"));

        StatCard s3ConfigsCard = new StatCard(VaadinIcon.CLOUD, StatCard.Variant.SUCCESS,
                I18n.t("sms.dashboard.stats.s3.configs"), "11")
                .withChange("+2", StatCard.Trend.UP);
        StatCard localConfigsCard = new StatCard(VaadinIcon.HARDDRIVE, StatCard.Variant.SUCCESS,
                I18n.t("sms.dashboard.stats.local.configs"), "7")
                .withChange("+1", StatCard.Trend.UP);
        StatCard connectionHealthCard = new StatCard(VaadinIcon.HEART, StatCard.Variant.SUCCESS,
                I18n.t("sms.dashboard.stats.connection.health"), "98.2%")
                .withChange("+0.4%", StatCard.Trend.UP);

        return new StatCardGrid(totalConfigsCard, bucketsCard, storageUsedCard,
                s3ConfigsCard, localConfigsCard, connectionHealthCard);
    }

    private VerticalLayout buildRecentActivityPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.addClassName("wams-recent-activity-panel");
        H2 title = new H2(I18n.t("sms.dashboard.recent.activity"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        Div content = new Div();
        content.setText(I18n.t("sms.dashboard.recent.activity.description"));
        content.addClassName("wams-recent-activity-content");
        panel.add(title, content);
        return panel;
    }

    private void loadStatistics() {
        try {
            ResponseEntity<PaginatedResponseDto<StorageConfigDto>> response = storageConfigService.findAll(0, 1);
            PaginatedResponseDto<StorageConfigDto> body = response.getBody();
            long total = body != null ? body.getTotalElements() : 0;
            ui.access(() -> totalConfigsCard.setValue(String.valueOf(total)));
        } catch (Exception e) {
            log.error("Failed to load storage configuration statistics", e);
            ui.access(() -> totalConfigsCard.setValue("0"));
        }
    }
}
