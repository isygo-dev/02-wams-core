package eu.isygoit.ui.plan.views.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.StreamResource;
import eu.isygoit.async.ContextAwareAsync;
import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.plan.PlanFeignService;
import eu.isygoit.ui.common.notification.AppNotifications;
import eu.isygoit.ui.plan.common.PlanErrors;
import eu.isygoit.ui.plan.views.job.dialog.CancelPlanJobDialog;
import eu.isygoit.ui.plan.views.job.dialog.DeletePlanJobDialog;
import eu.isygoit.ui.plan.views.job.dialog.PlanJobDetailsViewDialog;
import eu.isygoit.ui.plan.views.job.dialog.StopPlanJobDialog;
import eu.isygoit.ui.plan.views.job.dialog.PlanJobRequestDialog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The actions of a planning job, written once and used by both the jobs list ({@link PlanJobsView}, through its
 * cards and table rows) and the job page ({@link PlanJobView}): submit, edit request, clone, details, logs,
 * stop, cancel, export and delete.
 *
 * <p>Slow reads (loading a request, a job or an export) run off the UI thread through
 * {@link ContextAwareAsync} and come back through {@code ui.access}. Stop, cancel and delete are confirmed in
 * the dedicated {@link StopPlanJobDialog} (message only), {@link CancelPlanJobDialog} and {@link DeletePlanJobDialog} (9-digit code), whose action
 * runs inside the dialog like every other module; failures are mapped by {@link PlanErrors}.</p>
 */
@Slf4j
public class PlanJobActions {

    private final PlanFeignService service;
    private final ObjectMapper objectMapper;

    public PlanJobActions(PlanFeignService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    private static String label(PlanJobMinDto job) {
        return StringUtils.hasText(job.getCode()) ? job.getCode() : String.valueOf(job.getId());
    }

    /**
     * Runs a remote read off the UI thread, then hands its body to {@code onBody} on the UI thread; failures and
     * empty answers are reported with {@link AppNotifications}.
     */
    private <T> void load(Supplier<ResponseEntity<T>> call, String unavailableKey, Consumer<T> onBody) {
        UI ui = UI.getCurrent();
        ContextAwareAsync.supplyAsync(call).whenComplete((response, failure) -> ui.access(() -> {
            if (failure != null) {
                log.warn("Planning job call failed", failure);
                AppNotifications.error(PlanErrors.describe(failure));
            } else if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                AppNotifications.warning(I18n.t(unavailableKey));
            } else {
                onBody.accept(response.getBody());
            }
        }));
    }

    /**
     * Opens the empty submit dialog.
     */
    public void openSubmit(Runnable onDone) {
        PlanJobRequestDialog.create(service, objectMapper, onDone).open();
    }

    /**
     * Loads the stored request and opens it for editing (the job must still be CREATED).
     */
    public void openEdit(PlanJobMinDto job, Runnable onDone) {
        Long id = job.getId();
        load(() -> service.getRequest(id), "plan.ui.job.edit.unavailable",
                request -> PlanJobRequestDialog.edit(service, objectMapper, id, request, onDone).open());
    }

    /**
     * Loads the stored request and opens it prefilled in create mode (new client reference).
     */
    public void openClone(PlanJobMinDto job, Runnable onDone) {
        Long id = job.getId();
        load(() -> service.getRequest(id), "plan.ui.job.request.unavailable",
                request -> PlanJobRequestDialog.cloneOf(service, objectMapper, request, onDone).open());
    }

    /**
     * Loads the full job (request and result summaries are not part of the list payload) and shows it.
     */
    public void openDetails(PlanJobMinDto job, Runnable onDone) {
        Long id = job.getId();
        load(() -> service.findById(id), "plan.ui.job.load.error",
                full -> new PlanJobDetailsViewDialog(full, this, onDone).open());
    }

    /**
     * Opens the logs page filtered on the job.
     */
    public void openLogs(Long jobId) {
        UI.getCurrent().navigate("plan/logs", QueryParameters.simple(Map.of("jobId", String.valueOf(jobId))));
    }

    /**
     * Opens the full job page.
     */
    public void openJobPage(Long jobId) {
        UI.getCurrent().navigate("plan/job/" + jobId);
    }

    /**
     * Confirms (message only), then asks the running solver to stop.
     */
    public void confirmStop(PlanJobMinDto job, Runnable onDone, Consumer<Boolean> loading) {
        new StopPlanJobDialog(service, job, onDone, loading).open();
    }

    /**
     * Confirms with the 9-digit code, then cancels a job that has not started.
     */
    public void confirmCancel(PlanJobMinDto job, Runnable onDone, Consumer<Boolean> loading) {
        new CancelPlanJobDialog(service, job, onDone, loading).open();
    }

    /**
     * Confirms with the 9-digit code, then deletes a finished job record.
     */
    public void confirmDelete(PlanJobMinDto job, Runnable onDone, Consumer<Boolean> loading) {
        new DeletePlanJobDialog(service, job, onDone, loading).open();
    }

    /**
     * Downloads the result of a SUCCEEDED / INFEASIBLE job as a JSON file.
     */
    public void exportResult(PlanJobMinDto job) {
        Long id = job.getId();
        String fileName = "plan-job-" + label(job) + "-result.json";
        UI ui = UI.getCurrent();
        load(() -> service.exportResult(id, "json"), "plan.ui.result.unavailable",
                bytes -> download(ui, fileName, bytes));
    }

    private void download(UI ui, String fileName, byte[] content) {
        StreamResource resource = new StreamResource(fileName, () -> new ByteArrayInputStream(content));
        resource.setContentType("application/json");
        Anchor anchor = new Anchor(resource, fileName);
        anchor.getElement().setAttribute("download", true);
        anchor.getStyle().set("display", "none");
        ui.add(anchor);
        anchor.getElement().executeJs("this.click()").then(result -> anchor.getElement().removeFromParent());
    }
}
