package eu.isygoit.ui.dms.views.linkedFile.dialog;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.common.LinkedFileResponseDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.LinkedFileService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.dms.views.common.DmsActionDialog;
import eu.isygoit.ui.dms.views.common.DmsDialogSupport;
import eu.isygoit.ui.dms.views.linkedFile.LinkedFileManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.function.Consumer;

/**
 * Renames a linked file: the only editable {@link LinkedFileResponseDto} field
 * is {@code originalFileName}; the code stays untouched (read-only).
 */
@Slf4j
public class RenameLinkedFileDialog extends DmsActionDialog {

    private final LinkedFileManagementView parentView;
    private final LinkedFileService linkedFileService;
    private final LinkedFileResponseDto file;
    private final Consumer<LinkedFileResponseDto> onSuccess;

    private TextField newNameField;

    public RenameLinkedFileDialog(LinkedFileManagementView parentView,
                                  LinkedFileService linkedFileService,
                                  LinkedFileResponseDto file,
                                  Consumer<LinkedFileResponseDto> onSuccess) {
        super(I18n.t("dms.linkedfile.dialog.rename.title"));
        this.parentView = parentView;
        this.linkedFileService = linkedFileService;
        this.file = file;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("dms.linkedfile.dialog.rename.button"));
        DialogLayout.size(this, DialogLayout.WIDTH_S);

        buildContent();
    }

    private void buildContent() {
        String currentName = file.getOriginalFileName() != null ? file.getOriginalFileName() : "";

        TextField currentNameField = new TextField(I18n.t("dms.linkedfile.dialog.rename.current"));
        currentNameField.setValue(file.getOriginalFileName() != null ? file.getOriginalFileName() : file.getCode());
        currentNameField.setReadOnly(true);
        currentNameField.setWidthFull();

        newNameField = new TextField(I18n.t("dms.linkedfile.dialog.rename.field.name"));
        newNameField.setWidthFull();
        newNameField.setValue(currentName);
        newNameField.setRequired(true);
        newNameField.setRequiredIndicatorVisible(true);
        newNameField.setPlaceholder(I18n.t("dms.linkedfile.dialog.rename.field.name.placeholder"));
        newNameField.addValueChangeListener(e -> {
            String newName = e.getValue() != null ? e.getValue().trim() : "";
            enableOkButton(!newName.equals(currentName) && !newName.isEmpty());
        });

        VerticalLayout stack = DialogLayout.stack();
        stack.add(currentNameField, newNameField, DialogLayout.help(I18n.t("dms.linkedfile.dialog.rename.info")));
        addContent(stack);

        // Nothing to save until the name differs from the current one.
        enableOkButton(false);
        newNameField.focus();
    }

    @Override
    protected boolean onOk() {
        String newName = newNameField.getValue() != null ? newNameField.getValue().trim() : "";
        if (newName.isEmpty()) {
            append(I18n.t("dms.linkedfile.dialog.rename.field.name.required"));
            return false;
        }
        if (newName.equals(file.getOriginalFileName())) {
            append(I18n.t("dms.linkedfile.dialog.rename.unchanged"));
            return false;
        }

        parentView.showLoading(true);
        try {
            ResponseEntity<LinkedFileResponseDto> response = linkedFileService.renameFile(file.getCode(), newName);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("dms.linkedfile.dialog.rename.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("dms.linkedfile.dialog.rename.success"));
            if (onSuccess != null) {
                onSuccess.accept(response.getBody());
            }
            return true;
        } catch (FeignException ex) {
            append(I18n.t("dms.linkedfile.dialog.rename.error", DmsDialogSupport.extractErrorMessage(ex)));
            log.error("Rename failed for file: {}", file.getCode(), ex);
        } catch (Exception e) {
            append(I18n.t("dms.linkedfile.dialog.rename.error", e.getMessage()));
            log.error("Rename failed for file: {}", file.getCode(), e);
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}
