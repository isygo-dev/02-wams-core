package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Professional Profile Page with modern responsive design.
 */
@Slf4j
@VaadinSessionScope
@Route(value = "profile", layout = ImsMainLayout.class)
@PageTitle("Profile")
@CssImport("./styles/profile.css")
@PermitAll
public class ProfilePageView extends ManagementVerticalView {

    private final transient ProfileService profileService;

    private AccountDto currentAccount;
    private AccountStatDto accountStats;

    // UI Components
    private Avatar avatar;
    private Div avatarUploadOverlay;
    private VerticalLayout mainContent;
    private Tabs profileTabs;
    private Div tabContent;

    // Profile Form
    private TextField firstNameField;
    private TextField lastNameField;
    private EmailField emailField;
    private TextField phoneField;
    private TextField languageField;
    private TextField roleField;
    private TextField accountTypeField;
    private TextField statusField;

    // Password Form
    private PasswordField currentPasswordField;
    private PasswordField newPasswordField;
    private PasswordField confirmPasswordField;
    private Button savePasswordButton;

    // Stats Cards
    private Span memberSinceValue;
    private Span lastLoginValue;
    private Span totalLoginsValue;

    private final Binder<AccountDto> profileBinder = new Binder<>(AccountDto.class);

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
            // Load profile
            ResponseEntity<AccountDto> profileResponse = profileService.getProfile();
            if (profileResponse.getStatusCode().is2xxSuccessful() && profileResponse.hasBody()) {
                currentAccount = profileResponse.getBody();
            }

