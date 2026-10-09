package eu.isygoit.ui.sms.views.common;

import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import feign.FeignException;

import java.time.LocalDateTime;

/**
 * Helpers shared by the SMS dialogs (error extraction, date formatting).
 */
public final class SmsDialogSupport {

    private SmsDialogSupport() {
    }

    /** Server error body when available, otherwise the exception message. */
    public static String extractErrorMessage(FeignException ex) {
        try {
            String body = ex.contentUTF8();
            if (body != null && !body.isBlank()) {
                return body;
            }
        } catch (Exception ignored) {
            // fall back to the exception message
        }
        return ex.getMessage() != null ? ex.getMessage() : I18n.t("common.notification.error");
    }

    /** Human-readable date/time, or {@code null} when absent (shown as a dash by the details dialogs). */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : DateHelper.formatToHumanReadable(dateTime);
    }
}
