package eu.isygoit.ui.ims.views.parameters.dialog;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.AppParameterDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AppParameterService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.common.ImsActionDialog;
import eu.isygoit.ui.ims.views.common.ImsDialogSupport;
import eu.isygoit.ui.ims.views.parameters.ParameterManagementView;
import feign.FeignException;

/**
 * Shared form of the parameter create/update dialogs: every editable
 * {@link AppParameterDto} field is declared here once. {@code id} is never shown.
 * Subclasses only decide how the DTO is persisted.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
abstract class AbstractParameterFormDialog extends ImsActionDialog {

    final ParameterManagementView parentView;
    final AppParameterService parameterService;
    private final String messagePrefix;

    TextField nameField;
    TextField valueField;
    TextField tenantField;
    TextArea descriptionArea;

    AbstractParameterFormDialog(String title,
                                Runnable onSuccess,
                                String messagePrefix,
                                ParameterManagementView parentView,
                                AppParameterService parameterService) {
        super(title, onSuccess);
        this.messagePrefix = messagePrefix;
        this.parentView = parentView;
        this.parameterService = parameterService;
    }

    /** The DTO that receives the form values on save. */
    abstract AppParameterDto target();

    /** Persists the DTO; returns false after appending an error message. */
    abstract boolean persist(AppParameterDto dto);

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_M);

        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.parameter.dialog.section.parameter"), VaadinIcon.COG);
        FormLayout form = DialogLayout.responsiveForm();

        nameField = new TextField(I18n.t("ims.parameter.dialog.field.name"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder(I18n.t("ims.parameter.dialog.field.name.placeholder"));
        nameField.setWidthFull();

        valueField = new TextField(I18n.t("ims.parameter.dialog.field.value"));
        valueField.setRequiredIndicatorVisible(true);
        valueField.setPlaceholder(I18n.t("ims.parameter.dialog.field.value.placeholder"));
        valueField.setWidthFull();

        tenantField = new TextField(I18n.t("ims.parameter.dialog.field.tenant"));
        tenantField.setPlaceholder(I18n.t("ims.parameter.dialog.field.tenant.placeholder"));
        tenantField.setWidthFull();

        descriptionArea = DialogLayout.tall(new TextArea(I18n.t("ims.parameter.dialog.field.description")));
        descriptionArea.setPlaceholder(I18n.t("ims.parameter.dialog.field.description.placeholder"));

        form.add(nameField, valueField, tenantField, descriptionArea);
        form.setColspan(descriptionArea, 2);
        section.add(form);

        VerticalLayout root = DialogLayout.stack();
        root.add(section);
        add(root);
    }

    /** Fills the form from an existing parameter. */
    final void fillFrom(AppParameterDto dto) {
        nameField.setValue(dto.getName() != null ? dto.getName() : "");
        valueField.setValue(dto.getValue() != null ? dto.getValue() : "");
        tenantField.setValue(dto.getTenant() != null ? dto.getTenant() : "");
        descriptionArea.setValue(dto.getDescription() != null ? dto.getDescription() : "");
    }

    /** Copies the form values into the DTO ({@code id} is left untouched). */
    private void applyTo(AppParameterDto dto) {
        dto.setName(nameField.getValue());
        dto.setValue(valueField.getValue());
        dto.setTenant(tenantField.getValue().isBlank() ? null : tenantField.getValue());
        dto.setDescription(descriptionArea.getValue());
    }

    private boolean isValid() {
        if (nameField.getValue().isBlank()) {
            append(I18n.t("ims.parameter.dialog.field.name.required"));
            return false;
        }
        if (valueField.getValue().isBlank()) {
            append(I18n.t("ims.parameter.dialog.field.value.required"));
            return false;
        }
        return true;
    }

    @Override
    protected final boolean onOk() {
        if (!isValid()) {
            return false;
        }

        parentView.showLoading(true);
        try {
            AppParameterDto dto = target();
            applyTo(dto);
            if (!persist(dto)) {
                return false;
            }
            append(I18n.t(messagePrefix + ".success"));
            return true;
        } catch (FeignException ex) {
            append(ImsDialogSupport.extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t(messagePrefix + ".error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}
