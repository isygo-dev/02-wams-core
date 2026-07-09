package eu.isygoit.ui.kms.views.dashbord;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
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

    // Status group
    private StatCard totalCard;
    private StatCard activeCard;
    private StatCard disabledCard;
    private StatCard pendingDeletionCard;
    // Type group
    private StatCard symmetricCard;
    private StatCard asymmetricCard;
    private StatCard rotationEnabledCard;
    // Usage group
    private StatCard encryptDecryptCard;
    private StatCard signVerifyCard;
    private StatCard macCard;
    // Resources group
    private StatCard aliasesCard;
    private StatCard grantsCard;
    private StatCard customStoresCard;
    private StatCard incrementalConfigsCard;
    private StatCard randomKeysCard;

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

        totalCard = new StatCard(VaadinIcon.KEY, StatCard.Variant.PRIMARY, I18n.t("kms.stats.key.total"), null, I18n.t("kms.stats.key.total.tooltip"));
        activeCard = new StatCard(VaadinIcon.CHECK_CIRCLE, StatCard.Variant.SUCCESS, I18n.t("kms.stats.key.active"), null, I18n.t("kms.stats.key.active.tooltip"));
        disabledCard = new StatCard(VaadinIcon.BAN, StatCard.Variant.NEUTRAL, I18n.t("kms.stats.key.disabled"), null, I18n.t("kms.stats.key.disabled.tooltip"));
        pendingDeletionCard = new StatCard(VaadinIcon.CLOCK, StatCard.Variant.WARNING, I18n.t("kms.stats.key.pending.deletion"), null, I18n.t("kms.stats.key.pending.deletion.tooltip"));

        symmetricCard = new StatCard(VaadinIcon.CIRCLE, StatCard.Variant.PRIMARY, I18n.t("kms.stats.key.symmetric"), null, I18n.t("kms.stats.key.symmetric.tooltip"));
        asymmetricCard = new StatCard(VaadinIcon.KEY_O, StatCard.Variant.PRIMARY, I18n.t("kms.stats.key.asymmetric"), null, I18n.t("kms.stats.key.asymmetric.tooltip"));
        rotationEnabledCard = new StatCard(VaadinIcon.ROTATE_RIGHT, StatCard.Variant.SUCCESS, I18n.t("kms.stats.key.rotation.enabled"), null, I18n.t("kms.stats.key.rotation.enabled.tooltip"));

        encryptDecryptCard = new StatCard(VaadinIcon.LOCK, StatCard.Variant.PRIMARY, I18n.t("kms.stats.key.encrypt.decrypt"), null, I18n.t("kms.stats.key.encrypt.decrypt.tooltip"));
        signVerifyCard = new StatCard(VaadinIcon.PENCIL, StatCard.Variant.PRIMARY, I18n.t("kms.stats.key.sign.verify"), null, I18n.t("kms.stats.key.sign.verify.tooltip"));
        macCard = new StatCard(VaadinIcon.SIGNAL, StatCard.Variant.PRIMARY, I18n.t("kms.stats.key.mac"), null, I18n.t("kms.stats.key.mac.tooltip"));

        aliasesCard = new StatCard(VaadinIcon.TAG, StatCard.Variant.NEUTRAL, I18n.t("kms.stats.key.aliases"), null, I18n.t("kms.stats.key.aliases.tooltip"));
        grantsCard = new StatCard(VaadinIcon.SHARE, StatCard.Variant.NEUTRAL, I18n.t("kms.stats.key.grants"), null, I18n.t("kms.stats.key.grants.tooltip"));
        customStoresCard = new StatCard(VaadinIcon.STORAGE, StatCard.Variant.NEUTRAL, I18n.t("kms.stats.key.custom.stores"), null, I18n.t("kms.stats.key.custom.stores.tooltip"));
        incrementalConfigsCard = new StatCard(VaadinIcon.CODE, StatCard.Variant.NEUTRAL, I18n.t("kms.stats.key.incremental.configs"), null, I18n.t("kms.stats.key.incremental.configs.tooltip"));
        randomKeysCard = new StatCard(VaadinIcon.RANDOM, StatCard.Variant.NEUTRAL, I18n.t("kms.stats.key.random.keys"), null, I18n.t("kms.stats.key.random.keys.tooltip"));

        VerticalLayout statsContainer = new VerticalLayout();
        statsContainer.setWidthFull();
        statsContainer.setSpacing(false);
        statsContainer.setPadding(false);
        statsContainer.addClassName("kms-parta-stats-groups");
        statsContainer.add(
                buildStatGroup(I18n.t("kms.stats.key.group.status"), totalCard, activeCard, disabledCard, pendingDeletionCard),
                buildStatGroup(I18n.t("kms.stats.key.group.type"), symmetricCard, asymmetricCard, rotationEnabledCard),
                buildStatGroup(I18n.t("kms.stats.key.group.usage"), encryptDecryptCard, signVerifyCard, macCard),
                buildStatGroup(I18n.t("kms.stats.key.group.resources"), aliasesCard, grantsCard, customStoresCard, incrementalConfigsCard, randomKeysCard)
        );
        add(statsContainer);
    }

    /**
     * Wraps a category of stat cards (e.g. "Status", "Type") in its own compact
     * sub-row with a small uppercase sub-header, so 15+ cards read as a few
     * scannable groups instead of one long undifferentiated wrapping row.
     */
    private VerticalLayout buildStatGroup(String groupTitle, StatCard... cards) {
        VerticalLayout group = new VerticalLayout();
        group.setSpacing(false);
        group.setPadding(false);
        group.addClassName("kms-parta-stats-group");

        Span header = new Span(groupTitle);
        header.addClassName("kms-parta-stats-group__title");
        group.add(header);

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.addClassName("kms-parta-stats-wrap");
        row.add(cards);
        group.add(row);

        return group;
    }

    public void loadStatistics() {
        ui.access(() -> {
            loadingBar.setVisible(true);
            refreshButton.setEnabled(false);

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

                totalCard.setValue(String.valueOf(stats.totalKeys));
                activeCard.setValue(String.valueOf(stats.activeKeys));
                disabledCard.setValue(String.valueOf(stats.disabledKeys));
                pendingDeletionCard.setValue(String.valueOf(stats.pendingDeletion));

                symmetricCard.setValue(String.valueOf(stats.symmetricKeys));
                asymmetricCard.setValue(String.valueOf(stats.asymmetricKeys));
                rotationEnabledCard.setValue(String.valueOf(stats.rotationEnabled));

                encryptDecryptCard.setValue(String.valueOf(stats.encryptUsage));
                signVerifyCard.setValue(String.valueOf(stats.signUsage));
                macCard.setValue(String.valueOf(stats.macUsage));

                aliasesCard.setValue(String.valueOf(stats.totalAliases));
                grantsCard.setValue(String.valueOf(stats.totalGrants));
                customStoresCard.setValue(String.valueOf(stats.totalStores));
                incrementalConfigsCard.setValue(String.valueOf(stats.nextCodeTotal));
                randomKeysCard.setValue(String.valueOf(stats.randomKeysTotal));

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