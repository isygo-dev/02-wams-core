package eu.isygoit.ui.views.kms.cryptography.crypto.panel;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.kms.cryptography.crypto.CryptoPanelUtils;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

public class DataKeyPanel extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final Supplier<String> keyIdSupplier;
    private final Supplier<IEnumKeyUsage.Types> keyUsageSupplier;

    private ComboBox<String> keySpecCombo;
    private TextField keySizeField;
    private TextArea plaintextKeyArea;
    private TextArea ciphertextKeyArea;

    public DataKeyPanel(KmsApiService kmsApiService,
                        Supplier<String> keyIdSupplier,
                        Supplier<IEnumKeyUsage.Types> keyUsageSupplier) {
        this.kmsApiService = kmsApiService;
        this.keyIdSupplier = keyIdSupplier;
        this.keyUsageSupplier = keyUsageSupplier;

        setSpacing(true);
        setPadding(true);
        addClassName("crypto-data-key-panel");

        initUI();
    }

    private void initUI() {
        keySpecCombo = new ComboBox<>("Data Key Spec");
        keySpecCombo.setItems("AES_128", "AES_256");
        keySpecCombo.setValue("AES_256");

        keySizeField = new TextField("Key Size (bits)");
        keySizeField.setPlaceholder("e.g., 128, 192 or 256");

        Button generateBtn = new Button("Generate Data Key", new Icon(VaadinIcon.KEY));
        generateBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);

        plaintextKeyArea = new TextArea();
        plaintextKeyArea.setHeight("100px");
        plaintextKeyArea.setReadOnly(true);

        ciphertextKeyArea = new TextArea();
        ciphertextKeyArea.setHeight("100px");
        ciphertextKeyArea.setReadOnly(true);

        generateBtn.addClickListener(e -> generateDataKey());

        add(keySpecCombo, keySizeField, generateBtn,
                CryptoPanelUtils.createLabelledTextArea("Plaintext Data Key (Base64)", plaintextKeyArea),
                CryptoPanelUtils.createLabelledTextArea("Encrypted Data Key (Base64)", ciphertextKeyArea));
    }

    public void setKeyInfo(String keyId, IEnumKeyUsage.Types keyUsage) {
        // nothing to update dynamically for this panel, but we keep the method for consistency
    }

    private void generateDataKey() {
        String keyId = keyIdSupplier.get();
        if (keyId == null) {
            notifyWarning("Select a key first");
            return;
        }
        if (keyUsageSupplier.get() != IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
            notifyWarning("Selected key does not support encryption/decryption");
            return;
        }
        try {
            KmsDtos.GenerateDataKeyRequest request = KmsDtos.GenerateDataKeyRequest.builder()
                    .keyId(keyId)
                    .keySize(StringUtils.hasText(keySizeField.getValue()) ? Integer.parseInt(keySizeField.getValue()) : null)
                    .build();
            ResponseEntity<KmsDtos.GenerateDataKeyResponse> response = kmsApiService.generateDataKey(request);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                plaintextKeyArea.setValue(response.getBody().getPlaintext());
                ciphertextKeyArea.setValue(response.getBody().getCiphertextBlob());
                notifySuccess("Data key generated");
            } else {
                notifyError("Generation failed");
            }
        } catch (FeignException ex) {
            notifyError("Generation error: " + ((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage()));
        } catch (Exception ex) {
            notifyError("Generation error: " + ex.getMessage());
        }
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