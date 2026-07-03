package eu.isygoit.ui.kms.views.cryptography.crypto;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.cryptography.crypto.panel.DataKeyPanel;
import eu.isygoit.ui.kms.views.cryptography.crypto.panel.EncryptDecryptPanel;
import eu.isygoit.ui.kms.views.cryptography.crypto.panel.MacPanel;
import eu.isygoit.ui.kms.views.cryptography.crypto.panel.SignVerifyPanel;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@VaadinSessionScope
@Route(value = "kms/crypto", layout = KmsMainLayout.class)
@PageTitle("Cryptographic Operations")
@PermitAll
public class CryptoOperationsView extends ManagementVerticalView {

    private final KmsApiService kmsApiService;
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>(I18n.t("kms.crypto.view.select.key"));
    private final Button refreshKeysButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final ProgressBar loadingBar = new ProgressBar();

    private final Tabs tabs;
    private final Tab encryptDecryptTabHeader;
    private final Tab signVerifyTabHeader;
    private final Tab dataKeyTabHeader;
    private final Tab macTabHeader;

    private final EncryptDecryptPanel encryptDecryptPanel;
    private final SignVerifyPanel signVerifyPanel;
    private final DataKeyPanel dataKeyPanel;
    private final MacPanel macPanel;

    private List<KeyOption> keyOptions = new ArrayList<>();
    private String selectedKeyId = null;
    private IEnumKeySpec.Types selectedKeySpec = null;
    private IEnumKeyUsage.Types selectedKeyUsage = null;

    @Autowired
    public CryptoOperationsView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-crypto-view");

        H2 header = new H2(I18n.t("kms.crypto.view.title"));
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        // Key selection toolbar
        HorizontalLayout keyLayout = createKeyLayout();
        add(keyLayout);

        // Loading indicator
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        // Initialize panels with the API service and a callback to get current key info
        encryptDecryptPanel = new EncryptDecryptPanel(kmsApiService, this::getSelectedKeyId, this::getSelectedKeySpec, this::getSelectedKeyUsage);
        signVerifyPanel = new SignVerifyPanel(kmsApiService, this::getSelectedKeyId, this::getSelectedKeySpec, this::getSelectedKeyUsage);
        dataKeyPanel = new DataKeyPanel(kmsApiService, this::getSelectedKeyId, this::getSelectedKeyUsage);
        macPanel = new MacPanel(kmsApiService, this::getSelectedKeyId, this::getSelectedKeySpec, this::getSelectedKeyUsage);

        // Create tabs
        tabs = new Tabs();
        encryptDecryptTabHeader = new Tab(I18n.t("kms.crypto.view.tab.encrypt.decrypt"));
        signVerifyTabHeader = new Tab(I18n.t("kms.crypto.view.tab.sign.verify"));
        dataKeyTabHeader = new Tab(I18n.t("kms.crypto.view.tab.data.keys"));
        macTabHeader = new Tab(I18n.t("kms.crypto.view.tab.mac"));
        tabs.add(encryptDecryptTabHeader, signVerifyTabHeader, dataKeyTabHeader, macTabHeader);
        tabs.setWidthFull();
        tabs.getStyle().set("overflow-x", "auto");
        tabs.getStyle().set("white-space", "nowrap");

        encryptDecryptPanel.setVisible(true);
        signVerifyPanel.setVisible(false);
        dataKeyPanel.setVisible(false);
        macPanel.setVisible(false);

