package eu.isygoit.ui.common.profile;

import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.i18n.I18n;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * Formatting helpers shared by the profile page and its panels/cards, so date/relative-time/device-icon logic
 * lives in one place instead of being copied into each component. Dates follow the user's UI locale.
 */
final class ProfileFormatUtils {

    private ProfileFormatUtils() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_FMT);
    }

    static String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE_FMT);
    }

    static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME_FMT);
    }

    static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * "Today", "Yesterday" or the formatted date: the heading of a day group in the activity timeline.
     */
    static String formatDayLabel(LocalDate day) {
        if (day == null) {
            return I18n.t("profile.time.unknown");
        }
        LocalDate today = LocalDate.now();
        if (day.equals(today)) {
            return I18n.t("profile.time.today");
        }
        if (day.equals(today.minusDays(1))) {
            return I18n.t("profile.time.yesterday");
        }
        return formatDate(day);
    }

    static int totalSessions(AccountStatDto stats) {
        return stats != null && stats.getTotalConnections() != null ? stats.getTotalConnections() : 0;
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

    static String formatLanguage(Enum<?> language) {
        return I18n.enumLabel(language);
    }

    static String formatAdminStatus(Enum<?> status) {
        return I18n.enumLabel(status);
    }

    static Date toDate(LocalDateTime dateTime) {
        return dateTime != null ? Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    static String formatAbsoluteAndRelative(Date date) {
        if (date == null) {
            return I18n.t("profile.time.unknown");
        }
        LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return formatDateTime(ldt) + " · " + formatRelativeTime(date);
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
