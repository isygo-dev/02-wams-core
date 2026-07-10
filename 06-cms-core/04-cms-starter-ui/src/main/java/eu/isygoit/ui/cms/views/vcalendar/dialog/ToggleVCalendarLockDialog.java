package eu.isygoit.ui.cms.views.vcalendar.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.cms.views.vcalendar.VCalendarManagementView;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Confirmation dialog for toggling a calendar's locked flag. VCalendar has no
 * dedicated status-toggle endpoint, so this issues a normal {@code update()}
 * call with the {@code locked} field flipped — same simple-confirmation
 * (no PIN) shape as {@code ToggleTenantStatusDialog}.
 */
public class ToggleVCalendarLockDialog extends PinBaseActionDialog {

    private final VCalendarManagementView parentView;
    private final VCalendarService calendarService;
    private final VCalendarDto calendar;
    private final boolean currentlyLocked;

    public ToggleVCalendarLockDialog(VCalendarManagementView parentView,
                                     VCalendarService calendarService,
                                     VCalendarDto calendar,
                                     Runnable onSuccess) {
        super(
                Boolean.TRUE.equals(calendar.getLocked())
                        ? I18n.t("cms.calendar.dialog.toggle.title.unlock")
                        : I18n.t("cms.calendar.dialog.toggle.title.lock"),
                Boolean.TRUE.equals(calendar.getLocked())
                        ? I18n.t("cms.calendar.dialog.toggle.message.unlock")
                        : I18n.t("cms.calendar.dialog.toggle.message.lock"),
                onSuccess,
                false // simple confirmation, no PIN
        );
        this.parentView = parentView;
        this.calendarService = calendarService;
        this.calendar = calendar;
        this.currentlyLocked = Boolean.TRUE.equals(calendar.getLocked());

        setOkButtonText(currentlyLocked
                ? I18n.t("cms.calendar.dialog.toggle.button.unlock")
                : I18n.t("cms.calendar.dialog.toggle.button.lock"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_PRIMARY);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            calendar.setLocked(!currentlyLocked);

            ResponseEntity<VCalendarDto> response = calendarService.update(calendar.getId(), calendar);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                calendar.setLocked(currentlyLocked);
                append(I18n.t("cms.calendar.dialog.toggle.failed", response.getStatusCodeValue()));
                return false;
            }

            append(currentlyLocked
                    ? I18n.t("cms.calendar.dialog.toggle.success.unlock")
                    : I18n.t("cms.calendar.dialog.toggle.success.lock"));
            return true;
        } catch (FeignException ex) {
            calendar.setLocked(currentlyLocked);
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            calendar.setLocked(currentlyLocked);
            append(I18n.t("cms.calendar.dialog.toggle.error", e.getMessage()));
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
