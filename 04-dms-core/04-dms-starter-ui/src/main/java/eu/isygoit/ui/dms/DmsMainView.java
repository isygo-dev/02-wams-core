package eu.isygoit.ui.dms;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.async.ContextAwareAsync;
import eu.isygoit.dto.common.LinkedFileResponseDto;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.exception.ServiceOperationException;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.remote.dms.LinkedFileService;
import eu.isygoit.ui.common.component.DashboardBreakdownPanel;
import eu.isygoit.ui.common.component.DashboardShortcutsBar;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.dms.layout.DmsMainLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard view for DMS with live category/file totals and a document-library
 * overview. Accessible at "/dms" and "/dms/home".
 */
@RouteAlias(value = "dms/home", layout = DmsMainLayout.class)
@SpringComponent
@RouteScope
@Route(value = "dms", layout = DmsMainLayout.class)

public class DmsMainView extends ManagementVerticalView {
    private static final Logger log = LoggerFactory.getLogger(DmsMainView.class);
    private final CategoryService categoryService;
    private final LinkedFileService linkedFileService;
    private final UI ui;
    private StatCard categoriesCard;
    private StatCard filesCard;
    private DashboardBreakdownPanel inventoryPanel;
    @Autowired
    public DmsMainView(CategoryService categoryService, LinkedFileService linkedFileService) {
        this.categoryService = categoryService;
        this.linkedFileService = linkedFileService;
        this.ui = UI.getCurrent();

        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("dms-dashboard");

        add(buildHeader());
        add(buildShortcutsBar());
        add(buildStatsGrid());
        inventoryPanel = new DashboardBreakdownPanel(I18n.t("dms.dashboard.inventory"));
        inventoryPanel.setRefreshAction(this::loadStatistics);
        add(inventoryPanel);

        loadStatistics();
    }

    @Override
    public String getPageTitle() {
        return I18n.t("dms.dashboard.title");
    }

    private DashboardShortcutsBar buildShortcutsBar() {
        List<DashboardShortcutsBar.Shortcut> shortcuts = List.of(
                new DashboardShortcutsBar.Shortcut(VaadinIcon.PLUS,
                        I18n.t("dms.dashboard.quick.actions.create.category"),
                        () -> UI.getCurrent().navigate("dms/categories")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.UPLOAD,
                        I18n.t("dms.dashboard.quick.actions.upload.file"),
                        () -> UI.getCurrent().navigate("dms/categories")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.TAGS,
                        I18n.t("dms.dashboard.quick.actions.create.tag"),
                        () -> UI.getCurrent().navigate("dms/categories")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.LINK,
                        I18n.t("dms.dashboard.quick.actions.link.file.category"),
                        () -> UI.getCurrent().navigate("dms/categories"))
        );
        return new DashboardShortcutsBar(I18n.t("dms.dashboard.shortcuts.title"), shortcuts);
    }

    private H2 buildHeader() {
        H2 title = new H2(I18n.t("dms.dashboard.title"));
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        title.addClassName("wams-dashboard-header");
        return title;
    }

    /**
     * The real category and linked-file totals in a responsive shared grid.
     */
    private StatCardGrid buildStatsGrid() {
        categoriesCard = new StatCard(VaadinIcon.FOLDER, StatCard.Variant.PRIMARY,
                I18n.t("dms.dashboard.total.categories"), null)
                .withNavigation(() -> ui.navigate("dms/categories"));
        filesCard = new StatCard(VaadinIcon.FILE, StatCard.Variant.PRIMARY,
                I18n.t("dms.dashboard.total.files"), null)
                .withNavigation(() -> ui.navigate("dms/linked-files"));
        return new StatCardGrid(categoriesCard, filesCard);
    }

    private void loadStatistics() {
        inventoryPanel.setLoading(true);
        ContextAwareAsync.supplyAsync(this::fetchDashboardData)
                .whenComplete((data, failure) -> ui.access(() -> {
                    if (failure != null) {
                        log.error("Error fetching DMS dashboard statistics", failure);
                        inventoryPanel.setError(I18n.t("common.stats.load.error"));
                        ui.push();
                        return;
                    }

                    categoriesCard.setValue(String.valueOf(data.categories()));
                    filesCard.setValue(String.valueOf(data.files()));
                    inventoryPanel.setItems(List.of(
                            new DashboardBreakdownPanel.Item(
                                    I18n.t("dms.dashboard.total.categories"), data.categories()),
                            new DashboardBreakdownPanel.Item(
                                    I18n.t("dms.dashboard.total.files"), data.files())
                    ));
                    ui.push();
                }));
    }

    private DashboardData fetchDashboardData() {
        long categories = count(categoryService.findAll(0, 1).getBody());
        Map<String, LinkedFileResponseDto> linkedFiles = new LinkedHashMap<>();
        addFiles(linkedFiles, linkedFileService.searchByCategories(List.of("")).getBody());
        addFiles(linkedFiles, linkedFileService.searchByTags(List.of("")).getBody());
        return new DashboardData(categories, linkedFiles.size());
    }

    private void addFiles(Map<String, LinkedFileResponseDto> files, List<LinkedFileResponseDto> results) {
        if (results == null) {
            throw new ServiceOperationException("Linked-file search did not return a result list");
        }
        for (LinkedFileResponseDto file : results) {
            if (file.getCode() == null || file.getCode().isBlank()) {
                throw new ServiceOperationException("Linked-file result did not include a business code");
            }
            files.putIfAbsent(file.getCode(), file);
        }
    }

    private long count(PaginatedResponseDto<?> body) {
        if (body == null) {
            throw new ServiceOperationException("DMS category response did not contain data");
        }
        return body.getTotalElements();
    }

    private record DashboardData(long categories, long files) {
    }
}
