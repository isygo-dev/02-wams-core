package eu.isygoit.ui.common.files;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.server.StreamResource;
import eu.isygoit.dto.common.LinkedFileMinDto;
import eu.isygoit.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static eu.isygoit.ui.common.files.LinkedFilesSupport.PendingFile;

/**
 * Reusable component that manages the <b>additional (multi) linked files</b>
 * of any entity implementing {@code IMultiFileEntity<F>} — e.g.
 * {@code AcademicPeriod}, {@code SchoolYear}, {@code SchoolLevel},
 * {@code Budget}, {@code BudgetLineItem}, {@code BudgetAllocation},
 * {@code Institution}, {@code Staff}, {@code Student}, {@code Parent}.
 *
 * <h3>Operating modes</h3>
 * <ul>
 *   <li><b>Immediate upload</b> (default) — dropped files are uploaded right
 *       away via the {@link Uploader} callback; the returned DTO is appended
 *       to the committed list. Requires the parent entity to already exist.</li>
 *   <li><b>Staging</b> — dropped files are queued in {@link #getPendingFiles()}
 *       and rendered with a "pending" chip. The caller flushes them once the
 *       parent entity exists (or is updated). Use this in create dialogs and
 *       when cancel-safety matters.</li>
 *   <li><b>Read-only</b> — no upload area, no delete buttons; only download
 *       (when a {@link Downloader} is supplied).</li>
 * </ul>
 *
 * <h3>Read-only details</h3>
 * <pre>{@code
 * AdditionalFilesManager.from(dto.getAdditionalFiles())
 *     .title(I18n.t("period.field.additional.files"))
 *     .emptyMessage(I18n.t("period.dialog.details.files.empty"))
 *     .readOnly(true)
 *     .downloader((fileId, version) -> service.download(dto.getId(), fileId, version))
 *     .build();
 * }</pre>
 *
 * <h3>Staged upload — create dialog</h3>
 * <pre>{@code
 * filesManager = AdditionalFilesManager.<AcademicPeriodLinkedFileDto>empty()
 *     .title(I18n.t("period.field.additional.files"))
 *     .dropLabel(I18n.t("period.upload.drop.label"))
 *     .staging(true)
 *     .acceptedMimeTypes("application/pdf", "image/*")
 *     .build();
 * // … after service.create() returns an id:
 * for (PendingFile p : filesManager.getPendingFiles()) {
 *     service.uploadAdditionalFile(id, p.toMultipartFile());
 * }
 * filesManager.clearPendingFiles();
 * }</pre>
 *
 * <h3>Staged upload + committed delete — update dialog</h3>
 * <pre>{@code
 * AdditionalFilesManager.from(dto.getAdditionalFiles())
 *     .title(...).staging(true)
 *     .downloader((id, v) -> service.download(dto.getId(), id, v))
 *     .deleter(id -> service.deleteAdditionalFile(dto.getId(), id).getBody())
 *     .onFilesChanged(dto::setAdditionalFiles)
 *     .build();
 * }</pre>
 *
 * @param <F> the concrete linked-file DTO type
 */
@Slf4j
@CssImport(value = "./styles/upload-file-list-theme.scss", themeFor = "vaadin-upload")
public class AdditionalFilesManager<F extends LinkedFileMinDto<Long>> extends VerticalLayout {

    /* ═══════════════════════════════════════════════════════════════
     * Callbacks
     * ═══════════════════════════════════════════════════════════════ */

    private final List<F> files = new ArrayList<>();
    private final List<PendingFile> pendingFiles = new ArrayList<>();
    // ─── UI ───
    private final Span titleSpan = new Span();

    /* ═══════════════════════════════════════════════════════════════
     * State
     * ═══════════════════════════════════════════════════════════════ */
    private final VerticalLayout fileListLayout = new VerticalLayout();
    private final Span emptyState = new Span();
    private String emptyMessage;
    private String dropLabel;
    private boolean readOnly = false;
    private boolean staging = false;

    private Downloader downloader;
    private Uploader<F> uploader;
    private Deleter deleter;
    private Consumer<List<F>> onFilesChanged;

