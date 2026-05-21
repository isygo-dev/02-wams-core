package eu.isygoit.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.remote.kms.KmsApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RouteAlias(value = "", layout = MainLayout.class)
@Route(value = "home", layout = MainLayout.class)
@PageTitle("KMS Dashboard")
public class MainView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(MainView.class);

    private final KmsApiService kmsApiService;
    private final ProgressBar loadingBar = new ProgressBar();
    private final Button refreshButton = new Button("Refresh Stats", new Icon(VaadinIcon.REFRESH));
    private HorizontalLayout statsContainer;
    private final UI ui;

    // Audit log components
    private ComboBox<KeyOption> auditKeyCombo;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private Button loadLogsButton;
    private Grid<KmsDtos.AuditLogResponse.LogEntry> auditGrid;
    private HorizontalLayout paginationBar;
    private Button prevButton;
    private Button nextButton;
    private Span pageInfoSpan;
    private List<KmsDtos.AuditLogResponse.LogEntry> allLogs = new ArrayList<>();
    private List<KmsDtos.AuditLogResponse.LogEntry> displayedLogs = new ArrayList<>();
    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;

    private List<KeyOption> keyOptions = new ArrayList<>();

    public MainView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;
        this.ui = UI.getCurrent();

        // Enable push to ensure UI updates are sent to client immediately
        this.ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-dashboard");

        add(buildHeader());

        HorizontalLayout topBar = new HorizontalLayout();
        topBar.setWidthFull();
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(e -> loadStatistics());
        topBar.add(refreshButton);
        add(topBar);

        statsContainer = new HorizontalLayout();
        statsContainer.setWidthFull();
        statsContainer.setSpacing(true);
        statsContainer.getStyle()
                .set("flex-wrap", "wrap")
                .set("gap", "16px");
        add(statsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        // Add Audit Log Viewer section
        add(buildAuditLogViewer());

        add(buildQuickLinks());

        showPlaceholderCards();
        loadStatistics();
        loadAuditKeyOptions();
    }

    private H2 buildHeader() {
        H2 title = new H2("Key Management Service Dashboard");
        title.getStyle().set("margin-bottom", "10px");
        return title;
    }

    private VerticalLayout createStatCard(String label, String value, VaadinIcon icon, String color) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.setWidth("220px");
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("align-items", "center")
                .set("flex", "1 1 180px")
                .set("background-color", "var(--lumo-base-color)")
                .set("text-align", "center");

        Icon iconElement = icon.create();
        iconElement.setSize("32px");
        iconElement.getStyle().set("color", color);

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "bold")
                .set("margin-top", "8px");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        card.add(iconElement, valueSpan, labelSpan);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        return card;
    }

    private void showPlaceholderCards() {
        statsContainer.removeAll();
        statsContainer.add(
                createStatCard("Total Keys", "…", VaadinIcon.KEY, "#1E88E5"),
                createStatCard("Active Keys", "…", VaadinIcon.CHECK_CIRCLE, "#2E7D32"),
                createStatCard("Disabled Keys", "…", VaadinIcon.BAN, "#D32F2F"),
                createStatCard("Pending Deletion", "…", VaadinIcon.CLOCK, "#F57C00"),
                createStatCard("Rotation Enabled", "…", VaadinIcon.ROTATE_RIGHT, "#8E24AA"),
                createStatCard("Symmetric Keys", "…", VaadinIcon.CIRCLE, "#43A047"),
                createStatCard("Asymmetric Keys", "…", VaadinIcon.LOCK, "#FB8C00"),
                createStatCard("Encrypt/Decrypt Keys", "…", VaadinIcon.LOCK, "#1E88E5"),
                createStatCard("Sign/Verify Keys", "…", VaadinIcon.PENCIL, "#8E24AA"),
                createStatCard("MAC Keys", "…", VaadinIcon.SIGNAL, "#D81B60"),
                createStatCard("Aliases", "…", VaadinIcon.TAG, "#00ACC1"),
                createStatCard("Grants", "…", VaadinIcon.SHARE, "#546E7A"),
                createStatCard("Custom Key Stores", "…", VaadinIcon.STORAGE, "#37474F")
        );
    }

    private void loadStatistics() {
        loadingBar.setVisible(true);
        refreshButton.setEnabled(false);

        CompletableFuture.supplyAsync(() -> {
            Stats stats = new Stats();
            try {
                log.info("Starting KMS statistics collection...");
                ResponseEntity<KmsDtos.ListKeysResponse> keysResp = kmsApiService.listKeys(100, null);
                KmsDtos.ListKeysResponse keys = keysResp.getBody();
                if (keys != null && keys.getKeys() != null) {
                    stats.totalKeys = keys.getKeys().size();
                    for (KmsDtos.ListKeysResponse.KeyEntry entry : keys.getKeys()) {
                        try {
                            ResponseEntity<KmsDtos.DescribeKeyResponse> descResp = kmsApiService.describeKey(entry.getKeyId());
                            KmsDtos.DescribeKeyResponse desc = descResp.getBody();
                            if (desc != null && desc.getKeyMetadata() != null) {
                                var meta = desc.getKeyMetadata();
                                if (meta.getKeyStatus() == IEnumKeyStatus.Types.ENABLED) stats.activeKeys++;
                                else if (meta.getKeyStatus() == IEnumKeyStatus.Types.DISABLED) stats.disabledKeys++;
                                else if (meta.getKeyStatus() == IEnumKeyStatus.Types.PENDING_DELETION) stats.pendingDeletion++;
                                if (Boolean.TRUE.equals(meta.getRotationEnabled())) stats.rotationEnabled++;
                                IEnumKeySpec.Types spec = meta.getKeySpec();
                                if (spec != null) {
                                    if (spec.isAsymmetric()) stats.asymmetricKeys++;
                                    else stats.symmetricKeys++;
                                }
                                IEnumKeyUsage.Types usage = meta.getKeyUsage();
                                if (usage != null) {
                                    switch (usage) {
                                        case ENCRYPT_DECRYPT -> stats.encryptUsage++;
                                        case SIGN_VERIFY -> stats.signUsage++;
                                        case GENERATE_VERIFY_MAC -> stats.macUsage++;
                                    }
                                }
                                try {
                                    ResponseEntity<KmsDtos.ListGrantsResponse> grantsResp = kmsApiService.listGrants(entry.getKeyId(), 1000, null, null, null);
                                    if (grantsResp.getBody() != null && grantsResp.getBody().getGrants() != null)
                                        stats.totalGrants += grantsResp.getBody().getGrants().size();
                                } catch (Exception e) { /* ignore */ }
                            }
                        } catch (Exception e) { /* ignore */ }
                    }
                }
                try {
                    ResponseEntity<KmsDtos.ListAliasesResponse> aliasesResp = kmsApiService.listAliases(100, null);
                    if (aliasesResp.getBody() != null && aliasesResp.getBody().getAliases() != null)
                        stats.totalAliases = aliasesResp.getBody().getAliases().size();
                } catch (Exception e) { /* ignore */ }
                try {
                    ResponseEntity<KmsDtos.ListCustomKeyStoresResponse> storesResp = kmsApiService.listCustomKeyStores(100, null);
                    if (storesResp.getBody() != null && storesResp.getBody().getCustomKeyStores() != null)
                        stats.totalStores = storesResp.getBody().getCustomKeyStores().size();
                } catch (Exception e) { /* ignore */ }
                log.info("Stats collected: totalKeys={}, activeKeys={}, symmetricKeys={}, asymmetricKeys={}, encryptUsage={}, signUsage={}, macUsage={}, aliases={}, grants={}, stores={}",
                        stats.totalKeys, stats.activeKeys, stats.symmetricKeys, stats.asymmetricKeys,
                        stats.encryptUsage, stats.signUsage, stats.macUsage, stats.totalAliases, stats.totalGrants, stats.totalStores);
            } catch (Exception e) {
                log.error("Error in statistics collection", e);
            }
            return stats;
        }).orTimeout(30, TimeUnit.SECONDS).exceptionally(ex -> {
            log.error("Statistics timeout/failure", ex);
            return new Stats();
        }).thenAccept(stats -> {
            UI updateUi = ui != null ? ui : UI.getCurrent();
            if (updateUi == null) {
                log.error("UI is null – cannot update cards");
                return;
            }
            updateUi.access(() -> {
                try {
                    statsContainer.removeAll();
                    statsContainer.add(
                            createStatCard("Total Keys", String.valueOf(stats.totalKeys), VaadinIcon.KEY, "#1E88E5"),
                            createStatCard("Active Keys", String.valueOf(stats.activeKeys), VaadinIcon.CHECK_CIRCLE, "#2E7D32"),
                            createStatCard("Disabled Keys", String.valueOf(stats.disabledKeys), VaadinIcon.BAN, "#D32F2F"),
                            createStatCard("Pending Deletion", String.valueOf(stats.pendingDeletion), VaadinIcon.CLOCK, "#F57C00"),
                            createStatCard("Rotation Enabled", String.valueOf(stats.rotationEnabled), VaadinIcon.ROTATE_RIGHT, "#8E24AA"),
                            createStatCard("Symmetric Keys", String.valueOf(stats.symmetricKeys), VaadinIcon.CIRCLE, "#43A047"),
                            createStatCard("Asymmetric Keys", String.valueOf(stats.asymmetricKeys), VaadinIcon.LOCK, "#FB8C00"),
                            createStatCard("Encrypt/Decrypt Keys", String.valueOf(stats.encryptUsage), VaadinIcon.LOCK, "#1E88E5"),
                            createStatCard("Sign/Verify Keys", String.valueOf(stats.signUsage), VaadinIcon.PENCIL, "#8E24AA"),
                            createStatCard("MAC Keys", String.valueOf(stats.macUsage), VaadinIcon.SIGNAL, "#D81B60"),
                            createStatCard("Aliases", String.valueOf(stats.totalAliases), VaadinIcon.TAG, "#00ACC1"),
                            createStatCard("Grants", String.valueOf(stats.totalGrants), VaadinIcon.SHARE, "#546E7A"),
                            createStatCard("Custom Key Stores", String.valueOf(stats.totalStores), VaadinIcon.STORAGE, "#37474F")
                    );
                    loadingBar.setVisible(false);
                    refreshButton.setEnabled(true);
                    log.info("UI updated with stats. Pushing changes to client.");
                    updateUi.push();
                } catch (Exception e) {
                    log.error("Error updating UI cards", e);
                    loadingBar.setVisible(false);
                    refreshButton.setEnabled(true);
                    updateUi.push();
                }
            });
        });
    }

    // =========================================================================
    // Audit Log Viewer (filtered by selected key)
    // =========================================================================

    private VerticalLayout buildAuditLogViewer() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.addClassName(LumoUtility.BorderRadius.LARGE);
        layout.addClassName(LumoUtility.Background.BASE);
        layout.getStyle().set("margin-top", "24px");

        H3 title = new H3("Audit Logs");
        layout.add(title);

        // Filter bar
        HorizontalLayout filterBar = new HorizontalLayout();
        filterBar.setWidthFull();
        filterBar.setAlignItems(FlexComponent.Alignment.END);
        filterBar.setSpacing(true);
        filterBar.getStyle().set("flex-wrap", "wrap");

        auditKeyCombo = new ComboBox<>("KMS Key");
        auditKeyCombo.setPlaceholder("Select a key");
        auditKeyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        auditKeyCombo.setWidth("300px");
        auditKeyCombo.setRequired(true);

        fromDatePicker = new DatePicker("From Date");
        fromDatePicker.setPlaceholder("YYYY-MM-DD");
        fromDatePicker.setWidth("180px");

        toDatePicker = new DatePicker("To Date");
        toDatePicker.setPlaceholder("YYYY-MM-DD");
        toDatePicker.setWidth("180px");

        loadLogsButton = new Button("Load Logs", new Icon(VaadinIcon.SEARCH));
        loadLogsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loadLogsButton.addClickListener(e -> loadAuditLogs());

        filterBar.add(auditKeyCombo, fromDatePicker, toDatePicker, loadLogsButton);
        layout.add(filterBar);

        // Grid for logs
        auditGrid = new Grid<>();
        auditGrid.setWidthFull();
        auditGrid.setHeight("400px");
        auditGrid.setVisible(false);
        auditGrid.addColumn(new ComponentRenderer<>(entry -> {
            LocalDateTime ts = entry.getTimestamp();
            String formatted = ts != null ? ts.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "-";
            return new Span(formatted);
        })).setHeader("Timestamp").setSortable(true).setResizable(true);
        auditGrid.addColumn(KmsDtos.AuditLogResponse.LogEntry::getAction).setHeader("Action").setSortable(true).setResizable(true);
        auditGrid.addColumn(KmsDtos.AuditLogResponse.LogEntry::getKeyId).setHeader("Key ID").setSortable(true).setResizable(true);
        auditGrid.addColumn(KmsDtos.AuditLogResponse.LogEntry::getPrincipal).setHeader("Principal").setSortable(true).setResizable(true);
        auditGrid.addColumn(KmsDtos.AuditLogResponse.LogEntry::getIpAddress).setHeader("IP Address").setResizable(true);
        auditGrid.addColumn(KmsDtos.AuditLogResponse.LogEntry::getStatus).setHeader("Status").setResizable(true);
        auditGrid.addColumn(KmsDtos.AuditLogResponse.LogEntry::getErrorMessage).setHeader("Error Message").setResizable(true);
        auditGrid.addColumn(KmsDtos.AuditLogResponse.LogEntry::getExecutionTimeMs)
                .setHeader("Exec Time (ms)").setResizable(true);
        layout.add(auditGrid);

        // Pagination controls
        paginationBar = new HorizontalLayout();
        paginationBar.setWidthFull();
        paginationBar.setJustifyContentMode(JustifyContentMode.END);
        paginationBar.setAlignItems(FlexComponent.Alignment.CENTER);
        paginationBar.setSpacing(true);
        paginationBar.setVisible(false);

        prevButton = new Button("Previous", new Icon(VaadinIcon.ANGLE_LEFT));
        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        prevButton.addClickListener(e -> changePage(-1));

        nextButton = new Button("Next", new Icon(VaadinIcon.ANGLE_RIGHT));
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addClickListener(e -> changePage(1));

        pageInfoSpan = new Span();

        paginationBar.add(prevButton, pageInfoSpan, nextButton);
        layout.add(paginationBar);

        return layout;
    }

    private void loadAuditKeyOptions() {
        try {
            ResponseEntity<KmsDtos.ListKeysResponse> response = kmsApiService.listKeys(100, null);
            if (response.getBody() != null && response.getBody().getKeys() != null) {
                keyOptions = response.getBody().getKeys().stream()
                        .map(entry -> new KeyOption(entry.getKeyId(), fetchAlias(entry.getKeyId())))
                        .collect(Collectors.toList());
                auditKeyCombo.setItems(keyOptions);
            }
        } catch (Exception e) {
            log.error("Failed to load keys for audit log selection", e);
            Notification.show("Could not load keys for audit logs", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private String fetchAlias(String keyId) {
        try {
            ResponseEntity<KmsDtos.DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
            KmsDtos.DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null && desc.getKeyMetadata().getKeyAlias() != null) {
                return desc.getKeyMetadata().getKeyAlias();
            }
        } catch (Exception e) {
            // ignore
        }
        return keyId;
    }

    private void loadAuditLogs() {
        KeyOption selected = auditKeyCombo.getValue();
        if (selected == null) {
            Notification.show("Please select a KMS key", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        loadLogsButton.setEnabled(false);
        auditGrid.setVisible(false);
        paginationBar.setVisible(false);
        loadingBar.setVisible(true);

        LocalDateTime from = fromDatePicker.getValue() != null ? fromDatePicker.getValue().atStartOfDay() : null;
        LocalDateTime to = toDatePicker.getValue() != null ? toDatePicker.getValue().atTime(LocalTime.MAX) : null;

        // Fetch logs – we can request up to 500 entries to support client-side pagination
        CompletableFuture.supplyAsync(() -> {
            List<KmsDtos.AuditLogResponse.LogEntry> logs = new ArrayList<>();
            try {
                ResponseEntity<KmsDtos.AuditLogResponse> response =
                        kmsApiService.getAuditLogs(selected.getKeyId(), from, to, 500);
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
                updateAuditGrid();
                auditGrid.setVisible(true);
                paginationBar.setVisible(!logs.isEmpty());
                loadingBar.setVisible(false);
                loadLogsButton.setEnabled(true);
                if (logs.isEmpty()) {
                    Notification.show("No audit logs found for the selected criteria", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_WARNING);
                } else {
                    Notification.show("Loaded " + logs.size() + " log entries", 2000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
            });
        }).exceptionally(ex -> {
            UI updateUi = ui != null ? ui : UI.getCurrent();
            if (updateUi != null) {
                updateUi.access(() -> {
                    Notification.show("Error loading audit logs", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    loadingBar.setVisible(false);
                    loadLogsButton.setEnabled(true);
                });
            }
            return null;
        });
    }

    private void updateAuditGrid() {
        if (allLogs.isEmpty()) {
            auditGrid.setItems(new ArrayList<>());
            pageInfoSpan.setText("No logs found");
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
            return;
        }
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allLogs.size());
        List<KmsDtos.AuditLogResponse.LogEntry> pageItems = allLogs.subList(start, end);
        auditGrid.setItems(pageItems);

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
            updateAuditGrid();
        }
    }

    // =========================================================================
    // Quick Links
    // =========================================================================

    private VerticalLayout buildQuickLinks() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.getStyle().set("gap", "10px").set("margin-top", "24px");
        H2 title = new H2("Quick Actions");
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        Span actions = new Span("• Create Key\n• Encrypt / Decrypt\n• Manage Aliases\n• Configure Policies\n• Manage Grants");
        actions.getStyle().set("white-space", "pre-line");
        layout.add(title, actions);
        return layout;
    }

    private static class Stats {
        long totalKeys = 0, activeKeys = 0, disabledKeys = 0, pendingDeletion = 0, rotationEnabled = 0,
                symmetricKeys = 0, asymmetricKeys = 0, encryptUsage = 0, signUsage = 0, macUsage = 0,
                totalAliases = 0, totalGrants = 0, totalStores = 0;
    }

    private static class KeyOption {
        private final String keyId;
        private final String displayName;

        KeyOption(String keyId, String aliasOrId) {
            this.keyId = keyId;
            this.displayName = aliasOrId != null ? aliasOrId + " (" + keyId + ")" : keyId;
        }

        String getKeyId() { return keyId; }
        String getDisplayName() { return displayName; }
    }
}