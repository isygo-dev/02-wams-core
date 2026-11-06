package eu.isygoit.ui.sms;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.async.ContextAwareAsync;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.exception.ServiceOperationException;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.component.DashboardBreakdownPanel;
import eu.isygoit.ui.common.component.DashboardShortcutsBar;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.sms.layout.SmsMainLayout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dashboard view for SMS, displaying live storage configuration totals and
 * a provider-type breakdown.
 * Accessible at "/sms" and "/sms/home".
 */
@Slf4j
@RouteAlias(value = "sms/home", layout = SmsMainLayout.class)
@SpringComponent
@RouteScope
@Route(value = "sms", layout = SmsMainLayout.class)

public class SmsMainView extends ManagementVerticalView {
    private static final int PAGE_SIZE = 500;
    private final StorageConfigService storageConfigService;
    private final UI ui;
    private StatCard totalConfigsCard;
    private DashboardBreakdownPanel storageTypesPanel;
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
        add(buildShortcutsBar());
        add(buildStatsGrid());
        storageTypesPanel = new DashboardBreakdownPanel(
                I18n.t("sms.dashboard.configurations.by.type"));
        storageTypesPanel.setRefreshAction(this::loadStatistics);
        add(storageTypesPanel);

        loadStatistics();
    }

    @Override
    public String getPageTitle() {
        return I18n.t("sms.dashboard.title");
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
     * The one live total, with its provider breakdown shown below.
     */
    private StatCardGrid buildStatsGrid() {
        totalConfigsCard = new StatCard(VaadinIcon.DATABASE, StatCard.Variant.PRIMARY,
                I18n.t("sms.dashboard.total.configs"), null)
                .withNavigation(() -> ui.navigate("sms/storageconfigs"));
        return new StatCardGrid(totalConfigsCard);
    }

    private void loadStatistics() {
        storageTypesPanel.setLoading(true);
        ContextAwareAsync.supplyAsync(this::fetchDashboardData)
                .whenComplete((data, failure) -> ui.access(() -> {
                    if (failure != null) {
                        log.error("Failed to load storage configuration statistics", failure);
                        storageTypesPanel.setError(I18n.t("common.stats.load.error"));
                        ui.push();
                        return;
                    }

                    totalConfigsCard.setValue(String.valueOf(data.total()));
                    storageTypesPanel.setItems(data.countsByType().entrySet().stream()
                            .map(entry -> new DashboardBreakdownPanel.Item(entry.getKey(), entry.getValue()))
                            .toList());
                    ui.push();
                }));
    }

    private DashboardData fetchDashboardData() {
        ResponseEntity<PaginatedResponseDto<StorageConfigDto>> response =
                storageConfigService.findAll(0, PAGE_SIZE);
        PaginatedResponseDto<StorageConfigDto> body = response.getBody();
        if (body == null) {
            throw new ServiceOperationException("Storage configuration response did not contain data");
        }

        long total = body.getTotalElements();
        Map<String, Long> countsByType = new TreeMap<>();
        addTypeCounts(countsByType, body.getContent());
        long pageCount = (total + PAGE_SIZE - 1) / PAGE_SIZE;
        for (int page = 1; page < pageCount; page++) {
            PaginatedResponseDto<StorageConfigDto> nextPage =
                    storageConfigService.findAll(page, PAGE_SIZE).getBody();
            if (nextPage == null || nextPage.getContent() == null) {
                throw new ServiceOperationException("Storage configuration page " + page + " was empty");
            }
            addTypeCounts(countsByType, nextPage.getContent());
        }
        return new DashboardData(total, Map.copyOf(countsByType));
    }

    private void addTypeCounts(Map<String, Long> countsByType, List<StorageConfigDto> configs) {
        if (configs == null) {
            return;
        }
        configs.stream()
                .map(StorageConfigDto::getType)
                .filter(type -> type != null)
                .forEach(type -> countsByType.merge(I18n.enumLabel(type), 1L, Long::sum));
    }

    private record DashboardData(long total, Map<String, Long> countsByType) {
    }
}
