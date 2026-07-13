package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.i18n.I18n;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Formatting and connection-history helpers shared by the profile page and
 * its panels/cards, so date/relative-time/device-icon logic lives in one
 * place instead of being copied into each component.
 */
final class ProfileFormatUtils {

    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private ProfileFormatUtils() {
    }

    static List<ConnectionTrackingDto> connectionHistory(AccountDto account) {
        return account.getConnectionTracking() != null ? account.getConnectionTracking() : List.of();
    }

    static List<ConnectionTrackingDto> recentFirst(List<ConnectionTrackingDto> history) {
        return history.stream()
                .sorted(Comparator.comparing(ConnectionTrackingDto::getLoginDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    static long countWithinDays(List<ConnectionTrackingDto> history, int days) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        return history.stream()
                .filter(c -> c.getLoginDate() != null && c.getLoginDate().toInstant().isAfter(cutoff))
                .count();
    }

    static int totalSessions(AccountDto account, AccountStatDto stats) {
        if (stats.getTotalConnections() != null) {
            return stats.getTotalConnections();
        }
        return connectionHistory(account).size();
    }

    static VaadinIcon deviceIcon(String device) {
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

    static String formatAccountType(String accountType) {
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

    static Date toDate(LocalDateTime dateTime) {
        return dateTime != null ? Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    static String formatAbsoluteAndRelative(Date date) {
        if (date == null) {
            return I18n.t("profile.time.unknown");
        }
        LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return ldt.format(DATETIME_FMT) + " · " + formatRelativeTime(date);
    }

    static String formatRelativeTime(Date date) {
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
}
