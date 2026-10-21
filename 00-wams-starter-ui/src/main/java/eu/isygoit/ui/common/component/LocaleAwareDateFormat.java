package eu.isygoit.ui.common.component;

import eu.isygoit.i18n.I18n;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Single place that knows the UI's date convention: {@code dd/MM/yyyy} everywhere, except
 * {@code yyyy/MM/dd} for the Arabic locale. Used both by {@link MaskedDatePicker} (typing
 * and calendar display) and by every read-only date display (cards, grids, details
 * dialogs) so the two stay in sync without each hard-coding a pattern.
 */
public final class LocaleAwareDateFormat {

    private static final String DEFAULT_PATTERN = "dd/MM/yyyy";
    private static final String ARABIC_PATTERN = "yyyy/MM/dd";

    private LocaleAwareDateFormat() {
    }

    private static boolean isArabic(Locale locale) {
        return locale != null && "ar".equalsIgnoreCase(locale.getLanguage());
    }

    public static String pattern() {
        return isArabic(I18n.getCurrentLocale()) ? ARABIC_PATTERN : DEFAULT_PATTERN;
    }

    public static String dateTimePattern() {
        return pattern() + " HH:mm";
    }

    public static DateTimeFormatter formatter() {
        return DateTimeFormatter.ofPattern(pattern());
    }

    public static DateTimeFormatter dateTimeFormatter() {
        return DateTimeFormatter.ofPattern(dateTimePattern());
    }

    public static String format(LocalDate date) {
        return date == null ? null : date.format(formatter());
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(dateTimeFormatter());
    }
}
