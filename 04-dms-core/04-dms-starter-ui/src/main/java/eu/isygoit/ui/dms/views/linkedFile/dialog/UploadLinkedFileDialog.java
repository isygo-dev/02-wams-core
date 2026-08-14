package eu.isygoit.ui.dms.views.linkedFile.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.LinkedFileRequestDto;
import eu.isygoit.dto.common.LinkedFileResponseDto;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.remote.dms.LinkedFileService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.dms.views.linkedFile.LinkedFileManagementView;
import eu.isygoit.util.ByteArrayMultipartFile;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class UploadLinkedFileDialog extends BaseActionDialog {

    private final LinkedFileManagementView parentView;
    private final LinkedFileService linkedFileService;
    private final CategoryService categoryService;
    private final Runnable onSuccess;

    private MemoryBuffer memoryBuffer;
    private Upload upload;
    private TextField tagsField;
    private TextField pathField;
    private ComboBox<String> categoryComboBox;
    private Span fileNameDisplay;
    private Span fileSizeDisplay;
    private Span uploadStatus;
    private Icon uploadIcon;

    private String uploadedFileName;
    private MultipartFile uploadedFile;

    public UploadLinkedFileDialog(LinkedFileManagementView parentView,
                                  LinkedFileService linkedFileService,
                                  CategoryService categoryService,
                                  Runnable onSuccess) {
        super(I18n.t("dms.linkedfile.dialog.upload.title"), onSuccess);
        this.parentView = parentView;
        this.linkedFileService = linkedFileService;
        this.categoryService = categoryService;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("dms.linkedfile.dialog.upload.button"));
        setWidth("600px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
        loadCategories();

        // Disable OK button initially
        enableOkButton(false);
    }

    private void buildForm() {
        memoryBuffer = new MemoryBuffer();
        upload = new Upload(memoryBuffer);
        upload.setDropAllowed(true);
        upload.setMaxFiles(1);
        upload.setMaxFileSize(50 * 1024 * 1024); // 50MB
        upload.setAcceptedFileTypes("application/pdf", "image/*", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/plain", "application/zip");
        upload.addClassName("wams-upload-component");
        upload.getStyle().set("width", "100%");

        upload.addSucceededListener(event -> {
            uploadedFileName = event.getFileName();
            try {
                byte[] bytes = memoryBuffer.getInputStream().readAllBytes();
                uploadedFile = new ByteArrayMultipartFile(bytes, uploadedFileName, memoryBuffer.getFileData().getMimeType());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read uploaded file into memory", e);
            }
            fileNameDisplay.setText(I18n.t("dms.linkedfile.dialog.upload.file.name", uploadedFileName));
            fileSizeDisplay.setText(I18n.t("dms.linkedfile.dialog.upload.file.size", formatFileSize(uploadedFile.getSize())));
            uploadStatus.setText(I18n.t("dms.linkedfile.dialog.upload.success"));
            uploadStatus.getStyle().set("color", "var(--lumo-success-color)");
            uploadIcon.getStyle().set("color", "var(--lumo-success-color)");
            enableOkButton(true);
            log.info("File uploaded: {}", uploadedFileName);
        });

        upload.addFailedListener(event -> {
            uploadStatus.setText(I18n.t("dms.linkedfile.dialog.upload.failed", event.getReason().getMessage()));
            uploadStatus.getStyle().set("color", "var(--lumo-error-color)");
            uploadIcon.getStyle().set("color", "var(--lumo-error-color)");
            enableOkButton(false);
            log.error("Upload failed: {}", event.getReason().getMessage());
        });

        upload.addFileRejectedListener(event -> {
            uploadStatus.setText(I18n.t("dms.linkedfile.dialog.upload.rejected", event.getErrorMessage()));
            uploadStatus.getStyle().set("color", "var(--lumo-error-color)");
            uploadIcon.getStyle().set("color", "var(--lumo-error-color)");
            enableOkButton(false);
            log.warn("File rejected: {}", event.getErrorMessage());
        });

        tagsField = new TextField(I18n.t("dms.linkedfile.dialog.field.tags"));
        tagsField.setPlaceholder(I18n.t("dms.linkedfile.dialog.field.tags.placeholder"));
        tagsField.setWidthFull();

        pathField = new TextField(I18n.t("dms.linkedfile.dialog.field.path"));
        pathField.setPlaceholder(I18n.t("dms.linkedfile.dialog.field.path.placeholder", "e.g., documents/reports/2026"));
        pathField.setWidthFull();
        pathField.setClearButtonVisible(true);
        pathField.setHelperText(I18n.t("dms.linkedfile.dialog.field.path.helper", "Use format: folder/subfolder/... (no leading/trailing slashes)"));

        // Add validation for path format
        pathField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value != null && !value.isEmpty()) {
                if (value.startsWith("/") || value.endsWith("/")) {
                    pathField.setInvalid(true);
                    pathField.setErrorMessage(I18n.t("dms.linkedfile.dialog.field.path.error.invalid.format",
                            "Path cannot start or end with '/'"));
                } else if (!value.matches("^[a-zA-Z0-9-_/]+$")) {
                    pathField.setInvalid(true);
                    pathField.setErrorMessage(I18n.t("dms.linkedfile.dialog.field.path.error.invalid.characters",
                            "Path can only contain letters, numbers, hyphens, underscores, and slashes"));
                } else {
                    pathField.setInvalid(false);
                    pathField.setErrorMessage(null);
                }
            } else {
                pathField.setInvalid(false);
                pathField.setErrorMessage(null);
                // If empty, update helper text to show category will be used
                String selectedCategory = categoryComboBox.getValue();
                if (selectedCategory != null && !selectedCategory.isEmpty()) {
                    pathField.setHelperText(I18n.t("dms.linkedfile.dialog.field.path.category.will.used", selectedCategory));
                } else {
                    pathField.setHelperText(I18n.t("dms.linkedfile.dialog.field.path.helper",
                            "Use format: folder/subfolder/... (no leading/trailing slashes)"));
                }
            }
        });

        categoryComboBox = new ComboBox<>(I18n.t("dms.linkedfile.dialog.field.category"));
        categoryComboBox.setPlaceholder(I18n.t("dms.linkedfile.dialog.field.category.placeholder"));
        categoryComboBox.setWidthFull();
        categoryComboBox.setClearButtonVisible(true);

        // Update helper text when category changes
        categoryComboBox.addValueChangeListener(event -> {
            if (pathField.getValue() == null || pathField.getValue().isEmpty()) {
                String selectedCategory = event.getValue();
                if (selectedCategory != null && !selectedCategory.isEmpty()) {
                    pathField.setHelperText(I18n.t("dms.linkedfile.dialog.field.path.category.will.used", selectedCategory));
                } else {
                    pathField.setHelperText(I18n.t("dms.linkedfile.dialog.field.path.helper",
                            "Use format: folder/subfolder/... (no leading/trailing slashes)"));
                }
            }
        });

        fileNameDisplay = new Span();
        fileNameDisplay.addClassName("wams-upload-file-name");
        fileNameDisplay.getStyle().set("font-size", "var(--lumo-font-size-s)");

        fileSizeDisplay = new Span();
        fileSizeDisplay.addClassName("wams-upload-file-size");
        fileSizeDisplay.getStyle().set("font-size", "var(--lumo-font-size-s)");

        uploadStatus = new Span();
        uploadStatus.addClassName("wams-upload-status");
        uploadStatus.getStyle().set("font-size", "var(--lumo-font-size-s)");

        uploadIcon = VaadinIcon.UPLOAD.create();
        uploadIcon.setSize("20px");
        uploadIcon.getStyle().set("color", "var(--lumo-primary-color)");
    }

    private void loadCategories() {
        try {
            ResponseEntity<PaginatedResponseDto<CategoryDto>> response = categoryService.findAll(0, 100);
            if (response.getBody() != null && response.getBody().getContent() != null) {
                List<String> categoryNames = response.getBody().getContent().stream()
                        .map(CategoryDto::getName)
                        .collect(Collectors.toList());
                categoryComboBox.setItems(categoryNames);
            }
        } catch (Exception e) {
            log.error("Failed to load categories", e);
        }
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );

        // Upload section
        VerticalLayout uploadLayout = new VerticalLayout();
        uploadLayout.setSpacing(true);
        uploadLayout.setPadding(false);

        Span instruction = new Span(I18n.t("dms.linkedfile.dialog.upload.instruction"));
        instruction.addClassName(LumoUtility.FontSize.SMALL);
        instruction.getStyle().set("color", "var(--lumo-secondary-text-color)");
        uploadLayout.add(instruction);

        uploadLayout.add(upload);

        // File info with icon
        HorizontalLayout fileInfoLayout = new HorizontalLayout();
        fileInfoLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        fileInfoLayout.setSpacing(true);
        fileInfoLayout.addClassName("wams-upload-file-info");

        uploadIcon = VaadinIcon.UPLOAD.create();
        uploadIcon.setSize("20px");
        uploadIcon.getStyle().set("color", "var(--lumo-primary-color)");

        fileNameDisplay = new Span(I18n.t("dms.linkedfile.dialog.upload.no.file.selected"));
        fileNameDisplay.getStyle().set("color", "var(--lumo-secondary-text-color)");

        fileSizeDisplay = new Span("");

        fileInfoLayout.add(uploadIcon, fileNameDisplay, fileSizeDisplay);
        uploadLayout.add(fileInfoLayout);
        uploadLayout.add(uploadStatus);

        form.add(uploadLayout);
        form.add(tagsField, pathField, categoryComboBox);
        return form;
    }

    @Override
    protected boolean onOk() {
        if (uploadedFile == null) {
            append(I18n.t("dms.linkedfile.dialog.upload.no.file"));
            return false;
        }

        // Validate path if provided
        String path = pathField.getValue();
        if (path != null && !path.isEmpty()) {
            if (path.startsWith("/") || path.endsWith("/")) {
                append(I18n.t("dms.linkedfile.dialog.upload.error",
                        "Path cannot start or end with '/'"));
                return false;
            }
            if (!path.matches("^[a-zA-Z0-9-_/]+$")) {
                append(I18n.t("dms.linkedfile.dialog.upload.error",
                        "Path can only contain letters, numbers, hyphens, underscores, and slashes"));
                return false;
            }
            // Clean up multiple consecutive slashes
            path = path.replaceAll("/+", "/");
        }

        parentView.showLoading(true);
        try {
            LinkedFileRequestDto request = new LinkedFileRequestDto();
            request.setCode(null);
            request.setOriginalFileName(uploadedFileName);

            // Determine the path: use pathField if provided, otherwise use category
            String selectedCategory = categoryComboBox.getValue();

            if (path == null || path.isEmpty()) {
                // If path is empty, use the selected category
                if (selectedCategory != null && !selectedCategory.isEmpty()) {
                    path = selectedCategory;
                    log.info("Using category '{}' as path since path field is empty", selectedCategory);
                } else {
                    // If no category selected and path is empty, use default
                    path = "default";
                    log.info("No path or category provided, using default path");
                }
            }
            request.setPath(path);

            request.setFile(uploadedFile);

            if (tagsField.getValue() != null && !tagsField.getValue().isBlank()) {
                String[] tags = tagsField.getValue().split(",");
                List<String> tagList = new ArrayList<>();
                for (String tag : tags) {
                    String trimmed = tag.trim();
                    if (!trimmed.isEmpty()) {
                        tagList.add(trimmed);
                    }
                }
                request.setTags(tagList);
            }

            if (selectedCategory != null && !selectedCategory.isEmpty()) {
                request.setCategoryNames(List.of(selectedCategory));
            }

            ResponseEntity<LinkedFileResponseDto> response = linkedFileService.upload(request);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("dms.linkedfile.dialog.upload.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("dms.linkedfile.dialog.upload.success"));
            if (onSuccess != null) {
                onSuccess.run();
            }
            return true;
        } catch (FeignException ex) {
            String errorMsg = extractErrorMessage(ex);
            append(I18n.t("dms.linkedfile.dialog.upload.error", errorMsg));
            log.error("Upload error", ex);
        } catch (Exception e) {
            append(I18n.t("dms.linkedfile.dialog.upload.error", e.getMessage()));
            log.error("Upload error", e);
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) {
                return ex.contentUTF8();
            }
        } catch (Exception ignored) {
        }
        return ex.getMessage() != null ? ex.getMessage() : "Unknown error";
    }
}