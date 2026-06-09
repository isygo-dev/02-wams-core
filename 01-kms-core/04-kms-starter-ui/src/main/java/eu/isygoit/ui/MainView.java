package eu.isygoit.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.views.dashbord.AuditLogPanel;
import eu.isygoit.ui.views.dashbord.KeyStatisticsPanel;
import eu.isygoit.ui.views.dashbord.KeyUsageStatsPanel;
import eu.isygoit.ui.views.dashbord.TokenStatisticsPanel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@RouteAlias(value = "", layout = MainLayout.class)
@Route(value = "home", layout = MainLayout.class)
@PageTitle("KMS Dashboard")
public class MainView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final KmsTokenConfigService tokenConfigService;
    private final KmsAppNextCodeService nextCodeService;
    private final RandomKeyService randomKeyService;
    private final UI ui;

    private KeyStatisticsPanel keyStatsPanel;
    private TokenStatisticsPanel tokenStatsPanel;
    private KeyUsageStatsPanel usageStatsPanel;
    private AuditLogPanel auditLogPanel;

    @Autowired
    public MainView(KmsApiService kmsApiService,
                    KmsTokenConfigService tokenConfigService,
                    KmsAppNextCodeService nextCodeService,
                    RandomKeyService randomKeyService) {
        this.kmsApiService = kmsApiService;
        this.tokenConfigService = tokenConfigService;
        this.nextCodeService = nextCodeService;
        this.randomKeyService = randomKeyService;
        this.ui = UI.getCurrent();

        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-dashboard");

        add(buildHeader());
        keyStatsPanel = new KeyStatisticsPanel(kmsApiService, nextCodeService, randomKeyService, ui);
        add(keyStatsPanel);
        tokenStatsPanel = new TokenStatisticsPanel(tokenConfigService, ui);
        add(tokenStatsPanel);
        usageStatsPanel = new KeyUsageStatsPanel(kmsApiService, ui);
        add(usageStatsPanel);
        auditLogPanel = new AuditLogPanel(kmsApiService, ui);
        add(auditLogPanel);
        add(buildQuickLinks());

        injectResponsiveStyles();
        loadKeyOptions();
    }

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

    private H2 buildHeader() {
        H2 title = new H2("Key Management Service Dashboard");
        title.getStyle().set("margin-bottom", "10px");
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        return title;
    }

    private void loadKeyOptions() {
        try {
            var response = kmsApiService.listKeys(100, null);
            if (response.getBody() != null && response.getBody().getKeys() != null) {
                List<KeyUsageStatsPanel.KeyOption> options = response.getBody().getKeys().stream()
                        .map(entry -> {
                            String keyId = entry.getKeyId();
                            var usage = fetchKeyUsage(keyId);
                            String alias = fetchAlias(keyId);
                            return new KeyUsageStatsPanel.KeyOption(keyId, alias, usage);
                        })
                        .collect(Collectors.toList());
                usageStatsPanel.setKeyOptions(options);
                auditLogPanel.setKeyOptions(options);
            }
        } catch (Exception e) {
            // log error
        }
    }

    private eu.isygoit.enums.IEnumKeyUsage.Types fetchKeyUsage(String keyId) {
        try {
            var response = kmsApiService.describeKey(keyId);
            var desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null) {
                return desc.getKeyMetadata().getKeyUsage();
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    private String fetchAlias(String keyId) {
        try {
            var response = kmsApiService.describeKey(keyId);
            var desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null && desc.getKeyMetadata().getKeyAlias() != null) {
                return desc.getKeyMetadata().getKeyAlias();
            }
        } catch (Exception e) { /* ignore */ }
        return keyId;
    }

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
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    // Helper Span class for quick links
    private static class Span extends com.vaadin.flow.component.html.Span {
        public Span(String text) {
            super(text);
        }
    }
}