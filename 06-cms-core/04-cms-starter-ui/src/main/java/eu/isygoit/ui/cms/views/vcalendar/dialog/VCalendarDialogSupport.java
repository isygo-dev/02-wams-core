package eu.isygoit.ui.cms.views.vcalendar.dialog;

import feign.FeignException;

/**
 * Helpers shared by the calendar dialogs.
 */
final class VCalendarDialogSupport {

    /** Shared style hook added by every cms dialog. */
    static final String CLASS_CMS_DIALOG = "cms-dialog";

    private VCalendarDialogSupport() {
    }

    /** Returns the remote error body when available, otherwise the exception message. */
    static String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) {
                return ex.contentUTF8();
            }
        } catch (Exception ignored) {
            // fall back to the exception message
        }
        return ex.getMessage();
    }
}
