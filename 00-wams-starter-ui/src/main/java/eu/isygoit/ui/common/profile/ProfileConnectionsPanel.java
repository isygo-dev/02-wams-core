package eu.isygoit.ui.common.profile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.api.ProfileServiceApi;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.dto.data.ConnectionPageDto;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.dto.data.DailyLoginCountDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

/**
 * Connections tab: session-analytics mini-stats, a 7/30-day login-activity chart, and the recent sessions list.
 * "Connections" here means tracked login sessions ({@link ConnectionTrackingDto}), the only connection-shaped
 * data this domain has — not a social graph.
 *
 * <p>Nothing is loaded in bulk: the chart and counters use one bounded 30-day aggregate from the server, and the
 * sessions list is fetched one page at a time.</p>
 */
@Slf4j
class ProfileConnectionsPanel extends VerticalLayout {

    static final int CHART_DAYS = 30;
    static final int PAGE_SIZE = 5;

    private final transient ProfileServiceApi profileService;
    private final Div sessionsBody = new Div();

    ProfileConnectionsPanel(AccountStatDto stats, ProfileServiceApi profileService, Runnable onViewAllActivity) {
        this.profileService = profileService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        addClassName("profile-tab-panel");

        List<DailyLoginCountDto> activity = loadActivity();

        StatCardGrid miniStats = new StatCardGrid(
                new StatCard(VaadinIcon.SIGN_IN, StatCard.Variant.PRIMARY,
                        I18n.t("profile.connections.stat.total"),
                        String.valueOf(ProfileFormatUtils.totalSessions(stats))),
                new StatCard(VaadinIcon.CALENDAR_CLOCK, StatCard.Variant.SUCCESS,
                        I18n.t("profile.connections.stat.week"),
                        String.valueOf(sumLastDays(activity, 7))),
                new StatCard(VaadinIcon.CALENDAR, StatCard.Variant.NEUTRAL,
                        I18n.t("profile.connections.stat.month"),
                        String.valueOf(sumLastDays(activity, 30)))
        );
        miniStats.addClassName("profile-mini-stat-grid");

        add(miniStats, buildChartCard(activity), buildRecentSessionsCard(onViewAllActivity));
        loadSessions(0);
    }

    private static long sumLastDays(List<DailyLoginCountDto> activity, int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        return activity.stream()
                .filter(d -> d.getDate() != null && !d.getDate().isBefore(from))
                .mapToLong(DailyLoginCountDto::getCount)
                .sum();
    }

    /**
     * Applies LUMO theme variants + aria-pressed so the active window is obvious.
     */
    private static void applyToggleState(Button btn7, Button btn30, int activeDays) {
        boolean seven = activeDays == 7;

        btn7.removeThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_TERTIARY);
        btn30.removeThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_TERTIARY);

        btn7.addThemeVariants(ButtonVariant.LUMO_SMALL,
                seven ? ButtonVariant.LUMO_PRIMARY : ButtonVariant.LUMO_TERTIARY);
        btn30.addThemeVariants(ButtonVariant.LUMO_SMALL,
                seven ? ButtonVariant.LUMO_TERTIARY : ButtonVariant.LUMO_PRIMARY);

        btn7.getElement().setAttribute("aria-pressed", String.valueOf(seven));
        btn30.getElement().setAttribute("aria-pressed", String.valueOf(!seven));
    }    private final ProfilePagerBar sessionsPager = new ProfilePagerBar(this::loadSessions);

    private List<DailyLoginCountDto> loadActivity() {
        try {
            ResponseEntity<List<DailyLoginCountDto>> response = profileService.getLoginActivity(CHART_DAYS);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Failed to load login activity", e);
        }
        return List.of();
    }

    private Div buildChartCard(List<DailyLoginCountDto> activity) {
        Div chartCard = new Div();
        chartCard.addClassName("profile-section-card");

        H3 chartTitle = new H3(I18n.t("profile.connections.chart.title"));
        chartTitle.addClassName("section-title");

        // Single chart instance, reused across toggles.
        LoginActivityChart chart = new LoginActivityChart(activity, 7);

        Div chartHolder = new Div();
        chartHolder.addClassName("profile-chart-holder");
        chartHolder.add(chart);

        Button btn7 = new Button(I18n.t("profile.connections.chart.7d"));
        Button btn30 = new Button(I18n.t("profile.connections.chart.30d"));
        btn7.addClassName("profile-chart-toggle-btn");
        btn30.addClassName("profile-chart-toggle-btn");

        applyToggleState(btn7, btn30, 7);

        btn7.addClickListener(e -> {
            chart.update(activity, 7);
            applyToggleState(btn7, btn30, 7);
        });
        btn30.addClickListener(e -> {
            chart.update(activity, 30);
            applyToggleState(btn7, btn30, 30);
        });

        HorizontalLayout toggleRow = new HorizontalLayout(btn7, btn30);
        toggleRow.addClassName("profile-chart-toggle-row");
        toggleRow.setSpacing(true);

        HorizontalLayout chartHeader = new HorizontalLayout(chartTitle, toggleRow);
        chartHeader.setWidthFull();
        chartHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        chartHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        chartHeader.addClassName("profile-chart-header");

        chartCard.add(chartHeader, chartHolder);
        return chartCard;
    }

    private Div buildRecentSessionsCard(Runnable onViewAllActivity) {
        Div listCard = new Div();
        listCard.addClassName("profile-section-card");

        H3 listTitle = new H3(I18n.t("profile.connections.recent.title"));
        listTitle.addClassName("section-title");

        sessionsBody.addClassName("profile-connections-list");

        Button viewAll = new Button(I18n.t("profile.connections.viewAll"),
                VaadinIcon.ARROW_RIGHT.create());
        viewAll.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        viewAll.addClassName("profile-view-all-btn");
        viewAll.addClickListener(e -> onViewAllActivity.run());

        listCard.add(listTitle, sessionsBody, sessionsPager, viewAll);
        return listCard;
    }

    private void loadSessions(int page) {
        ConnectionPageDto result = fetchSessions(page);
        sessionsBody.removeAll();
        if (result == null) {
            sessionsPager.update(0, 0, 0);
            Button retry = new Button(I18n.t("profile.load.retry"));
            retry.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            retry.addClickListener(e -> loadSessions(page));
            Div error = new Div(new Span(I18n.t("profile.activity.load.error")), retry);
            error.addClassName("profile-load-error");
            sessionsBody.add(error);
            return;
        }

        sessionsPager.update(result.getPage(), result.getTotalPages(), result.getTotalElements());
        if (result.getContent() == null || result.getContent().isEmpty()) {
            sessionsBody.add(new ProfileEmptyState(I18n.t("profile.history.empty")));
        } else {
            result.getContent().forEach(c -> sessionsBody.add(new ConnectionCard(c)));
        }
    }

    private ConnectionPageDto fetchSessions(int page) {
        try {
            ResponseEntity<ConnectionPageDto> response = profileService.getConnections(page, PAGE_SIZE);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Failed to load sessions page {}", page, e);
        }
        return null;
    }




}
