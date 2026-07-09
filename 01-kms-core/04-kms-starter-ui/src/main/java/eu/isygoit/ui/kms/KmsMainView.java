package eu.isygoit.ui.kms;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.common.component.DashboardShortcutsBar;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.dashbord.AuditLogPanel;
import eu.isygoit.ui.kms.views.dashbord.KeyStatisticsPanel;
import eu.isygoit.ui.kms.views.dashbord.KeyUsageStatsPanel;
import eu.isygoit.ui.kms.views.dashbord.TokenStatisticsPanel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@RouteAlias(value = "kms/home", layout = KmsMainLayout.class)
@UIScope
@Route(value = "kms", layout = KmsMainLayout.class)
@PageTitle("KMS Dashboard")
public class KmsMainView extends ManagementVerticalView {

    private final KmsApiService kmsApiService;
    private final KmsTokenConfigService tokenConfigService;
    private final KmsAppNextCodeService nextCodeService;
    private final RandomKeyService randomKeyService;
    private final UI ui;

    private final KeyStatisticsPanel keyStatsPanel;
    private final TokenStatisticsPanel tokenStatsPanel;
    private final KeyUsageStatsPanel usageStatsPanel;
    private final AuditLogPanel auditLogPanel;

    @Autowired
    public KmsMainView(KmsApiService kmsApiService,
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

        add(buildShortcutsBar());
        add(buildHeader());
        keyStatsPanel = new KeyStatisticsPanel(kmsApiService, nextCodeService, randomKeyService, ui);
        add(keyStatsPanel);
        tokenStatsPanel = new TokenStatisticsPanel(tokenConfigService, ui);
        add(tokenStatsPanel);
        usageStatsPanel = new KeyUsageStatsPanel(kmsApiService, ui);
        add(usageStatsPanel);
        auditLogPanel = new AuditLogPanel(kmsApiService, ui);
        add(auditLogPanel);

        loadKeyOptions();
    }

    public static Button createCopyButton(VaadinIcon icon, String textToCopy, String tooltip) {
        Button btn = new Button(new Icon(icon));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        btn.setTooltipText(tooltip);
        btn.setWidth("20px");
        btn.setHeight("20px");
        btn.addClickListener(e -> copyToClipboard(textToCopy, I18n.t("kms.dashboard.copied", textToCopy)));
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
        H2 title = new H2(I18n.t("kms.dashboard.title"));
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        title.addClassName("kms-parta-dashboard-title");
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

    private DashboardShortcutsBar buildShortcutsBar() {
        return new DashboardShortcutsBar(I18n.t("kms.dashboard.quick.actions"), List.of(
                new DashboardShortcutsBar.Shortcut(VaadinIcon.KEY,
                        I18n.t("kms.dashboard.quick.actions.create.key"),
                        () -> UI.getCurrent().navigate("kms/keys")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.LOCK,
                        I18n.t("kms.dashboard.quick.actions.encrypt.decrypt"),
                        () -> UI.getCurrent().navigate("kms/crypto")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.LINK,
                        I18n.t("kms.dashboard.quick.actions.manage.aliases"),
                        () -> UI.getCurrent().navigate("kms/aliases")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.DIPLOMA,
                        I18n.t("kms.dashboard.quick.actions.configure.policies"),
                        () -> UI.getCurrent().navigate("kms/policies")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.SHIELD,
                        I18n.t("kms.dashboard.quick.actions.manage.grants"),
                        () -> UI.getCurrent().navigate("kms/grants"))
        ));
    }
}