    private int maxFileSize = 25 * 1024 * 1024;   // 25 MB
    private String[] acceptedMimeTypes;
    private Upload uploadComponent;
    private AdditionalFilesManager(List<F> initialFiles) {
        setSpacing(true);
        setPadding(false);
        setWidthFull();
        addClassName("files-section");

        if (initialFiles != null) {
            for (F f : initialFiles) {
                if (f != null) files.add(f);
            }
        }

        titleSpan.addClassName("files-section__title");
        titleSpan.setVisible(false);

        fileListLayout.setSpacing(false);
        fileListLayout.setPadding(false);
        fileListLayout.setWidthFull();
        fileListLayout.addClassName("files-section__list");

        emptyState.addClassName("files-section__empty");
        emptyState.setVisible(false);

        add(titleSpan, fileListLayout, emptyState);
    }

    public static <F extends LinkedFileMinDto<Long>> AdditionalFilesManager<F> from(List<F> files) {
        return new AdditionalFilesManager<>(files);
    }

    public static <F extends LinkedFileMinDto<Long>> AdditionalFilesManager<F> empty() {
        return new AdditionalFilesManager<>(Collections.emptyList());
    }

    /* ═══════════════════════════════════════════════════════════════
     * Construction
     * ═══════════════════════════════════════════════════════════════ */

    public AdditionalFilesManager<F> title(String title) {
        titleSpan.setText(title != null ? title : "");
        titleSpan.setVisible(title != null && !title.isBlank());
        return this;
    }

    public AdditionalFilesManager<F> emptyMessage(String message) {
        this.emptyMessage = message;
        return this;
    }

    public AdditionalFilesManager<F> dropLabel(String label) {
        this.dropLabel = label;
        return this;
    }

    /* ═══════════════════════════════════════════════════════════════
     * Fluent configuration
     * ═══════════════════════════════════════════════════════════════ */

    /**
     * Hides the upload area and the delete buttons. Downloads still work.
     */
    public AdditionalFilesManager<F> readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    /**
     * When {@code true}, uploads are queued in {@link #getPendingFiles()}.
     */
    public AdditionalFilesManager<F> staging(boolean staging) {
        this.staging = staging;
        return this;
    }

    public AdditionalFilesManager<F> downloader(Downloader downloader) {
        this.downloader = downloader;
        return this;
    }

    public AdditionalFilesManager<F> uploader(Uploader<F> uploader) {
        this.uploader = uploader;
        return this;
    }

    public AdditionalFilesManager<F> deleter(Deleter deleter) {
        this.deleter = deleter;
        return this;
    }

    public AdditionalFilesManager<F> onFilesChanged(Consumer<List<F>> callback) {
        this.onFilesChanged = callback;
        return this;
    }

    public AdditionalFilesManager<F> acceptedMimeTypes(String... mimeTypes) {
        this.acceptedMimeTypes = mimeTypes;
        return this;
    }

    public AdditionalFilesManager<F> maxFileSize(int bytes) {
        this.maxFileSize = bytes;
        return this;
    }

    /**
     * Renders the component. Must be called last in the fluent chain.
     */
    public AdditionalFilesManager<F> build() {
        renderFiles();
        if (!readOnly) {
            add(buildUploadArea());
        }
        return this;
    }

    public List<F> getFiles() {
        return Collections.unmodifiableList(files);
    }

    /**
     * Replaces the committed file list, re-renders and fires
     * {@link #onFilesChanged}.
     */
    public void setFiles(List<F> newFiles) {
        files.clear();
        if (newFiles != null) {
            for (F f : newFiles) if (f != null) files.add(f);
        }
        renderFiles();
        notifyChanged();
    }

    public List<PendingFile> getPendingFiles() {
        return Collections.unmodifiableList(pendingFiles);
    }

    /* ═══════════════════════════════════════════════════════════════
     * Public API
     * ═══════════════════════════════════════════════════════════════ */

    public boolean hasPendingFiles() {
        return !pendingFiles.isEmpty();
    }

    public boolean isEmpty() {
        return files.isEmpty() && pendingFiles.isEmpty();
    }

    /**
     * Adds one committed file, re-renders and fires {@link #onFilesChanged}.
     */
    public void addFile(F file) {
        if (file == null) return;
        files.add(file);
        renderFiles();
        notifyChanged();
    }

    /**
     * Clears pending files (typically after a successful flush).
     */
    public void clearPendingFiles() {
        pendingFiles.clear();
        renderFiles();
    }

