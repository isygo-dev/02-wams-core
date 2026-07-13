package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;

/**
 * Top-level key-metric row: member since, last active, total sessions, roles.
 */
class ProfileStatsPanel extends StatCardGrid {

    ProfileStatsPanel(AccountDto account, AccountStatDto stats) {
        super(
                new StatCard(VaadinIcon.CALENDAR, StatCard.Variant.PRIMARY,
                        I18n.t("profile.stat.member.since"),
                        stats.getCreateDate() != null ? stats.getCreateDate().format(ProfileFormatUtils.DATE_FMT) : "N/A"),
                new StatCard(VaadinIcon.CLOCK, StatCard.Variant.NEUTRAL,
                        I18n.t("profile.stat.last.active"),
                        stats.getLastLogin() != null
                                ? ProfileFormatUtils.formatRelativeTime(ProfileFormatUtils.toDate(stats.getLastLogin()))
                                : I18n.t("profile.time.never")),
                new StatCard(VaadinIcon.SIGN_IN, StatCard.Variant.SUCCESS,
                        I18n.t("profile.stat.total.sessions"),
                        String.valueOf(ProfileFormatUtils.totalSessions(account, stats))),
                new StatCard(VaadinIcon.USERS, StatCard.Variant.PRIMARY,
                        I18n.t("profile.stat.roles"),
                        stats.getRoleCount() != null ? String.valueOf(stats.getRoleCount()) : "0",
                        stats.getTotalPermissions() != null
                                ? I18n.t("profile.stat.permissions.tooltip", stats.getTotalPermissions())
                                : null)
        );
    }
}
