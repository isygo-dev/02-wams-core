package eu.isygoit.ui.views.crypto.panel;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumEncryptionAlgorithm;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.mapper.AlgorithmMapper;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.crypto.CryptoPanelUtils;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class EncryptDecryptPanel extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final Supplier<String> keyIdSupplier;
    private final Supplier<IEnumKeySpec.Types> keySpecSupplier;
    private final Supplier<IEnumKeyUsage.Types> keyUsageSupplier;

    private ComboBox<String> algorithmCombo;
    private TextArea plaintextArea;
    private TextArea ciphertextArea;
    private TextField contextField;

    public EncryptDecryptPanel(KmsApiService kmsApiService,
                               Supplier<String> keyIdSupplier,
                               Supplier<IEnumKeySpec.Types> keySpecSupplier,
                               Supplier<IEnumKeyUsage.Types> keyUsageSupplier) {
        this.kmsApiService = kmsApiService;
        this.keyIdSupplier = keyIdSupplier;
        this.keySpecSupplier = keySpecSupplier;
        this.keyUsageSupplier = keyUsageSupplier;

        setSpacing(true);
        setPadding(true);
        addClassName("crypto-panel");

        initUI();
    }

    private void initUI() {
        plaintextArea = new TextArea();
        plaintextArea.setHeight("150px");

        ciphertextArea = new TextArea();
        ciphertextArea.setHeight("150px");

        algorithmCombo = new ComboBox<>("Algorithm");
        algorithmCombo.setWidth("300px");
        algorithmCombo.setEnabled(false);
        algorithmCombo.setPlaceholder("Select a key first");

        contextField = new TextField("Encryption Context (key:value, comma-separated)");
        contextField.setWidth("300px");
        contextField.setPlaceholder("e.g., purpose=test,env=dev");

        Button encryptBtn = new Button("Encrypt", new Icon(VaadinIcon.LOCK));
        Button decryptBtn = new Button("Decrypt", new Icon(VaadinIcon.UNLOCK));
        encryptBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
        decryptBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout buttonRow = new HorizontalLayout(encryptBtn, decryptBtn);
        buttonRow.setSpacing(true);
        buttonRow.addClassName("crypto-button-row");

        encryptBtn.addClickListener(e -> encrypt());
        decryptBtn.addClickListener(e -> decrypt());

        add(CryptoPanelUtils.createLabelledTextArea("Plaintext (UTF-8)", plaintextArea),
                CryptoPanelUtils.createLabelledTextArea("Ciphertext (Base64)", ciphertextArea),
                algorithmCombo, contextField, buttonRow);
    }

    public void setKeyInfo(String keyId, IEnumKeySpec.Types keySpec, IEnumKeyUsage.Types keyUsage) {
        updateAlgorithmCombo(keySpec, keyUsage);
    }

    private void updateAlgorithmCombo(IEnumKeySpec.Types keySpec, IEnumKeyUsage.Types keyUsage) {
        algorithmCombo.clear();
        if (keyUsage == null || keySpec == null || keyUsage != IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
            algorithmCombo.setEnabled(false);
            algorithmCombo.setPlaceholder("No compatible encryption algorithm");
            return;
        }
        List<String> algorithms = AlgorithmMapper.keySpecToEncryptionAlgo(keySpec).stream()
                .map(IEnumEncryptionAlgorithm::name)
                .collect(Collectors.toList());
        if (algorithms.isEmpty()) {
            algorithmCombo.setEnabled(false);
            algorithmCombo.setPlaceholder("No compatible encryption algorithm");
            return;
        }
        algorithmCombo.setItems(algorithms);
        String defaultAlgo = AlgorithmMapper.getDefaultAlgorithm(keySpec, keyUsage);
        if (defaultAlgo != null && algorithms.contains(defaultAlgo)) {
            algorithmCombo.setValue(defaultAlgo);
            algorithmCombo.setEnabled(false);
        } else {
            algorithmCombo.setValue(algorithms.get(0));
            algorithmCombo.setEnabled(true);
        }
    }

    private void encrypt() {
        String keyId = keyIdSupplier.get();
        if (keyId == null) {
            notifyWarning("Select a key first");
            return;
        }
        String algorithm = algorithmCombo.getValue();
        if (algorithm == null) {
            notifyWarning("No compatible encryption algorithm for this key");
            return;
        }
        String plain = plaintextArea.getValue();
        if (!StringUtils.hasText(plain)) {
            notifyWarning("Plaintext is required");
            return;
        }
        try {
            String plainB64 = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
            Map<String, String> context = parseContext(contextField.getValue());
            KmsDtos.EncryptRequest request = KmsDtos.EncryptRequest.builder()
                    .keyId(keyId)
                    .plaintext(plainB64)
                    .encryptionContext(context.isEmpty() ? null : context)
                    .encryptionAlgorithmSpec(algorithm)
                    .build();
            ResponseEntity<KmsDtos.EncryptResponse> response = kmsApiService.encrypt(request);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ciphertextArea.setValue(response.getBody().getCiphertextBlob());
                notifySuccess("Encryption successful");
            } else {
                log.error("Encryption failed: {}", response.getBody());
                notifyError("Encryption failed");
            }
        } catch (FeignException ex) {
            log.error("Encryption error: {}", (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
            notifyError("Encryption error: " + ((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage()));
        } catch (Exception ex) {
            log.error("Encryption error: {}", ex.getMessage());
            notifyError("Encryption error: " + ex.getMessage());
        }
    }

    private void decrypt() {
        String keyId = keyIdSupplier.get();
        if (keyId == null) {
            notifyWarning("Select a key first");
            return;
        }
        String cipher = ciphertextArea.getValue();
        if (!StringUtils.hasText(cipher)) {
            notifyWarning("Ciphertext is required");
            return;
        }
        try {
            Map<String, String> context = parseContext(contextField.getValue());
            KmsDtos.DecryptRequest request = KmsDtos.DecryptRequest.builder()
                    .ciphertextBlob(cipher)
                    .encryptionContext(context.isEmpty() ? null : context)
                    .encryptionAlgorithmSpec(algorithmCombo.getValue())
                    .keyId(keyId)
                    .build();
            ResponseEntity<KmsDtos.DecryptResponse> response = kmsApiService.decrypt(request);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String plainB64 = response.getBody().getPlaintext();
                String plainText = new String(Base64.getDecoder().decode(plainB64), StandardCharsets.UTF_8);
                plaintextArea.setValue(plainText);
                notifySuccess("Decryption successful");
            } else {
                log.error("Decryption failed: {}", response.getBody());
                notifyError("Decryption failed");
            }
        } catch (FeignException ex) {
            log.error("Decryption error: {}", (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
            notifyError("Decryption error: " + ((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage()));
        } catch (Exception ex) {
            log.error("Decryption error: {}", ex.getMessage());
            notifyError("Decryption error: " + ex.getMessage());
        }
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

    private void notifyWarning(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    private void notifySuccess(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void notifyError(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}