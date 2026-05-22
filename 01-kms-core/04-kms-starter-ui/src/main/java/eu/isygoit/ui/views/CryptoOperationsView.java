package eu.isygoit.ui.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.*;
import eu.isygoit.mapper.AlgorithmMapper;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainLayout;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Route(value = "crypto", layout = MainLayout.class)
@PageTitle("Cryptographic Operations")
@PermitAll
public class CryptoOperationsView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>("KMS Key");
    private final Button refreshKeysButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final ProgressBar loadingBar = new ProgressBar();

    // Tabs
    private final Tabs tabs;
    private final Tab encryptDecryptTabHeader;
    private final Tab signVerifyTabHeader;
    private final Tab dataKeyTabHeader;
    private final Tab macTabHeader;
    private final VerticalLayout encryptDecryptTab;
    private final VerticalLayout signVerifyTab;
    private final VerticalLayout dataKeyTab;
    private final VerticalLayout macTab;

    // Dynamic combo boxes
    private ComboBox<String> algorithmCombo;
    private ComboBox<String> signAlgoCombo;
    private ComboBox<String> macAlgoCombo;

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

        H2 header = new H2("Cryptographic Operations");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        // Key selection toolbar
        HorizontalLayout keyLayout = new HorizontalLayout();
        keyLayout.setWidthFull();
        keyLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        keyLayout.setSpacing(true);
        keyLayout.getStyle().set("flex-wrap", "wrap");
        keyLayout.addClassName("crypto-key-layout");

        keyCombo.setPlaceholder("Select a KMS key...");
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
                updateAlgorithmCombos();
                updateTabBasedOnKey();
            }
        });

        refreshKeysButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshKeysButton.setTooltipText("Refresh key list");
        refreshKeysButton.addClickListener(e -> loadKeyOptions());

        keyLayout.add(keyCombo, refreshKeysButton);
        add(keyLayout);

        // Loading indicator
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        // Tabs
        tabs = new Tabs();
        encryptDecryptTabHeader = new Tab("Encrypt / Decrypt");
        signVerifyTabHeader = new Tab("Sign / Verify");
        dataKeyTabHeader = new Tab("Data Keys");
        macTabHeader = new Tab("MAC");
        tabs.add(encryptDecryptTabHeader, signVerifyTabHeader, dataKeyTabHeader, macTabHeader);
        tabs.setWidthFull();
        tabs.getStyle().set("overflow-x", "auto");
        tabs.getStyle().set("white-space", "nowrap");

        encryptDecryptTab = createEncryptDecryptPanel();
        signVerifyTab = createSignVerifyPanel();
        dataKeyTab = createDataKeyPanel();
        macTab = createMacPanel();

        encryptDecryptTab.setVisible(true);
        signVerifyTab.setVisible(false);
        dataKeyTab.setVisible(false);
        macTab.setVisible(false);

        tabs.addSelectedChangeListener(event -> {
            Tab selected = event.getSelectedTab();
            boolean supported = isTabSupported(selected);
            if (!supported && selectedKeyId != null) {
                Notification.show("The selected key does not support this operation.", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
            encryptDecryptTab.setVisible(selected == encryptDecryptTabHeader);
            signVerifyTab.setVisible(selected == signVerifyTabHeader);
            dataKeyTab.setVisible(selected == dataKeyTabHeader);
            macTab.setVisible(selected == macTabHeader);
        });

        add(tabs, encryptDecryptTab, signVerifyTab, dataKeyTab, macTab);

        injectResponsiveStyles();
        loadKeyOptions();
    }

    private void injectResponsiveStyles() {
        String css = """
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

    // Helper: create a labelled text area with copy button after the label
    private VerticalLayout createLabelledTextArea(String labelText, TextArea textArea) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        container.setWidthFull();
        container.addClassName("labelled-textarea-container");

        // Header row: label and copy button side by side
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        header.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        Label label = new Label(labelText);
        label.getStyle().set("font-weight", "500");
        label.getStyle().set("font-size", "var(--lumo-font-size-s)");

        Button copyButton = new Button(new Icon(VaadinIcon.COPY));
        copyButton.setTooltipText("Copy to clipboard");
        copyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        copyButton.addClickListener(e -> {
            String value = textArea.getValue();
            if (StringUtils.hasText(value)) {
                UI.getCurrent().getPage().executeJs(
                        "navigator.clipboard.writeText($0).catch(e => console.error('Copy failed:', e));",
                        value
                );
                Notification.show("Copied to clipboard", 2000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show("Nothing to copy", 2000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
        });

        header.add(label, copyButton);
        header.expand(label); // pushes button to the right

        textArea.setWidthFull();
        container.add(header, textArea);
        return container;
    }

    // ========== Helper methods ==========
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
                updateAlgorithmCombos();
                updateTabBasedOnKey();
            }
        } catch (Exception e) {
            Notification.show("Failed to load keys: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
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
        } catch (Exception e) {
            // ignore
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
                updateAlgorithmCombos();
                updateTabBasedOnKey();
            }
        } catch (Exception e) {
            Notification.show("Failed to load key metadata", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
        }
    }

    private void updateTabBasedOnKey() {
        if (selectedKeyId == null) return;
        Tab current = tabs.getSelectedTab();
        if (!isTabSupported(current)) {
            Tab supported = getFirstSupportedTab();
            if (supported != null) {
                tabs.setSelectedTab(supported);
                Notification.show("Switched to a supported operation for this key.", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
            } else {
                Notification.show("This key does not support any cryptographic operation.", 3000, Notification.Position.TOP_END)
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

    private void updateAlgorithmCombos() {
        if (algorithmCombo != null) algorithmCombo.clear();
        if (signAlgoCombo != null) signAlgoCombo.clear();
        if (macAlgoCombo != null) macAlgoCombo.clear();

        if (selectedKeyUsage == null || selectedKeySpec == null) {
            if (algorithmCombo != null) algorithmCombo.setEnabled(false);
            if (signAlgoCombo != null) signAlgoCombo.setEnabled(false);
            if (macAlgoCombo != null) macAlgoCombo.setEnabled(false);
            return;
        }

        boolean readOnly = selectedKeySpec != null;

        if (algorithmCombo != null) {
            updateComboForUsage(selectedKeyUsage, selectedKeySpec, algorithmCombo,
                    usage -> usage == IEnumKeyUsage.Types.ENCRYPT_DECRYPT,
                    spec -> AlgorithmMapper.keySpecToEncryptionAlgo(spec).stream()
                            .map(IEnumEncryptionAlgorithm::name)
                            .collect(Collectors.toList()),
                    readOnly);
        }

        if (signAlgoCombo != null) {
            updateComboForUsage(selectedKeyUsage, selectedKeySpec, signAlgoCombo,
                    usage -> usage == IEnumKeyUsage.Types.SIGN_VERIFY,
                    spec -> AlgorithmMapper.keySpecToSigningAlgo(spec).stream()
                            .map(IEnumSignatureAlgorithm::name)
                            .collect(Collectors.toList()),
                    readOnly);
        }

        if (macAlgoCombo != null) {
            updateComboForUsage(selectedKeyUsage, selectedKeySpec, macAlgoCombo,
                    usage -> usage == IEnumKeyUsage.Types.GENERATE_VERIFY_MAC,
                    spec -> AlgorithmMapper.keySpecToMacAlgo(spec).stream()
                            .map(IEnumMacAlgorithm::name)
                            .collect(Collectors.toList()),
                    readOnly);
        }
    }

    private String getDefaultAlgorithm(IEnumKeySpec.Types spec, IEnumKeyUsage.Types usage) {
        if (spec == null) return null;
        if (usage == IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
            if (spec.name().startsWith("RSA")) return IEnumEncryptionAlgorithm.RSAES_OAEP_SHA_256.name();
            if (spec == IEnumKeySpec.Types.SYMMETRIC_DEFAULT) return IEnumEncryptionAlgorithm.SYMMETRIC_DEFAULT.name();
        } else if (usage == IEnumKeyUsage.Types.SIGN_VERIFY) {
            if (spec.name().startsWith("RSA")) {
                int size = spec.getKeySizeBits();
                if (size == 2048) return IEnumSignatureAlgorithm.RSASSA_PKCS1_V1_5_SHA_256.name();
                if (size == 3072) return IEnumSignatureAlgorithm.RSASSA_PKCS1_V1_5_SHA_384.name();
                if (size == 4096) return IEnumSignatureAlgorithm.RSASSA_PKCS1_V1_5_SHA_512.name();
                return IEnumSignatureAlgorithm.RSASSA_PKCS1_V1_5_SHA_256.name();
            } else if (spec.name().startsWith("ECC") || spec == IEnumKeySpec.Types.ECC_NIST_P256 ||
                    spec == IEnumKeySpec.Types.ECC_NIST_P384 || spec == IEnumKeySpec.Types.ECC_NIST_P521 ||
                    spec == IEnumKeySpec.Types.ECC_SECG_P256K1) {
                int size = spec.getKeySizeBits();
                if (size == 256) return IEnumSignatureAlgorithm.ECDSA_SHA_256.name();
                if (size == 384) return IEnumSignatureAlgorithm.ECDSA_SHA_384.name();
                if (size == 521) return IEnumSignatureAlgorithm.ECDSA_SHA_512.name();
                return IEnumSignatureAlgorithm.ECDSA_SHA_256.name();
            } else if (spec == IEnumKeySpec.Types.SM2) {
                return IEnumSignatureAlgorithm.SM2DSA.name();
            }
        } else if (usage == IEnumKeyUsage.Types.GENERATE_VERIFY_MAC) {
            if (spec.name().startsWith("HMAC")) {
                int size = spec.getKeySizeBits();
                if (size == 224) return IEnumMacAlgorithm.HMAC_SHA_224.name();
                if (size == 256) return IEnumMacAlgorithm.HMAC_SHA_256.name();
                if (size == 384) return IEnumMacAlgorithm.HMAC_SHA_384.name();
                if (size == 512) return IEnumMacAlgorithm.HMAC_SHA_512.name();
                return IEnumMacAlgorithm.HMAC_SHA_256.name();
            }
        }
        return null;
    }

    private void updateComboForUsage(IEnumKeyUsage.Types usage,
                                     IEnumKeySpec.Types spec,
                                     ComboBox<String> comboBox,
                                     Predicate<IEnumKeyUsage.Types> usageCheck,
                                     Function<IEnumKeySpec.Types, List<String>> algoMapper,
                                     boolean readOnly) {
        if (usageCheck.test(usage)) {
            List<String> algorithms = algoMapper.apply(spec);
            if (!algorithms.isEmpty()) {
                comboBox.setItems(algorithms);
                if (readOnly) {
                    String defaultAlgo = getDefaultAlgorithm(spec, usage);
                    if (defaultAlgo != null && algorithms.contains(defaultAlgo)) {
                        comboBox.setValue(defaultAlgo);
                    } else {
                        comboBox.setValue(algorithms.get(0));
                    }
                    comboBox.setEnabled(false);
                } else {
                    comboBox.setValue(algorithms.get(0));
                    comboBox.setEnabled(true);
                }
                return;
            }
        }
        comboBox.setEnabled(false);
        if (comboBox == macAlgoCombo) {
            comboBox.setPlaceholder("Select an HMAC key to enable MAC operations");
        } else {
            comboBox.setPlaceholder("No algorithm available for this key");
        }
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        keyCombo.setEnabled(!show);
        refreshKeysButton.setEnabled(!show);
    }

    // ========== Encrypt / Decrypt Panel ==========
    private VerticalLayout createEncryptDecryptPanel() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.addClassName("crypto-panel");

        TextArea plaintextArea = new TextArea();
        plaintextArea.setHeight("150px");
        TextArea ciphertextArea = new TextArea();
        ciphertextArea.setHeight("150px");

        algorithmCombo = new ComboBox<>("Algorithm");
        algorithmCombo.setWidth("300px");
        algorithmCombo.setEnabled(false);
        algorithmCombo.setPlaceholder("Select a key first");

        TextField contextField = new TextField("Encryption Context (key:value, comma-separated)");
        contextField.setWidth("300px");
        contextField.setPlaceholder("e.g., purpose=test,env=dev");

        Button encryptBtn = new Button("Encrypt", new Icon(VaadinIcon.LOCK));
        Button decryptBtn = new Button("Decrypt", new Icon(VaadinIcon.UNLOCK));
        encryptBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        decryptBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout buttonRow = new HorizontalLayout(encryptBtn, decryptBtn);
        buttonRow.setSpacing(true);
        buttonRow.addClassName("crypto-button-row");

        encryptBtn.addClickListener(e -> {
            if (selectedKeyId == null) {
                Notification.show("Select a key first", 3000, Notification.Position.TOP_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            if (algorithmCombo.getValue() == null) {
                Notification.show("No compatible encryption algorithm for this key", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            String plain = plaintextArea.getValue();
            if (!StringUtils.hasText(plain)) {
                Notification.show("Plaintext is required", 3000, Notification.Position.TOP_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            try {
                String plainB64 = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
                Map<String, String> context = parseContext(contextField.getValue());
                KmsDtos.EncryptRequest request = KmsDtos.EncryptRequest.builder()
                        .keyId(selectedKeyId)
                        .plaintext(plainB64)
                        .encryptionContext(context.isEmpty() ? null : context)
                        .encryptionAlgorithmSpec(algorithmCombo.getValue())
                        .build();
                ResponseEntity<KmsDtos.EncryptResponse> response = kmsApiService.encrypt(request);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    ciphertextArea.setValue(response.getBody().getCiphertextBlob());
                    Notification.show("Encryption successful", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("Encryption failed", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (FeignException ex) {
                String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
                Notification.show("Encryption error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                String errorMsg = ex.getMessage();
                Notification.show("Encryption error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        decryptBtn.addClickListener(e -> {
            if (selectedKeyId == null) {
                Notification.show("Select a key first", 3000, Notification.Position.TOP_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            String cipher = ciphertextArea.getValue();
            if (!StringUtils.hasText(cipher)) {
                Notification.show("Ciphertext is required", 3000, Notification.Position.TOP_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            try {
                Map<String, String> context = parseContext(contextField.getValue());
                KmsDtos.DecryptRequest request = KmsDtos.DecryptRequest.builder()
                        .ciphertextBlob(cipher)
                        .encryptionContext(context.isEmpty() ? null : context)
                        .encryptionAlgorithmSpec(algorithmCombo.getValue())
                        .keyId(selectedKeyId)
                        .build();
                ResponseEntity<KmsDtos.DecryptResponse> response = kmsApiService.decrypt(request);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    String plainB64 = response.getBody().getPlaintext();
                    String plainText = new String(Base64.getDecoder().decode(plainB64), StandardCharsets.UTF_8);
                    plaintextArea.setValue(plainText);
                    Notification.show("Decryption successful", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("Decryption failed", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (FeignException ex) {
                String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
                Notification.show("Decryption error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                String errorMsg = ex.getMessage();
                Notification.show("Decryption error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        layout.add(createLabelledTextArea("Plaintext (UTF-8)", plaintextArea),
                createLabelledTextArea("Ciphertext (Base64)", ciphertextArea),
                algorithmCombo, contextField, buttonRow);
        return layout;
    }

    // ========== Sign / Verify Panel ==========
    private VerticalLayout createSignVerifyPanel() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.addClassName("crypto-panel");

        TextArea messageArea = new TextArea();
        messageArea.setHeight("150px");
        TextArea signatureArea = new TextArea();
        signatureArea.setHeight("150px");

        signAlgoCombo = new ComboBox<>("Signing Algorithm");
        signAlgoCombo.setEnabled(false);
        signAlgoCombo.setPlaceholder("Select a key that supports SIGN_VERIFY");

        Button signBtn = new Button("Sign", new Icon(VaadinIcon.PENCIL));
        Button verifyBtn = new Button("Verify", new Icon(VaadinIcon.CHECK));
        signBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        verifyBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout buttonRow = new HorizontalLayout(signBtn, verifyBtn);
        buttonRow.setSpacing(true);
        buttonRow.addClassName("crypto-button-row");

        signBtn.addClickListener(e -> {
            if (selectedKeyId == null) {
                Notification.show("Select a key first", 3000, Notification.Position.TOP_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            if (selectedKeyUsage != IEnumKeyUsage.Types.SIGN_VERIFY) {
                Notification.show("Selected key does not support SIGN_VERIFY", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            if (signAlgoCombo.getValue() == null) {
                Notification.show("No compatible signing algorithm for this key", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            String message = messageArea.getValue();
            if (!StringUtils.hasText(message)) {
                Notification.show("Message is required", 3000, Notification.Position.TOP_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            try {
                String msgB64 = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
                KmsDtos.SignRequest request = KmsDtos.SignRequest.builder()
                        .keyId(selectedKeyId)
                        .message(msgB64)
                        .messageType("RAW")
                        .signingAlgorithm(IEnumSignatureAlgorithm.valueOf(signAlgoCombo.getValue()))
                        .build();
                ResponseEntity<KmsDtos.SignResponse> response = kmsApiService.sign(request);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    signatureArea.setValue(response.getBody().getSignature());
                    Notification.show("Signature generated", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("Signing failed", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (FeignException ex) {
                String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
                Notification.show("Signing error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                String errorMsg = ex.getMessage();
                Notification.show("Signing error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        verifyBtn.addClickListener(e -> {
            if (selectedKeyId == null) {
                Notification.show("Select a key first", 3000, Notification.Position.TOP_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            String message = messageArea.getValue();
            String signature = signatureArea.getValue();
            if (!StringUtils.hasText(message) || !StringUtils.hasText(signature)) {
                Notification.show("Both message and signature are required", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            try {
                String msgB64 = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
                KmsDtos.VerifyRequest request = KmsDtos.VerifyRequest.builder()
                        .keyId(selectedKeyId)
                        .message(msgB64)
                        .messageType("RAW")
                        .signature(signature)
                        .signingAlgorithm(IEnumSignatureAlgorithm.valueOf(signAlgoCombo.getValue()))
                        .build();
                ResponseEntity<KmsDtos.VerifyResponse> response = kmsApiService.verify(request);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    boolean valid = response.getBody().isValid();
                    if (valid) {
                        Notification.show("Signature is valid", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    } else {
                        Notification.show("Signature is invalid", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                } else {
                    Notification.show("Verification failed", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (FeignException ex) {
                String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
                Notification.show("Verification error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                String errorMsg = ex.getMessage();
                Notification.show("Verification error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        layout.add(createLabelledTextArea("Message (UTF-8)", messageArea),
                createLabelledTextArea("Signature (Base64)", signatureArea),
                signAlgoCombo, buttonRow);
        return layout;
    }

    // ========== Data Key Panel ==========
    private VerticalLayout createDataKeyPanel() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.addClassName("crypto-data-key-panel");

        ComboBox<String> keySpecCombo = new ComboBox<>("Data Key Spec");
        keySpecCombo.setItems("AES_128", "AES_256");
        keySpecCombo.setValue("AES_256");

        TextField keySizeField = new TextField("Key Size (bits)");
        keySizeField.setPlaceholder("e.g., 128, 192 or 256");

        Button generateBtn = new Button("Generate Data Key", new Icon(VaadinIcon.KEY));
        generateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        TextArea plaintextKeyArea = new TextArea();
        plaintextKeyArea.setHeight("100px");
        plaintextKeyArea.setReadOnly(true);
        TextArea ciphertextKeyArea = new TextArea();
        ciphertextKeyArea.setHeight("100px");
        ciphertextKeyArea.setReadOnly(true);

        generateBtn.addClickListener(e -> {
            if (selectedKeyId == null) {
                Notification.show("Select a key first", 3000, Notification.Position.TOP_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            if (selectedKeyUsage != IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
                Notification.show("Selected key does not support encryption/decryption", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            try {
                KmsDtos.GenerateDataKeyRequest request = KmsDtos.GenerateDataKeyRequest.builder()
                        .keyId(selectedKeyId)
                        .keySpec(keySpecCombo.getValue())
                        .keySize(StringUtils.hasText(keySizeField.getValue()) ? Integer.parseInt(keySizeField.getValue()) : null)
                        .build();
                ResponseEntity<KmsDtos.GenerateDataKeyResponse> response = kmsApiService.generateDataKey(request);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    plaintextKeyArea.setValue(response.getBody().getPlaintext());
                    ciphertextKeyArea.setValue(response.getBody().getCiphertextBlob());
                    Notification.show("Data key generated", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("Generation failed", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (FeignException ex) {
                String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
                Notification.show("Generation error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                String errorMsg = ex.getMessage();
                Notification.show("Generation error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        layout.add(keySpecCombo, keySizeField, generateBtn,
                createLabelledTextArea("Plaintext Data Key (Base64)", plaintextKeyArea),
                createLabelledTextArea("Encrypted Data Key (Base64)", ciphertextKeyArea));
        return layout;
    }

    // ========== MAC Panel ==========
    private VerticalLayout createMacPanel() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.addClassName("crypto-panel");

        TextArea messageArea = new TextArea();
        messageArea.setHeight("150px");
        TextArea macArea = new TextArea();
        macArea.setHeight("100px");

        macAlgoCombo = new ComboBox<>("MAC Algorithm");
        macAlgoCombo.setEnabled(false);
        macAlgoCombo.setPlaceholder("Select an HMAC key");

        Button generateBtn = new Button("Generate MAC", new Icon(VaadinIcon.SIGNAL));
        Button verifyBtn = new Button("Verify MAC", new Icon(VaadinIcon.CHECK));
        generateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        verifyBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout buttonRow = new HorizontalLayout(generateBtn, verifyBtn);
        buttonRow.setSpacing(true);
        buttonRow.addClassName("crypto-button-row");

        generateBtn.addClickListener(e -> {
            if (selectedKeyId == null) {
                Notification.show("Select a key first", 3000, Notification.Position.TOP_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            if (selectedKeyUsage != IEnumKeyUsage.Types.GENERATE_VERIFY_MAC) {
                Notification.show("Selected key does not support MAC operations", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            if (macAlgoCombo.getValue() == null) {
                Notification.show("No compatible MAC algorithm for this key", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            String message = messageArea.getValue();
            if (!StringUtils.hasText(message)) {
                Notification.show("Message is required", 3000, Notification.Position.TOP_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            try {
                String msgB64 = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
                KmsDtos.GenerateMacRequest request = KmsDtos.GenerateMacRequest.builder()
                        .keyId(selectedKeyId)
                        .message(msgB64)
                        .macAlgorithm(IEnumMacAlgorithm.valueOf(macAlgoCombo.getValue()))
                        .build();
                ResponseEntity<KmsDtos.GenerateMacResponse> response = kmsApiService.generateMac(request);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    macArea.setValue(response.getBody().getMac());
                    Notification.show("MAC generated", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("MAC generation failed", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (FeignException ex) {
                String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
                Notification.show("MAC generation error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                String errorMsg = ex.getMessage();
                Notification.show("MAC generation error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        verifyBtn.addClickListener(e -> {
            if (selectedKeyId == null) {
                Notification.show("Select a key first", 3000, Notification.Position.TOP_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            String message = messageArea.getValue();
            String mac = macArea.getValue();
            if (!StringUtils.hasText(message) || !StringUtils.hasText(mac)) {
                Notification.show("Both message and MAC are required", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            try {
                String msgB64 = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
                KmsDtos.VerifyMacRequest request = KmsDtos.VerifyMacRequest.builder()
                        .keyId(selectedKeyId)
                        .message(msgB64)
                        .mac(mac)
                        .macAlgorithm(IEnumMacAlgorithm.valueOf(macAlgoCombo.getValue()))
                        .build();
                ResponseEntity<KmsDtos.VerifyMacResponse> response = kmsApiService.verifyMac(request);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    boolean valid = response.getBody().getMacValid();
                    if (valid) {
                        Notification.show("MAC is valid", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    } else {
                        Notification.show("MAC is invalid", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                } else {
                    Notification.show("MAC verification failed", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (FeignException ex) {
                String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
                Notification.show("MAC verification error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                String errorMsg = ex.getMessage();
                Notification.show("MAC verification error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        layout.add(createLabelledTextArea("Message (UTF-8)", messageArea),
                createLabelledTextArea("MAC (Base64)", macArea),
                macAlgoCombo, buttonRow);
        return layout;
    }

    private Map<String, String> parseContext(String contextStr) {
        Map<String, String> context = new HashMap<>();
        if (StringUtils.hasText(contextStr)) {
            for (String pair : contextStr.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    context.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        return context;
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