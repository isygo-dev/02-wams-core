package eu.isygoit.ui.cms.views.vcalendar.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.cms.views.vcalendar.VCalendarManagementView;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.common.dialog.DialogLayout;
import feign.FeignException;

/**
 * Shared form of the calendar create/update dialogs: every editable
 * {@link VCalendarDto} field is declared here once. {@code id} is never shown
 * and {@code code} is always read-only (assigned by the server). Subclasses
 * only decide how the DTO is persisted.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
abstract class AbstractVCalendarFormDialog extends BaseActionDialog {

    final VCalendarManagementView parentView;
    final VCalendarService calendarService;
    private final String messagePrefix;

    TextField tenantField;
    TextField codeField;
    TextField nameField;
    TextField icsPathField;
    Checkbox lockedCheckbox;
    TextArea descriptionArea;

    AbstractVCalendarFormDialog(String title,
                                Runnable onSuccess,
                                String messagePrefix,
                                VCalendarManagementView parentView,
                                VCalendarService calendarService) {
        super(title, onSuccess);
        addClassName(VCalendarDialogSupport.CLASS_CMS_DIALOG);
        this.messagePrefix = messagePrefix;
        this.parentView = parentView;
        this.calendarService = calendarService;
    }

    /** The DTO that receives the form values on save. */
    abstract VCalendarDto target();

    /** Persists the DTO; returns false after appending an error message. */
    abstract boolean persist(VCalendarDto dto);

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_M);

        VerticalLayout root = DialogLayout.stack();
        root.add(buildIdentitySection(), buildDetailsSection());
        addContent(root);
    }

    private VerticalLayout buildIdentitySection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("cms.calendar.details.section.identity"), VaadinIcon.CALENDAR);
        FormLayout form = DialogLayout.responsiveForm();

        tenantField = new TextField(I18n.t("cms.calendar.dialog.field.tenant"));
        tenantField.setRequired(true);
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setPlaceholder(I18n.t("cms.calendar.dialog.field.tenant.placeholder"));
        tenantField.setWidthFull();

        codeField = new TextField(I18n.t("cms.calendar.dialog.field.code"));
        codeField.setReadOnly(true);
        codeField.setWidthFull();

        nameField = new TextField(I18n.t("cms.calendar.dialog.field.name"));
        nameField.setRequired(true);
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder(I18n.t("cms.calendar.dialog.field.name.placeholder"));
        nameField.setWidthFull();

        icsPathField = new TextField(I18n.t("cms.calendar.dialog.field.ics.path"));
        icsPathField.setPlaceholder(I18n.t("cms.calendar.dialog.field.ics.path.placeholder"));
        icsPathField.setWidthFull();

        form.add(tenantField, codeField, nameField, icsPathField);
        section.add(form);
        return section;
    }

    private VerticalLayout buildDetailsSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("cms.calendar.details.section.description"), VaadinIcon.FILE_TEXT);
        FormLayout form = DialogLayout.responsiveForm();

        lockedCheckbox = new Checkbox(I18n.t("cms.calendar.dialog.field.locked"));

        descriptionArea = DialogLayout.tall(new TextArea(I18n.t("cms.calendar.dialog.field.description")));
        descriptionArea.setPlaceholder(I18n.t("cms.calendar.dialog.field.description.placeholder"));

        form.add(lockedCheckbox, descriptionArea);
        form.setColspan(lockedCheckbox, 2);
        form.setColspan(descriptionArea, 2);
        section.add(form);
        return section;
    }

    /** Fills the form from an existing calendar. */
    final void fillFrom(VCalendarDto dto) {
        tenantField.setValue(dto.getTenant() != null ? dto.getTenant() : "");
        codeField.setValue(dto.getCode() != null ? dto.getCode() : "");
        nameField.setValue(dto.getName() != null ? dto.getName() : "");
        icsPathField.setValue(dto.getIcsPath() != null ? dto.getIcsPath() : "");
        lockedCheckbox.setValue(Boolean.TRUE.equals(dto.getLocked()));
        descriptionArea.setValue(dto.getDescription() != null ? dto.getDescription() : "");
    }

    /** Copies the form values into the DTO ({@code id} and {@code code} are left untouched). */
    private void applyTo(VCalendarDto dto) {
        dto.setTenant(tenantField.getValue().trim());
        dto.setName(nameField.getValue().trim());
        dto.setIcsPath(icsPathField.getValue().trim());
        dto.setLocked(lockedCheckbox.getValue());
        dto.setDescription(descriptionArea.getValue());
    }

    private boolean isValid() {
        if (tenantField.getValue().isBlank()) {
            append(I18n.t("cms.calendar.dialog.field.tenant.required"));
            return false;
        }
        if (nameField.getValue().isBlank()) {
            append(I18n.t("cms.calendar.dialog.field.name.required"));
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
            VCalendarDto dto = target();
            applyTo(dto);
            if (!persist(dto)) {
                return false;
            }
            append(I18n.t(messagePrefix + ".success"));
            return true;
        } catch (FeignException ex) {
            append(VCalendarDialogSupport.extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t(messagePrefix + ".error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}
