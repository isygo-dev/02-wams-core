package eu.isygoit.ui.dms.views.linkedFile.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.common.LinkedFileRequestDto;
import eu.isygoit.dto.common.LinkedFileResponseDto;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.remote.dms.LinkedFileService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.dms.views.common.DmsActionDialog;
import eu.isygoit.ui.dms.views.common.DmsDialogSupport;
import eu.isygoit.ui.dms.views.common.DmsUploadPanel;
import eu.isygoit.ui.dms.views.linkedFile.LinkedFileManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Uploads a file as a {@link LinkedFileRequestDto}: the file itself (its name
 * becomes {@code originalFileName}), {@code path}, {@code tags} and
 * {@code category}. {@code code} is assigned by the server (read-only, not sent).
 *
 * <p>Tags are entered as comma-separated {@code key:value} pairs, e.g.
 * {@code priority:high, color:red, team:finance}.</p>
 */
@Slf4j
public class UploadLinkedFileDialog extends DmsActionDialog {

    private static final String PATH_PATTERN = "^[a-zA-Z0-9-_/]+$";
    private static final String DEFAULT_PATH = "default";
    private static final int CATEGORY_PAGE_SIZE = 100;

    private final LinkedFileManagementView parentView;
    private final LinkedFileService linkedFileService;
    private final CategoryService categoryService;

    private DmsUploadPanel uploadPanel;
    private TextField tagsField;
    private TextField pathField;
    private ComboBox<String> categoryComboBox;

    public UploadLinkedFileDialog(LinkedFileManagementView parentView,
                                  LinkedFileService linkedFileService,
                                  CategoryService categoryService,
                                  Runnable onSuccess) {
        super(I18n.t("dms.linkedfile.dialog.upload.title"), onSuccess);
        this.parentView = parentView;
        this.linkedFileService = linkedFileService;
        this.categoryService = categoryService;

        setOkButtonText(I18n.t("dms.linkedfile.dialog.upload.button"));
        DialogLayout.size(this, DialogLayout.WIDTH_M);

        buildForm();
        loadCategories();

        // Nothing to upload until a file has been received.
        enableOkButton(false);
    }

    /**
     * i18n key of the path validation error, or {@code null} when the path is valid or empty.
     */
    private static String pathErrorKey(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        if (path.startsWith("/") || path.endsWith("/")) {
            return "dms.linkedfile.dialog.field.path.error.invalid.format";
        }
        if (!path.matches(PATH_PATTERN)) {
            return "dms.linkedfile.dialog.field.path.error.invalid.characters";
        }
        return null;
    }

