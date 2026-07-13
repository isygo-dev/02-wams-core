package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.ChangePasswordRequestDto;
import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ProfileService;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.dialog.ImageCropperDialog;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Professional, mobile-first profile page: identity header, key-metric stat
 * cards, and Overview / Activity / Connections / Settings tabs. "Connections"
 * in this domain means tracked login sessions ({@link ConnectionTrackingDto}),
 * not a social graph, so its analytics (total/weekly/monthly session counts,
 * a login-activity chart, recent sessions) are framed around that data rather
 * than followers/following.
 */
@Slf4j
@VaadinSessionScope
@Route(value = "profile", layout = ImsMainLayout.class)
@PageTitle("Profile")
@CssImport("./styles/profile.css")
@PermitAll
public class ProfilePageView extends ManagementVerticalView {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter CHART_WEEKDAY_FMT = DateTimeFormatter.ofPattern("EEE");
    private static final DateTimeFormatter CHART_DAY_FMT = DateTimeFormatter.ofPattern("dd MMM");

    private final transient ProfileService profileService;

    private transient AccountDto currentAccount;
    private transient AccountStatDto accountStats;

    private Avatar avatar;
    private VerticalLayout mainContent;
    private Tabs profileTabs;
    private Div tabContent;

    @Autowired
    public ProfilePageView(ProfileService profileService) {
        this.profileService = profileService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("profile-page-view");
        loadUserData();
        add(buildMainContent());
    }

    private void loadUserData() {
        try {
            ResponseEntity<AccountDto> profileResponse = profileService.getProfile();
            if (profileResponse.getStatusCode().is2xxSuccessful() && profileResponse.hasBody()) {
                currentAccount = profileResponse.getBody();
            }

            ResponseEntity<AccountStatDto> statsResponse = profileService.getUserStatistics();
            if (statsResponse.getStatusCode().is2xxSuccessful() && statsResponse.hasBody()) {
                accountStats = statsResponse.getBody();
            }
        } catch (Exception e) {
            log.warn("Failed to load profile data, falling back to placeholder content", e);
        }

        if (currentAccount == null) {
            currentAccount = createFallbackAccount();
        }
        if (accountStats == null) {
            accountStats = createFallbackStats();
        }
    }

    private Component buildMainContent() {
        mainContent = new VerticalLayout();
        mainContent.setPadding(false);
        mainContent.setSpacing(true);
        mainContent.setSizeFull();
        mainContent.addClassName("profile-main-content");

        mainContent.add(buildProfileHeader());
        mainContent.add(buildStatsCards());

        profileTabs = buildTabs();
        mainContent.add(profileTabs);

        tabContent = new Div();
        tabContent.addClassName("profile-tab-content");
        tabContent.add(buildTabContent("overview"));

        profileTabs.addSelectedChangeListener(event -> {
            tabContent.removeAll();
            String tabId = event.getSelectedTab().getId().orElse("overview");
            tabContent.add(buildTabContent(tabId));
        });

        mainContent.add(tabContent);
        mainContent.add(buildFooter());
        return mainContent;
    }

    private Component buildTabContent(String tabId) {
        return switch (tabId) {
            case "activity" -> buildActivityTabContent();
            case "connections" -> buildConnectionsTabContent();
            case "settings" -> buildSettingsTabContent();
            default -> buildOverviewTabContent();
        };
    }

    // ------------------------------------------------------------------------
    // Header
    // ------------------------------------------------------------------------

    private Component buildProfileHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("profile-header-card");
        header.setAlignItems(FlexComponent.Alignment.START);
        header.setSpacing(true);
        header.setPadding(true);
        header.setWidthFull();

        Div avatarSection = new Div();
        avatarSection.addClassName("profile-avatar-section");

        avatar = new Avatar();
        avatar.setWidth("112px");
        avatar.setHeight("112px");
        avatar.addClassName("profile-avatar");
        String fullName = currentAccount.getFullName();
        avatar.setName(fullName != null && !fullName.isBlank() ? fullName : currentAccount.getEmail());
        loadProfileImage();

