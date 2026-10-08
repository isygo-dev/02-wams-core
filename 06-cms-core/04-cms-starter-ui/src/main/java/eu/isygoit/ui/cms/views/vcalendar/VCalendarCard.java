package eu.isygoit.ui.cms.views.vcalendar;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.cms.views.vcalendar.dialog.DeleteVCalendarDialog;
import eu.isygoit.ui.cms.views.vcalendar.dialog.ToggleVCalendarLockDialog;
import eu.isygoit.ui.cms.views.vcalendar.dialog.VCalendarDetailsViewDialog;
import eu.isygoit.ui.common.card.BaseCard;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class VCalendarCard extends BaseCard<VCalendarManagementView, VCalendarService> {

    private final VCalendarDto calendar;
    private final Runnable onRefresh;

    private Span lockStatusChip;
    private Button toggleLockBtn;

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
        titleLayout.addClassName("wams-title-row");

        String displayName = calendar.getName() != null ? calendar.getName() : I18n.t("cms.calendar.card.default.name", calendar.getId());
        Span titleSpan = buildTitleSpan(displayName, displayName);
        titleLayout.add(titleSpan);

        // Short code badge — how users commonly search/identify calendars (see search field)
        if (calendar.getCode() != null && !calendar.getCode().isBlank()) {
            Span codeChip = buildStatusChip(calendar.getCode(), "info");
            titleLayout.add(codeChip);
        }

        // Locked status chip
        boolean locked = calendar.getLocked() != null && calendar.getLocked();
        lockStatusChip = buildStatusChip(
                locked ? I18n.t("cms.calendar.card.status.locked") : I18n.t("cms.calendar.card.status.unlocked"),
                locked ? ChipColor.WARNING : ChipColor.SUCCESS
        );
        titleLayout.add(lockStatusChip);

        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createDetailsButton(I18n.t("cms.calendar.card.details.tooltip"),
                () -> new VCalendarDetailsViewDialog(parentView, objectService, calendar.getId()).open());

        Button editBtn = createEditButton(I18n.t("cms.calendar.card.edit.tooltip"),
                () -> parentView.openUpdateCalendarDialog(calendar, () -> {
                    if (onRefresh != null) onRefresh.run();
                }));

        boolean locked = calendar.getLocked() != null && calendar.getLocked();
        toggleLockBtn = createToggleButton(locked,
                I18n.t("cms.calendar.card.lock.tooltip"),
                I18n.t("cms.calendar.card.unlock.tooltip"),
                this::openToggleLockDialog);

        Button deleteBtn = createDeleteButton(I18n.t("cms.calendar.card.delete.tooltip"),
                () -> new DeleteVCalendarDialog(parentView, objectService, calendar.getId(), () -> {
                    if (onRefresh != null) onRefresh.run();
                }).open());

        return List.of(detailsBtn, editBtn, toggleLockBtn, deleteBtn);
    }

    private void openToggleLockDialog() {
        new ToggleVCalendarLockDialog(parentView, objectService, calendar, () -> {
            updateLockStatusDisplay();
            if (onRefresh != null) onRefresh.run();
        }).open();
    }

    private void updateLockStatusDisplay() {
        boolean locked = calendar.getLocked() != null && calendar.getLocked();
        if (lockStatusChip != null) {
            String text = locked ? I18n.t("cms.calendar.card.status.locked") : I18n.t("cms.calendar.card.status.unlocked");
            lockStatusChip.setText(text);
            lockStatusChip.getElement().setAttribute("title", text);
            applyChipColor(lockStatusChip, locked ? ChipColor.WARNING : ChipColor.SUCCESS);
        }
        if (toggleLockBtn != null) {
            toggleLockBtn.setIcon(locked ? VaadinIcon.LOCK.create() : VaadinIcon.UNLOCK.create());
            toggleLockBtn.setTooltipText(locked ? I18n.t("cms.calendar.card.unlock.tooltip") : I18n.t("cms.calendar.card.lock.tooltip"));
        }
    }

    @Override
    protected void buildBodyRows() {
        String tenant = calendar.getTenant();
        if (tenant != null && !tenant.isBlank()) {
            addMetaRow(I18n.t("cms.calendar.card.tenant") + ": " + tenant);
        }
    }

}