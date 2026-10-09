package eu.isygoit.ui.cms.views.vcalendar.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.cms.views.vcalendar.VCalendarManagementView;

public class DeleteVCalendarDialog extends DeleteActionDialog {

    public DeleteVCalendarDialog(VCalendarManagementView parentView,
                                 VCalendarService calendarService,
                                 Long calendarId,
                                 Runnable onSuccess) {
        super(new Texts(
                        I18n.t("cms.calendar.dialog.delete.title"),
                        I18n.t("cms.calendar.dialog.delete.message"),
                        I18n.t("cms.calendar.dialog.delete.button"),
                        I18n.t("cms.calendar.dialog.delete.invalid.code"),
                        I18n.t("cms.calendar.dialog.delete.success"),
                        detail -> I18n.t("cms.calendar.dialog.delete.error", detail)),
                () -> calendarService.delete(calendarId),
                onSuccess,
                parentView::showLoading,
                "cms-dialog");
    }
}
