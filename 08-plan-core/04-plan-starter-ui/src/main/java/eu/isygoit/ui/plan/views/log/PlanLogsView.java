package eu.isygoit.ui.plan.views.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.i18n.I18n;
import eu.isygoit.plan.domain.LogEntry;
import eu.isygoit.remote.plan.PlanFeignService;
import eu.isygoit.ui.common.component.PageHeader;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.plan.layout.PlanMainLayout;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@SpringComponent
@RouteScope
@Route(value = "plan/logs", layout = PlanMainLayout.class)
@PermitAll
public class PlanLogsView extends ManagementVerticalView implements BeforeEnterObserver {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final int LOG_LIMIT = 200;

    private final PlanFeignService planFeignService;
    private final ObjectMapper objectMapper;
    private final TextField jobIdField = new TextField();
    private final Grid<LogEntry> logsGrid = new Grid<>(LogEntry.class, false);

    public PlanLogsView(PlanFeignService planFeignService, ObjectMapper objectMapper) {
        this.planFeignService = planFeignService;
        this.objectMapper = objectMapper;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("plan-logs-view");
        add(new PageHeader("plan.ui.logs.title", "plan.ui.logs.subtitle"));
        add(buildToolbar());
        configureGrid();
        add(logsGrid);
    }

    @Override
    public String getPageTitle() {
        return I18n.t("plan.ui.logs.title");
    }

    private HorizontalLayout buildToolbar() {
        jobIdField.setLabel(I18n.t("plan.ui.logs.job.id"));
        jobIdField.setPlaceholder(I18n.t("plan.ui.job.id.placeholder"));
        jobIdField.setClearButtonVisible(true);
        jobIdField.setWidthFull();
        Button refresh = new Button(I18n.t("plan.ui.logs.refresh"), VaadinIcon.REFRESH.create());
        refresh.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refresh.addClickListener(event -> loadLogs());
        HorizontalLayout toolbar = new HorizontalLayout(jobIdField, refresh);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.expand(jobIdField);
        return toolbar;
    }

    private void configureGrid() {
        logsGrid.setWidthFull();
        logsGrid.setHeight("calc(100vh - 18rem)");
        logsGrid.addColumn(entry -> DATE_FORMAT.format(entry.eventTime()))
                .setHeader(I18n.t("plan.ui.logs.time"))
                .setAutoWidth(true);
        logsGrid.addComponentColumn(entry -> levelBadge(entry.level().meaning(), entry.level().name()))
                .setHeader(I18n.t("plan.ui.logs.level"))
                .setAutoWidth(true);
        logsGrid.addColumn(LogEntry::step)
                .setHeader(I18n.t("plan.ui.logs.step"))
                .setAutoWidth(true);
        logsGrid.addColumn(LogEntry::engine)
                .setHeader(I18n.t("plan.ui.logs.engine"))
                .setAutoWidth(true);
        logsGrid.addColumn(LogEntry::message)
                .setHeader(I18n.t("plan.ui.logs.message"))
                .setFlexGrow(1);
        logsGrid.addColumn(LogEntry::durationMillis)
                .setHeader(I18n.t("plan.ui.logs.duration"))
                .setAutoWidth(true);
        logsGrid.addComponentColumn(this::detailsButton)
                .setHeader(I18n.t("plan.ui.jobs.actions"))
                .setAutoWidth(true);
    }

    private Button detailsButton(LogEntry entry) {
        Button details = new Button(I18n.t("plan.ui.logs.details"), VaadinIcon.EYE.create());
        details.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        details.addClickListener(event -> showDetails(entry));
        return details;
    }

    private Span levelBadge(String label, String level) {
        Span badge = new Span(label);
        badge.addClassName("plan-log-level");
        badge.addClassName("plan-log-level--" + level.toLowerCase(Locale.ROOT));
        return badge;
    }

    private void loadLogs() {
        UUID jobId;
        try {
            jobId = UUID.fromString(jobIdField.getValue() == null ? "" : jobIdField.getValue().trim());
            jobIdField.setInvalid(false);
        } catch (IllegalArgumentException exception) {
            jobIdField.setInvalid(true);
            jobIdField.setErrorMessage(I18n.t("plan.ui.job.id.invalid"));
            return;
        }
        try {
            logsGrid.setItems(planFeignService.logs(jobId, null, null, null, LOG_LIMIT));
        } catch (RuntimeException exception) {
            log.error("Unable to load planning job logs for {}", jobId, exception);
            Notification.show(I18n.t("plan.ui.logs.load.error"));
        }
    }

    private void showDetails(LogEntry entry) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t("plan.ui.logs.details"));
        TextArea details = new TextArea();
        details.setReadOnly(true);
        details.setWidth("min(60rem, 90vw)");
        details.setMinHeight("24rem");
        try {
            details.setValue(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entry));
        } catch (JsonProcessingException exception) {
            log.error("Unable to format planning log details {}", entry.eventId(), exception);
            Notification.show(I18n.t("plan.ui.logs.load.error"));
            return;
        }
        Button close = new Button(I18n.t("plan.ui.action.close"), event -> dialog.close());
        dialog.add(details, close);
        dialog.open();
    }
}
