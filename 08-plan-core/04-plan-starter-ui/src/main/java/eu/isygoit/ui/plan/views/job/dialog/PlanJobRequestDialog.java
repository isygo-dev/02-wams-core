package eu.isygoit.ui.plan.views.job.dialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.request.PlanningMeta;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.response.ValidationReportDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.plan.PlanFeignService;
import eu.isygoit.ui.common.dialog.AbstractEditionDialog;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.plan.common.PlanErrors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Create / edit / clone dialog of a planning request, built on the shared {@link AbstractEditionDialog}
 * (title, Cancel and primary Save footer, error line, success notification).
 *
 * <p>The request is edited as JSON for now (a guided editor comes later). The optional client reference is a
 * separate field bound into {@code meta.clientRef}: when filled it wins over the value written in the JSON.
 * Before anything is stored the request is validated through {@link PlanFeignService#validate}; the issues
 * (code, message, path) are listed inline in the dialog and nothing is submitted until there are none.</p>
 *
 * <ul>
 *   <li>{@link #create}: empty form, submits a new job;</li>
 *   <li>{@link #edit}: replaces the request of a job still in CREATED status (client reference read-only);</li>
 *   <li>{@link #cloneOf}: the request of an existing job prefilled in create mode, with a fresh client reference.</li>
 * </ul>
 */
@Slf4j
public class PlanJobRequestDialog extends AbstractEditionDialog {

    private final PlanFeignService service;
    private final ObjectMapper objectMapper;
    private final Long jobId;
    private final TextField clientRefField = new TextField();
    private final TextArea requestJson = new TextArea();
    private final VerticalLayout issuesSection;
    private final Div issuesList = new Div();

    private PlanJobRequestDialog(String title,
                                 PlanFeignService service,
                                 ObjectMapper objectMapper,
                                 Long jobId,
                                 PlanningRequest request,
                                 String clientRef,
                                 Runnable onSuccess) {
        super(title, onSuccess);
        this.service = service;
        this.objectMapper = objectMapper;
        this.jobId = jobId;
        boolean editing = jobId != null;

        DialogLayout.size(this, DialogLayout.WIDTH_L);
        setCloseOnOutsideClick(false);
        setOkButtonText(I18n.t(editing ? "plan.ui.action.save" : "plan.ui.action.submit"));

        clientRefField.setLabel(I18n.t("plan.ui.job.client.ref"));
        clientRefField.setHelperText(I18n.t("plan.ui.job.client.ref.help"));
        clientRefField.setMaxLength(PlanningMeta.CLIENT_REF_MAX_LENGTH);
        clientRefField.setClearButtonVisible(!editing);
        clientRefField.setReadOnly(editing);
        clientRefField.setWidthFull();
        clientRefField.setValue(clientRef == null ? "" : clientRef);

        requestJson.setLabel(I18n.t("plan.ui.job.request.json"));
        requestJson.setHelperText(I18n.t("plan.ui.job.request.help"));
        requestJson.setRequired(true);
        requestJson.addClassName("plan-json-editor");
        DialogLayout.tall(requestJson);
        if (request != null) {
            requestJson.setValue(format(withClientRef(request, null)));
        }

        issuesSection = DialogLayout.section(I18n.t("plan.ui.job.validation.title"), VaadinIcon.WARNING);
        issuesList.addClassName("plan-issues");
        issuesSection.add(issuesList);
        issuesSection.setVisible(false);

        VerticalLayout stack = DialogLayout.stack();
        stack.add(clientRefField, requestJson, issuesSection);
        addContent(stack);
    }

    /**
     * Empty form: submits a new job.
     */
    public static PlanJobRequestDialog create(PlanFeignService service, ObjectMapper objectMapper, Runnable onSuccess) {
        return new PlanJobRequestDialog(I18n.t("plan.ui.job.add.title"), service, objectMapper, null, null, null,
                onSuccess);
    }

    /**
     * Replaces the request of a job that is still CREATED.
     */
    public static PlanJobRequestDialog edit(PlanFeignService service, ObjectMapper objectMapper, Long jobId,
                                            PlanningRequest request, Runnable onSuccess) {
        return new PlanJobRequestDialog(I18n.t("plan.ui.job.edit.title"), service, objectMapper, jobId, request,
                clientRefOf(request), onSuccess);
    }

    /**
     * Prefilled create form: the request of {@code source} resubmitted under a new client reference
     * (the old one would make the service return the existing job instead of creating a new one).
     */
    public static PlanJobRequestDialog cloneOf(PlanFeignService service, ObjectMapper objectMapper,
                                             PlanningRequest source, Runnable onSuccess) {
        return new PlanJobRequestDialog(I18n.t("plan.ui.job.clone.title"), service, objectMapper, null, source,
                cloneRef(clientRefOf(source)), onSuccess);
    }

    private static String clientRefOf(PlanningRequest request) {
        return request == null || request.meta() == null ? null : request.meta().clientRef();
    }

    private static String cloneRef(String original) {
        if (!StringUtils.hasText(original)) {
            return null;
        }
        String suffix = "-copy";
        String base = original.length() + suffix.length() > PlanningMeta.CLIENT_REF_MAX_LENGTH
                ? original.substring(0, PlanningMeta.CLIENT_REF_MAX_LENGTH - suffix.length())
                : original;
        return base + suffix;
    }

    /**
     * The request with {@code meta.clientRef} set to {@code clientRef} (possibly {@code null}, which keeps the
     * reference out of the JSON text since the dedicated field owns it).
     */
    private static PlanningRequest withClientRef(PlanningRequest request, String clientRef) {
        PlanningMeta meta = request.meta();
        if (meta == null && clientRef == null) {
            return request;
        }
        PlanningMeta updated = new PlanningMeta(clientRef,
                meta == null ? null : meta.version(),
                meta == null ? null : meta.domainName(),
                meta == null ? null : meta.locale(),
                meta == null ? null : meta.timezone());
        return new PlanningRequest(updated, request.domain(), request.horizon(), request.slots(),
                request.calendars(), request.setupMatrices(), request.resources(), request.tasks(),
                request.constraints(), request.objectives(), request.solverOptions(), request.initialSolution());
    }

    private String format(PlanningRequest request) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Could not format the planning request", e);
            return "";
        }
    }

    /**
     * Parses the JSON and applies the client reference field; {@code null} (with the field flagged) when the
     * text is not a valid planning request.
     */
    private PlanningRequest readRequest() {
        String json = requestJson.getValue();
        if (!StringUtils.hasText(json)) {
            requestJson.setInvalid(true);
            requestJson.setErrorMessage(I18n.t("plan.ui.job.request.invalid"));
            return null;
        }
        PlanningRequest request;
        try {
            request = objectMapper.readValue(json, PlanningRequest.class);
        } catch (JsonProcessingException e) {
            requestJson.setInvalid(true);
            requestJson.setErrorMessage(e.getLocation() == null
                    ? I18n.t("plan.ui.job.request.invalid")
                    : I18n.t("plan.ui.job.request.invalid.at", e.getLocation().getLineNr(),
                    e.getLocation().getColumnNr()));
            return null;
        }
        if (request == null) {
            requestJson.setInvalid(true);
            requestJson.setErrorMessage(I18n.t("plan.ui.job.request.invalid"));
            return null;
        }
        requestJson.setInvalid(false);

        String typed = clientRefField.getValue() == null ? "" : clientRefField.getValue().trim();
        String fromJson = clientRefOf(request);
        String effective = typed.isEmpty() ? fromJson : typed;
        try {
            return effective == null && request.meta() == null ? request : withClientRef(request, effective);
        } catch (IllegalArgumentException e) {
            clientRefField.setInvalid(true);
            clientRefField.setErrorMessage(e.getMessage());
            return null;
        }
    }

    @Override
    protected boolean onOk() {
        clearIssues();
        clientRefField.setInvalid(false);
        PlanningRequest request = readRequest();
        if (request == null) {
            append(I18n.t("plan.ui.job.request.invalid"));
            return false;
        }

        try {
            var validation = service.validate(request);
            ValidationReportDto report = validation == null ? null : validation.getBody();
            if (report != null && !report.isValid()) {
                List<ValidationReportDto.Issue> errors = report.getErrors() == null ? List.of() : report.getErrors();
                showIssues(errors);
                append(I18n.t("plan.ui.job.validation.failed", errors.size()));
                return false;
            }

            var response = jobId != null ? service.updateRequest(jobId, request) : service.submit(request);
            if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("plan.ui.job.save.error"));
                return false;
            }
            append(I18n.t(jobId != null ? "plan.ui.job.update.success" : "plan.ui.job.submit.success"));
            return true;
        } catch (Exception e) {
            log.error("Unable to {} planning job request", jobId != null ? "update" : "submit", e);
            append(PlanErrors.describe(e));
            return false;
        }
    }

    private void clearIssues() {
        issuesList.removeAll();
        issuesSection.setVisible(false);
    }

    private void showIssues(List<ValidationReportDto.Issue> issues) {
        issuesList.removeAll();
        for (ValidationReportDto.Issue issue : issues) {
            Div row = new Div();
            row.addClassName(DialogLayout.CLASS_ROW);
            Span code = new Span(issue.getCode() == null ? "-" : issue.getCode());
            code.addClassName(DialogLayout.CLASS_CHIP);
            Span message = new Span(issue.getMessage() == null ? "" : " " + issue.getMessage());
            row.add(code, message);
            if (StringUtils.hasText(issue.getPath())) {
                row.add(DialogLayout.help(" " + issue.getPath()));
            }
            issuesList.add(row);
        }
        issuesSection.setVisible(!issues.isEmpty());
    }
}
