package eu.isygoit.ui.cms.views.vcalendar.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.cms.views.vcalendar.VCalendarManagementView;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class VCalendarDetailsViewDialog extends DetailsViewDialog {

    private final VCalendarManagementView parentView;
    private final VCalendarService calendarService;
    private final Long calendarId;

    public VCalendarDetailsViewDialog(VCalendarManagementView parentView,
                                      VCalendarService calendarService,
                                      Long calendarId) {
        super(I18n.t("cms.calendar.details.title"));
        this.parentView = parentView;
        this.calendarService = calendarService;
        this.calendarId = calendarId;

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("calendar-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<VCalendarDto> response = calendarService.findById(calendarId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("cms.calendar.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("cms.calendar.details.load.error", extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("cms.calendar.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(VCalendarDto calendar) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity — name/code/description: what the calendar is
        Div identityGrid = createDetailGrid();

        // ID is a copyable identifier, force the copy button on even though it's short.
        addFieldToGrid(identityGrid, VaadinIcon.HASH, I18n.t("cms.calendar.details.field.id"), calendar.getId() != null ? String.valueOf(calendar.getId()) : null, true);
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("cms.calendar.details.field.name"), calendar.getName());
        // Code is a short identifier users look up/search by elsewhere — force copyable.
        addFieldToGrid(identityGrid, VaadinIcon.CODE, I18n.t("cms.calendar.details.field.code"), calendar.getCode(), true);
        // ICS path is a file path users would paste into other tools — force copyable.
        addFieldToGrid(identityGrid, VaadinIcon.FILE, I18n.t("cms.calendar.details.field.ics.path"), calendar.getIcsPath(), true);
        addFieldToGrid(identityGrid, VaadinIcon.FILE_TEXT, I18n.t("cms.calendar.details.field.description"), calendar.getDescription());

        mainLayout.add(createSection(I18n.t("cms.calendar.details.section.identity"), identityGrid));

        // Status — locked flag/tenant: current operational state
        Div statusGrid = createDetailGrid();

        addFieldToGrid(statusGrid, VaadinIcon.BUILDING, I18n.t("cms.calendar.details.field.tenant"), calendar.getTenant());
        addFieldToGrid(statusGrid, VaadinIcon.LOCK, I18n.t("cms.calendar.details.field.locked"),
                calendar.getLocked() != null && calendar.getLocked() ?
                        I18n.t("cms.calendar.details.field.locked.yes") :
                        I18n.t("cms.calendar.details.field.locked.no"));

        mainLayout.add(createSection(I18n.t("cms.calendar.details.section.status"), statusGrid));

        // Audit — created/updated by/date
        Div auditGrid = createDetailGrid();

        addFieldToGrid(auditGrid, VaadinIcon.USER_CHECK, I18n.t("cms.calendar.details.field.created.by"), calendar.getCreatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("cms.calendar.details.field.created.date"),
                calendar.getCreateDate() != null ? DateHelper.formatToHumanReadable(calendar.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.EDIT, I18n.t("cms.calendar.details.field.updated.by"), calendar.getUpdatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("cms.calendar.details.field.updated.date"),
                calendar.getUpdateDate() != null ? DateHelper.formatToHumanReadable(calendar.getUpdateDate()) : null);

        mainLayout.add(createSection(I18n.t("cms.calendar.details.section.audit"), auditGrid));

        add(mainLayout);
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
