package eu.isygoit.ui.cms.views.vcalendar.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.cms.views.vcalendar.VCalendarManagementView;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class UpdateVCalendarDialog extends BaseActionDialog {

    private final VCalendarManagementView parentView;
    private final VCalendarService calendarService;
    private final VCalendarDto calendar;
    private final Runnable onSuccess;

    private TextField tenantField;
    private TextField codeField;
    private TextField nameField;
    private TextField icsPathField;
    private Checkbox lockedCheckbox;
    private TextArea descriptionArea;

    public UpdateVCalendarDialog(VCalendarManagementView parentView,
                                 VCalendarService calendarService,
                                 VCalendarDto calendar,
                                 Runnable onSuccess) {
        super(I18n.t("cms.calendar.dialog.update.title"), onSuccess);
        this.parentView = parentView;
        this.calendarService = calendarService;
        this.calendar = calendar;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("cms.calendar.dialog.update.button"));
        setWidth("600px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
        populateFields();
    }

    private void buildForm() {
        tenantField = new TextField(I18n.t("cms.calendar.dialog.field.tenant"));
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setWidthFull();

        codeField = new TextField(I18n.t("cms.calendar.dialog.field.code"));
        codeField.setWidthFull();

        nameField = new TextField(I18n.t("cms.calendar.dialog.field.name"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();

        icsPathField = new TextField(I18n.t("cms.calendar.dialog.field.ics.path"));
        icsPathField.setWidthFull();

        lockedCheckbox = new Checkbox(I18n.t("cms.calendar.dialog.field.locked"));
        lockedCheckbox.setWidthFull();

        descriptionArea = new TextArea(I18n.t("cms.calendar.dialog.field.description"));
        descriptionArea.setWidthFull();
        descriptionArea.setHeight("100px");
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        form.add(tenantField, codeField, nameField, icsPathField);
        form.add(lockedCheckbox, 2);
        form.add(descriptionArea, 2);
        return form;
    }

    private void populateFields() {
        tenantField.setValue(calendar.getTenant() != null ? calendar.getTenant() : "");
        codeField.setValue(calendar.getCode() != null ? calendar.getCode() : "");
        nameField.setValue(calendar.getName() != null ? calendar.getName() : "");
        icsPathField.setValue(calendar.getIcsPath() != null ? calendar.getIcsPath() : "");
        lockedCheckbox.setValue(calendar.getLocked() != null && calendar.getLocked());
        descriptionArea.setValue(calendar.getDescription() != null ? calendar.getDescription() : "");
    }

    @Override
    protected boolean onOk() {
        if (tenantField.getValue().isBlank()) {
            append(I18n.t("cms.calendar.dialog.field.tenant.required"));
            return false;
        }
        if (nameField.getValue().isBlank()) {
            append(I18n.t("cms.calendar.dialog.field.name.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            calendar.setTenant(tenantField.getValue().trim());
            calendar.setCode(codeField.getValue().trim());
            calendar.setName(nameField.getValue().trim());
            calendar.setIcsPath(icsPathField.getValue().trim());
            calendar.setLocked(lockedCheckbox.getValue());
            calendar.setDescription(descriptionArea.getValue());

            ResponseEntity<VCalendarDto> response = calendarService.update(calendar.getId(), calendar);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("cms.calendar.dialog.update.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("cms.calendar.dialog.update.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("cms.calendar.dialog.update.error", e.getMessage()));
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