    private void renderFiles() {
        fileListLayout.removeAll();

        boolean nothing = files.isEmpty() && pendingFiles.isEmpty();
        if (nothing) {
            fileListLayout.setVisible(false);
            if (emptyMessage != null && !emptyMessage.isBlank()) {
                emptyState.setText(emptyMessage);
                emptyState.setVisible(true);
            } else {
                emptyState.setVisible(false);
            }
            return;
        }

        emptyState.setVisible(false);
        fileListLayout.setVisible(true);

        for (PendingFile pending : pendingFiles) {
            fileListLayout.add(buildPendingRow(pending));
        }
        for (F file : files) {
            fileListLayout.add(buildCommittedRow(file));
        }
    }

    private HorizontalLayout buildCommittedRow(F file) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("file-row");

        Icon fileIcon = LinkedFilesSupport.getFileIcon(file.getMimetype());
        fileIcon.addClassName("file-row__icon");

        String name = file.getOriginalFileName() != null ? file.getOriginalFileName() : "-";
        Span fileName = new Span(name);
        fileName.addClassName("file-row__name");
        fileName.setTitle(name);

        Span fileSize = new Span(LinkedFilesSupport.formatFileSize(file.getSize()));
        fileSize.addClassName("file-row__size");

        row.add(fileIcon, fileName, fileSize);

        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setPadding(false);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);
        actions.addClassName("file-row__actions");

        if (downloader != null) {
            actions.add(buildDownloadLink(file));
        }
        if (!readOnly && deleter != null) {
            actions.add(buildDeleteButton(file));
        }
        if (actions.getComponentCount() > 0) {
            row.add(actions);
        }

        row.expand(fileName);
        return row;
    }

    private HorizontalLayout buildPendingRow(PendingFile pending) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("file-row");

        Icon fileIcon = LinkedFilesSupport.getFileIcon(pending.mimeType);
        fileIcon.addClassName("file-row__icon");

        Span fileName = new Span(pending.name);
        fileName.addClassName("file-row__name");
        fileName.setTitle(pending.name);

        Span fileSize = new Span(LinkedFilesSupport.formatFileSize((long) pending.content.length));
        fileSize.addClassName("file-row__size");

        Span pendingChip = new Span(I18n.t("files.pending"));
        pendingChip.addClassName("status-chip");
        pendingChip.addClassName("status-chip--pending");
        pendingChip.getElement().setAttribute("title", I18n.t("files.pending.tooltip"));

        Button removeBtn = new Button(VaadinIcon.CLOSE_SMALL.create());
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        removeBtn.addClassName("file-row__remove-pending");
        removeBtn.setTooltipText(I18n.t("files.remove.pending"));
        removeBtn.addClickListener(e -> {
            pendingFiles.remove(pending);
            renderFiles();
        });

        row.add(fileIcon, fileName, fileSize, pendingChip, removeBtn);
        row.expand(fileName);
        return row;
    }

    /* ═══════════════════════════════════════════════════════════════
     * Rendering
     * ═══════════════════════════════════════════════════════════════ */

    private Anchor buildDownloadLink(F file) {
        String name = file.getOriginalFileName() != null
                ? file.getOriginalFileName() : "file";

        StreamResource resource = new StreamResource(name, () -> {
            ResponseEntity<Resource> response =
                    downloader.download(file.getId(), file.getVersion());
            if (response != null
                    && response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                try {
                    return response.getBody().getInputStream();
                } catch (Exception e) {
                    log.error("Error opening stream for file {}", file.getId(), e);
                    throw new RuntimeException(I18n.t("files.download.failed"));
                }
            }
            throw new RuntimeException(I18n.t("files.download.failed"));
        });

        Anchor link = new Anchor(resource, "");
        Icon downloadIcon = VaadinIcon.DOWNLOAD.create();
        downloadIcon.addClassName("file-row__download-icon");
        link.add(downloadIcon);
        link.setTarget("_blank");
        link.addClassName("file-row__download");
        link.getElement().setAttribute("title", I18n.t("files.download.tooltip"));
        return link;
    }

    private Button buildDeleteButton(F file) {
        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE,
                ButtonVariant.LUMO_ERROR);
        deleteButton.addClassName("file-row__delete");
        deleteButton.setTooltipText(I18n.t("files.delete.tooltip"));
        deleteButton.addClickListener(e -> confirmDelete(file));
        return deleteButton;
    }

    private Component buildUploadArea() {
        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        uploadComponent = new Upload(buffer);
        uploadComponent.setWidthFull();
        uploadComponent.addClassName("files-section__upload");
        uploadComponent.addClassName("upload-dragger");
        uploadComponent.addClassName("hide-upload-file-list");
        uploadComponent.setMaxFiles(10);
        uploadComponent.setAutoUpload(true);
        uploadComponent.setMaxFileSize(maxFileSize);

        if (dropLabel != null && !dropLabel.isBlank()) {
            uploadComponent.setDropLabel(new Span(dropLabel));
        }
        if (acceptedMimeTypes != null && acceptedMimeTypes.length > 0) {
            uploadComponent.setAcceptedFileTypes(acceptedMimeTypes);
        }

        uploadComponent.addSucceededListener(event -> {
            String fileName = event.getFileName();
            String mimeType = event.getMIMEType();
            long size = event.getContentLength();

            try {
                byte[] content = buffer.getInputStream(fileName).readAllBytes();

                if (staging || uploader == null) {
                    pendingFiles.add(new PendingFile(fileName, mimeType, content));
                    renderFiles();
                    uploadComponent.clearFileList();
                } else {
                    F created = uploader.upload(
                            new ByteArrayInputStream(content), fileName, mimeType, size);
                    if (created != null) {
                        files.add(created);
                        renderFiles();
                        notifyChanged();
                        uploadComponent.clearFileList();
                    } else {
                        Notification.show(I18n.t("files.upload.error",
                                                I18n.t("files.upload.emptyResponse")),
                                        4000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                }
            } catch (Exception ex) {
                log.error("Upload failed for {}", fileName, ex);
                Notification.show(I18n.t("files.upload.error", ex.getMessage()),
                                5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        uploadComponent.addFileRejectedListener(event ->
                Notification.show(I18n.t("files.upload.rejected", event.getErrorMessage()),
                                4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING));

        uploadComponent.addFailedListener(event ->
                Notification.show(I18n.t("files.upload.error",
                                        event.getReason() != null
                                                ? event.getReason().getMessage() : ""),
                                5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR));

        return uploadComponent;
    }

    private void confirmDelete(F file) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle(I18n.t("files.delete.confirm.title"));

        String name = file.getOriginalFileName() != null
                ? file.getOriginalFileName() : "-";
        confirm.add(new Span(I18n.t("files.delete.confirm.message", name)));

        Button confirmBtn = new Button(I18n.t("files.delete.confirm.ok"), e -> {
            confirm.close();
            doDelete(file);
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button(I18n.t("files.delete.confirm.cancel"),
                e -> confirm.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        confirm.getFooter().add(cancelBtn, confirmBtn);
        confirm.open();
    }

    private void doDelete(F file) {
        try {
            boolean ok = deleter.delete(file.getId());
            if (ok) {
                files.removeIf(f -> Objects.equals(f.getId(), file.getId()));
                renderFiles();
                notifyChanged();
                Notification.show(I18n.t("files.delete.success"),
                                3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show(I18n.t("files.delete.error"),
                                4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception ex) {
            log.error("Delete failed for file {}", file.getId(), ex);
            Notification.show(I18n.t("files.delete.error"),
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /* ═══════════════════════════════════════════════════════════════
     * Upload area
     * ═══════════════════════════════════════════════════════════════ */

    private void notifyChanged() {
        if (onFilesChanged != null) {
            onFilesChanged.accept(new ArrayList<>(files));
        }
    }

    /* ═══════════════════════════════════════════════════════════════
     * Delete flow
     * ═══════════════════════════════════════════════════════════════ */

    /**
     * Downloads a file's content. Return value must be 2xx with a body.
     */
    @FunctionalInterface
    public interface Downloader {
        ResponseEntity<Resource> download(Long fileId, Long version);
    }

    /**
     * Uploads a file. Must return the freshly-created DTO so the manager
     * can append it to the committed list. Only used in immediate mode.
     */
    @FunctionalInterface
    public interface Uploader<F extends LinkedFileMinDto<Long>> {
        F upload(InputStream content, String originalFileName,
                 String contentType, long size) throws Exception;
    }

    /* ═══════════════════════════════════════════════════════════════
     * Notify
     * ═══════════════════════════════════════════════════════════════ */

    /**
     * Deletes a file. Must return {@code true} on success.
     */
    @FunctionalInterface
    public interface Deleter {
        boolean delete(Long fileId) throws Exception;
    }
}