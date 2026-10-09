package eu.isygoit.ui.sms.views.common;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.common.dialog.DialogUploads;
import eu.isygoit.ui.common.files.LinkedFilesSupport;
import eu.isygoit.util.ByteArrayMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Single-file upload widget of the SMS dialogs: drop zone, selected file
 * summary and status line. The DMS module has its own panel of the same shape
 * (bundles and limits differ per module).
 */
public final class SmsUploadPanel extends VerticalLayout {

    /** Maximum size of one uploaded object (100 MB). */
    public static final int MAX_FILE_BYTES = 100 * 1024 * 1024;

    /** Document types of the shared dialogs plus text, archive and data formats. */
    public static final String[] ACCEPTED_MIME_TYPES = Stream.concat(
            Stream.of(DialogUploads.DOCUMENT_MIME_TYPES),
            Stream.of("text/plain", "application/zip", "application/json", "text/csv")).toArray(String[]::new);

    private final MemoryBuffer memoryBuffer = new MemoryBuffer();
    private final Span fileNameDisplay = new Span();
    private final Span fileSizeDisplay = new Span();
    private final Span uploadStatus = new Span();
    private final Icon uploadIcon = VaadinIcon.UPLOAD.create();

    private String uploadedFileName;
    private MultipartFile uploadedFile;

    /**
     * @param onReadyChanged called with {@code true} once a file is ready and
     *                       {@code false} when the upload failed or was rejected
     */
    public SmsUploadPanel(Consumer<Boolean> onReadyChanged) {
        setPadding(false);
        setSpacing(true);
        setWidthFull();

        Span instruction = new Span(I18n.t("sms.objects.dialog.upload.instruction"));
        instruction.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        Upload upload = new Upload(memoryBuffer);
        upload.setDropAllowed(true);
        upload.setMaxFiles(1);
        upload.setMaxFileSize(MAX_FILE_BYTES);
        upload.setAcceptedFileTypes(ACCEPTED_MIME_TYPES);
        upload.addClassName("wams-upload-component");

        fileNameDisplay.setText(I18n.t("sms.objects.dialog.upload.no.file.selected"));
        fileNameDisplay.addClassName(LumoUtility.TextColor.SECONDARY);
        uploadStatus.getElement().setAttribute("role", "status");
        uploadIcon.addClassName("wams-upload-icon");
        uploadIcon.getElement().setAttribute("aria-hidden", "true");

        HorizontalLayout fileInfo = new HorizontalLayout(uploadIcon, fileNameDisplay, fileSizeDisplay);
        fileInfo.setAlignItems(FlexComponent.Alignment.CENTER);
        fileInfo.setSpacing(true);
        fileInfo.addClassName(DialogLayout.CLASS_ROW);

        upload.addSucceededListener(event -> {
            uploadedFileName = event.getFileName();
            try {
                byte[] bytes = memoryBuffer.getInputStream().readAllBytes();
                uploadedFile = new ByteArrayMultipartFile(bytes, uploadedFileName,
                        memoryBuffer.getFileData().getMimeType());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            fileNameDisplay.setText(I18n.t("sms.objects.dialog.upload.file.name", uploadedFileName));
            fileSizeDisplay.setText(I18n.t("sms.objects.dialog.upload.file.size",
                    LinkedFilesSupport.formatFileSize(uploadedFile.getSize())));
            setStatus(true, I18n.t("sms.objects.dialog.upload.success"));
            onReadyChanged.accept(true);
        });
        upload.addFailedListener(event -> {
            setStatus(false, I18n.t("sms.objects.dialog.upload.failed", event.getReason().getMessage()));
            onReadyChanged.accept(false);
        });
        upload.addFileRejectedListener(event -> {
            setStatus(false, I18n.t("sms.objects.dialog.upload.rejected", event.getErrorMessage()));
            onReadyChanged.accept(false);
        });

        add(instruction, upload, DialogLayout.help(I18n.t("sms.objects.dialog.upload.allowed.types")),
                fileInfo, uploadStatus);
    }

    /** Toggles the success/error state of the status line and its icon via CSS classes. */
    private void setStatus(boolean success, String message) {
        uploadStatus.setText(message);
        uploadStatus.removeClassNames("wams-upload-status--success", "wams-upload-status--error");
        uploadIcon.removeClassNames("wams-upload-icon--success", "wams-upload-icon--error");
        uploadStatus.addClassName(success ? "wams-upload-status--success" : "wams-upload-status--error");
        uploadIcon.addClassName(success ? "wams-upload-icon--success" : "wams-upload-icon--error");
    }

    /** The uploaded file as a multipart file, or {@code null} while none is ready. */
    public MultipartFile getUploadedFile() {
        return uploadedFile;
    }

    /** Original name of the uploaded file, or {@code null} while none is ready. */
    public String getUploadedFileName() {
        return uploadedFileName;
    }
}
