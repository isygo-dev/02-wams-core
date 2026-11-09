package eu.isygoit.ui.plan.views.job;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.plan.PlanFeignService;
import eu.isygoit.ui.common.card.BaseCodeAssignableCard;
import eu.isygoit.ui.plan.common.PlanEnumTag;
import eu.isygoit.ui.plan.common.PlanFormats;
import eu.isygoit.ui.plan.common.PlanJobStatusRules;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Card of one {@link PlanJobMinDto}, on the shared {@link BaseCodeAssignableCard}.
 *
 * <ul>
 *   <li><b>Header</b>: code badge with copy icon (above), title = client reference or "Job code".</li>
 *   <li><b>Body</b>: engine, submitted, started, completed, duration, error code and message when present.</li>
 *   <li><b>Footer</b>: translated status tag (left) and the icon actions (right), gated by
 *       {@link PlanJobStatusRules}: details, edit request, clone, logs, stop, cancel, export result, delete.</li>
 * </ul>
 *
 * <p>The table mode of {@link PlanJobsView} reuses these very buttons (first four inline, the rest in the
 * "..." menu), so the card and the table always offer the same actions.</p>
 */
public class PlanJobCard extends BaseCodeAssignableCard<PlanJobsView, PlanFeignService, PlanJobMinDto> {

    public PlanJobCard(PlanJobsView parentView, PlanFeignService service, PlanJobMinDto job) {
        super(parentView, service, job);
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "card";
    }

    @Override
    protected String codeOf(PlanJobMinDto object) {
        return object.getCode();
    }

    @Override
    protected Component buildTitle() {
        String title = StringUtils.hasText(item.getClientRef())
                ? item.getClientRef()
                : I18n.t("plan.ui.card.title.default",
                StringUtils.hasText(item.getCode()) ? item.getCode() : String.valueOf(item.getId()));
        return buildTitleSpan(title, title);
    }

    @Override
    protected Span buildStatusTag() {
        return PlanEnumTag.ofStatus(item.getStatus());
    }

    @Override
    protected void buildBodyRows() {
        addRow(VaadinIcon.COG, I18n.t("plan.ui.jobs.engine"), item.getEngine(), null);
        addRow(VaadinIcon.CALENDAR, I18n.t("plan.ui.jobs.submitted"), PlanFormats.dateTime(item.getSubmittedAt()), null);
        if (item.getStartedAt() != null) {
            addRow(VaadinIcon.PLAY, I18n.t("plan.ui.jobs.started"), PlanFormats.dateTime(item.getStartedAt()), null);
        }
        if (item.getCompletedAt() != null) {
            addRow(VaadinIcon.CHECK, I18n.t("plan.ui.jobs.completed"), PlanFormats.dateTime(item.getCompletedAt()), null);
        }
        String duration = PlanFormats.duration(item.getStartedAt(), item.getCompletedAt());
        if (!"-".equals(duration)) {
            addRow(VaadinIcon.TIMER, I18n.t("plan.ui.jobs.duration"), duration, null);
        }
        addRow(VaadinIcon.WARNING, I18n.t("plan.ui.jobs.error.code"), item.getErrorCode(), LumoUtility.TextColor.ERROR);
        addRow(VaadinIcon.COMMENT_ELLIPSIS, I18n.t("plan.ui.jobs.message"), item.getMessage(), null);
    }

    /**
     * Body row {@code [icon] Label: value} with the shared card-row look; skipped when the value is blank.
     */
    private void addRow(VaadinIcon icon, String label, String value, String valueClass) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("wams-profile-card__row");

        Icon iconComponent = icon.create();
        iconComponent.addClassName("wams-profile-card__row-icon");
        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName("wams-profile-card__row-label");
        Span valueSpan = new Span(value);
        valueSpan.addClassName("wams-profile-card__row-value");
        valueSpan.getElement().setAttribute("title", value);
        if (valueClass != null) {
            valueSpan.addClassName(valueClass);
        }
        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        add(row);
    }

    @Override
    protected List<Button> buildActionButtons() {
        IEnumJobStatusCode.Types status = item.getStatus();
        List<Button> buttons = new ArrayList<>();

        buttons.add(action(VaadinIcon.EYE, "plan.ui.job.tooltip.view", () -> parentView.openDetailsDialog(item)));
        if (PlanJobStatusRules.isEditable(status)) {
            buttons.add(createEditButton(I18n.t("plan.ui.job.tooltip.edit"), () -> parentView.openEditDialog(item)));
        }
        buttons.add(action(VaadinIcon.COPY, "plan.ui.job.tooltip.clone", () -> parentView.cloneJob(item)));
        buttons.add(action(VaadinIcon.FILE_TEXT_O, "plan.ui.job.tooltip.logs", () -> parentView.openLogs(item)));
        if (PlanJobStatusRules.isStoppable(status)) {
            buttons.add(action(VaadinIcon.STOP, "plan.ui.job.tooltip.stop", () -> parentView.stopJob(item)));
        }
        if (PlanJobStatusRules.isCancellable(status)) {
            buttons.add(action(VaadinIcon.CLOSE_CIRCLE_O, "plan.ui.job.tooltip.cancel", () -> parentView.cancelJob(item)));
        }
        if (PlanJobStatusRules.isExportable(status)) {
            buttons.add(action(VaadinIcon.DOWNLOAD, "plan.ui.job.tooltip.export", () -> parentView.exportResult(item)));
        }
        if (PlanJobStatusRules.isDeletable(status)) {
            buttons.add(createDeleteButton(I18n.t("plan.ui.job.tooltip.delete"), () -> parentView.deleteJob(item)));
        }
        return buttons;
    }

    private Button action(VaadinIcon icon, String tooltipKey, Runnable onClick) {
        Button button = createIconButton(icon, I18n.t(tooltipKey));
        button.addClickListener(e -> onClick.run());
        return button;
    }
}
