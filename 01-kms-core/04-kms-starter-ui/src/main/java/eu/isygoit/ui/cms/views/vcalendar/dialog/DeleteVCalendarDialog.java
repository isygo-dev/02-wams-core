package eu.isygoit.ui.cms.views.vcalendar.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.cms.views.vcalendar.VCalendarManagementView;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import feign.FeignException;

public class DeleteVCalendarDialog extends PinBaseActionDialog {

    private final VCalendarManagementView parentView;
    private final VCalendarService calendarService;
    private final Long calendarId;

    public DeleteVCalendarDialog(VCalendarManagementView parentView,
                                 VCalendarService calendarService,
                                 Long calendarId,
                                 Runnable onSuccess) {
        super(I18n.t("calendar.dialog.delete.title"),
                I18n.t("calendar.dialog.delete.message"),
                onSuccess);
        this.parentView = parentView;
        this.calendarService = calendarService;
        this.calendarId = calendarId;

        setOkButtonText(I18n.t("calendar.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("calendar.dialog.delete.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            calendarService.delete(calendarId);
            append(I18n.t("calendar.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("calendar.dialog.delete.error", e.getMessage()));
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