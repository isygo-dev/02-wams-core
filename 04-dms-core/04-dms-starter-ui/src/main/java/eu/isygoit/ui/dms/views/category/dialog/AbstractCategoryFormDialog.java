package eu.isygoit.ui.dms.views.category.dialog;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.dms.views.category.CategoryManagementView;
import eu.isygoit.ui.dms.views.common.DmsActionDialog;
import eu.isygoit.ui.dms.views.common.DmsDialogSupport;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Shared form of the category create/update dialogs: every editable
 * {@link CategoryDto} field (name, description) is declared here once.
 * {@code id} is never shown; audit fields are only displayed in the details
 * dialog. Subclasses only decide how the DTO is persisted.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
abstract class AbstractCategoryFormDialog extends DmsActionDialog {

    final CategoryService service;
    private final CategoryManagementView parentView;
    private final String messagePrefix;

    TextField nameField;
    TextArea descriptionField;

    AbstractCategoryFormDialog(String title,
                               Runnable onSuccess,
                               String messagePrefix,
                               CategoryManagementView parentView,
                               CategoryService service) {
        super(title, onSuccess);
        this.messagePrefix = messagePrefix;
        this.parentView = parentView;
        this.service = service;
    }

    /** The DTO that receives the form values on save. */
    abstract CategoryDto target();

    /** Persists the DTO and returns the service response. */
    abstract ResponseEntity<CategoryDto> persist(CategoryDto dto);

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_S);

        nameField = new TextField(I18n.t("dms.category.dialog.field.name"));
        nameField.setRequired(true);
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder(I18n.t("dms.category.dialog.field.name.placeholder"));
        nameField.setWidthFull();

        descriptionField = DialogLayout.tall(new TextArea(I18n.t("dms.category.dialog.field.description")));
        descriptionField.setPlaceholder(I18n.t("dms.category.dialog.field.description.placeholder"));

        FormLayout form = DialogLayout.responsiveForm();
        form.add(nameField, descriptionField);
        form.setColspan(nameField, 2);
        form.setColspan(descriptionField, 2);
        addContent(form);
    }

    /** Fills the form from an existing category. */
    final void fillFrom(CategoryDto dto) {
        nameField.setValue(dto.getName() != null ? dto.getName() : "");
        descriptionField.setValue(dto.getDescription() != null ? dto.getDescription() : "");
    }

    @Override
    protected final boolean onOk() {
        if (nameField.getValue() == null || nameField.getValue().isBlank()) {
            append(I18n.t("dms.category.dialog.field.name.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            CategoryDto dto = target();
            dto.setName(nameField.getValue().trim());
            dto.setDescription(descriptionField.getValue());

            ResponseEntity<CategoryDto> response = persist(dto);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t(messagePrefix + ".failed", response.getStatusCodeValue()));
                return false;
            }
            append(I18n.t(messagePrefix + ".success"));
            return true;
        } catch (FeignException ex) {
            append(DmsDialogSupport.extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t(messagePrefix + ".error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}
