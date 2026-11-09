package eu.isygoit.ui.plan.views.job;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.ClipboardCopyButton;
import eu.isygoit.ui.plan.common.PlanEnumTag;
import eu.isygoit.ui.plan.common.PlanFormats;
import org.springframework.util.StringUtils;

/**
 * Read-only field grids describing a planning job, shared by the details dialog and the job page so both show
 * the same information the same way (same shared detail-field look as every details dialog).
 */
public final class PlanJobInfo {

    private PlanJobInfo() {
    }

    /**
     * Code (with copy button), client reference, engine and status tag.
     */
    public static Div identity(PlanJobMinDto job) {
        Div grid = newGrid();
        addText(grid, VaadinIcon.HASH, I18n.t("plan.ui.jobs.code"), job.getCode(), true);
        addText(grid, VaadinIcon.TAG, I18n.t("plan.ui.jobs.client.ref"), job.getClientRef(), true);
        addText(grid, VaadinIcon.COG, I18n.t("plan.ui.jobs.engine"), job.getEngine(), false);
        Span status = PlanEnumTag.ofStatus(job.getStatus());
        if (status != null) {
            grid.add(PlanEnumTag.detailField(VaadinIcon.FLAG, I18n.t("plan.ui.jobs.status"), status));
        }
        return grid;
    }

    /**
     * Submitted, started and completed timestamps and the duration.
     */
    public static Div timing(PlanJobMinDto job) {
        Div grid = newGrid();
        addText(grid, VaadinIcon.CALENDAR, I18n.t("plan.ui.jobs.submitted"),
                PlanFormats.dateTime(job.getSubmittedAt()), false);
        addText(grid, VaadinIcon.PLAY, I18n.t("plan.ui.jobs.started"),
                job.getStartedAt() == null ? null : PlanFormats.dateTime(job.getStartedAt()), false);
        addText(grid, VaadinIcon.CHECK, I18n.t("plan.ui.jobs.completed"),
                job.getCompletedAt() == null ? null : PlanFormats.dateTime(job.getCompletedAt()), false);
        String duration = PlanFormats.duration(job.getStartedAt(), job.getCompletedAt());
        addText(grid, VaadinIcon.TIMER, I18n.t("plan.ui.jobs.duration"), "-".equals(duration) ? null : duration, false);
        return grid;
    }

    /**
     * Error code and message, or {@code null} when the job has neither.
     */
    public static Div failure(PlanJobMinDto job) {
        if (!StringUtils.hasText(job.getErrorCode()) && !StringUtils.hasText(job.getMessage())) {
            return null;
        }
        Div grid = newGrid();
        addText(grid, VaadinIcon.WARNING, I18n.t("plan.ui.jobs.error.code"), job.getErrorCode(), true);
        addText(grid, VaadinIcon.COMMENT_ELLIPSIS, I18n.t("plan.ui.jobs.message"), job.getMessage(), false);
        return grid;
    }

    /**
     * An empty field grid (shared detail-grid layout).
     */
    public static Div newGrid() {
        Div grid = new Div();
        grid.addClassName("wams-card__detail-grid");
        return grid;
    }

    /**
     * Adds a text field to a grid; blank values are skipped.
     */
    public static void addText(Div grid, VaadinIcon icon, String label, String value, boolean copyable) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        Span text = new Span(value);
        Component content = text;
        if (copyable) {
            HorizontalLayout row = new HorizontalLayout(text, new ClipboardCopyButton(value));
            row.setSpacing(false);
            row.setPadding(false);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            content = row;
        }
        grid.add(PlanEnumTag.detailField(icon, label, content));
    }
}
