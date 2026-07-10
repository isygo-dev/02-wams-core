package eu.isygoit.ui.cms;

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
import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.cms.layout.CmsMainLayout;
import eu.isygoit.ui.common.component.DashboardShortcutsBar;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Dashboard view for CMS, displaying key statistics, a favorites/shortcuts
 * quick-action bar and recent activity.
 * Accessible at "/cms" and "/cms/home".
 */
@Slf4j
@RouteAlias(value = "cms/home", layout = CmsMainLayout.class)
@UIScope
@Route(value = "cms", layout = CmsMainLayout.class)
@PageTitle("CMS Dashboard")
public class CmsMainView extends ManagementVerticalView {

    private final VCalendarService calendarService;
    private final UI ui;

    private StatCard totalCalendarsCard;
    private StatCard lockedBreakdownCard;

    @Autowired
    public CmsMainView(VCalendarService calendarService) {
        this.calendarService = calendarService;
        this.ui = UI.getCurrent();

        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("cms-dashboard");

        add(buildShortcutsBar());
        add(buildHeader());
        add(buildStatsGrid());
        add(buildRecentActivityPanel());

        loadStatistics();
    }

    private DashboardShortcutsBar buildShortcutsBar() {
        List<DashboardShortcutsBar.Shortcut> shortcuts = List.of(
                new DashboardShortcutsBar.Shortcut(VaadinIcon.PLUS_CIRCLE,
                        I18n.t("cms.dashboard.quick.actions.create.calendar"),
                        () -> ui.navigate("cms/calendars")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.CALENDAR_CLOCK,
                        I18n.t("cms.dashboard.quick.actions.create.event"),
                        () -> ui.navigate("cms/calendars")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.UPLOAD,
                        I18n.t("cms.dashboard.quick.actions.import.ics"),
                        () -> ui.navigate("cms/calendars")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.CLOCK,
                        I18n.t("cms.dashboard.quick.actions.view.schedule"),
                        () -> ui.navigate("cms/calendars"))
        );
        return new DashboardShortcutsBar(I18n.t("cms.dashboard.quick.actions"), shortcuts);
    }

    private H2 buildHeader() {
        H2 title = new H2(I18n.t("cms.dashboard.title"));
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        title.addClassName("wams-dashboard-header");
        return title;
    }

    /**
     * The 3 core totals and 2 enrichment insights, unified into one
     * {@link StatCardGrid} (4 columns desktop / 3 tablet / 1 mobile).
     */
    private StatCardGrid buildStatsGrid() {
        totalCalendarsCard = new StatCard(VaadinIcon.CALENDAR, StatCard.Variant.PRIMARY,
                I18n.t("cms.dashboard.total.calendars"), null)
                .withNavigation(() -> ui.navigate("cms/calendars"));

        // "Total Events" / "Total Schedules": only the calendar (VCalendar) view
        // is registered in this module - no separate event/schedule views/services
        // exist yet, so these remain illustrative placeholder figures.
        StatCard eventsCard = new StatCard(VaadinIcon.CALENDAR_CLOCK, StatCard.Variant.PRIMARY,
                I18n.t("cms.dashboard.total.events"), "42")
                .withNavigation(() -> ui.navigate("cms/calendars"));
        StatCard schedulesCard = new StatCard(VaadinIcon.CLOCK, StatCard.Variant.PRIMARY,
                I18n.t("cms.dashboard.total.schedules"), "15")
                .withNavigation(() -> ui.navigate("cms/calendars"));

        StatCard upcomingWeekCard = new StatCard(VaadinIcon.BELL, StatCard.Variant.SUCCESS,
                I18n.t("cms.dashboard.stats.upcoming.week"), "6")
                .withChange("+2", StatCard.Trend.UP);

        // Real data: locked/unlocked split computed from the fetched calendar
        // list (VCalendarDto#getLocked()) - no fake change indicator, since
        // it's a live figure with no meaningful "trend" to show.
        lockedBreakdownCard = new StatCard(VaadinIcon.LOCK, StatCard.Variant.NEUTRAL,
                I18n.t("cms.dashboard.stats.locked.breakdown"), null);

        return new StatCardGrid(totalCalendarsCard, eventsCard, schedulesCard, upcomingWeekCard, lockedBreakdownCard);
    }

    private VerticalLayout buildRecentActivityPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.addClassName("wams-recent-activity-panel");
        H2 title = new H2(I18n.t("cms.dashboard.recent.activity"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        Div content = new Div();
        content.setText(I18n.t("cms.dashboard.recent.activity.description"));
        content.addClassName("wams-recent-activity-content");
        panel.add(title, content);
        return panel;
    }

    private void loadStatistics() {
        try {
            ResponseEntity<PaginatedResponseDto<VCalendarDto>> response = calendarService.findAll(0, 1);
            PaginatedResponseDto<VCalendarDto> body = response.getBody();
            long total = body != null ? body.getTotalElements() : 0;
            ui.access(() -> totalCalendarsCard.setValue(String.valueOf(total)));
        } catch (Exception e) {
            log.error("Failed to load calendar statistics", e);
            ui.access(() -> totalCalendarsCard.setValue("0"));
        }

        try {
            // Dashboard-scale sample: enough to give a representative locked/unlocked
            // split without requesting the entire table.
            ResponseEntity<PaginatedResponseDto<VCalendarDto>> response = calendarService.findAll(0, 500);
            PaginatedResponseDto<VCalendarDto> body = response.getBody();
            List<VCalendarDto> calendars = body != null && body.getContent() != null ? body.getContent() : List.of();
            long locked = calendars.stream().filter(c -> Boolean.TRUE.equals(c.getLocked())).count();
            long unlocked = calendars.size() - locked;
            ui.access(() -> lockedBreakdownCard.setValue(locked + " / " + unlocked));
        } catch (Exception e) {
            log.error("Failed to load calendar locked/unlocked breakdown", e);
            ui.access(() -> lockedBreakdownCard.setValue("0 / 0"));
        }
    }
}
