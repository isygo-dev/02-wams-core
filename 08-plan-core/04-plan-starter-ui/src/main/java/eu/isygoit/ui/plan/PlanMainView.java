package eu.isygoit.ui.plan;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.async.ContextAwareAsync;
import eu.isygoit.i18n.I18n;
import eu.isygoit.dto.response.JobDashboardSummary;
import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.remote.plan.PlanFeignService;
import eu.isygoit.ui.plan.common.PlanEnumTag;
import eu.isygoit.ui.plan.common.PlanFormats;
import eu.isygoit.ui.common.component.*;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.plan.layout.PlanMainLayout;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Planning dashboard (routes {@code plan} and {@code plan/home}): job counters, status breakdown and the most
 * recent jobs, loaded in one call off the UI thread. Status tags and dates use the shared plan formats.
 */
@Slf4j
@SpringComponent
@RouteScope
@RouteAlias(value = "plan/home", layout = PlanMainLayout.class)
@Route(value = "plan", layout = PlanMainLayout.class)
@PermitAll
public class PlanMainView extends ManagementVerticalView {
    private static final int RECENT_JOB_LIMIT = 50;

    private final PlanFeignService planFeignService;
    private final UI ui;
    private final Grid<PlanJobMinDto> jobsGrid = new Grid<>(PlanJobMinDto.class, false);
    private final DashboardBreakdownPanel statusPanel;
    private final StatCard totalJobsCard;
    private final StatCard activeJobsCard;
    private final StatCard succeededJobsCard;
    private final StatCard attentionJobsCard;
    private final TextField jobIdField = new TextField();

