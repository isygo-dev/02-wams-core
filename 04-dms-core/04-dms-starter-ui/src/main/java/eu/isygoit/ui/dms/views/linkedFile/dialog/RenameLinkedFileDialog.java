package eu.isygoit.ui.dms.views.linkedFile.dialog;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.LinkedFileResponseDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.LinkedFileService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.dms.views.linkedFile.LinkedFileManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.function.Consumer;

@Slf4j
public class RenameLinkedFileDialog extends BaseActionDialog {

    private final LinkedFileManagementView parentView;
    private final LinkedFileService linkedFileService;
    private final LinkedFileResponseDto file;
    private final Consumer<LinkedFileResponseDto> onSuccess;

    private TextField newNameField;
    private Span currentNameDisplay;

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
        setWidth("450px");
        setMaxWidth("95%");

        buildContent();
    }

    private void buildContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        // Current name display
        HorizontalLayout currentNameRow = new HorizontalLayout();
        currentNameRow.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        currentNameRow.setSpacing(true);

        com.vaadin.flow.component.icon.Icon fileIcon = VaadinIcon.FILE_O.create();
        fileIcon.setSize("16px");
        fileIcon.addClassName("detail-field-icon");

        Span currentLabel = new Span(I18n.t("dms.linkedfile.dialog.rename.current") + ":");
        currentLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        currentLabel.addClassName(LumoUtility.FontSize.SMALL);

        currentNameDisplay = new Span(file.getOriginalFileName() != null ? file.getOriginalFileName() : file.getCode());
        currentNameDisplay.addClassName(LumoUtility.FontSize.SMALL);
        currentNameDisplay.getStyle().set("color", "var(--lumo-secondary-text-color)");

        currentNameRow.add(fileIcon, currentLabel, currentNameDisplay);

        // New name input
        newNameField = new TextField(I18n.t("dms.linkedfile.dialog.rename.field.name"));
        newNameField.setWidthFull();
        newNameField.setValue(file.getOriginalFileName() != null ? file.getOriginalFileName() : "");
        newNameField.setRequiredIndicatorVisible(true);
        newNameField.setPlaceholder(I18n.t("dms.linkedfile.dialog.rename.field.name.placeholder"));
        newNameField.addValueChangeListener(e -> {
            String newName = e.getValue() != null ? e.getValue().trim() : "";
            String currentName = file.getOriginalFileName() != null ? file.getOriginalFileName() : "";
            boolean hasChanged = !newName.equals(currentName) && !newName.isEmpty();
            enableOkButton(hasChanged);
        });

        // Info text
        Span infoText = new Span(I18n.t("dms.linkedfile.dialog.rename.info"));
        infoText.addClassName(LumoUtility.FontSize.XXSMALL);
        infoText.getStyle().set("color", "var(--lumo-secondary-text-color)");

        layout.add(currentNameRow, newNameField, infoText);
        addContent(layout);

        // Initially disable OK if name hasn't changed
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
            String errorMsg = extractErrorMessage(ex);
            append(I18n.t("dms.linkedfile.dialog.rename.error", errorMsg));
            log.error("Rename failed for file: {}", file.getCode(), ex);
        } catch (Exception e) {
            append(I18n.t("dms.linkedfile.dialog.rename.error", e.getMessage()));
            log.error("Rename failed for file: {}", file.getCode(), e);
        } finally {
            parentView.showLoading(false);
        }
        return false;
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