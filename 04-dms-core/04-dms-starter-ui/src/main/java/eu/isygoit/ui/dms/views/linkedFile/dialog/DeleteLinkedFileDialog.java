package eu.isygoit.ui.dms.views.linkedFile.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.LinkedFileService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.dms.views.linkedFile.LinkedFileManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

@Slf4j
public class DeleteLinkedFileDialog extends PinBaseActionDialog {

    private final LinkedFileManagementView parentView;
    private final LinkedFileService linkedFileService;
    private final String fileCode;
    private final String fileName;
    private Span warningIcon;

    public DeleteLinkedFileDialog(LinkedFileManagementView parentView,
                                  LinkedFileService linkedFileService,
                                  String fileCode,
                                  String fileName,
                                  Runnable onSuccess) {
        super(I18n.t("dms.linkedfile.dialog.delete.title"),
                I18n.t("dms.linkedfile.dialog.delete.message", fileName != null ? fileName : fileCode),
                onSuccess);
        this.parentView = parentView;
        this.linkedFileService = linkedFileService;
        this.fileCode = fileCode;
        this.fileName = fileName;

        setOkButtonText(I18n.t("dms.linkedfile.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
        setMaxWidth("95%");

        // Add warning styling to the message
        addWarningStyling();
    }

    private void addWarningStyling() {
        // The message is rendered by the parent, but we can add additional warning elements
        VerticalLayout warningLayout = new VerticalLayout();
        warningLayout.setPadding(false);
        warningLayout.setSpacing(false);

        HorizontalLayout warningRow = new HorizontalLayout();
        warningRow.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        warningRow.setSpacing(true);

        com.vaadin.flow.component.icon.Icon warningIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
        warningIcon.setColor("var(--lumo-error-color)");
        warningIcon.setSize("20px");

        Span warningText = new Span(I18n.t("dms.linkedfile.dialog.delete.warning"));
        warningText.getStyle()
                .set("color", "var(--lumo-error-color)")
                .set("font-weight", "500")
                .set("font-size", "var(--lumo-font-size-s)");

        warningRow.add(warningIcon, warningText);
        warningLayout.add(warningRow);

        // Insert after the message (which is added by the parent)
        // We'll use the append method to add it after the message
        addContent(warningLayout);
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("dms.linkedfile.dialog.delete.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            ResponseEntity<Boolean> response = linkedFileService.deleteFile(fileCode);
            if (!response.getStatusCode().is2xxSuccessful() || !Boolean.TRUE.equals(response.getBody())) {
                append(I18n.t("dms.linkedfile.dialog.delete.failed"));
                return false;
            }
            append(I18n.t("dms.linkedfile.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            String errorMsg = extractErrorMessage(ex);
            append(I18n.t("dms.linkedfile.dialog.delete.error", errorMsg));
            log.error("Delete error for file: {}", fileCode, ex);
        } catch (Exception e) {
            append(I18n.t("dms.linkedfile.dialog.delete.error", e.getMessage()));
            log.error("Delete error for file: {}", fileCode, e);
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