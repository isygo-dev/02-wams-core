package eu.isygoit.ui.common.files;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.server.StreamResource;
import eu.isygoit.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Consumer;

import static eu.isygoit.ui.common.files.LinkedFilesSupport.PendingFile;

/**
 * Reusable component that manages a <b>single linked file</b> of an entity.
 * Similar to {@link AdditionalFilesManager} but for a single file field.
 */
@Slf4j
@CssImport(value = "./styles/upload-file-list-theme.scss", themeFor = "vaadin-upload")
@CssImport("./styles/scss/common.scss")
public class OneFilesManager extends VerticalLayout {

    private final VerticalLayout container = new VerticalLayout();
    private final HorizontalLayout existingFileRow = new HorizontalLayout();
    private final HorizontalLayout stagedFileRow = new HorizontalLayout();

    private String fileName;
    private String fileType;
    private Long fileId;

    private PendingFile pendingFile;
    private boolean markedForDeletion = false;

    private String title;
    private String dropLabel;
    private boolean readOnly = false;
    private boolean staging = false;
    private int maxFileSize = 25 * 1024 * 1024;
    private String[] acceptedMimeTypes;

    private Downloader downloader;
    private Uploader uploader;
    private Consumer<Boolean> onDeleteStateChanged;
    private Consumer<PendingFile> onFileStaged;

    public OneFilesManager() {
        setSpacing(true);
        setPadding(false);
        setWidthFull();

        container.setSpacing(true);
        container.setPadding(false);
        container.setWidthFull();
        add(container);
    }

    public static OneFilesManager create() {
        return new OneFilesManager();
    }

    public OneFilesManager title(String title) {
        this.title = title;
        return this;
    }

    public OneFilesManager dropLabel(String label) {
        this.dropLabel = label;
        return this;
    }

    public OneFilesManager readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public OneFilesManager staging(boolean staging) {
        this.staging = staging;
        return this;
    }

    public OneFilesManager maxFileSize(int bytes) {
        this.maxFileSize = bytes;
        return this;
    }

    public OneFilesManager acceptedMimeTypes(String... mimeTypes) {
        this.acceptedMimeTypes = mimeTypes;
        return this;
    }

    public OneFilesManager downloader(Downloader downloader) {
        this.downloader = downloader;
        return this;
    }

    public OneFilesManager uploader(Uploader uploader) {
        this.uploader = uploader;
        return this;
    }

    public OneFilesManager onDeleteStateChanged(Consumer<Boolean> callback) {
        this.onDeleteStateChanged = callback;
        return this;
    }

    public OneFilesManager onFileStaged(Consumer<PendingFile> callback) {
        this.onFileStaged = callback;
        return this;
    }

    public void setFile(String name, String type, Long id) {
        this.fileName = name;
        this.fileType = type;
        this.fileId = id;
        this.markedForDeletion = false;
        this.pendingFile = null;
        render();
    }

    public OneFilesManager build() {
        render();
        return this;
    }

    private void render() {
        container.removeAll();

        if (title != null && !title.isBlank()) {
            Span titleSpan = new Span(title);
            titleSpan.addClassName("files-section__title");
            container.add(titleSpan);
        }

        renderExistingFile();
        renderStagedFile();

        if (!readOnly && pendingFile == null && (fileName == null || markedForDeletion)) {
            container.add(buildUploadArea());
        }
    }

    private void renderExistingFile() {
        existingFileRow.removeAll();
        if (markedForDeletion || fileName == null || fileName.isBlank()) {
            existingFileRow.setVisible(false);
            return;
        }

        existingFileRow.setAlignItems(FlexComponent.Alignment.CENTER);
        existingFileRow.setSpacing(true);
        existingFileRow.setWidthFull();
        existingFileRow.addClassNames("file-row", "file-row--existing");

        Icon icon = LinkedFilesSupport.getFileIcon(fileType);
        icon.addClassName("file-row__icon");

        Span nameSpan = new Span(fileName);
        nameSpan.addClassName("file-row__name");
        nameSpan.setTitle(fileName);

        existingFileRow.add(icon, nameSpan);

        if (downloader != null && fileId != null) {
            Anchor download = new Anchor(buildDownloadResource(), "");
            download.add(VaadinIcon.DOWNLOAD.create());
            download.setTarget("_blank");
            download.addClassName("file-row__download");
            download.getElement().setAttribute("title", I18n.t("files.download.tooltip"));
            existingFileRow.add(download);
        }

        if (!readOnly) {
            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
            deleteBtn.setTooltipText(I18n.t("files.delete.tooltip"));
            deleteBtn.addClickListener(e -> {
                markedForDeletion = true;
                if (onDeleteStateChanged != null) onDeleteStateChanged.accept(true);
                render();
            });
            existingFileRow.add(deleteBtn);
        }

        existingFileRow.expand(nameSpan);
        existingFileRow.setVisible(true);
        container.add(existingFileRow);
    }

