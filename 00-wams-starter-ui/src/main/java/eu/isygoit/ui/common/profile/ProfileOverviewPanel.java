package eu.isygoit.ui.common.profile;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.i18n.I18n;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Overview tab: read-only cards with every available profile field, grouped as personal information, account and
 * security, access (roles and applications), and the last sign-in. Editing happens through
 * {@link EditProfileDialog}, opened from the header, and the sign-in method is changed in the Settings tab.
 */
class ProfileOverviewPanel extends VerticalLayout {

    ProfileOverviewPanel(AccountDto account, AccountStatDto stats) {
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        addClassName("profile-tab-panel");

        add(buildPersonalCard(account),
                buildAccountCard(account, stats),
                buildAccessCard(account));
        if (stats.getLastLogin() != null) {
            add(buildLastSignInCard(stats));
        }
    }

    private Div buildPersonalCard(AccountDto account) {
        AccountDetailsDto details = account.getAccountDetails();
        return card(I18n.t("profile.section.personal"),
                field(VaadinIcon.USER, I18n.t("profile.field.firstName"), details != null ? details.getFirstName() : null),
                field(VaadinIcon.USER_CARD, I18n.t("profile.field.lastName"), details != null ? details.getLastName() : null),
                field(VaadinIcon.ENVELOPE, I18n.t("profile.field.email"), account.getEmail()),
                field(VaadinIcon.PHONE, I18n.t("profile.field.phone"), account.getPhoneNumber()),
                field(VaadinIcon.GLOBE, I18n.t("profile.field.country"), details != null ? details.getCountry() : null),
                field(VaadinIcon.FLAG, I18n.t("profile.field.language"),
                        ProfileFormatUtils.formatLanguage(account.getLanguage())));
    }

    private Div buildAccountCard(AccountDto account, AccountStatDto stats) {
        return card(I18n.t("profile.section.account"),
                field(VaadinIcon.AT, I18n.t("profile.field.username"), account.getCode()),
                field(VaadinIcon.OFFICE, I18n.t("profile.field.tenant"), account.getTenant()),
                field(VaadinIcon.BRIEFCASE, I18n.t("profile.field.role"), account.getFunctionRole()),
                field(VaadinIcon.KEY, I18n.t("profile.field.accountType"),
                        ProfileFormatUtils.formatAccountType(account.getAccountType())),
                field(VaadinIcon.CHECK_CIRCLE, I18n.t("profile.field.status"),
                        ProfileFormatUtils.formatAdminStatus(account.getAdminStatus())),
                field(VaadinIcon.LOCK, I18n.t("profile.field.signInMethod"), authTypeLabel(account.getAuthType())),
                field(VaadinIcon.CALENDAR, I18n.t("profile.field.memberSince"),
                        ProfileFormatUtils.formatDate(stats.getCreateDate())),
                field(VaadinIcon.CLOCK, I18n.t("profile.field.lastActive"),
                        ProfileFormatUtils.formatDateTime(stats.getLastLogin())));
    }

    private Div buildAccessCard(AccountDto account) {
        Div card = new Div();
        card.addClassName("profile-section-card");
        H3 title = new H3(I18n.t("profile.section.access"));
        title.addClassName("section-title");
        card.add(title);

        List<RoleInfoDto> roles = account.getRoleInfo() != null ? account.getRoleInfo() : List.of();
        card.add(chipGroup(I18n.t("profile.access.roles"),
                roles.stream().map(RoleInfoDto::getName), "status-chip--info"));

        Set<String> applications = new LinkedHashSet<>();
        for (RoleInfoDto role : roles) {
            if (role.getAllowedTools() != null) {
                role.getAllowedTools().stream()
                        .map(ProfileOverviewPanel::applicationName)
                        .filter(name -> name != null && !name.isBlank())
                        .forEach(applications::add);
            }
        }
        card.add(chipGroup(I18n.t("profile.access.applications"), applications.stream(), "status-chip--neutral"));
        return card;
    }

    private Div buildLastSignInCard(AccountStatDto stats) {
        return card(I18n.t("profile.section.lastSignIn"),
                field(VaadinIcon.CLOCK, I18n.t("profile.field.signInTime"),
                        ProfileFormatUtils.formatDateTime(stats.getLastLogin())),
                field(VaadinIcon.DESKTOP, I18n.t("profile.field.device"), stats.getLastLoginDevice()),
                field(VaadinIcon.BROWSER, I18n.t("profile.field.browser"), stats.getLastLoginBrowser()),
                field(VaadinIcon.CONNECT, I18n.t("profile.field.ipAddress"), stats.getLastLoginIp()),
                field(VaadinIcon.TOOLS, I18n.t("profile.field.application"), stats.getLastLoginApp()));
    }

    private static String applicationName(ApplicationDto application) {
        if (application.getTitle() != null && !application.getTitle().isBlank()) {
            return application.getTitle();
        }
        return application.getName();
    }

    private static String authTypeLabel(IEnumAuth.Types type) {
        return type == null ? null : I18n.t("profile.authmethod.type." + type.name());
    }

    private static Div card(String titleText, Div... fields) {
        Div card = new Div();
        card.addClassName("profile-section-card");

        H3 title = new H3(titleText);
        title.addClassName("section-title");

        Div grid = new Div();
        grid.addClassName("wams-card__detail-grid");
        grid.add(fields);

        card.add(title, grid);
        return card;
    }

    private static Div chipGroup(String label, Stream<String> values, String variantClass) {
        Div group = new Div();
        group.addClassName("profile-chip-group");

        Span groupLabel = new Span(label);
        groupLabel.addClassName("wams-card__detail-field-label");
        group.add(groupLabel);

        Div chips = new Div();
        chips.addClassName("profile-badge-row");
        long[] count = {0};
        values.filter(v -> v != null && !v.isBlank()).forEach(v -> {
            Span chip = new Span(v);
            chip.addClassName("status-chip");
            chip.addClassName(variantClass);
            chips.add(chip);
            count[0]++;
        });
        if (count[0] == 0) {
            Span none = new Span("—");
            none.addClassName("wams-card__detail-field-value");
            chips.add(none);
        }
        group.add(chips);
        return group;
    }

    private static Div field(VaadinIcon icon, String label, String value) {
        Div field = new Div();
        field.addClassName("wams-card__detail-field");

        Div labelRow = new Div();
        labelRow.addClassName("wams-card__detail-field-label-row");
        Icon iconEl = icon.create();
        iconEl.addClassName("detail-field-icon");
        iconEl.setSize("16px");
        Span labelSpan = new Span(label);
        labelSpan.addClassName("wams-card__detail-field-label");
        labelRow.add(iconEl, labelSpan);

        Span valueSpan = new Span(value != null && !value.isBlank() ? value : "—");
        valueSpan.addClassName("wams-card__detail-field-value");

        field.add(labelRow, valueSpan);
        return field;
    }
}
