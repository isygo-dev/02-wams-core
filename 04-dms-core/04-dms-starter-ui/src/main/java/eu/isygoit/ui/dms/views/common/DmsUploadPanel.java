package eu.isygoit.ui.dms.views.common;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Single-file upload widget of the DMS dialogs: drop zone, selected file
 * summary and status line. The SMS module has its own panel of the same shape
 * (bundles and limits differ per module).
 */
@Slf4j
public final class DmsUploadPanel extends VerticalLayout {

    /** Maximum size of one uploaded file (50 MB). */
    public static final int MAX_FILE_BYTES = 50 * 1024 * 1024;

    /** Document types of the shared dialogs plus plain text and zip archives. */
    public static final String[] ACCEPTED_MIME_TYPES = Stream.concat(
            Stream.of(DialogUploads.DOCUMENT_MIME_TYPES),
            Stream.of("text/plain", "application/zip")).toArray(String[]::new);

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
    public DmsUploadPanel(Consumer<Boolean> onReadyChanged) {
        setPadding(false);
        setSpacing(true);
        setWidthFull();

        Span instruction = new Span(I18n.t("dms.linkedfile.dialog.upload.instruction"));
        instruction.addClassNames(LumoUtility.FontSize.SMALL, "dms-secondary-text", "dms-upload-instruction");

        Upload upload = new Upload(memoryBuffer);
        upload.setDropAllowed(true);
        upload.setMaxFiles(1);
        upload.setMaxFileSize(MAX_FILE_BYTES);
        upload.setAcceptedFileTypes(ACCEPTED_MIME_TYPES);
        upload.addClassNames("wams-upload-component", "dms-upload-component");

        fileNameDisplay.setText(I18n.t("dms.linkedfile.dialog.upload.no.file.selected"));
        fileNameDisplay.addClassNames("wams-upload-file-name", "dms-secondary-text");
        fileSizeDisplay.addClassName("wams-upload-file-size");
        uploadStatus.addClassNames("wams-upload-status", "dms-upload-status");
        uploadStatus.getElement().setAttribute("role", "status");
        uploadIcon.addClassName("dms-upload-icon");
        uploadIcon.getElement().setAttribute("aria-hidden", "true");

        HorizontalLayout fileInfo = new HorizontalLayout(uploadIcon, fileNameDisplay, fileSizeDisplay);
        fileInfo.setAlignItems(FlexComponent.Alignment.CENTER);
        fileInfo.setSpacing(true);
        fileInfo.addClassNames("wams-upload-file-info", DialogLayout.CLASS_ROW);

        upload.addSucceededListener(event -> {
            uploadedFileName = event.getFileName();
            try {
                byte[] bytes = memoryBuffer.getInputStream().readAllBytes();
                uploadedFile = new ByteArrayMultipartFile(bytes, uploadedFileName,
                        memoryBuffer.getFileData().getMimeType());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read uploaded file into memory", e);
            }
            fileNameDisplay.setText(I18n.t("dms.linkedfile.dialog.upload.file.name", uploadedFileName));
            fileSizeDisplay.setText(I18n.t("dms.linkedfile.dialog.upload.file.size",
                    LinkedFilesSupport.formatFileSize(uploadedFile.getSize())));
            setStatus(true, I18n.t("dms.linkedfile.dialog.upload.success"));
            onReadyChanged.accept(true);
            log.info("File uploaded: {}", uploadedFileName);
        });
        upload.addFailedListener(event -> {
            setStatus(false, I18n.t("dms.linkedfile.dialog.upload.failed", event.getReason().getMessage()));
            onReadyChanged.accept(false);
            log.error("Upload failed: {}", event.getReason().getMessage());
        });
        upload.addFileRejectedListener(event -> {
            setStatus(false, I18n.t("dms.linkedfile.dialog.upload.rejected", event.getErrorMessage()));
            onReadyChanged.accept(false);
            log.warn("File rejected: {}", event.getErrorMessage());
        });

        add(instruction, upload, fileInfo, uploadStatus);
    }

    private void setStatus(boolean success, String message) {
        uploadStatus.setText(message);
        uploadStatus.removeClassNames("dms-upload-status--success", "dms-upload-status--error");
        uploadIcon.removeClassNames("dms-upload-icon--success", "dms-upload-icon--error");
        uploadStatus.addClassName(success ? "dms-upload-status--success" : "dms-upload-status--error");
        uploadIcon.addClassName(success ? "dms-upload-icon--success" : "dms-upload-icon--error");
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