        tabs.addSelectedChangeListener(event -> {
            Tab selected = event.getSelectedTab();
            boolean supported = isTabSupported(selected);
            if (!supported && selectedKeyId != null) {
                Notification.show(I18n.t("kms.crypto.view.key.not.supported"), 6000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
            encryptDecryptPanel.setVisible(selected == encryptDecryptTabHeader);
            signVerifyPanel.setVisible(selected == signVerifyTabHeader);
            dataKeyPanel.setVisible(selected == dataKeyTabHeader);
            macPanel.setVisible(selected == macTabHeader);
        });

        add(tabs, encryptDecryptPanel, signVerifyPanel, dataKeyPanel, macPanel);

        injectResponsiveStyles();
        loadKeyOptions();
    }

    private HorizontalLayout createKeyLayout() {
        HorizontalLayout keyLayout = new HorizontalLayout();
        keyLayout.setWidthFull();
        keyLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        keyLayout.setSpacing(true);
        keyLayout.getStyle().set("flex-wrap", "wrap");
        keyLayout.addClassName("crypto-key-layout");

        keyCombo.setPlaceholder(I18n.t("kms.crypto.view.select.key"));
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("400px");
        keyCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                selectedKeyId = e.getValue().getKeyId();
                loadKeyMetadata();
            } else {
                selectedKeyId = null;
                selectedKeySpec = null;
                selectedKeyUsage = null;
                updatePanels();
                updateTabBasedOnKey();
            }
        });

        refreshKeysButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshKeysButton.setTooltipText(I18n.t("kms.crypto.view.refresh.tooltip"));
        refreshKeysButton.addClickListener(e -> loadKeyOptions());

        keyLayout.add(keyCombo, refreshKeysButton);
        return keyLayout;
    }

    private void injectResponsiveStyles() {
        String css = """
                .kms-crypto-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .crypto-key-layout {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    align-items: center;
                }
                @media (max-width: 768px) {
                    .crypto-key-layout {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .crypto-key-layout > * {
                        width: 100% !important;
                    }
                    .crypto-key-layout .vaadin-combo-box {
                        width: 100% !important;
                    }
                    .crypto-panel .vaadin-combo-box,
                    .crypto-panel .vaadin-text-field,
                    .crypto-panel .vaadin-text-area {
                        width: 100% !important;
                    }
                    .crypto-button-row {
                        flex-direction: column;
                        width: 100%;
                    }
                    .crypto-button-row > * {
                        width: 100% !important;
                        margin-bottom: var(--lumo-space-xs);
                    }
                    .crypto-data-key-panel .vaadin-combo-box,
                    .crypto-data-key-panel .vaadin-text-field {
                        width: 100% !important;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    private void loadKeyOptions() {
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.ListKeysResponse> response = kmsApiService.listKeys(100, null);
            KmsDtos.ListKeysResponse keys = response.getBody();
            if (keys != null && keys.getKeys() != null) {
                keyOptions = keys.getKeys().stream()
                        .map(entry -> new KeyOption(entry.getKeyId(), fetchAlias(entry.getKeyId())))
                        .collect(Collectors.toList());
                keyCombo.setItems(keyOptions);
            } else {
                keyOptions = new ArrayList<>();
                keyCombo.setItems(keyOptions);
            }
            if (selectedKeyId != null && keyOptions.stream().noneMatch(opt -> opt.getKeyId().equals(selectedKeyId))) {
                selectedKeyId = null;
                keyCombo.clear();
                updatePanels();
                updateTabBasedOnKey();
            }
        } catch (FeignException e) {
            Notification.show(I18n.t("kms.crypto.view.load.keys.error", (e.status() == 500 ? e.contentUTF8() : e.getMessage())))
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load keys: {}", e.getMessage());
        } catch (Exception e) {
            Notification.show(I18n.t("kms.crypto.view.load.keys.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load keys", e);
        } finally {
            showLoading(false);
        }
    }

    private String fetchAlias(String keyId) {
        try {
            ResponseEntity<KmsDtos.DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
            KmsDtos.DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null && StringUtils.hasText(desc.getKeyMetadata().getKeyAlias())) {
                return desc.getKeyMetadata().getKeyAlias();
            }
        } catch (FeignException e) {
            Notification.show(I18n.t("kms.crypto.view.fetch.alias.failed", (e.status() == 500 ? e.contentUTF8() : e.getMessage())));
            log.error("Failed to fetch alias for keyId: {}", keyId, e);
        } catch (Exception e) {
            Notification.show(I18n.t("kms.crypto.view.fetch.alias.failed", e.getMessage()));
            log.error("Failed to fetch alias for keyId: {}", keyId, e);
        }
        return keyId;
    }

    private void loadKeyMetadata() {
        if (selectedKeyId == null) return;
        try {
            ResponseEntity<KmsDtos.DescribeKeyResponse> response = kmsApiService.describeKey(selectedKeyId);
            KmsDtos.DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null) {
                selectedKeySpec = desc.getKeyMetadata().getKeySpec();
                selectedKeyUsage = desc.getKeyMetadata().getKeyUsage();
                updatePanels();
                updateTabBasedOnKey();
            }
        } catch (FeignException e) {
            Notification.show(I18n.t("kms.crypto.view.load.metadata.failed") + ": " + (e.status() == 500 ? e.contentUTF8() : e.getMessage()))
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load key metadata for keyId: {}", selectedKeyId, e);
        } catch (Exception e) {
            Notification.show(I18n.t("kms.crypto.view.load.metadata.failed"), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            log.error("Failed to load key metadata for keyId: {}", selectedKeyId, e);
        }
    }

    private void updatePanels() {
        encryptDecryptPanel.setKeyInfo(selectedKeyId, selectedKeySpec, selectedKeyUsage);
        signVerifyPanel.setKeyInfo(selectedKeyId, selectedKeySpec, selectedKeyUsage);
        dataKeyPanel.setKeyInfo(selectedKeyId, selectedKeyUsage);
        macPanel.setKeyInfo(selectedKeyId, selectedKeySpec, selectedKeyUsage);
    }

    private void updateTabBasedOnKey() {
        if (selectedKeyId == null) return;
        Tab current = tabs.getSelectedTab();
        if (!isTabSupported(current)) {
            Tab supported = getFirstSupportedTab();
            if (supported != null) {
                tabs.setSelectedTab(supported);
                Notification.show(I18n.t("kms.crypto.view.switched.supported"), 6000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
            } else {
                Notification.show(I18n.t("kms.crypto.view.no.operation.supported"), 6000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }

    private boolean isTabSupported(Tab tab) {
        if (tab == encryptDecryptTabHeader) return selectedKeyUsage == IEnumKeyUsage.Types.ENCRYPT_DECRYPT;
        if (tab == signVerifyTabHeader) return selectedKeyUsage == IEnumKeyUsage.Types.SIGN_VERIFY;
        if (tab == dataKeyTabHeader) return selectedKeyUsage == IEnumKeyUsage.Types.ENCRYPT_DECRYPT;
        if (tab == macTabHeader) return selectedKeyUsage == IEnumKeyUsage.Types.GENERATE_VERIFY_MAC;
        return false;
    }

    private Tab getFirstSupportedTab() {
        if (selectedKeyUsage == IEnumKeyUsage.Types.ENCRYPT_DECRYPT) return encryptDecryptTabHeader;
        if (selectedKeyUsage == IEnumKeyUsage.Types.SIGN_VERIFY) return signVerifyTabHeader;
        if (selectedKeyUsage == IEnumKeyUsage.Types.GENERATE_VERIFY_MAC) return macTabHeader;
        return null;
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        keyCombo.setEnabled(!show);
        refreshKeysButton.setEnabled(!show);
    }

    private String getSelectedKeyId() {
        return selectedKeyId;
    }

    private IEnumKeySpec.Types getSelectedKeySpec() {
        return selectedKeySpec;
    }

    private IEnumKeyUsage.Types getSelectedKeyUsage() {
        return selectedKeyUsage;
    }

    private static class KeyOption {
        private final String keyId;
        private final String displayName;

        KeyOption(String keyId, String aliasOrId) {
            this.keyId = keyId;
            this.displayName = aliasOrId != null ? aliasOrId + " (" + keyId + ")" : keyId;
        }

        String getKeyId() {
            return keyId;
        }

        String getDisplayName() {
            return displayName;
        }
    }
}