    /**
     * Parses a comma-separated list of {@code key:value} (or {@code key=value}) pairs
     * into an ordered map. Entries without a separator, or with an empty key/value,
     * are skipped.
     */
    private static Map<String, String> parseTags(String input) {
        Map<String, String> result = new LinkedHashMap<>();
        if (input == null) {
            return result;
        }
        for (String pair : input.split(",")) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int sep = indexOfSeparator(trimmed);
            if (sep <= 0 || sep >= trimmed.length() - 1) {
                continue;
            }
            String key = trimmed.substring(0, sep).trim();
            String value = trimmed.substring(sep + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                result.put(key, value);
            }
        }
        return result;
    }

    private static int indexOfSeparator(String s) {
        int colon = s.indexOf(':');
        int equal = s.indexOf('=');
        if (colon < 0) return equal;
        if (equal < 0) return colon;
        return Math.min(colon, equal);
    }

    private void buildForm() {
        uploadPanel = new DmsUploadPanel(this::enableOkButton);

        categoryComboBox = new ComboBox<>(I18n.t("dms.linkedfile.dialog.field.category"));
        categoryComboBox.setPlaceholder(I18n.t("dms.linkedfile.dialog.field.category.placeholder"));
        categoryComboBox.setWidthFull();
        categoryComboBox.setClearButtonVisible(true);

        pathField = new TextField(I18n.t("dms.linkedfile.dialog.field.path"));
        pathField.setPlaceholder(I18n.t("dms.linkedfile.dialog.field.path.placeholder"));
        pathField.setWidthFull();
        pathField.setClearButtonVisible(true);
        pathField.addValueChangeListener(event -> {
            String errorKey = pathErrorKey(event.getValue());
            pathField.setInvalid(errorKey != null);
            pathField.setErrorMessage(errorKey != null ? I18n.t(errorKey) : null);
            refreshPathHelper();
        });
        categoryComboBox.addValueChangeListener(event -> refreshPathHelper());
        refreshPathHelper();

        tagsField = new TextField(I18n.t("dms.linkedfile.dialog.field.tags"));
        tagsField.setPlaceholder(I18n.t("dms.linkedfile.dialog.field.tags.placeholder"));
        // Hint the new key:value format — add this key to the i18n bundle:
        //   EN: "Comma-separated pairs, e.g. priority:high, color:red"
        //   FR: "Paires séparées par virgule, ex. priority:high, color:red"
        tagsField.setHelperText(I18n.t("dms.linkedfile.dialog.field.tags.helper"));
        tagsField.setWidthFull();

        FormLayout form = DialogLayout.responsiveForm();
        form.add(categoryComboBox, pathField, tagsField);
        form.setColspan(pathField, 2);
        form.setColspan(tagsField, 2);

        VerticalLayout stack = DialogLayout.stack();
        stack.add(uploadPanel, form);
        addContent(stack);
    }

    /**
     * Helper text under the path: tells which value is used when the path is left empty.
     */
    private void refreshPathHelper() {
        String path = pathField.getValue();
        String category = categoryComboBox.getValue();
        if ((path == null || path.isEmpty()) && category != null && !category.isEmpty()) {
            pathField.setHelperText(I18n.t("dms.linkedfile.dialog.field.path.category.will.used", category));
        } else {
            pathField.setHelperText(I18n.t("dms.linkedfile.dialog.field.path.helper"));
        }
    }

    private void loadCategories() {
        try {
            ResponseEntity<PaginatedResponseDto<CategoryDto>> response = categoryService.findAll(0, CATEGORY_PAGE_SIZE);
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

    @Override
    protected boolean onOk() {
        if (uploadPanel.getUploadedFile() == null) {
            append(I18n.t("dms.linkedfile.dialog.upload.no.file"));
            return false;
        }

        String path = pathField.getValue();
        String pathError = pathErrorKey(path);
        if (pathError != null) {
            append(I18n.t(pathError));
            return false;
        }
        if (path != null && !path.isEmpty()) {
            // Collapse consecutive slashes.
            path = path.replaceAll("/+", "/");
        }

        // Validate tags *before* the network round-trip so a typo doesn't waste an upload.
        String rawTags = tagsField.getValue();
        Map<String, String> tagMap = parseTags(rawTags);
        if (rawTags != null && !rawTags.isBlank() && tagMap.isEmpty()) {
            append(I18n.t("dms.linkedfile.dialog.field.tags.error.invalid"));
            return false;
        }

        parentView.showLoading(true);
        try {
            String selectedCategory = categoryComboBox.getValue();
            boolean hasCategory = selectedCategory != null && !selectedCategory.isEmpty();

            LinkedFileRequestDto request = new LinkedFileRequestDto();
            request.setOriginalFileName(uploadPanel.getUploadedFileName());
            // Path: the field when provided, otherwise the category, otherwise the default folder.
            if (path == null || path.isEmpty()) {
                path = hasCategory ? selectedCategory : DEFAULT_PATH;
            }
            request.setPath(path);
            request.setFile(uploadPanel.getUploadedFile());

            if (!tagMap.isEmpty()) {
                request.setTags(tagMap);
            }
            if (hasCategory) {
                request.setFileCategory(selectedCategory);
            }

            ResponseEntity<LinkedFileResponseDto> response = linkedFileService.upload(request);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("dms.linkedfile.dialog.upload.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("dms.linkedfile.dialog.upload.success"));
            return true;
        } catch (FeignException ex) {
            append(I18n.t("dms.linkedfile.dialog.upload.error", DmsDialogSupport.extractErrorMessage(ex)));
            log.error("Upload error", ex);
        } catch (Exception e) {
            append(I18n.t("dms.linkedfile.dialog.upload.error", e.getMessage()));
            log.error("Upload error", e);
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}