package eu.isygoit.ui.ims.views.tenant.dialog;

import feign.FeignException;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Base64;

/**
 * Small helpers shared by the tenant dialogs (error text extraction and image
 * conversion). Package-private on purpose.
 */
final class TenantDialogSupport {

    private TenantDialogSupport() {
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

    /** Encodes a downloaded image as a data URI usable as an image source. */
    static String toDataUri(Resource resource) throws IOException {
        byte[] bytes = resource.getContentAsByteArray();
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
