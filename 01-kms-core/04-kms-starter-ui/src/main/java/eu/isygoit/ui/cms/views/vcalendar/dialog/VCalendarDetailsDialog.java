package eu.isygoit.ui.cms.views.vcalendar.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.cms.views.vcalendar.VCalendarManagementView;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class VCalendarDetailsDialog extends NoActionDialog {

    private final VCalendarManagementView parentView;
    private final VCalendarService calendarService;
    private final Long calendarId;

    public VCalendarDetailsDialog(VCalendarManagementView parentView,
                                  VCalendarService calendarService,
                                  Long calendarId) {
        super(I18n.t("calendar.details.title"));
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
                add(new Span(I18n.t("calendar.details.not.found")));
                addCloseButton();
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("calendar.details.load.error", extractErrorMessage(ex))));
            addCloseButton();
        } catch (Exception e) {
            add(new Span(I18n.t("calendar.details.load.error", e.getMessage())));
            addCloseButton();
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(VCalendarDto calendar) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div infoGrid = new Div();
        infoGrid.addClassName("details-grid");
        infoGrid.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-s)");

        addFieldToGrid(infoGrid, VaadinIcon.TAG, I18n.t("calendar.details.field.name"), calendar.getName());
        addFieldToGrid(infoGrid, VaadinIcon.CODE, I18n.t("calendar.details.field.code"), calendar.getCode());
        addFieldToGrid(infoGrid, VaadinIcon.BUILDING, I18n.t("calendar.details.field.tenant"), calendar.getTenant());
        addFieldToGrid(infoGrid, VaadinIcon.FILE, I18n.t("calendar.details.field.ics.path"), calendar.getIcsPath());
        addFieldToGrid(infoGrid, VaadinIcon.LOCK, I18n.t("calendar.details.field.locked"),
                calendar.getLocked() != null && calendar.getLocked() ?
                        I18n.t("calendar.details.field.locked.yes") :
                        I18n.t("calendar.details.field.locked.no"));
        addFieldToGrid(infoGrid, VaadinIcon.USER_CHECK, I18n.t("calendar.details.field.created.by"), calendar.getCreatedBy());
        addFieldToGrid(infoGrid, VaadinIcon.CALENDAR, I18n.t("calendar.details.field.created.date"),
                calendar.getCreateDate() != null ? DateHelper.formatToHumanReadable(calendar.getCreateDate()) : null);
        addFieldToGrid(infoGrid, VaadinIcon.EDIT, I18n.t("calendar.details.field.updated.by"), calendar.getUpdatedBy());
        addFieldToGrid(infoGrid, VaadinIcon.CALENDAR_O, I18n.t("calendar.details.field.updated.date"),
                calendar.getUpdateDate() != null ? DateHelper.formatToHumanReadable(calendar.getUpdateDate()) : null);
        addFieldToGrid(infoGrid, VaadinIcon.FILE_TEXT, I18n.t("calendar.details.field.description"), calendar.getDescription());

        mainLayout.add(createSection(I18n.t("calendar.details.section.info"), infoGrid));

        add(mainLayout);
        addCloseButton();
    }

    private void addFieldToGrid(Div container, VaadinIcon icon, String label, String value) {
        if (value == null || value.isBlank()) return;
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("detail-field");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-primary-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        container.add(row);
    }

    private Component createSection(String title, Component content) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-20pct)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("padding-bottom", "var(--lumo-space-xs)");
        section.add(titleSpan, content);
        return section;
    }

    private void addCloseButton() {
        Button closeButton = new Button(I18n.t("calendar.details.close"), e -> close());
        closeButton.addClassName(LumoUtility.Margin.Top.MEDIUM);
        HorizontalLayout buttonBar = new HorizontalLayout(closeButton);
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.setWidthFull();
        add(buttonBar);
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