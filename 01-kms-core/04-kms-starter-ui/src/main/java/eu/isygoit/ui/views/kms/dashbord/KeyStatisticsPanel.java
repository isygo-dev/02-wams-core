package eu.isygoit.ui.views.kms.dashbord;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.common.RandomKeyDto;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.remote.kms.RandomKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class KeyStatisticsPanel extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(KeyStatisticsPanel.class);

    private final KmsApiService kmsApiService;
    private final KmsAppNextCodeService nextCodeService;
    private final RandomKeyService randomKeyService;
    private final UI ui;
    private final ProgressBar loadingBar = new ProgressBar();
    private final Button refreshButton = new Button("Refresh Stats", VaadinIcon.REFRESH.create());
    private HorizontalLayout statsContainer;

    public KeyStatisticsPanel(KmsApiService kmsApiService,
                              KmsAppNextCodeService nextCodeService,
                              RandomKeyService randomKeyService,
                              UI ui) {
        this.kmsApiService = kmsApiService;
        this.nextCodeService = nextCodeService;
        this.randomKeyService = randomKeyService;
        this.ui = ui;
        buildUI();
        loadStatistics();
    }

    private void buildUI() {
        setSpacing(true);
        setPadding(false);
        setWidthFull();

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        H3 statsTitle = new H3("Key Statistics");
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(e -> loadStatistics());
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        titleRow.add(statsTitle, refreshButton, loadingBar);
        add(titleRow);

        statsContainer = new HorizontalLayout();
        statsContainer.setWidthFull();
        statsContainer.setSpacing(true);
        statsContainer.getStyle().set("flex-wrap", "wrap").set("gap", "16px");
        add(statsContainer);
    }

    public void loadStatistics() {
        loadingBar.setVisible(true);
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
                // Incremental Key configs
                try {
                    ResponseEntity<PaginatedResponseDto<NextCodeDto>> nextCodeResp = nextCodeService.findAll(0, 1);
                    PaginatedResponseDto<NextCodeDto> nextCodeBody = nextCodeResp.getBody();
                    if (nextCodeBody != null) stats.nextCodeTotal = nextCodeBody.getTotalElements();
                } catch (Exception e) {
                    log.error("Failed to load Incremental Key statistics", e);
                }
                // Random Keys
                try {
                    ResponseEntity<PaginatedResponseDto<RandomKeyDto>> randomResp = randomKeyService.listRandomKeys(0, 1);
                    PaginatedResponseDto<RandomKeyDto> randomBody = randomResp.getBody();
                    if (randomBody != null) stats.randomKeysTotal = randomBody.getTotalElements();
                } catch (Exception e) {
                    log.error("Failed to load Random Keys statistics", e);
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
                        new StatCard("Total Keys", String.valueOf(stats.totalKeys), VaadinIcon.KEY, "#1E88E5", "Total number of KMS keys in your account"),
                        new StatCard("Active Keys", String.valueOf(stats.activeKeys), VaadinIcon.CHECK_CIRCLE, "#2E7D32", "Keys that are enabled and usable for cryptographic operations"),
                        new StatCard("Disabled Keys", String.valueOf(stats.disabledKeys), VaadinIcon.BAN, "#D32F2F", "Keys that are disabled and cannot be used"),
                        new StatCard("Pending Deletion", String.valueOf(stats.pendingDeletion), VaadinIcon.CLOCK, "#F57C00", "Keys that have been scheduled for deletion"),
                        new StatCard("Rotation Enabled", String.valueOf(stats.rotationEnabled), VaadinIcon.ROTATE_RIGHT, "#8E24AA", "Keys with automatic key rotation enabled"),
                        new StatCard("Symmetric Keys", String.valueOf(stats.symmetricKeys), VaadinIcon.CIRCLE, "#43A047", "AES or HMAC keys (same key for encryption and decryption)"),
                        new StatCard("Asymmetric Keys", String.valueOf(stats.asymmetricKeys), VaadinIcon.LOCK, "#FB8C00", "RSA or EC keys (different keys for signing/verification)"),
                        new StatCard("Encrypt/Decrypt Keys", String.valueOf(stats.encryptUsage), VaadinIcon.LOCK, "#1E88E5", "Keys intended for encryption and decryption operations"),
                        new StatCard("Sign/Verify Keys", String.valueOf(stats.signUsage), VaadinIcon.PENCIL, "#8E24AA", "Keys intended for digital signature and verification"),
                        new StatCard("MAC Keys", String.valueOf(stats.macUsage), VaadinIcon.SIGNAL, "#D81B60", "Keys used for Message Authentication Code (HMAC) operations"),
                        new StatCard("Aliases", String.valueOf(stats.totalAliases), VaadinIcon.TAG, "#00ACC1", "Total number of friendly aliases pointing to keys"),
                        new StatCard("Grants", String.valueOf(stats.totalGrants), VaadinIcon.SHARE, "#546E7A", "Total number of access grants across all keys"),
                        new StatCard("Custom Key Stores", String.valueOf(stats.totalStores), VaadinIcon.STORAGE, "#37474F", "Number of configured custom key stores (CloudHSM or external)"),
                        new StatCard("Incremental Key Configs", String.valueOf(stats.nextCodeTotal), VaadinIcon.CODE, "#607D8B", "Number of incremental code generator configurations"),
                        new StatCard("Random Keys", String.valueOf(stats.randomKeysTotal), VaadinIcon.RANDOM, "#8E24AA", "Number of stored random key values")
                );
                loadingBar.setVisible(false);
                refreshButton.setEnabled(true);
                updateUi.push();
            });
        });
    }

    private static class Stats {
        long totalKeys = 0, activeKeys = 0, disabledKeys = 0, pendingDeletion = 0, rotationEnabled = 0,
                symmetricKeys = 0, asymmetricKeys = 0, encryptUsage = 0, signUsage = 0, macUsage = 0,
                totalAliases = 0, totalGrants = 0, totalStores = 0, nextCodeTotal = 0, randomKeysTotal = 0;
    }
}