    @Autowired
    public PlanMainView(PlanFeignService planFeignService) {
        this.planFeignService = planFeignService;
        this.ui = UI.getCurrent();
        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("plan-dashboard");

        add(new PageHeader("plan.ui.dashboard.title", "plan.ui.dashboard.subtitle"));
        add(new DashboardShortcutsBar(I18n.t("plan.ui.dashboard.quick.actions"), List.of(
                new DashboardShortcutsBar.Shortcut(VaadinIcon.REFRESH,
                        I18n.t("plan.ui.dashboard.refresh"), this::loadDashboard),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.LIST,
                        I18n.t("plan.ui.nav.jobs"), this::openJobs),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.LIST,
                        I18n.t("plan.ui.nav.logs"), this::openLogs)
        )));

        totalJobsCard = new StatCard(VaadinIcon.LIST, StatCard.Variant.PRIMARY,
                I18n.t("plan.ui.dashboard.total"), null);
        activeJobsCard = new StatCard(VaadinIcon.CLOCK, StatCard.Variant.WARNING,
                I18n.t("plan.ui.dashboard.active"), null);
        succeededJobsCard = new StatCard(VaadinIcon.CHECK_CIRCLE, StatCard.Variant.SUCCESS,
                I18n.t("plan.ui.dashboard.succeeded"), null);
        attentionJobsCard = new StatCard(VaadinIcon.WARNING, StatCard.Variant.NEUTRAL,
                I18n.t("plan.ui.dashboard.attention"), null);
        add(new StatCardGrid(totalJobsCard, activeJobsCard, succeededJobsCard, attentionJobsCard));

        statusPanel = new DashboardBreakdownPanel(I18n.t("plan.ui.dashboard.status.breakdown"));
        statusPanel.setRefreshAction(this::loadDashboard);
        add(statusPanel);

        add(buildJobLookup());
        configureJobsGrid();
        add(jobsGrid);
        loadDashboard();
    }

    @Override
    public String getPageTitle() {
        return I18n.t("plan.ui.dashboard.title");
    }

    private HorizontalLayout buildJobLookup() {
        jobIdField.setLabel(I18n.t("plan.ui.job.id"));
        jobIdField.setPlaceholder(I18n.t("plan.ui.job.id.placeholder"));
        jobIdField.setClearButtonVisible(true);
        jobIdField.setWidthFull();
        Button openButton = new Button(I18n.t("plan.ui.job.open"), VaadinIcon.SEARCH.create());
        openButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        openButton.addClickListener(event -> openJob(jobIdField.getValue()));
        HorizontalLayout lookup = new HorizontalLayout(jobIdField, openButton);
        lookup.setWidthFull();
        lookup.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
        lookup.addClassName("plan-job-lookup");
        return lookup;
    }

    private void configureJobsGrid() {
        jobsGrid.addClassName("plan-recent-jobs");
        jobsGrid.setWidthFull();
        jobsGrid.setHeight("28rem");
        jobsGrid.addColumn(job -> job.getCode() == null ? String.valueOf(job.getId()) : job.getCode())
                .setHeader(I18n.t("plan.ui.jobs.id"))
                .setAutoWidth(true)
                .setFlexGrow(1);
        jobsGrid.addComponentColumn(job -> job.getStatus() == null ? new Span() : PlanEnumTag.ofStatus(job.getStatus()))
                .setHeader(I18n.t("plan.ui.jobs.status"))
                .setAutoWidth(true);
        jobsGrid.addColumn(job -> PlanFormats.dateTime(job.getSubmittedAt()))
                .setHeader(I18n.t("plan.ui.jobs.submitted"))
                .setAutoWidth(true);
        jobsGrid.addColumn(job -> job.getMessage() == null ? "" : job.getMessage())
                .setHeader(I18n.t("plan.ui.jobs.message"))
                .setFlexGrow(2);
        jobsGrid.addComponentColumn(job -> {
            Button open = new Button(I18n.t("plan.ui.job.open"), VaadinIcon.ARROW_RIGHT.create());
            open.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            open.addClickListener(event -> openJob(String.valueOf(job.getId())));
            return open;
        }).setHeader(I18n.t("plan.ui.jobs.actions")).setAutoWidth(true);
    }

    private void openJob(String jobId) {
        try {
            long parsedJobId = Long.parseLong(jobId == null ? "" : jobId.trim());
            jobIdField.setInvalid(false);
            ui.navigate("plan/job/" + parsedJobId);
        } catch (NumberFormatException exception) {
            jobIdField.setInvalid(true);
            jobIdField.setErrorMessage(I18n.t("plan.ui.job.id.invalid"));
        }
    }

    private void loadDashboard() {
        statusPanel.setLoading(true);
        ContextAwareAsync.supplyAsync(this::fetchDashboardData)
                .whenComplete((data, failure) -> ui.access(() -> {
                    if (failure != null) {
                        log.error("Unable to load plan dashboard statistics", failure);
                        statusPanel.setError(I18n.t("plan.ui.dashboard.load.error"));
                        ui.push();
                        return;
                    }

                    totalJobsCard.setValue(String.valueOf(data.total()));
                    activeJobsCard.setValue(String.valueOf(data.active()));
                    succeededJobsCard.setValue(String.valueOf(data.succeeded()));
                    attentionJobsCard.setValue(String.valueOf(data.attention()));
                    statusPanel.setItems(List.of(
                            new DashboardBreakdownPanel.Item(I18n.t("plan.ui.status.created"), data.created()),
                            new DashboardBreakdownPanel.Item(I18n.t("plan.ui.status.processing"), data.processing()),
                            new DashboardBreakdownPanel.Item(I18n.t("plan.ui.status.stopping"), data.stopping()),
                            new DashboardBreakdownPanel.Item(I18n.t("plan.ui.status.succeeded"), data.succeeded()),
                            new DashboardBreakdownPanel.Item(I18n.t("plan.ui.status.infeasible"), data.infeasible()),
                            new DashboardBreakdownPanel.Item(I18n.t("plan.ui.status.failed"), data.failed()),
                            new DashboardBreakdownPanel.Item(I18n.t("plan.ui.status.cancelled"), data.cancelled())
                    ));
                    jobsGrid.setItems(data.recentJobs());
                    ui.push();
                }));
    }

    private DashboardData fetchDashboardData() {
        JobDashboardSummary summary = planFeignService.getDashboard(RECENT_JOB_LIMIT).getBody();
        long created = summary.created();
        long processing = summary.accepted() + summary.validating() + summary.normalizing() + summary.solving();
        return new DashboardData(
                summary.total(),
                created + processing + summary.stopping(),
                summary.succeeded(),
                summary.failed() + summary.infeasible(),
                created,
                processing,
                summary.stopping(),
                summary.infeasible(),
                summary.failed(),
                summary.cancelled(),
                summary.recentJobs());
    }

    private record DashboardData(
            long total,
            long active,
            long succeeded,
            long attention,
            long created,
            long processing,
            long stopping,
            long infeasible,
            long failed,
            long cancelled,
            List<PlanJobMinDto> recentJobs) {
    }

    private void openJobs() {
        ui.navigate("plan/jobs");
    }

    private void openLogs() {
        ui.navigate("plan/logs");
    }
}
