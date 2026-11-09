package eu.isygoit.ui.plan.views.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.async.ContextAwareAsync;
import eu.isygoit.dto.data.PlanJobDto;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.plan.PlanFeignService;
import eu.isygoit.ui.common.component.FloatingRefreshButton;
import eu.isygoit.ui.common.component.PageHeader;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.common.notification.AppNotifications;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.plan.common.PlanErrors;
import eu.isygoit.ui.plan.common.PlanJsonEditor;
import eu.isygoit.ui.plan.common.PlanJobStatusRules;
import eu.isygoit.ui.plan.common.PlanPush;
import eu.isygoit.ui.plan.layout.PlanMainLayout;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Full page of one planning job (route {@code plan/job/:id}): the same information as the details dialog
 * (shared {@link PlanJobInfo} grids), the status-gated actions of the cards ({@link PlanJobActions} and
 * {@link PlanJobStatusRules}) and the raw JSON result. The refresh action is the shared floating button.
 *
 * <p><b>Polling.</b> While the job is not finished the page polls its status every few seconds. The UI-wide poll
 * interval is shared by the whole UI, so this view sets it only while it is attached and the job is running,
 * removes its own poll listener and resets the interval on detach (and only when it was the one that set it),
 * so a stale instance can never leave polling switched on. The contract has no lighter status call, so the
 * poll reads the job through {@code findById}.</p>
 */
@Slf4j
@SpringComponent
@RouteScope
@Route(value = "plan/job", layout = PlanMainLayout.class)
@PermitAll
public class PlanJobView extends ManagementVerticalView implements HasUrlParameter<String> {

    private static final int POLL_INTERVAL_MILLIS = 3000;

    private final PlanFeignService service;
    private final ObjectMapper objectMapper;
    private final PlanJobActions actions;
    private final FloatingRefreshButton refreshButton = new FloatingRefreshButton(this::refreshStatus);
    private final Div infoHolder = new Div();
    private final HorizontalLayout actionsBar = new HorizontalLayout();
    private final PlanJsonEditor resultText;
    private final AtomicBoolean loading = new AtomicBoolean();
    private Long jobId;
    private PlanJobDto current;
    private Registration pollRegistration;
    private UI pollUi;

    @Autowired
    public PlanJobView(PlanFeignService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
        this.actions = new PlanJobActions(service, objectMapper);
        this.resultText = new PlanJsonEditor(objectMapper, I18n.t("plan.ui.result.title"), null, true);
        PlanPush.ensure(UI.getCurrent());

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("plan-job-view");
        add(new PageHeader("plan.ui.job.title", "plan.ui.job.subtitle"));

        infoHolder.setWidthFull();
        actionsBar.addClassName("plan-job-actions");
        actionsBar.setWidthFull();

        DialogLayout.tall(resultText);

        add(infoHolder, actionsBar, resultText, refreshButton);
    }

    @Override
    public String getPageTitle() {
        return I18n.t("plan.ui.job.title");
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        Long parsed;
        try {
            parsed = Long.valueOf(parameter == null ? "" : parameter.trim());
        } catch (NumberFormatException exception) {
            AppNotifications.error(I18n.t("plan.ui.job.id.invalid"));
            return;
        }
        if (!parsed.equals(jobId)) {
            stopPolling();
            loading.set(false);
            current = null;
            resultText.setValue("");
            infoHolder.removeAll();
            actionsBar.removeAll();
        }
        jobId = parsed;
        refreshStatus();
    }

    // ══════════════════════════════════════════════════════════════
    //  Loading
    // ══════════════════════════════════════════════════════════════

