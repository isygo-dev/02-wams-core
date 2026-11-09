package eu.isygoit.ui.plan.views.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.*;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.i18n.I18n;
import eu.isygoit.plan.domain.JobStatus;
import eu.isygoit.remote.plan.PlanFeignService;
import eu.isygoit.ui.common.component.PageHeader;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.plan.layout.PlanMainLayout;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@SpringComponent
@RouteScope
@Route(value = "plan/job", layout = PlanMainLayout.class)
@PermitAll
public class PlanJobView extends ManagementVerticalView
        implements HasUrlParameter<String>, BeforeLeaveObserver {
    private static final int POLL_INTERVAL_MILLIS = 3000;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final PlanFeignService planFeignService;
    private final ObjectMapper objectMapper;
    private final UI ui;
    private final Span statusBadge = new Span();
    private final Span jobIdValue = new Span();
    private final Span submittedValue = new Span();
    private final Span startedValue = new Span();
    private final Span completedValue = new Span();
    private final Span messageValue = new Span();
    private final TextArea resultText = new TextArea();
    private UUID jobId;
    private Registration pollRegistration;

    @Autowired
    public PlanJobView(PlanFeignService planFeignService, ObjectMapper objectMapper) {
        this.planFeignService = planFeignService;
        this.objectMapper = objectMapper;
        this.ui = UI.getCurrent();
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("plan-job-view");
        add(new PageHeader("plan.ui.job.title", "plan.ui.job.subtitle"));
        add(buildSummary());
        add(buildActions());
        resultText.setLabel(I18n.t("plan.ui.result.title"));
        resultText.setReadOnly(true);
        resultText.setWidthFull();
        resultText.setMinHeight("24rem");
        resultText.addClassName("plan-job-result");
        add(resultText);
    }

    @Override
    public String getPageTitle() {
        return I18n.t("plan.ui.job.title");
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        try {
            jobId = UUID.fromString(parameter);
            jobIdValue.setText(jobId.toString());
            resultText.setValue("");
            startPolling();
            refreshStatus();
        } catch (IllegalArgumentException exception) {
            Notification.show(I18n.t("plan.ui.job.id.invalid"));
        }
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        stopPolling();
    }

    private Div buildSummary() {
        Div summary = new Div();
        summary.addClassName("plan-job-summary");
        summary.add(summaryRow(I18n.t("plan.ui.jobs.id"), jobIdValue));
        summary.add(summaryRow(I18n.t("plan.ui.jobs.status"), statusBadge));
        summary.add(summaryRow(I18n.t("plan.ui.jobs.submitted"), submittedValue));
        summary.add(summaryRow(I18n.t("plan.ui.jobs.started"), startedValue));
        summary.add(summaryRow(I18n.t("plan.ui.jobs.completed"), completedValue));
        summary.add(new H3(I18n.t("plan.ui.jobs.message")));
        summary.add(messageValue);
        return summary;
    }

    private HorizontalLayout summaryRow(String label, Span value) {
        Span key = new Span(label);
        key.addClassName("plan-job-summary__label");
        value.addClassName("plan-job-summary__value");
        HorizontalLayout row = new HorizontalLayout(key, value);
        row.setWidthFull();
        row.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        row.addClassName("plan-job-summary__row");
        return row;
    }

    private HorizontalLayout buildActions() {
        Button refresh = new Button(I18n.t("plan.ui.job.refresh"), VaadinIcon.REFRESH.create());
        refresh.addClickListener(event -> refreshStatus());
        Button loadResult = new Button(I18n.t("plan.ui.result.load"), VaadinIcon.FILE_TEXT.create());
        loadResult.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loadResult.addClickListener(event -> loadResult());
        Button logs = new Button(I18n.t("plan.ui.logs.open"), VaadinIcon.LIST.create());
        logs.addClickListener(event -> {
            if (jobId != null) {
                getUI().ifPresent(ui -> ui.navigate("plan/logs?jobId=" + jobId));
            }
        });
        HorizontalLayout actions = new HorizontalLayout(refresh, loadResult, logs);
        actions.addClassName("plan-job-actions");
        return actions;
    }

    private void refreshStatus() {
        if (jobId == null) {
            return;
        }
        try {
            JobStatus status = planFeignService.status(jobId);
            statusBadge.setText(status.status().meaning());
            statusBadge.addClassName("plan-job-status");
            for (IEnumJobStatusCode.Types candidate : IEnumJobStatusCode.Types.values()) {
                statusBadge.removeClassName("plan-job-status--" + candidate.name().toLowerCase(Locale.ROOT));
            }
            statusBadge.addClassName("plan-job-status--" + status.status().name().toLowerCase(Locale.ROOT));
            submittedValue.setText(format(status.submittedAt()));
            startedValue.setText(format(status.startedAt()));
            completedValue.setText(format(status.finishedAt()));
            messageValue.setText(status.message() == null ? "" : status.message());
            if (isTerminal(status.status())) {
                stopPolling();
                if (status.status() == IEnumJobStatusCode.Types.SUCCEEDED
                        && resultText.getValue().isBlank()) {
                    loadResult();
                }
            }
        } catch (RuntimeException exception) {
            log.error("Unable to load plan job status for {}", jobId, exception);
            Notification.show(I18n.t("plan.ui.job.load.error"));
        }
    }

    private void loadResult() {
        if (jobId == null) {
            return;
        }
        try {
            resultText.setValue(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(planFeignService.result(jobId)));
        } catch (JsonProcessingException exception) {
            log.error("Unable to format plan result for {}", jobId, exception);
            Notification.show(I18n.t("plan.ui.result.load.error"));
        } catch (RuntimeException exception) {
            log.error("Unable to load plan result for {}", jobId, exception);
            Notification.show(I18n.t("plan.ui.result.unavailable"));
        }
    }

    private String format(java.time.Instant instant) {
        return instant == null ? I18n.t("plan.ui.value.not.available") : DATE_FORMAT.format(instant);
    }

    private void startPolling() {
        if (pollRegistration == null) {
            pollRegistration = ui.addPollListener(event -> refreshStatus());
        }
        ui.setPollInterval(POLL_INTERVAL_MILLIS);
    }

    private void stopPolling() {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }
        ui.setPollInterval(-1);
    }

    private boolean isTerminal(IEnumJobStatusCode.Types status) {
        return status == IEnumJobStatusCode.Types.SUCCEEDED
                || status == IEnumJobStatusCode.Types.INFEASIBLE
                || status == IEnumJobStatusCode.Types.FAILED
                || status == IEnumJobStatusCode.Types.CANCELLED;
    }
}
