package eu.isygoit.ui.ims.views.annex.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.ims.views.annex.AnnexManagementView;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import org.springframework.http.ResponseEntity;

public class UpdateAnnexDialog extends BaseActionDialog {

    private final AnnexManagementView parentView;
    private final AnnexService annexService;
    private final AnnexDto annex;
    private final Runnable onSuccess;

    private TextField tableCodeField;
    private ComboBox<IEnumLanguage.Types> languageCombo;
    private TextField valueField;
    private TextArea descriptionArea;
    private TextField referenceField;
    private IntegerField orderField;

    public UpdateAnnexDialog(AnnexManagementView parentView,
                             AnnexService annexService,
                             AnnexDto annex,
                             Runnable onSuccess) {
        super("Edit Annex", onSuccess);
        this.parentView = parentView;
        this.annexService = annexService;
        this.annex = annex;
        this.onSuccess = onSuccess;

        setOkButtonText("Save");
        setWidth("600px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
        populateFields();
    }

    private void buildForm() {
        tableCodeField = new TextField("Table code *");
        tableCodeField.setRequiredIndicatorVisible(true);
        tableCodeField.setWidthFull();

        languageCombo = new ComboBox<>("Language *");
        languageCombo.setItems(IEnumLanguage.Types.values());
        languageCombo.setRequiredIndicatorVisible(true);
        languageCombo.setWidthFull();

        valueField = new TextField("Value *");
        valueField.setRequiredIndicatorVisible(true);
        valueField.setWidthFull();

        descriptionArea = new TextArea("Description");
        descriptionArea.setWidthFull();

        referenceField = new TextField("Reference");
        referenceField.setWidthFull();

        orderField = new IntegerField("Order");
        orderField.setWidthFull();
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        form.add(tableCodeField, languageCombo, valueField,
                descriptionArea, referenceField, orderField);
        form.setColspan(descriptionArea, 2);
        return form;
    }

    private void populateFields() {
        tableCodeField.setValue(annex.getTableCode() != null ? annex.getTableCode() : "");
        languageCombo.setValue(annex.getLanguage());
        valueField.setValue(annex.getValue() != null ? annex.getValue() : "");
        descriptionArea.setValue(annex.getDescription() != null ? annex.getDescription() : "");
        referenceField.setValue(annex.getReference() != null ? annex.getReference() : "");
        orderField.setValue(annex.getAnnexOrder());
    }

    @Override
    protected boolean onOk() {
        if (tableCodeField.getValue().isBlank()) {
            append("Table code is required");
            return false;
        }
        if (languageCombo.getValue() == null) {
            append("Language is required");
            return false;
        }
        if (valueField.getValue().isBlank()) {
            append("Value is required");
            return false;
        }

        parentView.showLoading(true);
        try {
            annex.setTableCode(tableCodeField.getValue());
            annex.setLanguage(languageCombo.getValue());
            annex.setValue(valueField.getValue());
            annex.setDescription(descriptionArea.getValue());
            annex.setReference(referenceField.getValue());
            annex.setAnnexOrder(orderField.getValue());

            ResponseEntity<AnnexDto> response = annexService.update(annex.getId(), annex);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append("Update failed: HTTP " + response.getStatusCodeValue());
                return false;
            }

            append("Annex updated successfully");
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append("Failed operation: " + e.getMessage());
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}