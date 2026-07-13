package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.i18n.I18n;

/**
 * Overview tab: a read-only detail grid of every available profile field.
 * Editing happens through {@link EditProfileDialog}, opened from the header.
 */
class ProfileOverviewPanel extends VerticalLayout {

    ProfileOverviewPanel(AccountDto account, AccountStatDto stats) {
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        addClassName("profile-tab-panel");

        Div card = new Div();
        card.addClassName("profile-section-card");

        H3 title = new H3(I18n.t("profile.section.personal"));
        title.addClassName("section-title");

        Div grid = new Div();
        grid.addClassName("wams-card__detail-grid");

        AccountDetailsDto details = account.getAccountDetails();
        grid.add(
                buildDetailField(VaadinIcon.ENVELOPE, I18n.t("profile.field.email"), account.getEmail()),
                buildDetailField(VaadinIcon.PHONE, I18n.t("profile.field.phone"), account.getPhoneNumber()),
                buildDetailField(VaadinIcon.OFFICE, I18n.t("profile.field.tenant"), account.getTenant()),
                buildDetailField(VaadinIcon.GLOBE, I18n.t("profile.field.country"), details != null ? details.getCountry() : null),
                buildDetailField(VaadinIcon.FLAG, I18n.t("profile.field.language"),
                        account.getLanguage() != null ? account.getLanguage().name() : null),
                buildDetailField(VaadinIcon.BRIEFCASE, I18n.t("profile.field.role"), account.getFunctionRole()),
                buildDetailField(VaadinIcon.KEY, I18n.t("profile.field.accountType"), ProfileFormatUtils.formatAccountType(account.getAccountType())),
                buildDetailField(VaadinIcon.CHECK_CIRCLE, I18n.t("profile.field.status"),
                        account.getAdminStatus() != null ? account.getAdminStatus().name() : null),
                buildDetailField(VaadinIcon.CALENDAR, I18n.t("profile.field.memberSince"),
                        stats.getCreateDate() != null ? stats.getCreateDate().format(ProfileFormatUtils.DATE_FMT) : null),
                buildDetailField(VaadinIcon.CLOCK, I18n.t("profile.field.lastActive"),
                        stats.getLastLogin() != null ? stats.getLastLogin().format(ProfileFormatUtils.DATETIME_FMT) : null)
        );

        card.add(title, grid);
        add(card);
    }

    private Div buildDetailField(VaadinIcon icon, String label, String value) {
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
