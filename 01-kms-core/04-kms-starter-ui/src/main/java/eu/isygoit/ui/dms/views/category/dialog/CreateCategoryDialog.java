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

public class CreateCategoryDialog extends BaseActionDialog {

    private final CategoryManagementView parentView;
    private final CategoryService categoryService;
    private final Runnable onSuccess;

    private TextField nameField;
    private TextArea descriptionArea;

    public CreateCategoryDialog(CategoryManagementView parentView,
                                CategoryService categoryService,
                                Runnable onSuccess) {
        super(I18n.t("category.dialog.create.title"), onSuccess);
        this.parentView = parentView;
        this.categoryService = categoryService;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("category.dialog.create.button"));
        setWidth("500px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
    }

    private void buildForm() {
        nameField = new TextField(I18n.t("category.dialog.field.name"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder(I18n.t("category.dialog.field.name.placeholder"));
        nameField.setWidthFull();

        descriptionArea = new TextArea(I18n.t("category.dialog.field.description"));
        descriptionArea.setPlaceholder(I18n.t("category.dialog.field.description.placeholder"));
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

    @Override
    protected boolean onOk() {
        if (nameField.getValue().isBlank()) {
            append(I18n.t("category.dialog.field.name.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            CategoryDto newCategory = CategoryDto.builder()
                    .name(nameField.getValue().trim())
                    .description(descriptionArea.getValue())
                    .build();

            ResponseEntity<CategoryDto> response = categoryService.create(newCategory);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("category.dialog.create.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("category.dialog.create.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("category.dialog.create.error", e.getMessage()));
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