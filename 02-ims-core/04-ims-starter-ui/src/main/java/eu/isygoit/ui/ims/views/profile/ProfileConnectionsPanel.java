package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Connections tab: session-analytics mini-stats, a 7/30-day login-activity
 * chart, and a condensed recent-sessions list. "Connections" here means
 * tracked login sessions ({@link ConnectionTrackingDto}), the only
 * connection-shaped data this domain has — not a social graph.
 */
class ProfileConnectionsPanel extends VerticalLayout {

    ProfileConnectionsPanel(AccountDto account, AccountStatDto stats, List<ConnectionTrackingDto> history, Runnable onViewAllActivity) {
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        addClassName("profile-tab-panel");

        StatCardGrid miniStats = new StatCardGrid(
                new StatCard(VaadinIcon.SIGN_IN, StatCard.Variant.PRIMARY,
                        I18n.t("profile.connections.stat.total"), String.valueOf(ProfileFormatUtils.totalSessions(account, stats))),
                new StatCard(VaadinIcon.CALENDAR_CLOCK, StatCard.Variant.SUCCESS,
                        I18n.t("profile.connections.stat.week"), String.valueOf(ProfileFormatUtils.countWithinDays(history, 7))),
                new StatCard(VaadinIcon.CALENDAR, StatCard.Variant.NEUTRAL,
                        I18n.t("profile.connections.stat.month"), String.valueOf(ProfileFormatUtils.countWithinDays(history, 30)))
        );
        miniStats.addClassName("profile-mini-stat-grid");

        add(miniStats, buildChartCard(history), buildRecentSessionsCard(history, onViewAllActivity));
    }

    private Div buildChartCard(List<ConnectionTrackingDto> history) {
        Div chartCard = new Div();
        chartCard.addClassName("profile-section-card");

        H3 chartTitle = new H3(I18n.t("profile.connections.chart.title"));
        chartTitle.addClassName("section-title");

        Div chartHolder = new Div();
        chartHolder.addClassName("profile-chart-holder");
        chartHolder.add(new LoginActivityChart(history, 7));

        Button btn7 = new Button(I18n.t("profile.connections.chart.7d"));
        Button btn30 = new Button(I18n.t("profile.connections.chart.30d"));
        btn7.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        btn30.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        btn7.addClassName("profile-chart-toggle-btn");
        btn30.addClassName("profile-chart-toggle-btn");
        btn7.getElement().setAttribute("aria-pressed", "true");
        btn30.getElement().setAttribute("aria-pressed", "false");

        btn7.addClickListener(e -> {
            chartHolder.removeAll();
            chartHolder.add(new LoginActivityChart(history, 7));
            btn7.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btn7.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btn30.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btn30.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btn7.getElement().setAttribute("aria-pressed", "true");
            btn30.getElement().setAttribute("aria-pressed", "false");
        });
        btn30.addClickListener(e -> {
            chartHolder.removeAll();
            chartHolder.add(new LoginActivityChart(history, 30));
            btn30.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btn30.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btn7.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btn7.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btn30.getElement().setAttribute("aria-pressed", "true");
            btn7.getElement().setAttribute("aria-pressed", "false");
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

    private Div buildRecentSessionsCard(List<ConnectionTrackingDto> history, Runnable onViewAllActivity) {
        Div listCard = new Div();
        listCard.addClassName("profile-section-card");

        H3 listTitle = new H3(I18n.t("profile.connections.recent.title"));
        listTitle.addClassName("section-title");

        Div list = new Div();
        list.addClassName("profile-connections-list");

        List<ConnectionTrackingDto> recent = ProfileFormatUtils.recentFirst(history).stream().limit(5).collect(Collectors.toList());
        if (recent.isEmpty()) {
            list.add(new ProfileEmptyState(I18n.t("profile.history.empty")));
        } else {
            recent.forEach(c -> list.add(new ConnectionCard(c)));
        }

        Button viewAll = new Button(I18n.t("profile.connections.viewAll"), VaadinIcon.ARROW_RIGHT.create());
        viewAll.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        viewAll.addClassName("profile-view-all-btn");
        viewAll.addClickListener(e -> onViewAllActivity.run());

        listCard.add(listTitle, list, viewAll);
        return listCard;
    }
}