    private void refreshStatus() {
        if (jobId == null || !loading.compareAndSet(false, true)) {
            return;
        }
        UI ui = getUI().orElseGet(UI::getCurrent);
        Long requested = jobId;
        refreshButton.setLoading(true);
        ContextAwareAsync.supplyAsync(() -> service.findById(requested)).whenComplete((response, failure) -> ui.access(() -> {
            loading.set(false);
            refreshButton.setLoading(false);
            if (!requested.equals(jobId)) {
                return;
            }
            if (failure != null) {
                log.error("Unable to load plan job {}", requested, failure);
                AppNotifications.error(I18n.t("plan.ui.job.load.error") + " " + PlanErrors.describe(failure));
                stopPolling();
            } else if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                AppNotifications.error(I18n.t("plan.ui.job.load.error"));
                stopPolling();
            } else {
                apply(response.getBody());
            }
        }));
    }

    private void apply(PlanJobDto job) {
        current = job;
        infoHolder.removeAll();
        infoHolder.add(buildInfo(job));
        rebuildActions(job);

        if (PlanJobStatusRules.isTerminal(job.getStatus())) {
            stopPolling();
            if (job.getResult() != null && resultText.getValue().isBlank()) {
                showResult(job.getResult());
            }
        } else {
            startPolling();
        }
    }

    private Div buildInfo(PlanJobDto job) {
        Div holder = new Div();
        VerticalLayout stack = DialogLayout.stack();
        VerticalLayout identity = DialogLayout.section(I18n.t("plan.ui.job.details.section.identity"), VaadinIcon.INFO_CIRCLE);
        identity.add(PlanJobInfo.identity(job));
        VerticalLayout timing = DialogLayout.section(I18n.t("plan.ui.job.details.section.timing"), VaadinIcon.CLOCK);
        timing.add(PlanJobInfo.timing(job));
        stack.add(identity, timing);
        Div failure = PlanJobInfo.failure(job);
        if (failure != null) {
            VerticalLayout error = DialogLayout.section(I18n.t("plan.ui.job.details.section.error"), VaadinIcon.WARNING);
            error.add(failure);
            stack.add(error);
        }
        holder.add(stack);
        return holder;
    }

    private void showResult(Object result) {
        try {
            resultText.setValue(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
        } catch (JsonProcessingException exception) {
            log.error("Unable to format plan result for {}", jobId, exception);
            AppNotifications.error(I18n.t("plan.ui.result.load.error"));
        }
    }

    private void loadResult() {
        if (jobId == null) {
            return;
        }
        UI ui = getUI().orElseGet(UI::getCurrent);
        Long requested = jobId;
        ContextAwareAsync.supplyAsync(() -> service.getResult(requested)).whenComplete((response, failure) -> ui.access(() -> {
            if (failure != null) {
                log.error("Unable to load plan result for {}", requested, failure);
                AppNotifications.warning(I18n.t("plan.ui.result.unavailable"));
            } else if (response == null || response.getBody() == null) {
                AppNotifications.warning(I18n.t("plan.ui.result.unavailable"));
            } else {
                showResult(response.getBody());
            }
        }));
    }

    // ══════════════════════════════════════════════════════════════
    //  Actions (same gating as the cards)
    // ══════════════════════════════════════════════════════════════

    private void rebuildActions(PlanJobDto job) {
        actionsBar.removeAll();
        IEnumJobStatusCode.Types status = job.getStatus();

        if (PlanJobStatusRules.isExportable(status)) {
            Button result = button("plan.ui.result.load", VaadinIcon.FILE_TEXT, this::loadResult);
            result.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            actionsBar.add(result);
        }
        if (PlanJobStatusRules.isEditable(status)) {
            actionsBar.add(button("plan.ui.action.edit", VaadinIcon.EDIT, () -> actions.openEdit(job, this::refreshStatus)));
        }
        actionsBar.add(button("plan.ui.action.clone", VaadinIcon.COPY, () -> actions.openClone(job, this::openList)));
        actionsBar.add(button("plan.ui.logs.open", VaadinIcon.FILE_TEXT_O, () -> actions.openLogs(job.getId())));
        if (PlanJobStatusRules.isStoppable(status)) {
            actionsBar.add(button("plan.ui.action.stop", VaadinIcon.STOP,
                    () -> actions.confirmStop(job, this::refreshStatus, null)));
        }
        if (PlanJobStatusRules.isCancellable(status)) {
            actionsBar.add(button("plan.ui.action.cancel", VaadinIcon.CLOSE_CIRCLE_O,
                    () -> actions.confirmCancel(job, this::refreshStatus, null)));
        }
        if (PlanJobStatusRules.isExportable(status)) {
            actionsBar.add(button("plan.ui.action.export", VaadinIcon.DOWNLOAD, () -> actions.exportResult(job)));
        }
        if (PlanJobStatusRules.isDeletable(status)) {
            Button delete = button("plan.ui.action.delete", VaadinIcon.TRASH,
                    () -> actions.confirmDelete(job, this::openList, null));
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
            actionsBar.add(delete);
        }
    }

    private Button button(String labelKey, VaadinIcon icon, Runnable onClick) {
        Button button = new Button(I18n.t(labelKey), icon.create());
        button.addClickListener(event -> onClick.run());
        return button;
    }

    private void openList() {
        getUI().ifPresent(ui -> ui.navigate("plan/jobs"));
    }

    // ══════════════════════════════════════════════════════════════
    //  Polling (scoped to this view)
    // ══════════════════════════════════════════════════════════════

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (current != null && !PlanJobStatusRules.isTerminal(current.getStatus())) {
            startPolling();
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopPolling();
        super.onDetach(detachEvent);
    }

    private void startPolling() {
        if (pollRegistration != null) {
            return;
        }
        UI ui = getUI().orElse(null);
        if (ui == null) {
            return;   // not attached yet: onAttach starts it
        }
        pollUi = ui;
        pollRegistration = ui.addPollListener(event -> {
            if (isAttached()) {
                refreshStatus();
            }
        });
        ui.setPollInterval(POLL_INTERVAL_MILLIS);
    }

    private void stopPolling() {
        if (pollRegistration == null) {
            return;
        }
        pollRegistration.remove();
        pollRegistration = null;
        if (pollUi != null) {
            pollUi.setPollInterval(-1);
            pollUi = null;
        }
    }
}
