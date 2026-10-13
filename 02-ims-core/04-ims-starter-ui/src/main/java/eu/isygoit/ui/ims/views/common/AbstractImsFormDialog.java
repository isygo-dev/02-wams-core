package eu.isygoit.ui.ims.views.common;

import eu.isygoit.ui.common.dialog.ProfilePhotoSection;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

/**
 * Shared base for the IMS create/update form dialogs. Centralizes the profile
 * image download pattern used consistently by the account, customer and tenant
 * dialogs:
 *
 * <ul>
 *   <li>{@link #downloadProfileImage(Long)} — hook implemented by the concrete
 *       abstract form (e.g. {@code AbstractAccountFormDialog}) using the
 *       matching image service;</li>
 *   <li>{@link #showExistingPhoto(ProfilePhotoSection, Long)} /
 *       {@link #showExistingPhoto(ProfilePhotoSection, Long, String)} — called
 *       from the subclass's {@code fillFrom(...)} to display the avatar,
 *       downloading first and (optionally) falling back to a static
 *       {@code imagePath} when the download yields nothing.</li>
 * </ul>
 *
 * <p>The download is performed on the UI thread, so implementations may
 * perform blocking I/O (e.g. a Feign call).
 */
@Slf4j
public abstract class AbstractImsFormDialog extends ImsActionDialog {

    protected AbstractImsFormDialog(String title, Runnable onSuccess) {
        super(title, onSuccess);
    }

    private static String toImageDataUri(byte[] bytes) {
        return "data:" + detectImageMime(bytes) + ";base64,"
                + Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Best-effort sniff of the image MIME type; defaults to JPEG.
     */
    private static String detectImageMime(byte[] b) {
        if (b.length >= 4) {
            if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
            if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
            if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "image/gif";
        }
        return "image/jpeg";
    }

    /**
     * Hook for subclasses that want to <em>download</em> the profile image
     * rather than rely on a static path. Default returns {@code null}.
     *
     * @param id the entity id (never {@code null} when this method is invoked)
     * @return raw image bytes, or {@code null}/empty to skip
     */
    protected byte[] downloadProfileImage(Long id) {
        return null;
    }

    /**
     * Shows the existing profile photo in the given section, downloading the
     * bytes via {@link #downloadProfileImage(Long)}.
     */
    protected final void showExistingPhoto(ProfilePhotoSection section, Long id) {
        showExistingPhoto(section, id, null);
    }

    /**
     * Shows the existing profile photo in the given section by preferring
     * freshly-downloaded bytes and falling back to {@code fallbackPath} when
     * the download yields nothing. No-op if the section is {@code null} or
     * both sources are blank.
     */
    protected final void showExistingPhoto(ProfilePhotoSection section, Long id, String fallbackPath) {
        if (section == null) return;
        String src = resolveAvatar(id, fallbackPath);
        if (src != null && !src.isBlank()) {
            section.showExisting(src);
        }
    }

    private String resolveAvatar(Long id, String fallbackPath) {
        if (id != null) {
            try {
                byte[] bytes = downloadProfileImage(id);
                if (bytes != null && bytes.length > 0) {
                    return toImageDataUri(bytes);
                }
            } catch (Exception e) {
                log.warn("Failed to download profile image for id {}", id, e);
            }
        }
        return fallbackPath;
    }
}