        Div avatarUploadOverlay = new Div();
        avatarUploadOverlay.addClassName("avatar-upload-overlay");
        avatarUploadOverlay.add(VaadinIcon.CAMERA.create());
        avatarUploadOverlay.getElement().setAttribute("aria-label", I18n.t("profile.change.avatar"));
        avatarUploadOverlay.getElement().setAttribute("role", "button");
        avatarUploadOverlay.addClickListener(e -> openAvatarCropperDialog());
        Tooltip.forComponent(avatarUploadOverlay).setText(I18n.t("profile.change.avatar"));

        avatarSection.add(avatar, avatarUploadOverlay);

        VerticalLayout infoSection = new VerticalLayout();
        infoSection.setSpacing(false);
        infoSection.setPadding(false);
        infoSection.addClassName("profile-info-section");

        H2 name = new H2(fullName != null && !fullName.isBlank() ? fullName : currentAccount.getEmail());
        name.addClassName("profile-name");
        name.addClassName(LumoUtility.Margin.Bottom.NONE);

        boolean active = currentAccount.getAdminStatus() != null
                && "ENABLED".equals(currentAccount.getAdminStatus().name());
        Span statusBadge = new Span(active ? I18n.t("profile.status.active") : I18n.t("profile.status.inactive"));
        statusBadge.addClassName("wams-chip");
        statusBadge.addClassName(active ? "wams-chip--success" : "wams-chip--error");
        statusBadge.addClassName("profile-status-badge");

        HorizontalLayout topRow = new HorizontalLayout(name, statusBadge);
        topRow.setAlignItems(FlexComponent.Alignment.CENTER);
        topRow.setSpacing(true);
        topRow.addClassName("profile-name-row");

        HorizontalLayout emailRow = new HorizontalLayout(VaadinIcon.ENVELOPE_O.create(), new Span(currentAccount.getEmail()));
        emailRow.setAlignItems(FlexComponent.Alignment.CENTER);
        emailRow.setSpacing(false);
        emailRow.addClassName("profile-email-row");

        Div badgeRow = new Div();
        badgeRow.addClassName("profile-badge-row");
        badgeRow.add(buildBadge(VaadinIcon.BRIEFCASE, currentAccount.getFunctionRole(), "wams-chip--info"));
        badgeRow.add(buildBadge(VaadinIcon.OFFICE, currentAccount.getTenant(), "wams-chip--neutral"));
        badgeRow.add(buildBadge(VaadinIcon.KEY, formatAccountType(currentAccount.getAccountType()), "wams-chip--neutral"));
        if (Boolean.TRUE.equals(currentAccount.getIsAdmin())) {
            badgeRow.add(buildBadge(VaadinIcon.STAR, I18n.t("profile.badge.admin"), "wams-chip--warning"));
        }

        Button editButton = new Button(I18n.t("profile.edit"), VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editButton.addClassName("profile-edit-btn");
        editButton.addClickListener(e -> openEditProfileDialog());

        HorizontalLayout actions = new HorizontalLayout(editButton);
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.addClassName("profile-header-actions");

        infoSection.add(topRow, emailRow, badgeRow, actions);

        header.add(avatarSection, infoSection);
        header.expand(infoSection);
        return header;
    }

    private Component buildBadge(VaadinIcon icon, String label, String variantClass) {
        Span badge = new Span();
        badge.addClassName("wams-chip");
        badge.addClassName(variantClass);
        badge.addClassName("profile-badge");
        if (label == null || label.isBlank()) {
            badge.setVisible(false);
            return badge;
        }
        Icon iconEl = icon.create();
        iconEl.setSize("12px");
        badge.add(iconEl, new Span(label));
        return badge;
    }

    private Component buildStatsCards() {
        return new StatCardGrid(
                new StatCard(VaadinIcon.CALENDAR, StatCard.Variant.PRIMARY,
                        I18n.t("profile.stat.member.since"),
                        accountStats.getCreateDate() != null ? accountStats.getCreateDate().format(DATE_FMT) : "N/A"),
                new StatCard(VaadinIcon.CLOCK, StatCard.Variant.NEUTRAL,
                        I18n.t("profile.stat.last.active"),
                        accountStats.getLastLogin() != null ? formatRelativeTime(toDate(accountStats.getLastLogin())) : I18n.t("profile.time.never")),
                new StatCard(VaadinIcon.SIGN_IN, StatCard.Variant.SUCCESS,
                        I18n.t("profile.stat.total.sessions"),
                        String.valueOf(totalSessions())),
                new StatCard(VaadinIcon.USERS, StatCard.Variant.PRIMARY,
                        I18n.t("profile.stat.roles"),
                        accountStats.getRoleCount() != null ? String.valueOf(accountStats.getRoleCount()) : "0",
                        accountStats.getTotalPermissions() != null
                                ? I18n.t("profile.stat.permissions.tooltip", accountStats.getTotalPermissions())
                                : null)
        );
    }

