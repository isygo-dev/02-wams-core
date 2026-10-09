package eu.isygoit.ui.mms.views.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.exception.TransferNotSupportedException;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Helpers shared by the MMS dialogs: error extraction, e-mail validation,
 * in-memory {@link MultipartFile} creation and browser-side file download.
 */
@Slf4j
public final class MmsDialogSupport {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Browser-side download of a base64 payload ($0) under a given file name ($1). */
    private static final String DOWNLOAD_SCRIPT =
            "const byteCharacters = atob($0);" +
                    "const byteNumbers = new Array(byteCharacters.length);" +
                    "for (let i = 0; i < byteCharacters.length; i++) {" +
                    "    byteNumbers[i] = byteCharacters.charCodeAt(i);" +
                    "}" +
                    "const byteArray = new Uint8Array(byteNumbers);" +
                    "const blob = new Blob([byteArray]);" +
                    "const url = URL.createObjectURL(blob);" +
                    "const a = document.createElement('a');" +
                    "a.href = url;" +
                    "a.download = $1;" +
                    "a.click();" +
                    "URL.revokeObjectURL(url);";

    private MmsDialogSupport() {
    }

    /** Remote error body for client/server errors (400/500), otherwise the exception message. */
    public static String errorMessage(FeignException ex) {
        return (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
    }

    /** Simple e-mail validation; an empty value is valid (optional field). */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return true;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /** Content type deduced from the file extension of a template file. */
    public static String contentTypeOf(String fileName, String defaultType) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".html") || name.endsWith(".htm")) {
            return "text/html";
        }
        if (name.endsWith(".xml")) {
            return "application/xml";
        }
        if (name.endsWith(".json")) {
            return "application/json";
        }
        if (name.endsWith(".txt") || name.endsWith(".ftl") || name.endsWith(".vm")
                || name.endsWith(".properties")) {
            return "text/plain";
        }
        return defaultType;
    }

    /**
     * Wraps in-memory bytes into a {@link MultipartFile}.
     *
     * @param partName    value returned by {@link MultipartFile#getName()}
     * @param fileName    original file name
     * @param data        file content
     * @param defaultType content type used when the extension is unknown
     */
    public static MultipartFile multipartOf(String partName, String fileName, byte[] data, String defaultType) {
        final String contentType = contentTypeOf(fileName, defaultType);
        return new MultipartFile() {
            @Override
            public String getName() {
                return partName;
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public boolean isEmpty() {
                return data == null || data.length == 0;
            }

            @Override
            public long getSize() {
                return data != null ? data.length : 0;
            }

            @Override
            public byte[] getBytes() {
                return data;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(data);
            }

            @Override
            public void transferTo(java.io.File dest) throws IllegalStateException {
                // Not implemented - use getBytes() or getInputStream() instead
                throw new TransferNotSupportedException("transferTo not supported");
            }
        };
    }

    /** Sends bytes to the browser as a file download. */
    public static void downloadBytes(Component owner, byte[] content, String fileName) {
        String base64Content = Base64.getEncoder().encodeToString(content);
        owner.getUI().ifPresent(ui -> ui.getPage().executeJs(DOWNLOAD_SCRIPT, base64Content, fileName));
    }

    /** Downloads the stored file of a template; shows an error notification on failure. */
    public static void downloadTemplateFile(Component owner,
                                            MsgTemplateFileService templateFileService,
                                            MsgTemplateDto template) {
        if (template.getFileName() == null || template.getFileName().isEmpty()) {
            return;
        }
        try {
            ResponseEntity<Resource> response = templateFileService.downloadFile(template.getId(), 0L);
            if (response.getBody() != null) {
                byte[] content = response.getBody().getInputStream().readAllBytes();
                String fileName = template.getOriginalFileName() != null
                        ? template.getOriginalFileName() : template.getFileName();
                downloadBytes(owner, content, fileName);
            }
        } catch (Exception e) {
            log.error("Failed to download template file for {}", template.getId(), e);
            Notification.show(I18n.t("mms.msgtemplate.download.error", e.getMessage()), 5000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
