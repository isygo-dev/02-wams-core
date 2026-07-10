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

public class CreateVCalendarDialog extends BaseActionDialog {

    private final VCalendarManagementView parentView;
    private final VCalendarService calendarService;
    private final Runnable onSuccess;

    private TextField tenantField;
    private TextField codeField;
    private TextField nameField;
    private TextField icsPathField;
    private Checkbox lockedCheckbox;
    private TextArea descriptionArea;

    public CreateVCalendarDialog(VCalendarManagementView parentView,
                                 VCalendarService calendarService,
                                 Runnable onSuccess) {
        super(I18n.t("cms.calendar.dialog.create.title"), onSuccess);
        this.parentView = parentView;
        this.calendarService = calendarService;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("cms.calendar.dialog.create.button"));
        setWidth("600px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
    }

    private void buildForm() {
        tenantField = new TextField(I18n.t("cms.calendar.dialog.field.tenant"));
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setPlaceholder(I18n.t("cms.calendar.dialog.field.tenant.placeholder"));
        tenantField.setWidthFull();

        codeField = new TextField(I18n.t("cms.calendar.dialog.field.code"));
        codeField.setPlaceholder(I18n.t("cms.calendar.dialog.field.code.placeholder"));
        codeField.setWidthFull();

        nameField = new TextField(I18n.t("cms.calendar.dialog.field.name"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder(I18n.t("cms.calendar.dialog.field.name.placeholder"));
        nameField.setWidthFull();

        icsPathField = new TextField(I18n.t("cms.calendar.dialog.field.ics.path"));
        icsPathField.setPlaceholder(I18n.t("cms.calendar.dialog.field.ics.path.placeholder"));
        icsPathField.setWidthFull();

        lockedCheckbox = new Checkbox(I18n.t("cms.calendar.dialog.field.locked"));
        lockedCheckbox.setWidthFull();

        descriptionArea = new TextArea(I18n.t("cms.calendar.dialog.field.description"));
        descriptionArea.setPlaceholder(I18n.t("cms.calendar.dialog.field.description.placeholder"));
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
            VCalendarDto newCalendar = VCalendarDto.builder()
                    .tenant(tenantField.getValue().trim())
                    .code(codeField.getValue().trim())
                    .name(nameField.getValue().trim())
                    .icsPath(icsPathField.getValue().trim())
                    .locked(lockedCheckbox.getValue())
                    .description(descriptionArea.getValue())
                    .build();

            ResponseEntity<VCalendarDto> response = calendarService.create(newCalendar);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("cms.calendar.dialog.create.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("cms.calendar.dialog.create.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("cms.calendar.dialog.create.error", e.getMessage()));
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