    // ------------------------------------------------------------------------
    // Tabs
    // ------------------------------------------------------------------------

    private Tabs buildTabs() {
        Tab overviewTab = new Tab(new Icon(VaadinIcon.USER), new Span(I18n.t("profile.tab.overview")));
        overviewTab.setId("overview");

        Tab activityTab = new Tab(new Icon(VaadinIcon.CALENDAR_CLOCK), new Span(I18n.t("profile.tab.activity")));
        activityTab.setId("activity");

        Tab connectionsTab = new Tab(new Icon(VaadinIcon.CONNECT), new Span(I18n.t("profile.tab.connections")));
        connectionsTab.setId("connections");

        Tab settingsTab = new Tab(new Icon(VaadinIcon.COG), new Span(I18n.t("profile.tab.settings")));
        settingsTab.setId("settings");

        Tabs tabs = new Tabs(overviewTab, activityTab, connectionsTab, settingsTab);
        tabs.addClassName("profile-tabs");
        tabs.setWidthFull();
        tabs.getElement().setAttribute("aria-label", I18n.t("profile.tabs.ariaLabel"));
        return tabs;
    }

    // ------------------------------------------------------------------------
    // Overview tab
    // ------------------------------------------------------------------------

    private Component buildOverviewTabContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.addClassName("profile-tab-panel");

        Div card = new Div();
        card.addClassName("profile-section-card");

        H3 title = new H3(I18n.t("profile.section.personal"));
        title.addClassName("section-title");

        Div grid = new Div();
        grid.addClassName("wams-card__detail-grid");

        AccountDetailsDto details = currentAccount.getAccountDetails();
        grid.add(
                buildDetailField(VaadinIcon.ENVELOPE, I18n.t("profile.field.email"), currentAccount.getEmail()),
                buildDetailField(VaadinIcon.PHONE, I18n.t("profile.field.phone"), currentAccount.getPhoneNumber()),
                buildDetailField(VaadinIcon.OFFICE, I18n.t("profile.field.tenant"), currentAccount.getTenant()),
                buildDetailField(VaadinIcon.GLOBE, I18n.t("profile.field.country"), details != null ? details.getCountry() : null),
                buildDetailField(VaadinIcon.FLAG, I18n.t("profile.field.language"),
                        currentAccount.getLanguage() != null ? currentAccount.getLanguage().name() : null),
                buildDetailField(VaadinIcon.BRIEFCASE, I18n.t("profile.field.role"), currentAccount.getFunctionRole()),
                buildDetailField(VaadinIcon.KEY, I18n.t("profile.field.accountType"), formatAccountType(currentAccount.getAccountType())),
                buildDetailField(VaadinIcon.CHECK_CIRCLE, I18n.t("profile.field.status"),
                        currentAccount.getAdminStatus() != null ? currentAccount.getAdminStatus().name() : null),
                buildDetailField(VaadinIcon.CALENDAR, I18n.t("profile.field.memberSince"),
                        accountStats.getCreateDate() != null ? accountStats.getCreateDate().format(DATE_FMT) : null),
                buildDetailField(VaadinIcon.CLOCK, I18n.t("profile.field.lastActive"),
                        accountStats.getLastLogin() != null ? accountStats.getLastLogin().format(DATETIME_FMT) : null)
        );

