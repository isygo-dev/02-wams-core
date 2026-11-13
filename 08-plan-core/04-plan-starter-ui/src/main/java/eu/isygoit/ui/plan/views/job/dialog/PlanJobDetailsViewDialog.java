package eu.isygoit.ui.plan.views.job.dialog;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.PlanJobDto;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.response.Metrics;
import eu.isygoit.dto.response.PlanningSolution;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.plan.common.PlanEnumTag;
import eu.isygoit.ui.plan.common.PlanFormats;
import eu.isygoit.ui.plan.common.PlanJobStatusRules;
import eu.isygoit.ui.plan.views.job.PlanJobActions;
import eu.isygoit.ui.plan.views.job.PlanJobInfo;

import java.text.NumberFormat;
import java.util.List;

/**
 * Read-only summary of a planning job on the shared {@link DetailsViewDialog}: identity (code with copy button,
 * client reference, engine, status tag), timing, error, a count summary of the submitted request and a summary
 * of the solver result, with a button that opens the full job page ({@code plan/job/:id}) for the raw result.
 */
public class PlanJobDetailsViewDialog extends DetailsViewDialog {

    private final PlanJobDto job;
    private final PlanJobActions actions;
    private final Runnable onChange;

    public PlanJobDetailsViewDialog(PlanJobDto job, PlanJobActions actions, Runnable onChange) {
        super(I18n.t("plan.ui.job.details.title", job.getCode() == null ? String.valueOf(job.getId()) : job.getCode()));
        this.job = job;
        this.actions = actions;
        this.onChange = onChange;
        DialogLayout.size(this, DialogLayout.WIDTH_L);
        buildContent();
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private void buildContent() {
        VerticalLayout main = DialogLayout.stack();
        main.add(createSection(I18n.t("plan.ui.job.details.section.identity"), PlanJobInfo.identity(job)));
        main.add(createSection(I18n.t("plan.ui.job.details.section.timing"), PlanJobInfo.timing(job)));
        Div failure = PlanJobInfo.failure(job);
        if (failure != null) {
            main.add(createSection(I18n.t("plan.ui.job.details.section.error"), failure));
        }
        if (job.getRequest() != null) {
            main.add(createSection(I18n.t("plan.ui.job.details.section.request"), requestSummary(job.getRequest())));
        }
        if (job.getResult() != null) {
            main.add(createSection(I18n.t("plan.ui.job.details.section.result"), resultSummary(job.getResult())));
        }

        Button openPage = new Button(I18n.t("plan.ui.job.details.open.page"), VaadinIcon.EXTERNAL_LINK.create());
        openPage.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        openPage.addClickListener(e -> {
            close();
            UI.getCurrent().navigate("plan/job/" + job.getId());
        });
        main.add(openPage);
        addDangerActions(main);
        add(main);
        setAudit(job);
    }

    /**
     * Stop, cancel and delete, gated by the status, opening the same confirmation dialogs as the cards.
     */
    private void addDangerActions(VerticalLayout main) {
        if (PlanJobStatusRules.isStoppable(job.getStatus())) {
            main.add(actionButton("plan.ui.action.stop", VaadinIcon.STOP, () -> actions.confirmStop(job, onChange, null)));
        }
        if (PlanJobStatusRules.isCancellable(job.getStatus())) {
            main.add(actionButton("plan.ui.action.cancel", VaadinIcon.CLOSE_CIRCLE_O, () -> actions.confirmCancel(job, onChange, null)));
        }
        if (PlanJobStatusRules.isDeletable(job.getStatus())) {
            main.add(actionButton("plan.ui.action.delete", VaadinIcon.TRASH, () -> actions.confirmDelete(job, onChange, null)));
        }
    }

    private Button actionButton(String labelKey, VaadinIcon icon, Runnable action) {
        Button button = new Button(I18n.t(labelKey), icon.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        button.addClickListener(e -> {
            close();
            action.run();
        });
        return button;
    }

    private Div requestSummary(PlanningRequest request) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.CLOCK, I18n.t("plan.ui.request.slots"), String.valueOf(size(request.slots())), false);
        addFieldToGrid(grid, VaadinIcon.USERS, I18n.t("plan.ui.request.resources"), String.valueOf(size(request.resources())), false);
        addFieldToGrid(grid, VaadinIcon.TASKS, I18n.t("plan.ui.request.tasks"), String.valueOf(size(request.tasks())), false);
        addFieldToGrid(grid, VaadinIcon.LOCK, I18n.t("plan.ui.request.constraints"), String.valueOf(size(request.constraints())), false);
        addFieldToGrid(grid, VaadinIcon.BULLSEYE, I18n.t("plan.ui.request.objectives"), String.valueOf(size(request.objectives())), false);
        return grid;
    }

    private Div resultSummary(SolveResult result) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.FLAG, I18n.t("plan.ui.result.status"),
                PlanEnumTag.solverStatusLabel(result.status()), false);
        PlanningSolution solution = result.solution();
        if (solution != null) {
            addFieldToGrid(grid, VaadinIcon.CHECK, I18n.t("plan.ui.result.assignments"),
                    String.valueOf(size(solution.assignments())), false);
            addFieldToGrid(grid, VaadinIcon.EXCLAMATION_CIRCLE, I18n.t("plan.ui.result.unassigned"),
                    String.valueOf(size(solution.unassignedTasks())), false);
            addFieldToGrid(grid, VaadinIcon.STAR, I18n.t("plan.ui.result.quality"),
                    NumberFormat.getPercentInstance(I18n.getCurrentLocale()).format(solution.qualityScore()), false);
        }
        Metrics metrics = result.metrics();
        if (metrics != null) {
            addFieldToGrid(grid, VaadinIcon.TIMER, I18n.t("plan.ui.jobs.duration"),
                    PlanFormats.millis(metrics.durationMillis()), false);
            if (metrics.search() != null) {
                addFieldToGrid(grid, VaadinIcon.REFRESH, I18n.t("plan.ui.result.solutionsFound"),
                        String.valueOf(metrics.search().solutionsFound()), false);
            }
        }
        return grid;
    }
}
