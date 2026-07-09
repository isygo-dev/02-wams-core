package eu.isygoit.ui.kms.views.dashbord;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.common.component.StatCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

public class KeyStatisticsPanel extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(KeyStatisticsPanel.class);

    private final KmsApiService kmsApiService;
    private final KmsAppNextCodeService nextCodeService;
    private final RandomKeyService randomKeyService;
    private final UI ui;
    private final ProgressBar loadingBar = new ProgressBar();
    private final Button refreshButton = new Button(I18n.t("kms.stats.key.refresh.button"), VaadinIcon.REFRESH.create());
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
        H3 statsTitle = new H3(I18n.t("kms.stats.key.title"));
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
        statsContainer.addClassName("kms-parta-stats-wrap");
        add(statsContainer);
    }

    public void loadStatistics() {
        ui.access(() -> {
            loadingBar.setVisible(true);
            refreshButton.setEnabled(false);
            statsContainer.removeAll();

            try {
                log.info("Starting KMS statistics collection...");
                Stats stats = new Stats();

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

                // Build stat cards
                statsContainer.add(
                        new StatCard(I18n.t("kms.stats.key.total"), String.valueOf(stats.totalKeys), VaadinIcon.KEY, "#1E88E5", I18n.t("kms.stats.key.total.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.active"), String.valueOf(stats.activeKeys), VaadinIcon.CHECK_CIRCLE, "#2E7D32", I18n.t("kms.stats.key.active.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.disabled"), String.valueOf(stats.disabledKeys), VaadinIcon.BAN, "#D32F2F", I18n.t("kms.stats.key.disabled.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.pending.deletion"), String.valueOf(stats.pendingDeletion), VaadinIcon.CLOCK, "#F57C00", I18n.t("kms.stats.key.pending.deletion.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.rotation.enabled"), String.valueOf(stats.rotationEnabled), VaadinIcon.ROTATE_RIGHT, "#8E24AA", I18n.t("kms.stats.key.rotation.enabled.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.symmetric"), String.valueOf(stats.symmetricKeys), VaadinIcon.CIRCLE, "#43A047", I18n.t("kms.stats.key.symmetric.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.asymmetric"), String.valueOf(stats.asymmetricKeys), VaadinIcon.KEY_O, "#FB8C00", I18n.t("kms.stats.key.asymmetric.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.encrypt.decrypt"), String.valueOf(stats.encryptUsage), VaadinIcon.LOCK, "#1E88E5", I18n.t("kms.stats.key.encrypt.decrypt.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.sign.verify"), String.valueOf(stats.signUsage), VaadinIcon.PENCIL, "#8E24AA", I18n.t("kms.stats.key.sign.verify.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.mac"), String.valueOf(stats.macUsage), VaadinIcon.SIGNAL, "#D81B60", I18n.t("kms.stats.key.mac.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.aliases"), String.valueOf(stats.totalAliases), VaadinIcon.TAG, "#00ACC1", I18n.t("kms.stats.key.aliases.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.grants"), String.valueOf(stats.totalGrants), VaadinIcon.SHARE, "#546E7A", I18n.t("kms.stats.key.grants.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.custom.stores"), String.valueOf(stats.totalStores), VaadinIcon.STORAGE, "#37474F", I18n.t("kms.stats.key.custom.stores.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.incremental.configs"), String.valueOf(stats.nextCodeTotal), VaadinIcon.CODE, "#607D8B", I18n.t("kms.stats.key.incremental.configs.tooltip")),
                        new StatCard(I18n.t("kms.stats.key.random.keys"), String.valueOf(stats.randomKeysTotal), VaadinIcon.RANDOM, "#8E24AA", I18n.t("kms.stats.key.random.keys.tooltip"))
                );

                loadingBar.setVisible(false);
                refreshButton.setEnabled(true);
                ui.push();

            } catch (Exception ex) {
                log.error("Error in statistics collection", ex);
                loadingBar.setVisible(false);
                refreshButton.setEnabled(true);
                Notification.show(I18n.t("kms.stats.key.load.error"), 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
    }

    private static class Stats {
        long totalKeys = 0, activeKeys = 0, disabledKeys = 0, pendingDeletion = 0, rotationEnabled = 0,
                symmetricKeys = 0, asymmetricKeys = 0, encryptUsage = 0, signUsage = 0, macUsage = 0,
                totalAliases = 0, totalGrants = 0, totalStores = 0, nextCodeTotal = 0, randomKeysTotal = 0;
    }
}