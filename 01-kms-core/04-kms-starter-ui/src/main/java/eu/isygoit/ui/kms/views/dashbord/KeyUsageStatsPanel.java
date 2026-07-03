package eu.isygoit.ui.kms.views.dashbord;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import eu.isygoit.dto.KmsDtos.KeyUsageStatsResponse;
import eu.isygoit.dto.KmsDtos.ListKeyRotationsResponse;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class KeyUsageStatsPanel extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(KeyUsageStatsPanel.class);

    private final KmsApiService kmsApiService;
    private final UI ui;
    private final ProgressBar loadingBar = new ProgressBar();
    private ComboBox<KeyOption> keyCombo;
    private Button loadButton;
    private HorizontalLayout statsContainer;

    public KeyUsageStatsPanel(KmsApiService kmsApiService, UI ui) {
        this.kmsApiService = kmsApiService;
        this.ui = ui;
        buildUI();
    }

    private void buildUI() {
        setSpacing(true);
        setPadding(true);
        addClassName("stats-filter-bar");
        addClassName("kms-parta-panel-spaced");

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        H3 title = new H3(I18n.t("kms.stats.key.usage.title"));
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        titleRow.add(title, loadingBar);
        add(titleRow);

        HorizontalLayout filterBar = new HorizontalLayout();
        filterBar.setWidthFull();
        filterBar.setAlignItems(FlexComponent.Alignment.END);
        filterBar.setSpacing(true);
        filterBar.addClassName("kms-parta-filter-bar-wrap");

        keyCombo = new ComboBox<>(I18n.t("kms.stats.key.usage.select.key"));
        keyCombo.setPlaceholder(I18n.t("kms.stats.key.usage.choose.key"));
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("300px");
        keyCombo.setRequired(true);

        loadButton = new Button(I18n.t("kms.stats.key.usage.load.button"), VaadinIcon.CHART.create());
        loadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loadButton.addClickListener(e -> loadUsageStats());

        filterBar.add(keyCombo, loadButton);
        add(filterBar);

        statsContainer = new HorizontalLayout();
        statsContainer.setWidthFull();
        statsContainer.setSpacing(true);
        statsContainer.addClassName("kms-parta-stats-wrap");
        statsContainer.setVisible(false);
        add(statsContainer);
    }

    public void setKeyOptions(List<KeyOption> options) {
        keyCombo.setItems(options);
    }

    private void loadUsageStats() {
        KeyOption selected = keyCombo.getValue();
        if (selected == null) {
            Notification.show(I18n.t("kms.stats.key.usage.select.key.warning"), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        ui.access(() -> {
            statsContainer.setVisible(false);
            loadingBar.setVisible(true);
            loadButton.setEnabled(false);

            try {
                String keyId = selected.getKeyId();
                IEnumKeyUsage.Types keyUsage = selected.getKeyUsage();

                ResponseEntity<KeyUsageStatsResponse> statsResponse = kmsApiService.getKeyUsageStats(keyId);
                KeyUsageStatsResponse stats = statsResponse.getBody();

                ResponseEntity<ListKeyRotationsResponse> rotationsResponse = kmsApiService.listKeyRotations(keyId, 1000, null);
                ListKeyRotationsResponse rotationsBody = rotationsResponse.getBody();
                int versionCount = (rotationsBody != null && rotationsBody.getRotations() != null)
                        ? rotationsBody.getRotations().size()
                        : 0;

                statsContainer.removeAll();

                if (stats == null) {
                    Span errorSpan = new Span(I18n.t("kms.stats.key.usage.failed.load"));
                    errorSpan.addClassName("kms-parta-usage-stats-error");
                    statsContainer.add(errorSpan);
                } else {
                    if (keyUsage == IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
                        statsContainer.add(createSmallStatCard(I18n.t("kms.stats.key.usage.encrypts"), stats.getEncryptCount()));
                        statsContainer.add(createSmallStatCard(I18n.t("kms.stats.key.usage.decrypts"), stats.getDecryptCount()));
                        statsContainer.add(createSmallStatCard(I18n.t("kms.stats.key.usage.generate.data.keys"), stats.getGenerateDataKeyCount()));
                        statsContainer.add(createSmallStatCard(I18n.t("kms.stats.key.usage.reencrypts"), stats.getReEncryptCount()));
                    } else if (keyUsage == IEnumKeyUsage.Types.SIGN_VERIFY) {
                        statsContainer.add(createSmallStatCard(I18n.t("kms.stats.key.usage.signs"), stats.getSignCount()));
                        statsContainer.add(createSmallStatCard(I18n.t("kms.stats.key.usage.verifies"), stats.getVerifyCount()));
                    } else if (keyUsage == IEnumKeyUsage.Types.GENERATE_VERIFY_MAC) {
                        long gen = stats.getGenerateMacCount() != null ? stats.getGenerateMacCount() : 0L;
                        long ver = stats.getVerifyMacCount() != null ? stats.getVerifyMacCount() : 0L;
                        statsContainer.add(createSmallStatCard(I18n.t("kms.stats.key.usage.generate.mac"), gen));
                        statsContainer.add(createSmallStatCard(I18n.t("kms.stats.key.usage.verify.mac"), ver));
                    }
                    statsContainer.add(createSmallStatCard(I18n.t("kms.stats.key.usage.key.versions"), versionCount));
                    if (stats.getLastUsedDate() != null) {
                        statsContainer.add(createSmallStatCard(I18n.t("kms.stats.key.usage.last.used"), DateHelper.formatToHumanReadable(stats.getLastUsedDate())));
                    }
                }

                loadingBar.setVisible(false);
                loadButton.setEnabled(true);
                statsContainer.setVisible(true);

            } catch (Exception ex) {
                log.error("Failed to load usage stats", ex);
                loadingBar.setVisible(false);
                loadButton.setEnabled(true);
                statsContainer.removeAll();
                statsContainer.add(new Span(I18n.t("kms.stats.key.usage.load.error")));
                statsContainer.setVisible(true);
                Notification.show(I18n.t("kms.stats.key.usage.load.error"), 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
    }

    private VerticalLayout createSmallStatCard(String label, Object value) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.addClassName("kms-parta-usage-stat-card");

        Span valueSpan = new Span(value != null ? value.toString() : "0");
        valueSpan.addClassName("kms-parta-usage-stat-card__value");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("kms-parta-usage-stat-card__label");

        card.add(valueSpan, labelSpan);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        return card;
    }

    public static class KeyOption {
        private final String keyId;
        private final String displayName;
        private final IEnumKeyUsage.Types keyUsage;

        public KeyOption(String keyId, String aliasOrId, IEnumKeyUsage.Types keyUsage) {
            this.keyId = keyId;
            this.displayName = (aliasOrId != null && !aliasOrId.equals(keyId)) ? aliasOrId + " (" + keyId + ")" : keyId;
            this.keyUsage = keyUsage;
        }

        public String getKeyId() {
            return keyId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public IEnumKeyUsage.Types getKeyUsage() {
            return keyUsage;
        }
    }
}