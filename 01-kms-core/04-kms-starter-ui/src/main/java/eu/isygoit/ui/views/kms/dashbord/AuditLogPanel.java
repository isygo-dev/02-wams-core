package eu.isygoit.ui.views.kms.dashbord;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import eu.isygoit.dto.KmsDtos.AuditLogResponse;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.remote.kms.KmsApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuditLogPanel extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(AuditLogPanel.class);
    private static final int PAGE_SIZE = 20;

    private final KmsApiService kmsApiService;
    private final UI ui;
    private final ProgressBar loadingBar = new ProgressBar();
    private ComboBox<KeyUsageStatsPanel.KeyOption> keyCombo;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private Button loadButton;
    private Grid<AuditLogResponse.LogEntry> grid;
    private HorizontalLayout paginationBar;
    private Button prevButton;
    private Button nextButton;
    private Span pageInfoSpan;
    private List<AuditLogResponse.LogEntry> allLogs = new ArrayList<>();
    private int currentPage = 0;

    public AuditLogPanel(KmsApiService kmsApiService, UI ui) {
        this.kmsApiService = kmsApiService;
        this.ui = ui;
        buildUI();
    }

    private void buildUI() {
        setSpacing(true);
        setPadding(true);
        getStyle().set("margin-top", "24px");

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        H3 title = new H3("Audit Logs");
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        titleRow.add(title, loadingBar);
        add(titleRow);

        HorizontalLayout filterBar = new HorizontalLayout();
        filterBar.setWidthFull();
        filterBar.setAlignItems(FlexComponent.Alignment.END);
        filterBar.setSpacing(true);
        filterBar.getStyle().set("flex-wrap", "wrap");

        keyCombo = new ComboBox<>("KMS Key");
        keyCombo.setPlaceholder("Select a key");
        keyCombo.setItemLabelGenerator(KeyUsageStatsPanel.KeyOption::getDisplayName);
        keyCombo.setWidth("300px");
        keyCombo.setRequired(true);

        fromDatePicker = new DatePicker("From Date");
        fromDatePicker.setPlaceholder("YYYY-MM-DD");
        fromDatePicker.setWidth("180px");

        toDatePicker = new DatePicker("To Date");
        toDatePicker.setPlaceholder("YYYY-MM-DD");
        toDatePicker.setWidth("180px");

        loadButton = new Button("Load Logs", VaadinIcon.SEARCH.create());
        loadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loadButton.addClickListener(e -> loadLogs());

        filterBar.add(keyCombo, fromDatePicker, toDatePicker, loadButton);
        add(filterBar);

        grid = new Grid<>();
        grid.setWidthFull();
        grid.setHeight("400px");
        grid.setVisible(false);
        grid.addColumn(new ComponentRenderer<>(entry -> {
            LocalDateTime ts = entry.getTimestamp();
            return new Span(ts != null ? DateHelper.formatToHumanReadable(ts) : "-");
        })).setHeader("Timestamp").setSortable(true).setResizable(true);
        grid.addColumn(AuditLogResponse.LogEntry::getAction).setHeader("Action").setSortable(true).setResizable(true);
        grid.addColumn(AuditLogResponse.LogEntry::getKeyId).setHeader("Key ID").setSortable(true).setResizable(true);
        grid.addColumn(AuditLogResponse.LogEntry::getPrincipal).setHeader("Principal").setSortable(true).setResizable(true);
        grid.addColumn(AuditLogResponse.LogEntry::getIpAddress).setHeader("IP Address").setResizable(true);
        grid.addColumn(AuditLogResponse.LogEntry::getStatus).setHeader("Status").setResizable(true);
        grid.addColumn(AuditLogResponse.LogEntry::getErrorMessage).setHeader("Error Message").setResizable(true);
        grid.addColumn(AuditLogResponse.LogEntry::getExecutionTimeMs).setHeader("Exec Time (ms)").setResizable(true);
        add(grid);

        paginationBar = new HorizontalLayout();
        paginationBar.setWidthFull();
        paginationBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        paginationBar.setAlignItems(FlexComponent.Alignment.CENTER);
        paginationBar.setSpacing(true);
        paginationBar.setVisible(false);
        prevButton = new Button("Previous", VaadinIcon.ANGLE_LEFT.create());
        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        prevButton.addClickListener(e -> changePage(-1));
        nextButton = new Button("Next", VaadinIcon.ANGLE_RIGHT.create());
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addClickListener(e -> changePage(1));
        pageInfoSpan = new Span();
        paginationBar.add(prevButton, pageInfoSpan, nextButton);
        add(paginationBar);
    }

    public void setKeyOptions(List<KeyUsageStatsPanel.KeyOption> options) {
        keyCombo.setItems(options);
    }

    private void loadLogs() {
        KeyUsageStatsPanel.KeyOption selected = keyCombo.getValue();
        if (selected == null) {
            Notification.show("Please select a KMS key", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        loadButton.setEnabled(false);
        grid.setVisible(false);
        paginationBar.setVisible(false);
        loadingBar.setVisible(true);

        LocalDateTime from = fromDatePicker.getValue() != null ? fromDatePicker.getValue().atStartOfDay() : null;
        LocalDateTime to = toDatePicker.getValue() != null ? toDatePicker.getValue().atTime(LocalTime.MAX) : null;

        CompletableFuture.supplyAsync(() -> {
            List<AuditLogResponse.LogEntry> logs = new ArrayList<>();
            try {
                ResponseEntity<AuditLogResponse> response = kmsApiService.getAuditLogs(selected.getKeyId(), from, to, 500);
                if (response.getBody() != null && response.getBody().getLogs() != null) {
                    logs = response.getBody().getLogs();
                }
            } catch (Exception e) {
                log.error("Failed to load audit logs for key {}", selected.getKeyId(), e);
            }
            return logs;
        }).thenAccept(logs -> {
            UI updateUi = ui != null ? ui : UI.getCurrent();
            if (updateUi == null) return;
            updateUi.access(() -> {
                allLogs = logs;
                currentPage = 0;
                updateGrid();
                grid.setVisible(true);
                paginationBar.setVisible(!logs.isEmpty());
                loadingBar.setVisible(false);
                loadButton.setEnabled(true);
                String msg = logs.isEmpty() ? "No audit logs found" : "Loaded " + logs.size() + " log entries";
                Notification.show(msg, 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(logs.isEmpty() ? NotificationVariant.LUMO_WARNING : NotificationVariant.LUMO_SUCCESS);
            });
        }).exceptionally(ex -> {
            UI updateUi = ui != null ? ui : UI.getCurrent();
            if (updateUi != null) {
                updateUi.access(() -> {
                    Notification.show("Error loading audit logs", 3000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    loadingBar.setVisible(false);
                    loadButton.setEnabled(true);
                });
            }
            return null;
        });
    }

    private void updateGrid() {
        if (allLogs.isEmpty()) {
            grid.setItems(new ArrayList<>());
            pageInfoSpan.setText("No logs found");
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
            return;
        }
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allLogs.size());
        grid.setItems(allLogs.subList(start, end));
        int totalPages = (int) Math.ceil((double) allLogs.size() / PAGE_SIZE);
        pageInfoSpan.setText("Page " + (currentPage + 1) + " of " + totalPages);
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < totalPages - 1);
    }

    private void changePage(int delta) {
        int totalPages = (int) Math.ceil((double) allLogs.size() / PAGE_SIZE);
        int newPage = currentPage + delta;
        if (newPage >= 0 && newPage < totalPages) {
            currentPage = newPage;
            updateGrid();
        }
    }
}