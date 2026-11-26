package eu.isygoit.ui.common.files;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.server.StreamResource;
import eu.isygoit.dto.common.FileEntityDto;
import eu.isygoit.exception.StorageException;
import eu.isygoit.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static eu.isygoit.ui.common.files.LinkedFilesSupport.CategoryUpdater;
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

    private String fileName;
    private String fileType;
    private Long fileId;
    private String fileCategory;
    private String fileExtension;
    private Long fileSize;
    private LocalDateTime fileDate;

    private PendingFile pendingFile;
    private boolean markedForDeletion = false;

    private String title;
    private String dropLabel;
    private boolean readOnly = false;
    private boolean staging = false;
    private int maxFileSize = 25 * 1024 * 1024;
    private String[] acceptedMimeTypes;

    private boolean categoryEditable = true;
    private CategoryUpdater categoryUpdater;
    private BiFunction<String, Consumer<String>, Component> categoryEditorFactory;

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

    /**
     * Makes the category an input (a tag otherwise): typed on the staged file, saved through the
     * {@link #categoryUpdater(CategoryUpdater)} for the stored one. Never in read-only mode.
     */
    public OneFilesManager categoryEditable(boolean categoryEditable) {
        this.categoryEditable = categoryEditable;
        return this;
    }

    public OneFilesManager categoryUpdater(CategoryUpdater categoryUpdater) {
        this.categoryUpdater = categoryUpdater;
        return this;
    }

    /**
     * Replaces the free-text category field of the row with a caller-supplied
     * editor (e.g. an annex-backed picker). When {@code null}, the row falls
     * back to the default {@link com.vaadin.flow.component.textfield.TextField}.
     * Only consulted while the category is editable.
     * <p>
     * The factory receives the current category value and the {@code onChange}
     * callback the returned component must invoke when the user picks or enters
     * a new value.
     */
    public OneFilesManager categoryEditor(
            BiFunction<String, Consumer<String>, Component> factory) {
        this.categoryEditorFactory = factory;
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
        if (!Objects.equals(this.fileId, id)) {
            this.fileCategory = null;
            this.fileExtension = null;
            this.fileSize = null;
            this.fileDate = null;
        }
        this.fileName = name;
        this.fileType = type;
        this.fileId = id;
        this.markedForDeletion = false;
        this.pendingFile = null;
        render();
    }

    /**
     * Shows the stored file with all its columns (category, upload date, size, type).
     */
    public void setFile(FileEntityDto<Long> dto) {
        if (dto == null) {
            clearFile();
            return;
        }
        setFile(dto.getOriginalFileName(), dto.getMimetype(), dto.getId());
        this.fileCategory = dto.getFileCategory();
        this.fileExtension = dto.getExtension();
        this.fileSize = dto.getSize();
        // the audit of the file itself (last upload, else first upload); empty when it was never recorded
        this.fileDate = dto.getFileUpdateDate() != null ? dto.getFileUpdateDate() : dto.getFileCreateDate();
        render();
    }

    public void clearFile() {
        this.fileName = null;
        this.fileType = null;
        this.fileId = null;
        this.fileCategory = null;
        this.fileExtension = null;
        this.fileSize = null;
        this.fileDate = null;
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
        if (markedForDeletion || fileName == null || fileName.isBlank()) {
            return;
        }

        List<Component> actions = new ArrayList<>();
        if (downloader != null && fileId != null) {
            Anchor download = new Anchor(buildDownloadResource(), "");
            Icon downloadIcon = VaadinIcon.DOWNLOAD.create();
            downloadIcon.addClassName("file-row__download-icon");
            download.add(downloadIcon);
            download.setTarget("_blank");
            download.addClassName("file-row__download");
            download.getElement().setAttribute("title", I18n.t("files.download.tooltip"));
            actions.add(download);
        }
        if (!readOnly) {
            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
            deleteBtn.addClassName("file-row__delete");
            deleteBtn.setTooltipText(I18n.t("files.delete.tooltip"));
            deleteBtn.addClickListener(e -> new FileDeleteConfirmDialog(fileName, () -> {
                markedForDeletion = true;
                if (onDeleteStateChanged != null) onDeleteStateChanged.accept(true);
                render();
            }).open());
            actions.add(deleteBtn);
        }

        Div row = LinkedFileRow.build(new LinkedFileRow.Content(fileType, fileName, fileCategory,
                categoryEditable && !readOnly && categoryUpdater != null, this::updateCategory, false, fileDate, fileSize, fileExtension,
                categoryEditorFactory,
                actions.toArray(new Component[0])));
        row.addClassName("file-row--existing");
        container.add(row);
    }

    private void renderStagedFile() {
        if (pendingFile == null) {
            return;
        }

        Button removeBtn = new Button(VaadinIcon.TRASH.create());
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        removeBtn.addClassName("file-row__remove-pending");
        removeBtn.setTooltipText(I18n.t("files.remove.pending"));
        removeBtn.addClickListener(e -> {
            pendingFile = null;
            if (onFileStaged != null) onFileStaged.accept(null);
            render();
        });

        Div row = LinkedFileRow.build(new LinkedFileRow.Content(pendingFile.mimeType, pendingFile.name,
                pendingFile.category, categoryEditable && !readOnly, value -> pendingFile.category = value, true, LocalDateTime.now(),
                (long) pendingFile.content.length, null,
                categoryEditorFactory,
                removeBtn));
        row.getElement().setAttribute("title", I18n.t("files.pending.tooltip"));
        container.add(row);
    }

    private void updateCategory(String category) {
        try {
            if (categoryUpdater.update(fileId, category)) {
                fileCategory = category;
                Notification.show(I18n.t("files.category.updated"), 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                return;
            }
        } catch (Exception ex) {
            log.error("Category update failed for file {}", fileId, ex);
        }
        Notification.show(I18n.t("files.category.error"), 4000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        render(); // back to the stored value
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
                                5000, Notification.Position.BOTTOM_END)
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
            throw new StorageException(I18n.t("files.download.failed"));
        });
    }

    public boolean isMarkedForDeletion() {
        return markedForDeletion;
    }

    public PendingFile getPendingFile() {
        return pendingFile;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFileCategory() {
        return fileCategory;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public LocalDateTime getFileDate() {
        return fileDate;
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