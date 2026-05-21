package eu.isygoit.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
        add(buildQuickLinks());

        showPlaceholderCards();
        loadStatistics();
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
                    ResponseEntity<KmsDtos.ListCustomKeyStoresResponse> storesResp = kmsApiService.listCustomKeyStores(100, null, null, null);
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
                    // Replace all cards with real statistics
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
                    // Push changes to the client immediately
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
}