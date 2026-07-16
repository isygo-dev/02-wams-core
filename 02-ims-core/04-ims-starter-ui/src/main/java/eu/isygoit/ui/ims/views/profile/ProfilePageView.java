package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ProfileService;
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
import java.time.LocalDateTime;
import java.util.List;

/**
 * Professional, mobile-first profile page: identity header, key-metric stat
 * cards, and Overview / Activity / Connections / Settings tabs. Orchestrates
 * data loading and tab switching; each section is its own component:
 * {@link ProfileHeaderCard}, {@link ProfileStatsPanel}, {@link ProfileOverviewPanel},
 * {@link ProfileActivityPanel}, {@link ProfileConnectionsPanel},
 * {@link ProfileSettingsPanel}, {@link ProfileFooterBar}.
 *
 * <p>"Connections" in this domain means tracked login sessions
 * ({@link ConnectionTrackingDto}), not a social graph, so its analytics
 * (session counts, a login-activity chart, recent sessions) are framed
 * around that data rather than followers/following.
 */
@Slf4j
@VaadinSessionScope
@Route(value = "profile", layout = ImsMainLayout.class)
@PageTitle("Profile")
@CssImport("./styles/profile.css")
@PermitAll
public class ProfilePageView extends ManagementVerticalView {

    private final transient ProfileService profileService;

    private transient AccountDto currentAccount;
    private transient AccountStatDto accountStats;

    private ProfileHeaderCard header;
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

        mainContent.add(buildHeader());
        mainContent.add(new ProfileStatsPanel(currentAccount, accountStats));

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
        mainContent.add(new ProfileFooterBar(accountStats, this::logout));
        return mainContent;
    }

    private Component buildHeader() {
        header = new ProfileHeaderCard(currentAccount, this::openEditProfileDialog, this::openAvatarCropperDialog);
        loadProfileImage(header.getAvatar());
        return header;
    }

    private Component buildTabContent(String tabId) {
        List<ConnectionTrackingDto> history = ProfileFormatUtils.connectionHistory(currentAccount);
        return switch (tabId) {
            case "activity" -> new ProfileActivityPanel(history);
            case "connections" ->
                    new ProfileConnectionsPanel(currentAccount, accountStats, history, () -> scrollToTab("activity"));
            case "settings" -> new ProfileSettingsPanel(profileService);
            default -> new ProfileOverviewPanel(currentAccount, accountStats);
        };
    }

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
    // Actions
    // ------------------------------------------------------------------------

    private void logout() {
        eu.isygoit.util.SecurityUtils.logout();
        SecurityContextHolder.clearContext();
        UI.getCurrent().getPage().setLocation("login");
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
                loadProfileImage(header.getAvatar());
                Notification.show(I18n.t("profile.avatar.updated"), 3000, Notification.Position.MIDDLE);
            } else {
                Notification.show(I18n.t("profile.avatar.error"), 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception e) {
            Notification.show(I18n.t("profile.avatar.error"), 3000, Notification.Position.MIDDLE);
        }
    }

    private void loadProfileImage(Avatar avatar) {
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
        mainContent.add(buildHeader());
        mainContent.add(new ProfileStatsPanel(currentAccount, accountStats));
        mainContent.add(profileTabs);
        tabContent.removeAll();
        tabContent.add(buildTabContent(profileTabs.getSelectedTab().getId().orElse("overview")));
        mainContent.add(tabContent);
        mainContent.add(new ProfileFooterBar(accountStats, this::logout));
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

    // ------------------------------------------------------------------------
    // Fallback content (used only if the backend call fails)
    // ------------------------------------------------------------------------

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