            // Load statistics
            ResponseEntity<AccountStatDto> statsResponse = profileService.getUserStatistics();
            if (statsResponse.getStatusCode().is2xxSuccessful() && statsResponse.hasBody()) {
                accountStats = statsResponse.getBody();
            }

        } catch (Exception e) {
            // Fallback for development
            currentAccount = createFallbackAccount();
            accountStats = createFallbackStats();
        }
    }

    private Component buildMainContent() {
        mainContent = new VerticalLayout();
        mainContent.setPadding(false);
        mainContent.setSpacing(true);
        mainContent.setSizeFull();
        mainContent.addClassName("profile-main-content");

        // Header with profile card
        mainContent.add(buildProfileHeader());

        // Stats cards
        mainContent.add(buildStatsCards());

        // Tabs
        profileTabs = buildTabs();
        mainContent.add(profileTabs);

        // Tab content
        tabContent = new Div();
        tabContent.addClassName("profile-tab-content");
        tabContent.add(buildProfileTabContent());

        profileTabs.addSelectedChangeListener(event -> {
            tabContent.removeAll();
            Tab selected = event.getSelectedTab();
            String tabId = selected.getId().orElse("profile");
            switch (tabId) {
                case "profile":
                    tabContent.add(buildProfileTabContent());
                    break;
                case "security":
                    tabContent.add(buildSecurityTabContent());
                    break;
                case "history":
                    tabContent.add(buildHistoryTabContent());
                    break;
                default:
                    tabContent.add(buildProfileTabContent());
            }
        });

        mainContent.add(tabContent);
        return mainContent;
    }

    private Component buildProfileHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("profile-header-card");
        header.setAlignItems(FlexComponent.Alignment.START);
        header.setSpacing(true);
        header.setPadding(true);
        header.setWidthFull();

        // Avatar section
        VerticalLayout avatarSection = new VerticalLayout();
        avatarSection.setAlignItems(FlexComponent.Alignment.CENTER);
        avatarSection.setSpacing(false);
        avatarSection.addClassName("profile-avatar-section");

        avatar = new Avatar();
        avatar.setWidth("120px");
        avatar.setHeight("120px");
        avatar.addClassName("profile-avatar");

        if (currentAccount != null) {
            String fullName = currentAccount.getFullName();
            avatar.setName(fullName != null ? fullName : currentAccount.getEmail());
            loadProfileImage();
        }

        // Upload overlay
        avatarUploadOverlay = new Div();
        avatarUploadOverlay.addClassName("avatar-upload-overlay");
        avatarUploadOverlay.add(new Icon(VaadinIcon.CAMERA));
        avatarUploadOverlay.add(new Span(I18n.t("profile.change.avatar")));
        avatarUploadOverlay.addClickListener(e -> openAvatarCropperDialog());
        avatarUploadOverlay.getElement().getStyle().set("cursor", "pointer");

        avatarSection.add(avatar, avatarUploadOverlay);

        // User info
        VerticalLayout infoSection = new VerticalLayout();
        infoSection.setSpacing(false);
        infoSection.setPadding(false);
        infoSection.addClassName("profile-info-section");

        if (currentAccount != null) {
            H2 name = new H2(currentAccount.getFullName());
            name.addClassName("profile-name");
            name.addClassName(LumoUtility.Margin.Bottom.NONE);

            Span email = new Span(currentAccount.getEmail());
            email.addClassName("profile-email");
            email.addClassName(LumoUtility.TextColor.SECONDARY);

            Span role = new Span(currentAccount.getFunctionRole());
            role.addClassName("profile-role");
            role.addClassName(LumoUtility.TextColor.TERTIARY);

            // Status badge
            Div statusBadge = new Div();
            statusBadge.addClassName("profile-status-badge");
            statusBadge.addClassName(currentAccount.getAdminStatus() != null &&
                    "ENABLED".equals(currentAccount.getAdminStatus().name()) ?
                    "status-active" : "status-inactive");
            statusBadge.add(new Span(currentAccount.getAdminStatus() != null ?
                    currentAccount.getAdminStatus().name() : "ACTIVE"));

            HorizontalLayout topRow = new HorizontalLayout(name, statusBadge);
            topRow.setAlignItems(FlexComponent.Alignment.CENTER);
            topRow.setSpacing(true);

            infoSection.add(topRow, email, role);
        }

        // Edit button
        Button editButton = new Button(I18n.t("profile.edit"), VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editButton.addClassName("profile-edit-btn");
        editButton.addClickListener(e -> scrollToTab("profile"));

        HorizontalLayout actions = new HorizontalLayout(editButton);
        actions.setPadding(false);
        actions.setSpacing(true);

        infoSection.add(actions);

        // Right side - quick stats
        VerticalLayout quickStats = buildQuickStats();

        header.add(avatarSection, infoSection, quickStats);
        header.expand(infoSection);

        return header;
    }

    private VerticalLayout buildQuickStats() {
        VerticalLayout stats = new VerticalLayout();
        stats.addClassName("profile-quick-stats");
        stats.setSpacing(false);
        stats.setPadding(true);

        Span statsTitle = new Span(I18n.t("profile.quick.stats"));
        statsTitle.addClassName("quick-stats-title");

        Div memberSince = new Div();
        memberSince.addClassName("quick-stat-item");
        memberSince.add(new Span(I18n.t("profile.member.since") + ": "));
        memberSinceValue = new Span(accountStats != null && accountStats.getCreateDate() != null ?
                accountStats.getCreateDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "N/A");
        memberSince.add(memberSinceValue);

        Div lastLogin = new Div();
        lastLogin.addClassName("quick-stat-item");
        lastLogin.add(new Span(I18n.t("profile.last.login") + ": "));
        lastLoginValue = new Span(accountStats != null && accountStats.getLastLogin() != null ?
                accountStats.getLastLogin().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")) : "Never");
        lastLogin.add(lastLoginValue);

        Div totalLogins = new Div();
        totalLogins.addClassName("quick-stat-item");
        totalLogins.add(new Span(I18n.t("profile.total.logins") + ": "));
        totalLoginsValue = new Span(accountStats != null && accountStats.getTotalConnections() != null ?
                String.valueOf(accountStats.getTotalConnections()) : "0");
        totalLogins.add(totalLoginsValue);

        stats.add(statsTitle, memberSince, lastLogin, totalLogins);
        return stats;
    }

    private Component buildStatsCards() {
        return new StatCardGrid(
                new StatCard(VaadinIcon.CALENDAR, StatCard.Variant.PRIMARY,
                        I18n.t("profile.stat.member.since"),
                        accountStats != null && accountStats.getCreateDate() != null ?
                                accountStats.getCreateDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "N/A"),
                new StatCard(VaadinIcon.CLOCK, StatCard.Variant.NEUTRAL,
                        I18n.t("profile.stat.last.active"),
                        accountStats != null && accountStats.getLastLogin() != null ?
                                accountStats.getLastLogin().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")) : "Never"),
                new StatCard(VaadinIcon.SIGN_IN, StatCard.Variant.SUCCESS,
                        I18n.t("profile.stat.total.logins"),
                        accountStats != null && accountStats.getTotalConnections() != null ?
                                String.valueOf(accountStats.getTotalConnections()) : "0"),
                new StatCard(VaadinIcon.USERS, StatCard.Variant.PRIMARY,
                        I18n.t("profile.stat.roles"),
                        accountStats != null && accountStats.getRoleCount() != null ?
                                String.valueOf(accountStats.getRoleCount()) : "0")
        );
    }

    private Tabs buildTabs() {
        Tab profileTab = new Tab(new Icon(VaadinIcon.USER), new Span(I18n.t("profile.tab.profile")));
        profileTab.setId("profile");

        Tab securityTab = new Tab(new Icon(VaadinIcon.LOCK), new Span(I18n.t("profile.tab.security")));
        securityTab.setId("security");

        Tab historyTab = new Tab(new Icon(VaadinIcon.CLOCK), new Span(I18n.t("profile.tab.history")));
        historyTab.setId("history");

        Tabs tabs = new Tabs(profileTab, securityTab, historyTab);
        tabs.addClassName("profile-tabs");
        tabs.setWidthFull();
        return tabs;
    }

    private Component buildProfileTabContent() {
        FormLayout form = new FormLayout();
        form.addClassName("profile-form");

        // Personal Information
        H3 personalTitle = new H3(I18n.t("profile.section.personal"));
        personalTitle.addClassName("section-title");

        firstNameField = new TextField(I18n.t("profile.field.firstName"));
        firstNameField.setWidthFull();
        firstNameField.setRequiredIndicatorVisible(true);

        lastNameField = new TextField(I18n.t("profile.field.lastName"));
        lastNameField.setWidthFull();
        lastNameField.setRequiredIndicatorVisible(true);

        emailField = new EmailField(I18n.t("profile.field.email"));
        emailField.setWidthFull();
        emailField.setRequiredIndicatorVisible(true);

        phoneField = new TextField(I18n.t("profile.field.phone"));
        phoneField.setWidthFull();

        // Account Information
        H3 accountTitle = new H3(I18n.t("profile.section.account"));
        accountTitle.addClassName("section-title");

        languageField = new TextField(I18n.t("profile.field.language"));
        languageField.setWidthFull();
        languageField.setReadOnly(true);

        roleField = new TextField(I18n.t("profile.field.role"));
        roleField.setWidthFull();
        roleField.setReadOnly(true);

        accountTypeField = new TextField(I18n.t("profile.field.accountType"));
        accountTypeField.setWidthFull();
        accountTypeField.setReadOnly(true);

        statusField = new TextField(I18n.t("profile.field.status"));
        statusField.setWidthFull();
        statusField.setReadOnly(true);

        // Bind data
        if (currentAccount != null) {
            profileBinder.readBean(currentAccount);

            AccountDetailsDto details = currentAccount.getAccountDetails();
            firstNameField.setValue(details != null && details.getFirstName() != null ?
                    details.getFirstName() : "");
            lastNameField.setValue(details != null && details.getLastName() != null ?
                    details.getLastName() : "");
            emailField.setValue(currentAccount.getEmail());
            phoneField.setValue(currentAccount.getPhoneNumber() != null ?
                    currentAccount.getPhoneNumber() : "");
            languageField.setValue(currentAccount.getLanguage() != null ?
                    currentAccount.getLanguage().name() : "EN");
            roleField.setValue(currentAccount.getFunctionRole() != null ?
                    currentAccount.getFunctionRole() : "User");
            accountTypeField.setValue(currentAccount.getAccountType() != null ?
                    currentAccount.getAccountType() : "TENANT_USER");
            statusField.setValue(currentAccount.getAdminStatus() != null ?
                    currentAccount.getAdminStatus().name() : "ENABLED");
        }

        // Validators
        profileBinder.forField(firstNameField)
                .withValidator(new StringLengthValidator("First name must be at least 2 characters", 2, 50))
                .bind(dto -> dto.getAccountDetails() != null ? dto.getAccountDetails().getFirstName() : null,
                        (dto, value) -> ensureAccountDetails(dto).setFirstName(value));

        profileBinder.forField(lastNameField)
                .withValidator(new StringLengthValidator("Last name must be at least 2 characters", 2, 50))
                .bind(dto -> dto.getAccountDetails() != null ? dto.getAccountDetails().getLastName() : null,
                        (dto, value) -> ensureAccountDetails(dto).setLastName(value));

        profileBinder.forField(emailField)
                .withValidator(new EmailValidator("Please enter a valid email address"))
                .bind(AccountDto::getEmail, AccountDto::setEmail);

        profileBinder.forField(phoneField)
                .bind(AccountDto::getPhoneNumber, AccountDto::setPhoneNumber);

        // Save button
        Button saveButton = new Button(I18n.t("profile.save"), VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClassName("profile-save-btn");
        saveButton.addClickListener(e -> saveProfile());

        Button resetButton = new Button(I18n.t("profile.reset"), VaadinIcon.REFRESH.create());
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetButton.addClickListener(e -> {
            if (currentAccount != null) {
                profileBinder.readBean(currentAccount);
            }
        });

        HorizontalLayout buttons = new HorizontalLayout(saveButton, resetButton);
        buttons.setSpacing(true);

        form.add(
                personalTitle,
                firstNameField, lastNameField,
                emailField, phoneField,
                accountTitle,
                languageField, roleField,
                accountTypeField, statusField,
                buttons
        );

        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        return form;
    }

    private Component buildSecurityTabContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        // Password change section
        H3 passwordTitle = new H3(I18n.t("profile.section.change.password"));
        passwordTitle.addClassName("section-title");

        Paragraph passwordDesc = new Paragraph(I18n.t("profile.password.description"));
        passwordDesc.addClassName(LumoUtility.TextColor.SECONDARY);

        FormLayout form = new FormLayout();
        form.addClassName("password-form");

        currentPasswordField = new PasswordField(I18n.t("profile.field.current.password"));
        currentPasswordField.setWidthFull();
        currentPasswordField.setRequiredIndicatorVisible(true);

        newPasswordField = new PasswordField(I18n.t("profile.field.new.password"));
        newPasswordField.setWidthFull();
        newPasswordField.setRequiredIndicatorVisible(true);

        confirmPasswordField = new PasswordField(I18n.t("profile.field.confirm.password"));
        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequiredIndicatorVisible(true);

        // Password strength indicator
        ProgressBar strengthBar = new ProgressBar();
        strengthBar.setWidthFull();
        strengthBar.setValue(0);
        strengthBar.addClassName("password-strength-bar");

        Span strengthLabel = new Span(I18n.t("profile.password.strength.weak"));
        strengthLabel.addClassName("password-strength-label");

        newPasswordField.addValueChangeListener(e -> {
            String pwd = e.getValue();
            int strength = calculatePasswordStrength(pwd);
            strengthBar.setValue(strength / 100.0);
            strengthLabel.setText(getStrengthText(strength));
            strengthBar.getStyle().set("--strength-color", getStrengthColor(strength));
        });

        HorizontalLayout strengthRow = new HorizontalLayout(strengthBar, strengthLabel);
        strengthRow.setAlignItems(FlexComponent.Alignment.CENTER);
        strengthRow.setWidthFull();
        strengthRow.setSpacing(true);

        savePasswordButton = new Button(I18n.t("profile.password.change"), VaadinIcon.LOCK.create());
        savePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        savePasswordButton.addClassName("profile-save-btn");
        savePasswordButton.addClickListener(e -> changePassword());

        form.add(
                currentPasswordField,
                newPasswordField,
                confirmPasswordField,
                strengthRow,
                savePasswordButton
        );

        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        content.add(passwordTitle, passwordDesc, form);
        return content;
    }

    private Component buildHistoryTabContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        H3 historyTitle = new H3(I18n.t("profile.section.connection.history"));
        historyTitle.addClassName("section-title");

        Grid<ConnectionTrackingDto> grid = new Grid<>(ConnectionTrackingDto.class, false);
        grid.addClassName("profile-history-grid");
        grid.setWidthFull();

        grid.addColumn(ConnectionTrackingDto::getLoginDate)
                .setHeader(I18n.t("profile.history.date"))
                .setWidth("200px");

        grid.addColumn(ConnectionTrackingDto::getDevice)
                .setHeader(I18n.t("profile.history.device"))
                .setWidth("150px");

        grid.addColumn(ConnectionTrackingDto::getBrowser)
                .setHeader(I18n.t("profile.history.browser"))
                .setWidth("150px");

        grid.addColumn(ConnectionTrackingDto::getIpAddress)
                .setHeader(I18n.t("profile.history.ip"))
                .setWidth("150px");

        grid.addColumn(ConnectionTrackingDto::getLogApp)
                .setHeader(I18n.t("profile.history.app"))
                .setWidth("120px");

        // Load history data
        List<ConnectionTrackingDto> history = loadConnectionHistory();
        grid.setItems(history);

        if (history.isEmpty()) {
            grid.setEmptyStateText(I18n.t("profile.history.empty"));
        }

        content.add(historyTitle, grid);
        return content;
    }

    private List<ConnectionTrackingDto> loadConnectionHistory() {
        if (currentAccount != null && currentAccount.getConnectionTracking() != null) {
            return currentAccount.getConnectionTracking();
        }
        return List.of();
    }

    private void saveProfile() {
        try {
            if (!profileBinder.isValid()) {
                Notification.show(I18n.t("profile.validation.error"), 3000, Notification.Position.MIDDLE);
                return;
            }

            AccountDto updated = new AccountDto();
            profileBinder.writeBean(updated);

            ResponseEntity<AccountDto> response = profileService.updateProfile(updated);

            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                currentAccount = response.getBody();
                Notification.show(I18n.t("profile.update.success"), 3000, Notification.Position.MIDDLE);
                refreshView();
            }
        } catch (ValidationException e) {
            Notification.show(I18n.t("profile.validation.error"), 3000, Notification.Position.MIDDLE);
        } catch (Exception e) {
            Notification.show(I18n.t("profile.update.error"), 3000, Notification.Position.MIDDLE);
        }
    }

    private void changePassword() {
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
        if (currentAccount == null || currentAccount.getId() == null) return;

        try {
            ResponseEntity<Resource> response = profileService.downloadAvatar();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] imageBytes = response.getBody().getContentAsByteArray();
                avatar.setImageResource(new StreamResource("profile.jpg",
                        () -> new ByteArrayInputStream(imageBytes)));
            }
        } catch (FeignException ex) {
            // Avatar not found - use default
        } catch (IOException e) {
            // Error loading avatar
        }
    }

    private void refreshView() {
        mainContent.removeAll();
        mainContent.add(buildProfileHeader());
        mainContent.add(buildStatsCards());
        mainContent.add(profileTabs);
        tabContent.removeAll();
        tabContent.add(buildProfileTabContent());
        mainContent.add(tabContent);
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
                            "document.querySelector('.profile-tab-content').scrollIntoView({ behavior: 'smooth' })"
                    );
                });
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
        if (strength < 60) return "var(--lumo-warning-color)";
        if (strength < 80) return "var(--lumo-success-color)";
        return "var(--lumo-success-color)";
    }

    private AccountDto createFallbackAccount() {
        AccountDto dto = new AccountDto();
        dto.setId(1L);
        dto.setEmail("demo@isygoit.eu");
        dto.setPhoneNumber("+1234567890");
        dto.setFunctionRole("Administrator");
        dto.setAccountType("TENANT_USER");
        ensureAccountDetails(dto).setFirstName("Demo");
        ensureAccountDetails(dto).setLastName("User");
        return dto;
    }

    private static AccountDetailsDto ensureAccountDetails(AccountDto dto) {
        AccountDetailsDto details = dto.getAccountDetails();
        if (details == null) {
            details = new AccountDetailsDto();
            dto.setAccountDetails(details);
        }
        return details;
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