        card.add(title, grid);
        content.add(card);
        return content;
    }

    private Component buildDetailField(VaadinIcon icon, String label, String value) {
        Div field = new Div();
        field.addClassName("wams-card__detail-field");

        Div labelRow = new Div();
        labelRow.addClassName("wams-card__detail-field-label-row");
        Icon iconEl = icon.create();
        iconEl.addClassName("detail-field-icon");
        iconEl.setSize("14px");
        Span labelSpan = new Span(label);
        labelSpan.addClassName("wams-card__detail-field-label");
        labelRow.add(iconEl, labelSpan);

        Span valueSpan = new Span(value != null && !value.isBlank() ? value : "—");
        valueSpan.addClassName("wams-card__detail-field-value");

        field.add(labelRow, valueSpan);
        return field;
    }

    // ------------------------------------------------------------------------
    // Activity tab (full connection timeline)
    // ------------------------------------------------------------------------

    private Component buildActivityTabContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.addClassName("profile-tab-panel");

        Div card = new Div();
        card.addClassName("profile-section-card");

        H3 title = new H3(I18n.t("profile.section.activity"));
        title.addClassName("section-title");
        card.add(title);

        List<ConnectionTrackingDto> history = recentFirst(loadConnectionHistory());

        if (history.isEmpty()) {
            card.add(buildEmptyState(I18n.t("profile.history.empty")));
        } else {
            Div timeline = new Div();
            timeline.addClassName("profile-timeline");
            timeline.getElement().setAttribute("aria-label", I18n.t("profile.section.activity"));
            history.forEach(c -> timeline.add(buildTimelineItem(c)));
            card.add(timeline);
        }

        content.add(card);
        return content;
    }

    private Component buildTimelineItem(ConnectionTrackingDto c) {
        Div item = new Div();
        item.addClassName("profile-timeline-item");

        Div dot = new Div();
        dot.addClassName("profile-timeline-dot");
        dot.add(deviceIcon(c.getDevice()).create());

        Div body = new Div();
        body.addClassName("profile-timeline-body");

        Span titleSpan = new Span(I18n.t("profile.activity.signin",
                c.getLogApp() != null && !c.getLogApp().isBlank() ? c.getLogApp() : I18n.t("profile.history.unknown.app")));
        titleSpan.addClassName("profile-timeline-title");

        String metaLine = Stream.of(c.getBrowser(), c.getDevice(), c.getIpAddress())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" · "));
        Span meta = new Span(metaLine);
        meta.addClassName("profile-timeline-meta");

        Span time = new Span(formatAbsoluteAndRelative(c.getLoginDate()));
        time.addClassName("profile-timeline-time");

        body.add(titleSpan, meta, time);
        item.add(dot, body);
        return item;
    }

    // ------------------------------------------------------------------------
    // Connections tab (session analytics + growth chart + recent list)
    // ------------------------------------------------------------------------

    private Component buildConnectionsTabContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.addClassName("profile-tab-panel");

        List<ConnectionTrackingDto> history = loadConnectionHistory();

        StatCardGrid miniStats = new StatCardGrid(
                new StatCard(VaadinIcon.SIGN_IN, StatCard.Variant.PRIMARY,
                        I18n.t("profile.connections.stat.total"), String.valueOf(totalSessions())),
                new StatCard(VaadinIcon.CALENDAR_CLOCK, StatCard.Variant.SUCCESS,
                        I18n.t("profile.connections.stat.week"), String.valueOf(countWithinDays(history, 7))),
                new StatCard(VaadinIcon.CALENDAR, StatCard.Variant.NEUTRAL,
                        I18n.t("profile.connections.stat.month"), String.valueOf(countWithinDays(history, 30)))
        );

        Div chartCard = new Div();
        chartCard.addClassName("profile-section-card");

        H3 chartTitle = new H3(I18n.t("profile.connections.chart.title"));
        chartTitle.addClassName("section-title");

        Div chartHolder = new Div();
        chartHolder.addClassName("profile-chart-holder");
        chartHolder.add(renderActivityChart(history, 7));

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
            chartHolder.add(renderActivityChart(history, 7));
            btn7.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btn7.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btn30.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btn30.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btn7.getElement().setAttribute("aria-pressed", "true");
            btn30.getElement().setAttribute("aria-pressed", "false");
        });
        btn30.addClickListener(e -> {
            chartHolder.removeAll();
            chartHolder.add(renderActivityChart(history, 30));
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

        Div listCard = new Div();
        listCard.addClassName("profile-section-card");

        H3 listTitle = new H3(I18n.t("profile.connections.recent.title"));
        listTitle.addClassName("section-title");

        Div list = new Div();
        list.addClassName("profile-connections-list");

        List<ConnectionTrackingDto> recent = recentFirst(history).stream().limit(5).collect(Collectors.toList());
        if (recent.isEmpty()) {
            list.add(buildEmptyState(I18n.t("profile.history.empty")));
        } else {
            recent.forEach(c -> list.add(buildConnectionListItem(c)));
        }

        Button viewAll = new Button(I18n.t("profile.connections.viewAll"), VaadinIcon.ARROW_RIGHT.create());
        viewAll.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        viewAll.addClassName("profile-view-all-btn");
        viewAll.addClickListener(e -> scrollToTab("activity"));

        listCard.add(listTitle, list, viewAll);

        content.add(miniStats, chartCard, listCard);
        return content;
    }

    private Component buildConnectionListItem(ConnectionTrackingDto c) {
        Div item = new Div();
        item.addClassName("profile-connection-item");

        Div iconCircle = new Div();
        iconCircle.addClassName("profile-connection-icon");
        iconCircle.add(deviceIcon(c.getDevice()).create());

        Div text = new Div();
        text.addClassName("profile-connection-text");

        String primaryLine = Stream.of(c.getBrowser(), c.getDevice())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" · "));
        Span primary = new Span(!primaryLine.isBlank() ? primaryLine : I18n.t("profile.history.unknown.browser"));
        primary.addClassName("profile-connection-primary");

        String metaLine = Stream.of(c.getIpAddress(), formatRelativeTime(c.getLoginDate()))
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" · "));
        Span meta = new Span(metaLine);
        meta.addClassName("profile-connection-meta");

        text.add(primary, meta);

        Span appChip = new Span(c.getLogApp() != null && !c.getLogApp().isBlank() ? c.getLogApp() : I18n.t("profile.history.unknown.app"));
        appChip.addClassName("wams-chip");
        appChip.addClassName("wams-chip--info");
        appChip.addClassName("profile-connection-chip");

        item.add(iconCircle, text, appChip);
        return item;
    }

    private Component renderActivityChart(List<ConnectionTrackingDto> history, int days) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            counts.put(today.minusDays(i), 0L);
        }
        for (ConnectionTrackingDto c : history) {
            if (c.getLoginDate() == null) {
                continue;
            }
            LocalDate day = c.getLoginDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            counts.merge(day, 1L, Long::sum);
        }
        // Discard buckets outside the requested window (merge above may have
        // added days beyond it if history contains future-dated test data).
        counts.keySet().removeIf(d -> d.isBefore(today.minusDays(days - 1)) || d.isAfter(today));

        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        if (total == 0) {
            return buildEmptyState(I18n.t("profile.connections.chart.empty"));
        }

        int slot = days <= 7 ? 40 : 12;
        int barWidth = days <= 7 ? 22 : 8;
        int chartHeight = 120;
        int labelHeight = 22;
        int width = counts.size() * slot;
        int totalHeight = chartHeight + labelHeight;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg viewBox=\"0 0 ").append(width).append(' ').append(totalHeight)
                .append("\" xmlns=\"http://www.w3.org/2000/svg\" role=\"img\" aria-label=\"")
                .append(I18n.t("profile.connections.chart.aria", total, days))
                .append("\" class=\"profile-chart-svg\">");

        int x = 0;
        int index = 0;
        int lastIndex = counts.size() - 1;
        for (Map.Entry<LocalDate, Long> entry : counts.entrySet()) {
            long count = entry.getValue();
            int barHeight = max == 0 ? 0 : (int) Math.round((count / (double) max) * (chartHeight - 8));
            if (count > 0) {
                barHeight = Math.max(barHeight, 3);
            }
            int barX = x + (slot - barWidth) / 2;
            int barY = chartHeight - barHeight;

            svg.append("<rect class=\"profile-chart-bar\" x=\"").append(barX).append("\" y=\"").append(barY)
                    .append("\" width=\"").append(barWidth).append("\" height=\"").append(barHeight)
                    .append("\" rx=\"2\"><title>").append(entry.getKey()).append(": ").append(count)
                    .append("</title></rect>");

            boolean showLabel = days <= 7 || index % 5 == 0 || index == lastIndex;
            if (showLabel) {
                String label = days <= 7 ? entry.getKey().format(CHART_WEEKDAY_FMT) : entry.getKey().format(CHART_DAY_FMT);
                svg.append("<text class=\"profile-chart-label\" x=\"").append(x + slot / 2.0).append("\" y=\"")
                        .append(chartHeight + 15).append("\" text-anchor=\"middle\">").append(label).append("</text>");
            }
            x += slot;
            index++;
        }
        svg.append("</svg>");

        Html chart = new Html("<div class=\"profile-chart-svg-wrapper\">" + svg + "</div>");
        return chart;
    }

    // ------------------------------------------------------------------------
    // Settings tab (security)
    // ------------------------------------------------------------------------

    private Component buildSettingsTabContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.addClassName("profile-tab-panel");

        Div card = new Div();
        card.addClassName("profile-section-card");

        H3 passwordTitle = new H3(I18n.t("profile.section.change.password"));
        passwordTitle.addClassName("section-title");

        Paragraph passwordDesc = new Paragraph(I18n.t("profile.password.description"));
        passwordDesc.addClassName(LumoUtility.TextColor.SECONDARY);

        FormLayout form = new FormLayout();
        form.addClassName("password-form");

        PasswordField currentPasswordField = new PasswordField(I18n.t("profile.field.current.password"));
        currentPasswordField.setWidthFull();
        currentPasswordField.setRequiredIndicatorVisible(true);

        PasswordField newPasswordField = new PasswordField(I18n.t("profile.field.new.password"));
        newPasswordField.setWidthFull();
        newPasswordField.setRequiredIndicatorVisible(true);

        PasswordField confirmPasswordField = new PasswordField(I18n.t("profile.field.confirm.password"));
        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequiredIndicatorVisible(true);

        ProgressBar strengthBar = new ProgressBar();
        strengthBar.setWidthFull();
        strengthBar.setValue(0);
        strengthBar.addClassName("password-strength-bar");

        Span strengthLabel = new Span(I18n.t("profile.password.strength.weak"));
        strengthLabel.addClassName("password-strength-label");

        newPasswordField.addValueChangeListener(e -> {
            int strength = calculatePasswordStrength(e.getValue());
            strengthBar.setValue(strength / 100.0);
            strengthLabel.setText(getStrengthText(strength));
            strengthBar.getStyle().set("--strength-color", getStrengthColor(strength));
        });

        HorizontalLayout strengthRow = new HorizontalLayout(strengthBar, strengthLabel);
        strengthRow.setAlignItems(FlexComponent.Alignment.CENTER);
        strengthRow.setWidthFull();
        strengthRow.setSpacing(true);

        Button savePasswordButton = new Button(I18n.t("profile.password.change"), VaadinIcon.LOCK.create());
        savePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        savePasswordButton.addClassName("profile-save-btn");
        savePasswordButton.addClickListener(e -> changePassword(currentPasswordField, newPasswordField, confirmPasswordField));

        form.add(currentPasswordField, newPasswordField, confirmPasswordField, strengthRow, savePasswordButton);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2));

        card.add(passwordTitle, passwordDesc, form);
        content.add(card);
        return content;
    }

    private void changePassword(PasswordField currentPasswordField, PasswordField newPasswordField, PasswordField confirmPasswordField) {
        String currentPwd = currentPasswordField.getValue();
        String newPwd = newPasswordField.getValue();
        String confirmPwd = confirmPasswordField.getValue();

        if (newPwd.isEmpty() || confirmPwd.isEmpty() || currentPwd.isEmpty()) {
            Notification.show(I18n.t("profile.password.fields.required"), 3000, Notification.Position.MIDDLE);
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            Notification.show(I18n.t("profile.password.mismatch"), 3000, Notification.Position.MIDDLE);
            return;
        }
        if (newPwd.length() < 8) {
            Notification.show(I18n.t("profile.password.min.length"), 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                    .currentPassword(currentPwd)
                    .newPassword(newPwd)
                    .confirmPassword(confirmPwd)
                    .build();

            ResponseEntity<Void> response = profileService.changePassword(request);

            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show(I18n.t("profile.password.success"), 3000, Notification.Position.MIDDLE);
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
            }
        } catch (Exception e) {
            Notification.show(I18n.t("profile.password.error"), 3000, Notification.Position.MIDDLE);
        }
    }

    private int calculatePasswordStrength(String password) {
        int score = 0;
        if (password.length() >= 8) score += 25;
        if (password.length() >= 12) score += 15;
        if (password.matches(".*[A-Z].*")) score += 20;
        if (password.matches(".*[a-z].*")) score += 20;
        if (password.matches(".*\\d.*")) score += 10;
        if (password.matches(".*[!@#$%^&*()].*")) score += 10;
        return Math.min(score, 100);
    }

    private String getStrengthText(int strength) {
        if (strength < 30) return I18n.t("profile.password.strength.weak");
        if (strength < 60) return I18n.t("profile.password.strength.medium");
        if (strength < 80) return I18n.t("profile.password.strength.strong");
        return I18n.t("profile.password.strength.very.strong");
    }

    private String getStrengthColor(int strength) {
        if (strength < 30) return "var(--lumo-error-color)";
        if (strength < 60) return "var(--lumo-warning-color, var(--wams-warning-color))";
        return "var(--lumo-success-color)";
    }

    // ------------------------------------------------------------------------
    // Footer
    // ------------------------------------------------------------------------

    private Component buildFooter() {
        HorizontalLayout footer = new HorizontalLayout();
        footer.addClassName("profile-footer");
        footer.setWidthFull();
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        HorizontalLayout lastActive = new HorizontalLayout(VaadinIcon.CLOCK.create(), new Span(
                accountStats.getLastLogin() != null
                        ? I18n.t("profile.footer.last.active", formatRelativeTime(toDate(accountStats.getLastLogin())))
                        : I18n.t("profile.time.never")));
        lastActive.setAlignItems(FlexComponent.Alignment.CENTER);
        lastActive.addClassName("profile-last-active");

        Button logoutButton = new Button(I18n.t("common.layout.avatar.logout"), VaadinIcon.SIGN_OUT.create());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        logoutButton.addClassName("profile-logout-btn");
        logoutButton.addClickListener(e -> logout());

        footer.add(lastActive, logoutButton);
        return footer;
    }

    private void logout() {
        eu.isygoit.util.SecurityUtils.logout();
        SecurityContextHolder.clearContext();
        UI.getCurrent().getPage().setLocation("login");
    }

    // ------------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------------

    private List<ConnectionTrackingDto> loadConnectionHistory() {
        return currentAccount.getConnectionTracking() != null ? currentAccount.getConnectionTracking() : List.of();
    }

    private List<ConnectionTrackingDto> recentFirst(List<ConnectionTrackingDto> history) {
        return history.stream()
                .sorted(Comparator.comparing(ConnectionTrackingDto::getLoginDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private long countWithinDays(List<ConnectionTrackingDto> history, int days) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        return history.stream()
                .filter(c -> c.getLoginDate() != null && c.getLoginDate().toInstant().isAfter(cutoff))
                .count();
    }

    private int totalSessions() {
        if (accountStats.getTotalConnections() != null) {
            return accountStats.getTotalConnections();
        }
        return loadConnectionHistory().size();
    }

    private VaadinIcon deviceIcon(String device) {
        if (device == null) {
            return VaadinIcon.DESKTOP;
        }
        String d = device.toLowerCase(Locale.ROOT);
        if (d.contains("mobile") || d.contains("phone")) {
            return VaadinIcon.MOBILE_RETRO;
        }
        if (d.contains("tablet") || d.contains("ipad")) {
            return VaadinIcon.TABLET;
        }
        return VaadinIcon.DESKTOP;
    }

    private Component buildEmptyState(String message) {
        Div empty = new Div();
        empty.addClassName("profile-empty-state");
        Icon icon = VaadinIcon.INBOX.create();
        icon.addClassName("profile-empty-state__icon");
        empty.add(icon, new Span(message));
        return empty;
    }

    private String formatAccountType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return null;
        }
        String[] parts = accountType.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private Date toDate(LocalDateTime dateTime) {
        return dateTime != null ? Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    private String formatAbsoluteAndRelative(Date date) {
        if (date == null) {
            return I18n.t("profile.time.unknown");
        }
        LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return ldt.format(DATETIME_FMT) + " · " + formatRelativeTime(date);
    }

    private String formatRelativeTime(Date date) {
        if (date == null) {
            return I18n.t("profile.time.unknown");
        }
        long seconds = Duration.between(date.toInstant(), Instant.now()).getSeconds();
        if (seconds < 0) {
            seconds = 0;
        }
        if (seconds < 60) {
            return I18n.t("profile.time.justnow");
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return I18n.t("profile.time.minutes.ago", minutes);
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return I18n.t("profile.time.hours.ago", hours);
        }
        long days = hours / 24;
        if (days < 7) {
            return I18n.t("profile.time.days.ago", days);
        }
        long weeks = days / 7;
        if (weeks < 5) {
            return I18n.t("profile.time.weeks.ago", weeks);
        }
        long months = days / 30;
        return I18n.t("profile.time.months.ago", months);
    }

    private void openEditProfileDialog() {
        EditProfileDialog dialog = new EditProfileDialog(profileService, currentAccount, updated -> {
            currentAccount = updated;
            refreshView();
        });
        dialog.open();
    }

    private void openAvatarCropperDialog() {
        ImageCropperDialog cropperDialog = new ImageCropperDialog(this::uploadAvatar);
        cropperDialog.open();
    }

    private void uploadAvatar(MultipartFile imageFile) {
        try {
            ResponseEntity<AccountDto> response = profileService.uploadAvatar(imageFile);
            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                currentAccount = response.getBody();
                loadProfileImage();
                Notification.show(I18n.t("profile.avatar.updated"), 3000, Notification.Position.MIDDLE);
            } else {
                Notification.show(I18n.t("profile.avatar.error"), 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception e) {
            Notification.show(I18n.t("profile.avatar.error"), 3000, Notification.Position.MIDDLE);
        }
    }

    private void loadProfileImage() {
        if (currentAccount.getId() == null) {
            return;
        }
        try {
            ResponseEntity<Resource> response = profileService.downloadAvatar();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] imageBytes = response.getBody().getContentAsByteArray();
                avatar.setImageResource(new StreamResource("profile.jpg", () -> new ByteArrayInputStream(imageBytes)));
            }
        } catch (FeignException ex) {
            // Avatar not found - fall back to initials
        } catch (IOException e) {
            log.warn("Failed to load profile avatar", e);
        }
    }

    private void refreshView() {
        mainContent.removeAll();
        mainContent.add(buildProfileHeader());
        mainContent.add(buildStatsCards());
        mainContent.add(profileTabs);
        tabContent.removeAll();
        tabContent.add(buildTabContent(profileTabs.getSelectedTab().getId().orElse("overview")));
        mainContent.add(tabContent);
        mainContent.add(buildFooter());
    }

    private void scrollToTab(String tabId) {
        profileTabs.getChildren()
                .filter(Tab.class::isInstance)
                .map(Tab.class::cast)
                .filter(tab -> tabId.equals(tab.getId().orElse("")))
                .findFirst()
                .ifPresent(tab -> {
                    profileTabs.setSelectedTab(tab);
                    UI.getCurrent().getPage().executeJs(
                            "document.querySelector('.profile-tab-content').scrollIntoView({ behavior: 'smooth' })");
                });
    }

    private AccountDto createFallbackAccount() {
        AccountDto dto = new AccountDto();
        dto.setId(1L);
        dto.setEmail("demo@isygoit.eu");
        dto.setPhoneNumber("+1234567890");
        dto.setFunctionRole("Administrator");
        dto.setAccountType("TENANT_USER");
        dto.setTenant("demo");
        AccountDetailsDto details = new AccountDetailsDto();
        details.setFirstName("Demo");
        details.setLastName("User");
        dto.setAccountDetails(details);
        return dto;
    }

    private AccountStatDto createFallbackStats() {
        return AccountStatDto.builder()
                .createDate(LocalDateTime.now().minusMonths(6))
                .lastLogin(LocalDateTime.now().minusHours(2))
                .totalConnections(42)
                .roleCount(2)
                .totalPermissions(15)
                .isActive(true)
                .build();
    }
}
