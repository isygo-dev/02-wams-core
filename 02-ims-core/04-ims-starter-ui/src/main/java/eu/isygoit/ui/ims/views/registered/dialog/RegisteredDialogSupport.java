package eu.isygoit.ui.ims.views.registered.dialog;

import feign.FeignException;

/**
 * Small helper shared by the registered-user dialogs. Package-private on purpose.
 */
final class RegisteredDialogSupport {

    private RegisteredDialogSupport() {
    }

    /** Server error body when available, otherwise the exception message. */
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
