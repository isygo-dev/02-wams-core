package eu.isygoit.ui.dms;

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
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.dms.layout.DmsMainLayout;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Dashboard view for DMS, displaying key statistics and quick actions.
 * Accessible at "/dms" and "/dms/home".
 */
@RouteAlias(value = "dms/home", layout = DmsMainLayout.class)
@UIScope
@Route(value = "dms", layout = DmsMainLayout.class)
@PageTitle("Document Dashboard")
public class DmsMainView extends ManagementVerticalView {

    private final CategoryService categoryService;
    private final UI ui;

    @Autowired
    public DmsMainView(CategoryService categoryService) {
        this.categoryService = categoryService;
        this.ui = UI.getCurrent();

        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("dms-dashboard");

        add(buildHeader());
        add(buildStatisticsRow());
        add(buildRecentActivityPanel());
        add(buildQuickActions());
    }

    private H2 buildHeader() {
        H2 title = new H2(I18n.t("dms.dashboard.title"));
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        title.addClassName("wams-dashboard-header");
        return title;
    }

    private HorizontalLayout buildStatisticsRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(true);
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);
        row.addClassName("stats-row");

        row.add(createStatCard(I18n.t("dms.dashboard.total.categories"), "0", VaadinIcon.FOLDER, "categories-link"));
        row.add(createStatCard(I18n.t("dms.dashboard.total.files"), "0", VaadinIcon.FILE, "files-link"));
        row.add(createStatCard(I18n.t("dms.dashboard.total.tags"), "0", VaadinIcon.TAGS, "tags-link"));

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
        panel.addClassName("wams-recent-activity-panel");
        H2 title = new H2(I18n.t("dms.dashboard.recent.activity"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        Div content = new Div();
        content.setText(I18n.t("dms.dashboard.recent.activity.description"));
        content.addClassName("wams-recent-activity-content");
        panel.add(title, content);
        return panel;
    }

    private VerticalLayout buildQuickActions() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.addClassName("wams-quick-actions");
        H2 title = new H2(I18n.t("dms.dashboard.quick.actions"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        Div actions = new Div();
        StringBuilder sb = new StringBuilder();
        sb.append("• ").append(I18n.t("dms.dashboard.quick.actions.create.category")).append("\n");
        sb.append("• ").append(I18n.t("dms.dashboard.quick.actions.upload.file")).append("\n");
        sb.append("• ").append(I18n.t("dms.dashboard.quick.actions.create.tag")).append("\n");
        sb.append("• ").append(I18n.t("dms.dashboard.quick.actions.link.file.category"));
        actions.add(new Span(sb.toString()));
        actions.addClassName("wams-quick-actions-text");
        layout.add(title, actions);
        return layout;
    }
}