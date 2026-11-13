package eu.isygoit.ui.plan.views.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.async.ContextAwareAsync;
import eu.isygoit.dto.data.LogEntry;
import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.plan.PlanFeignService;
import eu.isygoit.ui.common.component.FloatingRefreshButton;
import eu.isygoit.ui.common.component.PageHeader;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.common.notification.AppNotifications;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.plan.common.PlanEnumTag;
import eu.isygoit.ui.plan.common.PlanErrors;
import eu.isygoit.ui.plan.common.PlanFormats;
import eu.isygoit.ui.plan.common.PlanJsonEditor;
import eu.isygoit.ui.plan.common.PlanPush;
import eu.isygoit.ui.plan.layout.PlanMainLayout;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Execution logs of a planning job (route {@code plan/logs}): filtered by job id and minimum level, refreshed
 * with the shared floating refresh button. The job can be preset with {@code ?jobId=}.
 */
@Slf4j
@SpringComponent
@RouteScope
@Route(value = "plan/logs", layout = PlanMainLayout.class)
@PermitAll
public class PlanLogsView extends ManagementVerticalView implements AfterNavigationObserver {

    private static final int LOG_LIMIT = 200;

    private final PlanFeignService planFeignService;
    private final ObjectMapper objectMapper;
    private final TextField jobIdField = new TextField();
    private final ComboBox<IEnumLogLevel.Types> minLevelCombo = new ComboBox<>();
    private final Grid<LogEntry> logsGrid = new Grid<>(LogEntry.class, false);
    private final eu.isygoit.ui.common.component.GridPager<LogEntry> logsPager =
            new eu.isygoit.ui.common.component.GridPager<>(logsGrid);
    private final FloatingRefreshButton refreshButton = new FloatingRefreshButton(this::loadLogs);

    public PlanLogsView(PlanFeignService planFeignService, ObjectMapper objectMapper) {
        this.planFeignService = planFeignService;
        this.objectMapper = objectMapper;
        PlanPush.ensure(UI.getCurrent());
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("plan-logs-view");
        add(new PageHeader("plan.ui.logs.title", "plan.ui.logs.subtitle"));
        add(buildToolbar());
        configureGrid();
        add(logsGrid, refreshButton);
    }

    @Override
    public String getPageTitle() {
        return I18n.t("plan.ui.logs.title");
    }

    /**
     * Pre-fills and loads the logs when opened with a {@code ?jobId=} query parameter (the
     * {@code beforeEnter} hook is final in {@link ManagementVerticalView}, hence the after-navigation hook).
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        event.getLocation().getQueryParameters().getParameters().getOrDefault("jobId", List.of()).stream()
                .findFirst()
                .filter(value -> !value.isBlank())
                .ifPresent(value -> {
                    jobIdField.setValue(value.trim());
                    loadLogs();
                });
    }

    private HorizontalLayout buildToolbar() {
        jobIdField.setLabel(I18n.t("plan.ui.logs.job.id"));
        jobIdField.setPlaceholder(I18n.t("plan.ui.job.id.placeholder"));
        jobIdField.setClearButtonVisible(true);
        jobIdField.setWidthFull();
        jobIdField.addKeyDownListener(Key.ENTER, event -> loadLogs());

        minLevelCombo.setLabel(I18n.t("plan.ui.logs.min.level"));
        minLevelCombo.setItems(Arrays.asList(IEnumLogLevel.Types.values()));
        minLevelCombo.setItemLabelGenerator(PlanEnumTag::levelLabel);
        minLevelCombo.setClearButtonVisible(true);
        minLevelCombo.addValueChangeListener(event -> {
            if (event.isFromClient() && !jobIdField.isEmpty()) {
                loadLogs();
            }
        });

        HorizontalLayout toolbar = new HorizontalLayout(jobIdField, minLevelCombo);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.expand(jobIdField);
        return toolbar;
    }

    private void configureGrid() {
        logsGrid.setWidthFull();
        logsGrid.setHeight("calc(100vh - 24rem)");
        logsGrid.addColumn(entry -> PlanFormats.dateTimeSeconds(entry.eventTime()))
                .setHeader(I18n.t("plan.ui.logs.time"))
                .setAutoWidth(true);
        logsGrid.addComponentColumn(entry -> entry.level() == null ? new Span() : PlanEnumTag.ofLevel(entry.level()))
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
        Button details = new Button(VaadinIcon.EYE.create());
        details.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        details.addClassName("wams-action-btn");
        details.setTooltipText(I18n.t("plan.ui.logs.details"));
        details.setAriaLabel(I18n.t("plan.ui.logs.details"));
        details.addClickListener(event -> showDetails(entry));
        return details;
    }

    private void loadLogs() {
        Long jobId;
        try {
            jobId = Long.valueOf(jobIdField.getValue() == null ? "" : jobIdField.getValue().trim());
            jobIdField.setInvalid(false);
        } catch (NumberFormatException exception) {
            jobIdField.setInvalid(true);
            jobIdField.setErrorMessage(I18n.t("plan.ui.job.id.invalid"));
            return;
        }
        UI ui = getUI().orElseGet(UI::getCurrent);
        final Long requested = jobId;
        IEnumLogLevel.Types minLevel = minLevelCombo.getValue();
        refreshButton.setLoading(true);
        ContextAwareAsync.supplyAsync(() -> planFeignService.getLogs(requested, null, null, minLevel, LOG_LIMIT))
                .whenComplete((response, failure) -> ui.access(() -> {
                    refreshButton.setLoading(false);
                    if (failure != null) {
                        log.error("Unable to load planning job logs for {}", requested, failure);
                        AppNotifications.error(I18n.t("plan.ui.logs.load.error") + " " + PlanErrors.describe(failure));
                        return;
                    }
                    List<LogEntry> entries = response == null ? null : response.getBody();
                    logsPager.setItems(entries == null ? List.<LogEntry>of() : entries);
                }));
    }

    private void showDetails(LogEntry entry) {
        PlanJsonEditor details = new PlanJsonEditor(
                objectMapper, I18n.t("plan.ui.logs.details"), null, true);
        DialogLayout.tall(details);
        try {
            details.setValue(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entry));
        } catch (JsonProcessingException exception) {
            log.error("Unable to format planning log details {}", entry.eventId(), exception);
            AppNotifications.error(I18n.t("plan.ui.logs.load.error"));
            return;
        }
        NoActionDialog dialog = new NoActionDialog(I18n.t("plan.ui.logs.details"));
        DialogLayout.size(dialog, DialogLayout.WIDTH_L);
        dialog.add(details);
        dialog.open();
    }
}
