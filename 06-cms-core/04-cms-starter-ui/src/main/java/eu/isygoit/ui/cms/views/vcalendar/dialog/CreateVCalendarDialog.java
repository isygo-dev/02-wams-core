package eu.isygoit.ui.cms.views.vcalendar.dialog;

import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.cms.views.vcalendar.VCalendarManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Create dialog for {@link VCalendarDto}. The form itself lives in
 * {@link AbstractVCalendarFormDialog}; this class only creates the calendar.
 */
public class CreateVCalendarDialog extends AbstractVCalendarFormDialog {

    public CreateVCalendarDialog(VCalendarManagementView parentView,
                                 VCalendarService calendarService,
                                 Runnable onSuccess) {
        super(I18n.t("cms.calendar.dialog.create.title"), onSuccess,
                "cms.calendar.dialog.create", parentView, calendarService);
        setOkButtonText(I18n.t("cms.calendar.dialog.create.button"));

        buildForm();
    }

    @Override
    VCalendarDto target() {
        return new VCalendarDto();
    }

    @Override
    boolean persist(VCalendarDto dto) {
        ResponseEntity<VCalendarDto> response = calendarService.create(dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("cms.calendar.dialog.create.failed", response.getStatusCodeValue()));
            return false;
        }
        return true;
    }
}
