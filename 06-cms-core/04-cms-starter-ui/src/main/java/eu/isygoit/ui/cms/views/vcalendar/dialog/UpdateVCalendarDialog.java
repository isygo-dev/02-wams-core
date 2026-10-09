package eu.isygoit.ui.cms.views.vcalendar.dialog;

import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.cms.views.vcalendar.VCalendarManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link VCalendarDto}. The form itself lives in
 * {@link AbstractVCalendarFormDialog}; this class only updates the calendar.
 */
public class UpdateVCalendarDialog extends AbstractVCalendarFormDialog {

    private final VCalendarDto calendar;

    public UpdateVCalendarDialog(VCalendarManagementView parentView,
                                 VCalendarService calendarService,
                                 VCalendarDto calendar,
                                 Runnable onSuccess) {
        super(I18n.t("cms.calendar.dialog.update.title"), onSuccess,
                "cms.calendar.dialog.update", parentView, calendarService);
        this.calendar = calendar;
        setOkButtonText(I18n.t("cms.calendar.dialog.update.button"));

        buildForm();
        fillFrom(calendar);
    }

    @Override
    VCalendarDto target() {
        return calendar;
    }

    @Override
    boolean persist(VCalendarDto dto) {
        ResponseEntity<VCalendarDto> response = calendarService.update(dto.getId(), dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("cms.calendar.dialog.update.failed", response.getStatusCodeValue()));
            return false;
        }
        return true;
    }
}
