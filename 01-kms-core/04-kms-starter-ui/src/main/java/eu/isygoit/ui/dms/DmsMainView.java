package eu.isygoit.ui.dms;

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
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.ui.common.component.DashboardShortcutsBar;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.dms.layout.DmsMainLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Dashboard view for DMS, displaying key statistics, enrichment insights,
 * and a quick-shortcuts bar. Accessible at "/dms" and "/dms/home".
 */
@RouteAlias(value = "dms/home", layout = DmsMainLayout.class)
@UIScope
@Route(value = "dms", layout = DmsMainLayout.class)
@PageTitle("Document Dashboard")
public class DmsMainView extends ManagementVerticalView {

    private static final Logger log = LoggerFactory.getLogger(DmsMainView.class);

    private final CategoryService categoryService;
    private final UI ui;

    private StatCard categoriesCard;

    @Autowired
    public DmsMainView(CategoryService categoryService) {
        this.categoryService = categoryService;
        this.ui = UI.getCurrent();

        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("dms-dashboard");

        add(buildShortcutsBar());
        add(buildHeader());
        add(buildStatsGrid());
        add(buildRecentActivityPanel());

        loadStatistics();
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
     * The 3 core totals and 2 enrichment insights, unified into one
     * {@link StatCardGrid} (4 columns desktop / 3 tablet / 1 mobile).
     */
    private StatCardGrid buildStatsGrid() {
        categoriesCard = new StatCard(VaadinIcon.FOLDER, StatCard.Variant.PRIMARY,
                I18n.t("dms.dashboard.total.categories"), null)
                .withNavigation(() -> ui.navigate("dms/categories"));

        // "Total Files" / "Total Tags": no File/Tag entity or service exists yet in
        // this codebase (Category is the only implemented DMS entity), so these
        // remain illustrative placeholder figures rather than "0" dead values.
        StatCard filesCard = new StatCard(VaadinIcon.FILE, StatCard.Variant.PRIMARY,
                I18n.t("dms.dashboard.total.files"), "248")
                .withNavigation(() -> ui.navigate("dms/categories"));
        StatCard tagsCard = new StatCard(VaadinIcon.TAGS, StatCard.Variant.PRIMARY,
                I18n.t("dms.dashboard.total.tags"), "37")
                .withNavigation(() -> ui.navigate("dms/categories"));

        // Enrichment insights: Categories is the only DMS entity with a real
        // backing service today, so these remain plausible illustrative figures.
        StatCard newDocumentsCard = new StatCard(VaadinIcon.TRENDING_UP, StatCard.Variant.SUCCESS,
                I18n.t("dms.dashboard.stats.new.documents"), "+32")
                .withChange("+9%", StatCard.Trend.UP);
        StatCard topCategoryCard = new StatCard(VaadinIcon.STAR, StatCard.Variant.PRIMARY,
                I18n.t("dms.dashboard.stats.top.category"), I18n.t("dms.dashboard.stats.top.category.value"))
                .withChange("38%", StatCard.Trend.UP);

        return new StatCardGrid(categoriesCard, filesCard, tagsCard, newDocumentsCard, topCategoryCard);
    }

    private VerticalLayout buildRecentActivityPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.addClassName("wams-recent-activity-panel");
        H2 title = new H2(I18n.t("dms.dashboard.recent.activity"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        Div content = new Div();
        content.setText(I18n.t("dms.dashboard.recent.activity.description"));
        content.addClassName("wams-recent-activity-content");
        panel.add(title, content);
        return panel;
    }

    private void loadStatistics() {
        ui.access(() -> {
            try {
                PaginatedResponseDto<?> body = categoryService.findAll(0, 1).getBody();
                categoriesCard.setValue(body != null ? String.valueOf(body.getTotalElements()) : "0");

                ui.push();
            } catch (Exception e) {
                log.error("Error fetching DMS dashboard statistics", e);
            }
        });
    }
}
