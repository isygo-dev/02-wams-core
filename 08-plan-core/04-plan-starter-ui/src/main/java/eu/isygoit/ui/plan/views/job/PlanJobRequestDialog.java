package eu.isygoit.ui.plan.views.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import eu.isygoit.i18n.I18n;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.remote.plan.PlanFeignService;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class PlanJobRequestDialog extends Dialog {
    private final PlanFeignService planFeignService;
    private final ObjectMapper objectMapper;
    private final UUID jobId;
    private final boolean editing;
    private final Runnable afterSave;
    private final TextArea requestJson = new TextArea();

    public PlanJobRequestDialog(
            PlanFeignService planFeignService,
            ObjectMapper objectMapper,
            UUID jobId,
            PlanningRequest request,
            Runnable afterSave) {
        this.planFeignService = planFeignService;
        this.objectMapper = objectMapper;
        this.jobId = jobId;
        this.editing = jobId != null;
        this.afterSave = afterSave;
        setHeaderTitle(I18n.t(editing ? "plan.ui.job.edit.title" : "plan.ui.job.add.title"));
        setWidth("min(64rem, 95vw)");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);

        requestJson.setLabel(I18n.t("plan.ui.job.request.json"));
        requestJson.setHelperText(I18n.t("plan.ui.job.request.help"));
        requestJson.setWidthFull();
        requestJson.setMinHeight("28rem");
        requestJson.setRequired(true);
        if (request != null) {
            try {
                requestJson.setValue(objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(request));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Could not format the planning request", exception);
            }
        }

        Button cancel = new Button(I18n.t("plan.ui.action.cancel"), event -> close());
        Button save = new Button(I18n.t(editing ? "plan.ui.action.save" : "plan.ui.action.submit"),
                event -> saveRequest());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout actions = new HorizontalLayout(cancel, save);
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        add(requestJson, actions);
    }

    private void saveRequest() {
        PlanningRequest request;
        try {
            request = objectMapper.readValue(requestJson.getValue(), PlanningRequest.class);
            if (request == null || request.meta() == null) {
                Notification.show(I18n.t("plan.ui.job.request.invalid"));
                return;
            }
        } catch (JsonProcessingException exception) {
            Notification.show(I18n.t("plan.ui.job.request.invalid"));
            return;
        }

        try {
            if (editing) {
                var response = planFeignService.update(jobId, request);
                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    Notification.show(I18n.t("plan.ui.job.save.error"));
                    return;
                }
            } else {
                var response = planFeignService.submit(request);
                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    Notification.show(I18n.t("plan.ui.job.save.error"));
                    return;
                }
            }
            close();
            Notification.show(I18n.t(editing ? "plan.ui.job.update.success" : "plan.ui.job.submit.success"));
            afterSave.run();
        } catch (RuntimeException exception) {
            log.error("Unable to {} planning job request",
                    editing ? "update" : "submit", exception);
            Notification.show(I18n.t("plan.ui.job.save.error"));
        }
    }
}
