package eu.isygoit.ui.cms.views.vcalendar;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.cms.views.vcalendar.dialog.DeleteVCalendarDialog;
import eu.isygoit.ui.cms.views.vcalendar.dialog.VCalendarDetailsDialog;
import eu.isygoit.ui.common.card.BaseCard;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class VCalendarCard extends BaseCard<VCalendarManagementView, VCalendarService> {

    private final VCalendarDto calendar;
    private final Runnable onRefresh;

    public VCalendarCard(VCalendarManagementView parentView,
                         VCalendarService calendarService,
                         VCalendarDto calendar,
                         Runnable onRefresh) {
        super(parentView, calendarService);
        this.calendar = calendar;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "calendar-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        titleLayout.getStyle().set("flex-wrap", "wrap");

        String displayName = calendar.getName() != null ? calendar.getName() : I18n.t("cms.calendar.card.default.name", calendar.getId());
        Span titleSpan = buildTitleSpan(displayName, calendar.getDescription());

        // Locked status chip
        if (calendar.getLocked() != null && calendar.getLocked()) {
            Span lockChip = buildStatusChip(
                    I18n.t("cms.calendar.card.status.locked"),
                    "LOCKED"
            );
            titleLayout.add(titleSpan, lockChip);
        } else {
            Span unlockedChip = buildStatusChip(
                    I18n.t("cms.calendar.card.status.unlocked"),
                    "UNLOCKED"
            );
            titleLayout.add(titleSpan, unlockedChip);
        }

        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, I18n.t("cms.calendar.card.details.tooltip"));
        detailsBtn.addClickListener(e -> new VCalendarDetailsDialog(parentView, objectService, calendar.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, I18n.t("cms.calendar.card.edit.tooltip"));
        editBtn.addClickListener(e -> parentView.openUpdateCalendarDialog(calendar, () -> {
            if (onRefresh != null) onRefresh.run();
        }));

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, I18n.t("cms.calendar.card.delete.tooltip"));
        deleteBtn.addClickListener(e -> new DeleteVCalendarDialog(parentView, objectService, calendar.getId(), () -> {
            if (onRefresh != null) onRefresh.run();
        }).open());

        return List.of(detailsBtn, editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.getStyle().set("margin-top", "var(--lumo-space-s)");

        body.add(createIconRow(VaadinIcon.CODE, I18n.t("cms.calendar.card.code"), calendar.getCode()));
        body.add(createIconRow(VaadinIcon.BUILDING, I18n.t("cms.calendar.card.tenant"), calendar.getTenant()));
        body.add(createIconRow(VaadinIcon.FILE, I18n.t("cms.calendar.card.ics.path"), calendar.getIcsPath()));
        body.add(createIconRow(VaadinIcon.USER, I18n.t("cms.calendar.card.created.by"), calendar.getCreatedBy()));

        if (calendar.getDescription() != null && !calendar.getDescription().isBlank()) {
            body.add(createDescriptionRow(calendar.getDescription()));
        }

        add(body);
    }

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        com.vaadin.flow.component.icon.Icon iconComponent = icon.create();
        iconComponent.setSize("14px");
        iconComponent.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        labelSpan.getStyle().set("min-width", "80px");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private HorizontalLayout createDescriptionRow(String description) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.START);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        com.vaadin.flow.component.icon.Icon iconComponent = VaadinIcon.FILE_TEXT.create();
        iconComponent.setSize("14px");
        iconComponent.getStyle().set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "2px");

        Span labelSpan = new Span(I18n.t("cms.calendar.card.description") + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        labelSpan.getStyle().set("min-width", "80px");

        Span valueSpan = new Span(description);
        valueSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        valueSpan.getStyle().set("word-break", "break-word");
        valueSpan.getStyle().set("flex", "1");
        valueSpan.getStyle().set("white-space", "pre-wrap");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    @Override
    protected void onCardAttach(AttachEvent event) {
        // nothing special
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .calendar-card {
                    padding: var(--lumo-space-s) var(--lumo-space-m);
                }
                .calendar-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                    margin-bottom: var(--lumo-space-xs);
                }
                .calendar-card .meta-row:last-child {
                    border-bottom: none;
                    margin-bottom: 0;
                }
                @media (max-width: 640px) {
                    .calendar-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .calendar-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                    .calendar-card .calendar-card__button-bar {
                        width: 100%;
                        justify-content: flex-start;
                    }
                }
                """;
    }
}