    private void renderStagedFile() {
        stagedFileRow.removeAll();
        if (pendingFile == null) {
            stagedFileRow.setVisible(false);
            return;
        }

        stagedFileRow.setAlignItems(FlexComponent.Alignment.CENTER);
        stagedFileRow.setSpacing(true);
        stagedFileRow.setWidthFull();
        stagedFileRow.addClassName("file-row");

        Icon fileIcon = LinkedFilesSupport.getFileIcon(pendingFile.mimeType);
        fileIcon.addClassName("file-row__icon");

        Span nameSpan = new Span(pendingFile.name);
        nameSpan.addClassName("file-row__name");
        nameSpan.setTitle(pendingFile.name);

        Span pendingChip = new Span(I18n.t("files.pending"));
        pendingChip.addClassName("status-chip");
        pendingChip.addClassName("status-chip--pending");

        Button removeBtn = new Button(VaadinIcon.CLOSE_SMALL.create());
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        removeBtn.setTooltipText(I18n.t("files.remove.pending"));
        removeBtn.addClickListener(e -> {
            pendingFile = null;
            if (onFileStaged != null) onFileStaged.accept(null);
            render();
        });

        stagedFileRow.add(fileIcon, nameSpan, pendingChip, removeBtn);
        stagedFileRow.expand(nameSpan);
        stagedFileRow.setVisible(true);
        container.add(stagedFileRow);
    }

    private Component buildUploadArea() {
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setWidthFull();
        upload.addClassName("files-section__upload");
        upload.addClassName("upload-dragger");
        upload.addClassName("hide-upload-file-list");
        upload.setMaxFiles(1);
        upload.setAutoUpload(true);
        upload.setMaxFileSize(maxFileSize);

        if (dropLabel != null && !dropLabel.isBlank()) {
            upload.setDropLabel(new Span(dropLabel));
        }
        if (acceptedMimeTypes != null && acceptedMimeTypes.length > 0) {
            upload.setAcceptedFileTypes(acceptedMimeTypes);
        }

        upload.addSucceededListener(event -> {
            try {
                String name = event.getFileName();
                String mime = event.getMIMEType();
                byte[] content = buffer.getInputStream().readAllBytes();

                if (staging || uploader == null) {
                    pendingFile = new PendingFile(name, mime, content);
                    if (onFileStaged != null) onFileStaged.accept(pendingFile);
                    render();
                } else {
                    uploader.upload(new ByteArrayInputStream(content), name, mime, event.getContentLength());
                    // In immediate mode, we expect the caller to update the file state via setFile() 
                    // or handle it themselves. But usually OneFilesManager is used in staging mode in dialogs.
                }
            } catch (Exception ex) {
                log.error("Upload failed", ex);
                Notification.show(I18n.t("files.upload.error", ex.getMessage()),
                                5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        return upload;
    }

    private StreamResource buildDownloadResource() {
        return new StreamResource(fileName, () -> {
            try {
                ResponseEntity<Resource> resp = downloader.download(fileId, null);
                if (resp != null && resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    return resp.getBody().getInputStream();
                }
            } catch (Exception ex) {
                log.error("Download failed for file {}", fileId, ex);
            }
            throw new RuntimeException(I18n.t("files.download.failed"));
        });
    }

    public boolean isMarkedForDeletion() {
        return markedForDeletion;
    }

    public PendingFile getPendingFile() {
        return pendingFile;
    }

    @FunctionalInterface
    public interface Downloader {
        ResponseEntity<Resource> download(Long fileId, Long version);
    }

    @FunctionalInterface
    public interface Uploader {
        void upload(InputStream content, String originalFileName, String contentType, long size) throws Exception;
    }
}
