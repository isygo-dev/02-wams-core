package eu.isygoit.ui.dms.views.category.dialog;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.dms.views.category.CategoryManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class UpdateCategoryDialog extends BaseActionDialog {

    private final CategoryManagementView parentView;
    private final CategoryService categoryService;
    private final CategoryDto category;
    private final Runnable onSuccess;

    private TextField nameField;
    private TextArea descriptionArea;

    public UpdateCategoryDialog(CategoryManagementView parentView,
                                CategoryService categoryService,
                                CategoryDto category,
                                Runnable onSuccess) {
        super(I18n.t("dms.category.dialog.update.title"), onSuccess);
        this.parentView = parentView;
        this.categoryService = categoryService;
        this.category = category;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("dms.category.dialog.update.button"));
        setWidth("500px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
        populateFields();
    }

    private void buildForm() {
        nameField = new TextField(I18n.t("dms.category.dialog.field.name"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();

        descriptionArea = new TextArea(I18n.t("dms.category.dialog.field.description"));
        descriptionArea.setWidthFull();
        descriptionArea.setHeight("150px");
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );
        form.add(nameField, descriptionArea);
        return form;
    }

    private void populateFields() {
        nameField.setValue(category.getName() != null ? category.getName() : "");
        descriptionArea.setValue(category.getDescription() != null ? category.getDescription() : "");
    }

    @Override
    protected boolean onOk() {
        if (nameField.getValue().isBlank()) {
            append(I18n.t("dms.category.dialog.field.name.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            category.setName(nameField.getValue().trim());
            category.setDescription(descriptionArea.getValue());

            ResponseEntity<CategoryDto> response = categoryService.update(category.getId(), category);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("dms.category.dialog.update.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("dms.category.dialog.update.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("dms.category.dialog.update.error", e.getMessage()));
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