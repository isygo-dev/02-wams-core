package eu.isygoit.ui.cms;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.async.ContextAwareAsync;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.common.component.DashboardBreakdownPanel;
import eu.isygoit.ui.common.component.DashboardShortcutsBar;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.cms.layout.CmsMainLayout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Dashboard view for CMS, displaying live calendar totals and lock-state
 * metrics.
 * Accessible at "/cms" and "/cms/home".
 */
@Slf4j
@RouteAlias(value = "cms/home", layout = CmsMainLayout.class)
@VaadinSessionScope
@Route(value = "cms", layout = CmsMainLayout.class)
@PageTitle("CMS Dashboard")
public class CmsMainView extends ManagementVerticalView {

    private static final int PAGE_SIZE = 500;

    private final VCalendarService calendarService;
    private final UI ui;

    private StatCard totalCalendarsCard;
    private StatCard lockedCalendarsCard;
    private StatCard unlockedCalendarsCard;
    private DashboardBreakdownPanel calendarStatusPanel;

    @Autowired
    public CmsMainView(VCalendarService calendarService) {
        this.calendarService = calendarService;
        this.ui = UI.getCurrent();

        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("cms-dashboard");

        add(buildHeader());
        add(buildShortcutsBar());
        add(buildStatsGrid());
        calendarStatusPanel = new DashboardBreakdownPanel(
                I18n.t("cms.dashboard.stats.locked.breakdown"));
        calendarStatusPanel.setRefreshAction(this::loadStatistics);
        add(calendarStatusPanel);

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
                        () -> ui.navigate("cms/calendar"))
        );
        return new DashboardShortcutsBar(I18n.t("cms.dashboard.quick.actions"), shortcuts);
    }

    private H2 buildHeader() {
        H2 title = new H2(I18n.t("cms.dashboard.title"));
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        title.addClassName("wams-dashboard-header");
        return title;
    }

    /** Calendar totals and their live lock-state breakdown. */
    private StatCardGrid buildStatsGrid() {
        totalCalendarsCard = new StatCard(VaadinIcon.CALENDAR, StatCard.Variant.PRIMARY,
                I18n.t("cms.dashboard.total.calendars"), null)
                .withNavigation(() -> ui.navigate("cms/calendars"));
        lockedCalendarsCard = new StatCard(VaadinIcon.LOCK, StatCard.Variant.WARNING,
                I18n.t("cms.calendar.card.status.locked"), null)
                .withNavigation(() -> ui.navigate("cms/calendars"));
        unlockedCalendarsCard = new StatCard(VaadinIcon.UNLOCK, StatCard.Variant.SUCCESS,
                I18n.t("cms.calendar.card.status.unlocked"), null)
                .withNavigation(() -> ui.navigate("cms/calendars"));
        return new StatCardGrid(totalCalendarsCard, lockedCalendarsCard, unlockedCalendarsCard);
    }

    private void loadStatistics() {
        calendarStatusPanel.setLoading(true);
        ContextAwareAsync.supplyAsync(this::fetchDashboardData)
                .whenComplete((data, failure) -> ui.access(() -> {
                    if (failure != null) {
                        log.error("Failed to load calendar lock-state statistics", failure);
                        calendarStatusPanel.setError(I18n.t("common.stats.load.error"));
                        ui.push();
                        return;
                    }

                    totalCalendarsCard.setValue(String.valueOf(data.total()));
                    lockedCalendarsCard.setValue(String.valueOf(data.locked()));
                    unlockedCalendarsCard.setValue(String.valueOf(data.unlocked()));
                    calendarStatusPanel.setItems(List.of(
                            new DashboardBreakdownPanel.Item(
                                    I18n.t("cms.calendar.card.status.locked"), data.locked()),
                            new DashboardBreakdownPanel.Item(
                                    I18n.t("cms.calendar.card.status.unlocked"), data.unlocked())
                    ));
                    ui.push();
                }));
    }

    private DashboardData fetchDashboardData() {
        ResponseEntity<PaginatedResponseDto<VCalendarDto>> response =
                calendarService.findAll(0, PAGE_SIZE);
        PaginatedResponseDto<VCalendarDto> body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Calendar response did not contain data");
        }

        long total = body.getTotalElements();
        long locked = 0;
        long unlocked = 0;
        List<VCalendarDto> calendars = body.getContent() != null ? body.getContent() : List.of();
        for (VCalendarDto calendar : calendars) {
            if (Boolean.TRUE.equals(calendar.getLocked())) {
                locked++;
            } else {
                unlocked++;
            }
        }
        long pageCount = (total + PAGE_SIZE - 1) / PAGE_SIZE;
        for (int page = 1; page < pageCount; page++) {
            PaginatedResponseDto<VCalendarDto> nextPage =
                    calendarService.findAll(page, PAGE_SIZE).getBody();
            if (nextPage == null || nextPage.getContent() == null) {
                throw new IllegalStateException("Calendar page " + page + " was empty");
            }
            for (VCalendarDto calendar : nextPage.getContent()) {
                if (Boolean.TRUE.equals(calendar.getLocked())) {
                    locked++;
                } else {
                    unlocked++;
                }
            }
        }
        return new DashboardData(total, locked, unlocked);
    }

    private record DashboardData(long total, long locked, long unlocked) {
    }
}
