package eu.isygoit.ui.ims.views.common;

import feign.FeignException;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * Small static helpers shared by the IMS dialogs (error message extraction and
 * image download rendering) so they are not copy-pasted in every dialog.
 */
public final class ImsDialogSupport {

    private static final String JPEG_DATA_URI_PREFIX = "data:image/jpeg;base64,";

    private ImsDialogSupport() {
    }

    /** Returns the UTF-8 body of a Feign error when present, otherwise the exception message. */
    public static String extractErrorMessage(FeignException ex) {
        try {
            String content = ex.contentUTF8();
            if (content != null && !content.isBlank()) {
                return content;
            }
        } catch (Exception ignored) {
            // fall back to the exception message
        }
        return ex.getMessage();
    }

    /** Reads the whole resource into memory. */
    public static byte[] toByteArray(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        }
    }

    /**
     * Converts a downloaded image response into a JPEG data URI usable as an image source.
     *
     * @return the data URI, or {@code null} when the response is not successful, empty or unreadable
     */
    public static String toImageDataUri(ResponseEntity<Resource> response) {
        try {
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return JPEG_DATA_URI_PREFIX + Base64.getEncoder().encodeToString(toByteArray(response.getBody()));
            }
        } catch (Exception ignored) {
            // no readable image: the caller keeps its placeholder
        }
        return null;
    }
}
