package eu.isygoit.ui.plan.views.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.i18n.I18n;
import eu.isygoit.plan.api.JobDashboardSummary;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.domain.JobStatus;
import eu.isygoit.remote.plan.PlanFeignService;
import eu.isygoit.ui.common.component.PageHeader;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.plan.layout.PlanMainLayout;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@SpringComponent
@RouteScope
@Route(value = "plan/jobs", layout = PlanMainLayout.class)
@PermitAll
public class PlanJobsView extends ManagementVerticalView {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final int JOB_LIMIT = 500;

    private final PlanFeignService planFeignService;
    private final ObjectMapper objectMapper;
    private final Grid<JobStatus> jobsGrid = new Grid<>(JobStatus.class, false);
    private final Div cards = new Div();
    private final TextField searchField = new TextField();
    private final ComboBox<IEnumJobStatusCode.Types> statusFilter = new ComboBox<>();
    private final Button modeButton = new Button();
    private List<JobStatus> jobs = List.of();
    private boolean cardMode;

    public PlanJobsView(PlanFeignService planFeignService, ObjectMapper objectMapper) {
        this.planFeignService = planFeignService;
        this.objectMapper = objectMapper;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("plan-jobs-view");
        add(new PageHeader("plan.ui.jobs.title", "plan.ui.jobs.subtitle"));
        add(buildToolbar());
        configureGrid();
        cards.addClassName("plan-job-cards");
        cards.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(20rem, 1fr))")
                .set("gap", "var(--lumo-space-m)")
                .set("width", "100%");
        add(jobsGrid, cards);
        setCardMode(false);
        loadJobs();
    }

    @Override
    public String getPageTitle() {
        return I18n.t("plan.ui.jobs.title");
    }

    private HorizontalLayout buildToolbar() {
        searchField.setPlaceholder(I18n.t("plan.ui.jobs.search"));
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidthFull();
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(event -> applyFilters());

        statusFilter.setPlaceholder(I18n.t("plan.ui.jobs.status.filter"));
        statusFilter.setItems(Arrays.asList(IEnumJobStatusCode.Types.values()));
        statusFilter.setItemLabelGenerator(IEnumJobStatusCode.Types::meaning);
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(event -> applyFilters());

        Button addJob = new Button(I18n.t("plan.ui.job.add"), VaadinIcon.PLUS.create());
        addJob.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addJob.addClickListener(event -> new PlanJobRequestDialog(
                planFeignService, objectMapper, null, null, this::loadJobs).open());
        modeButton.setIcon(VaadinIcon.LIST.create());
        modeButton.addClickListener(event -> setCardMode(!cardMode));
        Button refresh = new Button(I18n.t("plan.ui.dashboard.refresh"), VaadinIcon.REFRESH.create());
        refresh.addClickListener(event -> loadJobs());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, statusFilter, modeButton, refresh, addJob);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.expand(searchField);
        toolbar.addClassName("plan-jobs-toolbar");
        return toolbar;
    }

    private void configureGrid() {
        jobsGrid.setWidthFull();
        jobsGrid.setHeight("calc(100vh - 18rem)");
        jobsGrid.addColumn(status -> status.jobId().toString())
                .setHeader(I18n.t("plan.ui.jobs.id"))
                .setAutoWidth(true)
                .setFlexGrow(1);
        jobsGrid.addComponentColumn(status -> statusBadge(status.status()))
                .setHeader(I18n.t("plan.ui.jobs.status"))
                .setAutoWidth(true);
        jobsGrid.addColumn(status -> DATE_FORMAT.format(status.submittedAt()))
                .setHeader(I18n.t("plan.ui.jobs.submitted"))
                .setAutoWidth(true);
        jobsGrid.addColumn(status -> status.message() == null ? "" : status.message())
                .setHeader(I18n.t("plan.ui.jobs.message"))
                .setFlexGrow(2);
        jobsGrid.addComponentColumn(this::buildActions)
                .setHeader(I18n.t("plan.ui.jobs.actions"))
                .setAutoWidth(true);
    }

    private HorizontalLayout buildActions(JobStatus status) {
        Button view = new Button(I18n.t("plan.ui.action.view"), VaadinIcon.EYE.create());
        view.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        view.addClickListener(event -> navigateToJob(status.jobId()));
        HorizontalLayout actions = new HorizontalLayout(view);
        actions.setSpacing(false);
        if (status.status() == IEnumJobStatusCode.Types.CREATED) {
            Button edit = new Button(I18n.t("plan.ui.action.edit"), VaadinIcon.EDIT.create());
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            edit.addClickListener(event -> editJob(status.jobId()));
            actions.add(edit, actionButton(I18n.t("plan.ui.action.cancel"),
                    VaadinIcon.CLOSE, () -> confirmAction(
                            I18n.t("plan.ui.job.cancel.confirm"), () -> cancelJob(status.jobId()))));
        } else if (isActive(status.status())) {
            actions.add(actionButton(I18n.t("plan.ui.action.stop"),
                    VaadinIcon.STOP, () -> confirmAction(
                            I18n.t("plan.ui.job.stop.confirm"), () -> stopJob(status.jobId()))));
        }
        if (isTerminal(status.status())) {
            actions.add(actionButton(I18n.t("plan.ui.action.delete"),
                    VaadinIcon.TRASH, () -> confirmAction(
                            I18n.t("plan.ui.job.delete.confirm"), () -> deleteJob(status.jobId()))));
        }
        return actions;
    }

    private Button actionButton(String label, VaadinIcon icon, Runnable action) {
        Button button = new Button(label, icon.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        button.addClickListener(event -> action.run());
        return button;
    }

    private void loadJobs() {
        try {
            JobDashboardSummary summary = planFeignService.dashboardSummary(JOB_LIMIT);
            jobs = summary.recentJobs();
            applyFilters();
        } catch (RuntimeException exception) {
            log.error("Unable to load planning jobs", exception);
            Notification.show(I18n.t("plan.ui.jobs.load.error"));
        }
    }

    private void applyFilters() {
        String query = searchField.getValue() == null
                ? "" : searchField.getValue().trim().toLowerCase(Locale.ROOT);
        IEnumJobStatusCode.Types selectedStatus = statusFilter.getValue();
        List<JobStatus> filtered = jobs.stream()
                .filter(job -> selectedStatus == null || job.status() == selectedStatus)
                .filter(job -> query.isBlank()
                        || job.jobId().toString().toLowerCase(Locale.ROOT).contains(query)
                        || (job.message() != null
                        && job.message().toLowerCase(Locale.ROOT).contains(query)))
                .toList();
        jobsGrid.setItems(filtered);
        cards.removeAll();
        filtered.forEach(job -> cards.add(buildCard(job)));
    }

    private VerticalLayout buildCard(JobStatus job) {
        H3 id = new H3(job.jobId().toString());
        id.addClassName("plan-job-card__id");
        HorizontalLayout heading = new HorizontalLayout(id, statusBadge(job.status()));
        heading.setWidthFull();
        heading.setAlignItems(FlexComponent.Alignment.CENTER);
        heading.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span submitted = new Span(I18n.t("plan.ui.jobs.submitted") + ": "
                + DATE_FORMAT.format(job.submittedAt()));
        Span message = new Span(job.message() == null ? "" : job.message());
        message.addClassName("plan-job-card__message");
        VerticalLayout card = new VerticalLayout(heading, submitted, message, buildActions(job));
        card.setPadding(true);
        card.setSpacing(true);
        card.addClassName("plan-job-card");
        return card;
    }

    private Span statusBadge(IEnumJobStatusCode.Types status) {
        Span badge = new Span(status.meaning());
        badge.addClassName("plan-job-status");
        badge.addClassName("plan-job-status--" + status.name().toLowerCase(Locale.ROOT));
        return badge;
    }

    private void setCardMode(boolean enabled) {
        cardMode = enabled;
        jobsGrid.setVisible(!enabled);
        cards.setVisible(enabled);
        modeButton.setText(I18n.t(enabled ? "plan.ui.jobs.view.grid" : "plan.ui.jobs.view.cards"));
    }

    private void editJob(UUID jobId) {
        try {
            var response = planFeignService.request(jobId);
            PlanningRequest request = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || request == null) {
                Notification.show(I18n.t("plan.ui.job.edit.unavailable"));
                return;
            }
            new PlanJobRequestDialog(planFeignService, objectMapper, jobId, request, this::loadJobs).open();
        } catch (RuntimeException exception) {
            log.error("Unable to load planning job request {}", jobId, exception);
            Notification.show(I18n.t("plan.ui.job.edit.unavailable"));
        }
    }

    private void cancelJob(UUID jobId) {
        performAction(jobId, () -> planFeignService.cancel(jobId), "plan.ui.job.cancel.success");
    }

    private void stopJob(UUID jobId) {
        performAction(jobId, () -> planFeignService.stop(jobId), "plan.ui.job.stop.success");
    }

    private void deleteJob(UUID jobId) {
        performAction(jobId, () -> planFeignService.delete(jobId), "plan.ui.job.delete.success");
    }

    private void performAction(
            UUID jobId,
            java.util.function.Supplier<org.springframework.http.ResponseEntity<Void>> action,
            String successMessage) {
        try {
            var response = action.get();
            if (!response.getStatusCode().is2xxSuccessful()) {
                Notification.show(I18n.t("plan.ui.job.action.conflict"));
                return;
            }
            Notification.show(I18n.t(successMessage));
            loadJobs();
        } catch (RuntimeException exception) {
            log.error("Unable to perform planning job action for {}", jobId, exception);
            Notification.show(I18n.t("plan.ui.job.action.error"));
        }
    }

    private void confirmAction(String message, Runnable action) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t("plan.ui.action.confirm.title"));
        Button cancel = new Button(I18n.t("plan.ui.action.cancel"), event -> dialog.close());
        Button confirm = new Button(I18n.t("plan.ui.action.confirm"), event -> {
            dialog.close();
            action.run();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout buttons = new HorizontalLayout(cancel, confirm);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        dialog.add(new Span(message), buttons);
        dialog.open();
    }

    private void navigateToJob(UUID jobId) {
        getUI().ifPresent(ui -> ui.navigate("plan/job/" + jobId));
    }

    private boolean isActive(IEnumJobStatusCode.Types status) {
        return status == IEnumJobStatusCode.Types.NORMALIZING
                || status == IEnumJobStatusCode.Types.SOLVING;
    }

    private boolean isTerminal(IEnumJobStatusCode.Types status) {
        return status == IEnumJobStatusCode.Types.SUCCEEDED
                || status == IEnumJobStatusCode.Types.INFEASIBLE
                || status == IEnumJobStatusCode.Types.FAILED
                || status == IEnumJobStatusCode.Types.CANCELLED;
    }
}
