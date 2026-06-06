package eu.isygoit.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private static final int PAGE_SIZE = 20;
    private final KmsApiService kmsApiService;
    private final KmsTokenConfigService tokenConfigService;
    private final KmsAppNextCodeService nextCodeService;
    private final UI ui;

    // Key Statistics
    private final ProgressBar statsLoadingBar = new ProgressBar();
    private final Button refreshButton = new Button("Refresh Stats", new Icon(VaadinIcon.REFRESH));
    // Token Configuration Statistics
    private final ProgressBar tokenStatsLoadingBar = new ProgressBar();
    // Key Usage Statistics
    private final ProgressBar usageLoadingBar = new ProgressBar();
    // Audit Logs
    private final ProgressBar auditLoadingBar = new ProgressBar();
    private HorizontalLayout statsContainer;
    private HorizontalLayout tokenStatsContainer;
    private ComboBox<KeyOption> usageKeyCombo;
    private Button loadUsageStatsButton;
    private HorizontalLayout usageStatsContainer;
    private ComboBox<KeyOption> auditKeyCombo;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private Button loadLogsButton;
    private Grid<AuditLogResponse.LogEntry> auditGrid;
    private HorizontalLayout paginationBar;
    private Button prevButton;
    private Button nextButton;
    private Span pageInfoSpan;
    private List<AuditLogResponse.LogEntry> allLogs = new ArrayList<>();
    private int currentPage = 0;

    private List<KeyOption> keyOptions = new ArrayList<>();

    @Autowired
    public MainView(KmsApiService kmsApiService,
                    KmsTokenConfigService tokenConfigService,
                    KmsAppNextCodeService nextCodeService) {
        this.kmsApiService = kmsApiService;
        this.tokenConfigService = tokenConfigService;
        this.nextCodeService = nextCodeService;
        this.ui = UI.getCurrent();

        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-dashboard");

        add(buildHeader());
        add(buildGlobalKeysStatistics());
        add(buildTokenConfigStatistics());
        add(buildKeyUsageStatsSection());
        add(buildAuditLogViewer());
        add(buildQuickLinks());

        injectResponsiveStyles();

        showPlaceholderCards();
        showTokenPlaceholderCards();
        loadStatistics();
        loadTokenStatistics();
        loadKeyOptions();
    }

    // =========================================================================
    // Helper methods (copy to clipboard)
    // =========================================================================
    public static Button createCopyButton(VaadinIcon icon, String textToCopy, String tooltip) {
        Button btn = new Button(new Icon(icon));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        btn.setTooltipText(tooltip);
        btn.setWidth("20px");
        btn.setHeight("20px");
        btn.addClickListener(e -> copyToClipboard(textToCopy, "Copied " + textToCopy + " to clipboard"));
        return btn;
    }

    public static void copyToClipboard(String text, String notificationText) {
        UI.getCurrent().getPage().executeJs(
                "navigator.clipboard.writeText($0).then(() => { $0.dispatchEvent(new Event('copy-success')); }).catch(() => { $0.dispatchEvent(new Event('copy-error')); });",
                text
        );
        Notification.show(notificationText, 1500, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    // =========================================================================
    // Header
    // =========================================================================
    private H2 buildHeader() {
        H2 title = new H2("Key Management Service Dashboard");
        title.getStyle().set("margin-bottom", "10px");
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        return title;
    }

    // =========================================================================
    // Global Key Statistics (including NextCode)
    // =========================================================================
    private VerticalLayout buildGlobalKeysStatistics() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.setWidthFull();

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        H3 statsTitle = new H3("Key Statistics");
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(e -> loadStatistics());
        statsLoadingBar.setIndeterminate(true);
        statsLoadingBar.setVisible(false);
        statsLoadingBar.setWidth("200px");
        titleRow.add(statsTitle, refreshButton, statsLoadingBar);
        layout.add(titleRow);

        statsContainer = new HorizontalLayout();
        statsContainer.setWidthFull();
        statsContainer.setSpacing(true);
        statsContainer.getStyle().set("flex-wrap", "wrap").set("gap", "16px");
        layout.add(statsContainer);

        return layout;
    }

    private VerticalLayout createStatCard(String label, String value, VaadinIcon icon, String color, String tooltip) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.setWidthFull();
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("align-items", "center")
                .set("flex", "1 1 180px")
                .set("background-color", "var(--lumo-base-color)")
                .set("text-align", "center")
                .set("transition", "all 0.2s ease");
        card.addClassName("stat-card");
        card.getElement().setAttribute("title", tooltip);

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
                createStatCard("Total Keys", "…", VaadinIcon.KEY, "#1E88E5", "Total number of KMS keys in your account"),
                createStatCard("Active Keys", "…", VaadinIcon.CHECK_CIRCLE, "#2E7D32", "Keys that are enabled and usable for cryptographic operations"),
                createStatCard("Disabled Keys", "…", VaadinIcon.BAN, "#D32F2F", "Keys that are disabled and cannot be used"),
                createStatCard("Pending Deletion", "…", VaadinIcon.CLOCK, "#F57C00", "Keys that have been scheduled for deletion"),
                createStatCard("Rotation Enabled", "…", VaadinIcon.ROTATE_RIGHT, "#8E24AA", "Keys with automatic key rotation enabled"),
                createStatCard("Symmetric Keys", "…", VaadinIcon.CIRCLE, "#43A047", "AES or HMAC keys (same key for encryption and decryption)"),
                createStatCard("Asymmetric Keys", "…", VaadinIcon.LOCK, "#FB8C00", "RSA or EC keys (different keys for signing/verification)"),
                createStatCard("Encrypt/Decrypt Keys", "…", VaadinIcon.LOCK, "#1E88E5", "Keys intended for encryption and decryption operations"),
                createStatCard("Sign/Verify Keys", "…", VaadinIcon.PENCIL, "#8E24AA", "Keys intended for digital signature and verification"),
                createStatCard("MAC Keys", "…", VaadinIcon.SIGNAL, "#D81B60", "Keys used for Message Authentication Code (HMAC) operations"),
                createStatCard("Aliases", "…", VaadinIcon.TAG, "#00ACC1", "Total number of friendly aliases pointing to keys"),
                createStatCard("Grants", "…", VaadinIcon.SHARE, "#546E7A", "Total number of access grants across all keys"),
                createStatCard("Custom Key Stores", "…", VaadinIcon.STORAGE, "#37474F", "Number of configured custom key stores (CloudHSM or external)"),
                createStatCard("NextCode Configs", "…", VaadinIcon.CODE, "#607D8B", "Number of incremental code generator configurations")
        );
    }

    private void loadStatistics() {
        statsLoadingBar.setVisible(true);
        refreshButton.setEnabled(false);

        CompletableFuture.supplyAsync(() -> {
            Stats stats = new Stats();
            try {
                log.info("Starting KMS statistics collection...");
                ResponseEntity<ListKeysResponse> keysResp = kmsApiService.listKeys(100, null);
                ListKeysResponse keys = keysResp.getBody();
                if (keys != null && keys.getKeys() != null) {
                    stats.totalKeys = keys.getKeys().size();
                    for (ListKeysResponse.KeyEntry entry : keys.getKeys()) {
                        try {
                            ResponseEntity<DescribeKeyResponse> descResp = kmsApiService.describeKey(entry.getKeyId());
                            DescribeKeyResponse desc = descResp.getBody();
                            if (desc != null && desc.getKeyMetadata() != null) {
                                var meta = desc.getKeyMetadata();
                                if (meta.getKeyStatus() == IEnumKeyStatus.Types.ENABLED) stats.activeKeys++;
                                else if (meta.getKeyStatus() == IEnumKeyStatus.Types.DISABLED) stats.disabledKeys++;
                                else if (meta.getKeyStatus() == IEnumKeyStatus.Types.PENDING_DELETION)
                                    stats.pendingDeletion++;
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
                                    ResponseEntity<ListGrantsResponse> grantsResp = kmsApiService.listGrants(entry.getKeyId(), 1000, null, null, null);
                                    if (grantsResp.getBody() != null && grantsResp.getBody().getGrants() != null)
                                        stats.totalGrants += grantsResp.getBody().getGrants().size();
                                } catch (Exception e) { /* ignore */ }
                            }
                        } catch (Exception e) { /* ignore */ }
                    }
                }
                try {
                    ResponseEntity<ListAliasesResponse> aliasesResp = kmsApiService.listAliases(100, null);
                    if (aliasesResp.getBody() != null && aliasesResp.getBody().getAliases() != null)
                        stats.totalAliases = aliasesResp.getBody().getAliases().size();
                } catch (Exception e) { /* ignore */ }
                try {
                    ResponseEntity<ListCustomKeyStoresResponse> storesResp = kmsApiService.listCustomKeyStores(100, null);
                    if (storesResp.getBody() != null && storesResp.getBody().getCustomKeyStores() != null)
                        stats.totalStores = storesResp.getBody().getCustomKeyStores().size();
                } catch (Exception e) { /* ignore */ }
                // NextCode configs count
                try {
                    ResponseEntity<PaginatedResponseDto<NextCodeDto>> nextCodeResp = nextCodeService.findAll(0, 1);
                    PaginatedResponseDto<NextCodeDto> nextCodeBody = nextCodeResp.getBody();
                    if (nextCodeBody != null) {
                        stats.nextCodeTotal = nextCodeBody.getTotalElements();
                    }
                } catch (Exception e) {
                    log.error("Failed to load NextCode statistics", e);
                }
            } catch (Exception e) {
                log.error("Error in statistics collection", e);
            }
            return stats;
        }).orTimeout(30, TimeUnit.SECONDS).exceptionally(ex -> {
            log.error("Statistics timeout/failure", ex);
            return new Stats();
        }).thenAccept(stats -> {
            UI updateUi = ui != null ? ui : UI.getCurrent();
            if (updateUi == null) return;
            updateUi.access(() -> {
                statsContainer.removeAll();
                statsContainer.add(
                        createStatCard("Total Keys", String.valueOf(stats.totalKeys), VaadinIcon.KEY, "#1E88E5", "Total number of KMS keys in your account"),
                        createStatCard("Active Keys", String.valueOf(stats.activeKeys), VaadinIcon.CHECK_CIRCLE, "#2E7D32", "Keys that are enabled and usable for cryptographic operations"),
                        createStatCard("Disabled Keys", String.valueOf(stats.disabledKeys), VaadinIcon.BAN, "#D32F2F", "Keys that are disabled and cannot be used"),
                        createStatCard("Pending Deletion", String.valueOf(stats.pendingDeletion), VaadinIcon.CLOCK, "#F57C00", "Keys that have been scheduled for deletion"),
                        createStatCard("Rotation Enabled", String.valueOf(stats.rotationEnabled), VaadinIcon.ROTATE_RIGHT, "#8E24AA", "Keys with automatic key rotation enabled"),
                        createStatCard("Symmetric Keys", String.valueOf(stats.symmetricKeys), VaadinIcon.CIRCLE, "#43A047", "AES or HMAC keys (same key for encryption and decryption)"),
                        createStatCard("Asymmetric Keys", String.valueOf(stats.asymmetricKeys), VaadinIcon.LOCK, "#FB8C00", "RSA or EC keys (different keys for signing/verification)"),
                        createStatCard("Encrypt/Decrypt Keys", String.valueOf(stats.encryptUsage), VaadinIcon.LOCK, "#1E88E5", "Keys intended for encryption and decryption operations"),
                        createStatCard("Sign/Verify Keys", String.valueOf(stats.signUsage), VaadinIcon.PENCIL, "#8E24AA", "Keys intended for digital signature and verification"),
                        createStatCard("MAC Keys", String.valueOf(stats.macUsage), VaadinIcon.SIGNAL, "#D81B60", "Keys used for Message Authentication Code (HMAC) operations"),
                        createStatCard("Aliases", String.valueOf(stats.totalAliases), VaadinIcon.TAG, "#00ACC1", "Total number of friendly aliases pointing to keys"),
                        createStatCard("Grants", String.valueOf(stats.totalGrants), VaadinIcon.SHARE, "#546E7A", "Total number of access grants across all keys"),
                        createStatCard("Custom Key Stores", String.valueOf(stats.totalStores), VaadinIcon.STORAGE, "#37474F", "Number of configured custom key stores (CloudHSM or external)"),
                        createStatCard("NextCode Configs", String.valueOf(stats.nextCodeTotal), VaadinIcon.CODE, "#607D8B", "Number of incremental code generator configurations")
                );
                statsLoadingBar.setVisible(false);
                refreshButton.setEnabled(true);
                updateUi.push();
            });
        });
    }

    // =========================================================================
    // Token Configuration Statistics
    // =========================================================================
    private VerticalLayout buildTokenConfigStatistics() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.setWidthFull();

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        H3 tokenStatsTitle = new H3("Token Configuration Statistics");
        tokenStatsLoadingBar.setIndeterminate(true);
        tokenStatsLoadingBar.setVisible(false);
        tokenStatsLoadingBar.setWidth("200px");
        titleRow.add(tokenStatsTitle, tokenStatsLoadingBar);
        layout.add(titleRow);

        tokenStatsContainer = new HorizontalLayout();
        tokenStatsContainer.setWidthFull();
        tokenStatsContainer.setSpacing(true);
        tokenStatsContainer.getStyle().set("flex-wrap", "wrap").set("gap", "16px");
        layout.add(tokenStatsContainer);

        return layout;
    }

    private void showTokenPlaceholderCards() {
        tokenStatsContainer.removeAll();
        tokenStatsContainer.add(
                createStatCard("Total Configs", "…", VaadinIcon.COG, "#607D8B", "Total number of JWT token configurations"),
                createStatCard("ACCESS", "…", VaadinIcon.KEY, "#1E88E5", "Configurations for access tokens (used for API authorization)"),
                createStatCard("REFRESH", "…", VaadinIcon.REFRESH, "#43A047", "Configurations for refresh tokens (used to obtain new access tokens)"),
                createStatCard("RSTPWD", "…", VaadinIcon.LOCK, "#F57C00", "Configurations for password reset tokens"),
                createStatCard("AUTHORITY", "…", VaadinIcon.USER, "#8E24AA", "Configurations for authority tokens (granting specific permissions)")
        );
    }

    private void loadTokenStatistics() {
        tokenStatsLoadingBar.setVisible(true);
        CompletableFuture.supplyAsync(() -> {
            TokenStats stats = new TokenStats();
            try {
                // Get total count via pagination
                ResponseEntity<PaginatedResponseDto<TokenConfigDto>> totalResp = tokenConfigService.findAll(0, 1);
                PaginatedResponseDto<TokenConfigDto> totalBody = totalResp.getBody();
                if (totalBody != null) {
                    stats.total = totalBody.getTotalElements();
                }
                // Fetch up to 500 configs to compute per-type counts (acceptable for dashboard)
                ResponseEntity<PaginatedResponseDto<TokenConfigDto>> listResp = tokenConfigService.findAll(0, 500);
                if (listResp.getBody() != null && listResp.getBody().getContent() != null) {
                    List<TokenConfigDto> configs = listResp.getBody().getContent();
                    stats.access = configs.stream().filter(c -> c.getTokenType() == IEnumToken.Types.ACCESS).count();
                    stats.refresh = configs.stream().filter(c -> c.getTokenType() == IEnumToken.Types.REFRESH).count();
                    stats.rstpwd = configs.stream().filter(c -> c.getTokenType() == IEnumToken.Types.RSTPWD).count();
                    stats.authority = configs.stream().filter(c -> c.getTokenType() == IEnumToken.Types.AUTHORITY).count();
                }
            } catch (Exception e) {
                log.error("Error fetching token configuration statistics", e);
            }
            return stats;
        }).orTimeout(30, TimeUnit.SECONDS).exceptionally(ex -> {
            log.error("Token stats timeout/failure", ex);
            return new TokenStats();
        }).thenAccept(stats -> {
            UI updateUi = ui != null ? ui : UI.getCurrent();
            if (updateUi == null) return;
            updateUi.access(() -> {
                tokenStatsContainer.removeAll();
                tokenStatsContainer.add(
                        createStatCard("Total Configs", String.valueOf(stats.total), VaadinIcon.COG, "#607D8B", "Total number of JWT token configurations"),
                        createStatCard("ACCESS", String.valueOf(stats.access), VaadinIcon.KEY, "#1E88E5", "Configurations for access tokens (used for API authorization)"),
                        createStatCard("REFRESH", String.valueOf(stats.refresh), VaadinIcon.REFRESH, "#43A047", "Configurations for refresh tokens (used to obtain new access tokens)"),
                        createStatCard("RSTPWD", String.valueOf(stats.rstpwd), VaadinIcon.LOCK, "#F57C00", "Configurations for password reset tokens"),
                        createStatCard("AUTHORITY", String.valueOf(stats.authority), VaadinIcon.USER, "#8E24AA", "Configurations for authority tokens (granting specific permissions)")
                );
                tokenStatsLoadingBar.setVisible(false);
                updateUi.push();
            });
        });
    }

    // =========================================================================
    // Key Usage Statistics
    // =========================================================================
    private VerticalLayout buildKeyUsageStatsSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.addClassName(LumoUtility.BorderRadius.LARGE);
        layout.addClassName(LumoUtility.Background.BASE);
        layout.getStyle().set("margin-top", "24px");

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        H3 usageTitle = new H3("Key Usage Statistics");
        usageLoadingBar.setIndeterminate(true);
        usageLoadingBar.setVisible(false);
        usageLoadingBar.setWidth("200px");
        titleRow.add(usageTitle, usageLoadingBar);
        layout.add(titleRow);

        HorizontalLayout filterBar = new HorizontalLayout();
        filterBar.setWidthFull();
        filterBar.setAlignItems(FlexComponent.Alignment.END);
        filterBar.setSpacing(true);
        filterBar.getStyle().set("flex-wrap", "wrap");
        filterBar.addClassName("stats-filter-bar");

        usageKeyCombo = new ComboBox<>("Select Key");
        usageKeyCombo.setPlaceholder("Choose a key");
        usageKeyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        usageKeyCombo.setWidth("300px");
        usageKeyCombo.setRequired(true);

        loadUsageStatsButton = new Button("Load Usage Stats", new Icon(VaadinIcon.CHART));
        loadUsageStatsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loadUsageStatsButton.addClickListener(e -> loadKeyUsageStats());

        filterBar.add(usageKeyCombo, loadUsageStatsButton);
        layout.add(filterBar);

        usageStatsContainer = new HorizontalLayout();
        usageStatsContainer.setWidthFull();
        usageStatsContainer.setSpacing(true);
        usageStatsContainer.getStyle().set("flex-wrap", "wrap").set("gap", "16px");
        usageStatsContainer.setVisible(false);
        layout.add(usageStatsContainer);

        return layout;
    }

    private void loadKeyOptions() {
        try {
            ResponseEntity<ListKeysResponse> response = kmsApiService.listKeys(100, null);
            if (response.getBody() != null && response.getBody().getKeys() != null) {
                keyOptions = response.getBody().getKeys().stream()
                        .map(entry -> {
                            String keyId = entry.getKeyId();
                            IEnumKeyUsage.Types usage = fetchKeyUsage(keyId);
                            String alias = fetchAlias(keyId);
                            return new KeyOption(keyId, alias, usage);
                        })
                        .collect(Collectors.toList());
                usageKeyCombo.setItems(keyOptions);
                auditKeyCombo.setItems(keyOptions);
            }
        } catch (Exception e) {
            log.error("Failed to load key options", e);
        }
    }

    private IEnumKeyUsage.Types fetchKeyUsage(String keyId) {
        try {
            ResponseEntity<DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
            DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null) {
                return desc.getKeyMetadata().getKeyUsage();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String fetchAlias(String keyId) {
        try {
            ResponseEntity<DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
            DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null && desc.getKeyMetadata().getKeyAlias() != null) {
                return desc.getKeyMetadata().getKeyAlias();
            }
        } catch (Exception e) {
            // ignore
        }
        return keyId;
    }

    private void loadKeyUsageStats() {
        KeyOption selected = usageKeyCombo.getValue();
        if (selected == null) {
            Notification.show("Please select a key", 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        usageStatsContainer.setVisible(false);
        usageLoadingBar.setVisible(true);
        loadUsageStatsButton.setEnabled(false);

        String keyId = selected.getKeyId();
        IEnumKeyUsage.Types keyUsage = selected.getKeyUsage();

        CompletableFuture<KeyUsageStatsResponse> statsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                ResponseEntity<KeyUsageStatsResponse> response = kmsApiService.getKeyUsageStats(keyId);
                return response.getBody();
            } catch (Exception e) {
                log.error("Failed to load usage stats for key {}", keyId, e);
                return null;
            }
        });

        CompletableFuture<Integer> versionsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                ResponseEntity<ListKeyRotationsResponse> response = kmsApiService.listKeyRotations(keyId, 1000, null);
                ListKeyRotationsResponse body = response.getBody();
                if (body != null && body.getRotations() != null) {
                    return body.getRotations().size();
                }
            } catch (Exception e) {
                log.error("Failed to load key versions for key {}", keyId, e);
            }
            return 0;
        });

        CompletableFuture.allOf(statsFuture, versionsFuture)
                .thenAccept(v -> {
                    KeyUsageStatsResponse stats = statsFuture.join();
                    Integer versionCount = versionsFuture.join();

                    UI updateUi = ui != null ? ui : UI.getCurrent();
                    if (updateUi == null) return;
                    updateUi.access(() -> {
                        usageLoadingBar.setVisible(false);
                        loadUsageStatsButton.setEnabled(true);

                        usageStatsContainer.removeAll();
                        if (stats == null) {
                            Span errorSpan = new Span("Failed to load statistics. Check server logs.");
                            errorSpan.getStyle().set("color", "var(--lumo-error-text-color)");
                            usageStatsContainer.add(errorSpan);
                            usageStatsContainer.setVisible(true);
                            return;
                        }

                        if (keyUsage == IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
                            usageStatsContainer.add(
                                    createSmallStatCard("Encrypts", stats.getEncryptCount()),
                                    createSmallStatCard("Decrypts", stats.getDecryptCount()),
                                    createSmallStatCard("Generate Data Keys", stats.getGenerateDataKeyCount()),
                                    createSmallStatCard("Re-Encrypts", stats.getReEncryptCount())
                            );
                        } else if (keyUsage == IEnumKeyUsage.Types.SIGN_VERIFY) {
                            usageStatsContainer.add(
                                    createSmallStatCard("Signs", stats.getSignCount()),
                                    createSmallStatCard("Verifies", stats.getVerifyCount())
                            );
                        } else if (keyUsage == IEnumKeyUsage.Types.GENERATE_VERIFY_MAC) {
                            long generateMacCount = (stats.getGenerateMacCount() != null) ? stats.getGenerateMacCount() : 0L;
                            long verifyMacCount = (stats.getVerifyMacCount() != null) ? stats.getVerifyMacCount() : 0L;
                            usageStatsContainer.add(
                                    createSmallStatCard("Generate MAC", generateMacCount),
                                    createSmallStatCard("Verify MAC", verifyMacCount)
                            );
                        }

                        usageStatsContainer.add(createSmallStatCard("Key Versions", versionCount));

                        if (stats.getLastUsedDate() != null) {
                            usageStatsContainer.add(
                                    createSmallStatCard("Last Used", DateHelper.formatToHumanReadable(stats.getLastUsedDate()))
                            );
                        }

                        usageStatsContainer.setVisible(true);
                    });
                })
                .exceptionally(ex -> {
                    UI updateUi = ui != null ? ui : UI.getCurrent();
                    if (updateUi != null) {
                        updateUi.access(() -> {
                            usageLoadingBar.setVisible(false);
                            loadUsageStatsButton.setEnabled(true);
                            usageStatsContainer.removeAll();
                            usageStatsContainer.add(new Span("Error loading statistics"));
                            usageStatsContainer.setVisible(true);
                        });
                    }
                    return null;
                });
    }

    private VerticalLayout createSmallStatCard(String label, Object value) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.setWidth("160px");
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("align-items", "center")
                .set("background-color", "var(--lumo-base-color)")
                .set("text-align", "center")
                .set("flex", "1 1 auto");

        Span valueSpan = new Span(value != null ? value.toString() : "0");
        valueSpan.getStyle()
                .set("font-size", "24px")
                .set("font-weight", "bold");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-xs)");

        card.add(valueSpan, labelSpan);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        return card;
    }

    // =========================================================================
    // Audit Logs
    // =========================================================================
    private VerticalLayout buildAuditLogViewer() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.addClassName(LumoUtility.BorderRadius.LARGE);
        layout.addClassName(LumoUtility.Background.BASE);
        layout.getStyle().set("margin-top", "24px");

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        H3 auditTitle = new H3("Audit Logs");
        auditLoadingBar.setIndeterminate(true);
        auditLoadingBar.setVisible(false);
        auditLoadingBar.setWidth("200px");
        titleRow.add(auditTitle, auditLoadingBar);
        layout.add(titleRow);

        HorizontalLayout filterBar = new HorizontalLayout();
        filterBar.setWidthFull();
        filterBar.setAlignItems(FlexComponent.Alignment.END);
        filterBar.setSpacing(true);
        filterBar.getStyle().set("flex-wrap", "wrap");
        filterBar.addClassName("audit-filter-bar");

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

        auditGrid = new Grid<>();
        auditGrid.setWidthFull();
        auditGrid.setHeight("400px");
        auditGrid.setVisible(false);
        auditGrid.addClassName("audit-grid");
        auditGrid.addColumn(new ComponentRenderer<>(entry -> {
            LocalDateTime ts = entry.getTimestamp();
            String formatted = ts != null ? DateHelper.formatToHumanReadable(ts) : "-";
            return new Span(formatted);
        })).setHeader("Timestamp").setSortable(true).setResizable(true);
        auditGrid.addColumn(AuditLogResponse.LogEntry::getAction).setHeader("Action").setSortable(true).setResizable(true);
        auditGrid.addColumn(AuditLogResponse.LogEntry::getKeyId).setHeader("Key ID").setSortable(true).setResizable(true);
        auditGrid.addColumn(AuditLogResponse.LogEntry::getPrincipal).setHeader("Principal").setSortable(true).setResizable(true);
        auditGrid.addColumn(AuditLogResponse.LogEntry::getIpAddress).setHeader("IP Address").setResizable(true);
        auditGrid.addColumn(AuditLogResponse.LogEntry::getStatus).setHeader("Status").setResizable(true);
        auditGrid.addColumn(AuditLogResponse.LogEntry::getErrorMessage).setHeader("Error Message").setResizable(true);
        auditGrid.addColumn(AuditLogResponse.LogEntry::getExecutionTimeMs)
                .setHeader("Exec Time (ms)").setResizable(true);
        layout.add(auditGrid);

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

    private void loadAuditLogs() {
        KeyOption selected = auditKeyCombo.getValue();
        if (selected == null) {
            Notification.show("Please select a KMS key", 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        loadLogsButton.setEnabled(false);
        auditGrid.setVisible(false);
        paginationBar.setVisible(false);
        auditLoadingBar.setVisible(true);

        LocalDateTime from = fromDatePicker.getValue() != null ? fromDatePicker.getValue().atStartOfDay() : null;
        LocalDateTime to = toDatePicker.getValue() != null ? toDatePicker.getValue().atTime(LocalTime.MAX) : null;

        CompletableFuture.supplyAsync(() -> {
            List<AuditLogResponse.LogEntry> logs = new ArrayList<>();
            try {
                ResponseEntity<AuditLogResponse> response =
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
                auditLoadingBar.setVisible(false);
                loadLogsButton.setEnabled(true);
                if (logs.isEmpty()) {
                    Notification.show("No audit logs found for the selected criteria", 6000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_WARNING);
                } else {
                    Notification.show("Loaded " + logs.size() + " log entries", 6000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
            });
        }).exceptionally(ex -> {
            UI updateUi = ui != null ? ui : UI.getCurrent();
            if (updateUi != null) {
                updateUi.access(() -> {
                    Notification.show("Error loading audit logs", 6000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    auditLoadingBar.setVisible(false);
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
        List<AuditLogResponse.LogEntry> pageItems = allLogs.subList(start, end);
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
        Div actions = new Div();
        actions.add(new Span("• Create Key\n• Encrypt / Decrypt\n• Manage Aliases\n• Configure Policies\n• Manage Grants"));
        actions.getStyle().set("white-space", "pre-line");
        layout.add(title, actions);
        return layout;
    }

    // =========================================================================
    // Responsive CSS
    // =========================================================================
    private void injectResponsiveStyles() {
        String css = """
                .kms-dashboard .stat-card {
                    transition: transform 0.2s ease, box-shadow 0.2s ease;
                }
                .kms-dashboard .stat-card:hover {
                    transform: translateY(-4px);
                    box-shadow: var(--lumo-box-shadow-m);
                }
                .kms-dashboard .stats-filter-bar,
                .kms-dashboard .audit-filter-bar {
                    background: var(--lumo-base-color);
                    padding: var(--lumo-space-s);
                    border-radius: var(--lumo-border-radius-m);
                    margin-bottom: var(--lumo-space-s);
                }
                .kms-dashboard .audit-grid {
                    overflow-x: auto;
                }
                @media (max-width: 768px) {
                    .kms-dashboard .stats-filter-bar,
                    .kms-dashboard .audit-filter-bar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .kms-dashboard .stats-filter-bar > *,
                    .kms-dashboard .audit-filter-bar > * {
                        width: 100% !important;
                        margin-bottom: var(--lumo-space-xs);
                    }
                    .kms-dashboard .audit-grid .vaadin-grid-table {
                        min-width: 800px;
                    }
                    .kms-dashboard .stat-card {
                        flex-basis: calc(50% - 16px);
                    }
                    .kms-dashboard .stat-card .vaadin-button {
                        width: auto;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    // =========================================================================
    // Inner classes for statistics
    // =========================================================================
    private static class Stats {
        long totalKeys = 0, activeKeys = 0, disabledKeys = 0, pendingDeletion = 0, rotationEnabled = 0,
                symmetricKeys = 0, asymmetricKeys = 0, encryptUsage = 0, signUsage = 0, macUsage = 0,
                totalAliases = 0, totalGrants = 0, totalStores = 0;
        long nextCodeTotal = 0;
    }

    private static class TokenStats {
        long total = 0;
        long access = 0;
        long refresh = 0;
        long rstpwd = 0;
        long authority = 0;
    }

    private static class KeyOption {
        private final String keyId;
        private final String displayName;
        private final IEnumKeyUsage.Types keyUsage;

        KeyOption(String keyId, String aliasOrId, IEnumKeyUsage.Types keyUsage) {
            this.keyId = keyId;
            this.displayName = (aliasOrId != null && !aliasOrId.equals(keyId)) ? aliasOrId + " (" + keyId + ")" : keyId;
            this.keyUsage = keyUsage;
        }

        String getKeyId() {
            return keyId;
        }

        String getDisplayName() {
            return displayName;
        }

        IEnumKeyUsage.Types getKeyUsage() {
            return keyUsage;
        }
    }
}