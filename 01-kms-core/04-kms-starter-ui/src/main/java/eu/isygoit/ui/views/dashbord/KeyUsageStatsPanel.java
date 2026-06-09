package eu.isygoit.ui.views.dashbord;

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
import eu.isygoit.remote.kms.KmsApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        getStyle().set("margin-top", "24px");

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        H3 title = new H3("Key Usage Statistics");
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

        keyCombo = new ComboBox<>("Select Key");
        keyCombo.setPlaceholder("Choose a key");
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("300px");
        keyCombo.setRequired(true);

        loadButton = new Button("Load Usage Stats", VaadinIcon.CHART.create());
        loadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loadButton.addClickListener(e -> loadUsageStats());

        filterBar.add(keyCombo, loadButton);
        add(filterBar);

        statsContainer = new HorizontalLayout();
        statsContainer.setWidthFull();
        statsContainer.setSpacing(true);
        statsContainer.getStyle().set("flex-wrap", "wrap").set("gap", "16px");
        statsContainer.setVisible(false);
        add(statsContainer);
    }

    public void setKeyOptions(List<KeyOption> options) {
        keyCombo.setItems(options);
    }

    private void loadUsageStats() {
        KeyOption selected = keyCombo.getValue();
        if (selected == null) {
            Notification.show("Please select a key", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        statsContainer.setVisible(false);
        loadingBar.setVisible(true);
        loadButton.setEnabled(false);

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
                return (body != null && body.getRotations() != null) ? body.getRotations().size() : 0;
            } catch (Exception e) {
                log.error("Failed to load key versions for key {}", keyId, e);
                return 0;
            }
        });

        CompletableFuture.allOf(statsFuture, versionsFuture)
                .thenAccept(v -> {
                    KeyUsageStatsResponse stats = statsFuture.join();
                    Integer versionCount = versionsFuture.join();
                    UI updateUi = ui != null ? ui : UI.getCurrent();
                    if (updateUi == null) return;
                    updateUi.access(() -> {
                        loadingBar.setVisible(false);
                        loadButton.setEnabled(true);
                        statsContainer.removeAll();
                        if (stats == null) {
                            Span errorSpan = new Span("Failed to load statistics. Check server logs.");
                            errorSpan.getStyle().set("color", "var(--lumo-error-text-color)");
                            statsContainer.add(errorSpan);
                        } else {
                            if (keyUsage == IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
                                statsContainer.add(createSmallStatCard("Encrypts", stats.getEncryptCount()));
                                statsContainer.add(createSmallStatCard("Decrypts", stats.getDecryptCount()));
                                statsContainer.add(createSmallStatCard("Generate Data Keys", stats.getGenerateDataKeyCount()));
                                statsContainer.add(createSmallStatCard("Re-Encrypts", stats.getReEncryptCount()));
                            } else if (keyUsage == IEnumKeyUsage.Types.SIGN_VERIFY) {
                                statsContainer.add(createSmallStatCard("Signs", stats.getSignCount()));
                                statsContainer.add(createSmallStatCard("Verifies", stats.getVerifyCount()));
                            } else if (keyUsage == IEnumKeyUsage.Types.GENERATE_VERIFY_MAC) {
                                long gen = stats.getGenerateMacCount() != null ? stats.getGenerateMacCount() : 0L;
                                long ver = stats.getVerifyMacCount() != null ? stats.getVerifyMacCount() : 0L;
                                statsContainer.add(createSmallStatCard("Generate MAC", gen));
                                statsContainer.add(createSmallStatCard("Verify MAC", ver));
                            }
                            statsContainer.add(createSmallStatCard("Key Versions", versionCount));
                            if (stats.getLastUsedDate() != null) {
                                statsContainer.add(createSmallStatCard("Last Used", DateHelper.formatToHumanReadable(stats.getLastUsedDate())));
                            }
                        }
                        statsContainer.setVisible(true);
                    });
                })
                .exceptionally(ex -> {
                    UI updateUi = ui != null ? ui : UI.getCurrent();
                    if (updateUi != null) {
                        updateUi.access(() -> {
                            loadingBar.setVisible(false);
                            loadButton.setEnabled(true);
                            statsContainer.removeAll();
                            statsContainer.add(new Span("Error loading statistics"));
                            statsContainer.setVisible(true);
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
        valueSpan.getStyle().set("font-size", "24px").set("font-weight", "bold");

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-xs)");

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