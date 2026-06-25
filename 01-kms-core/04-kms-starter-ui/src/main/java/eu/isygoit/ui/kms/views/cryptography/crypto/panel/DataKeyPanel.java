package eu.isygoit.ui.kms.views.cryptography.crypto.panel;

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
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.kms.views.cryptography.crypto.CryptoPanelUtils;
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
        keySpecCombo = new ComboBox<>(I18n.t("crypto.data.key.spec"));
        keySpecCombo.setItems("AES_128", "AES_256");
        keySpecCombo.setValue("AES_256");

        keySizeField = new TextField(I18n.t("crypto.data.key.size"));
        keySizeField.setPlaceholder(I18n.t("crypto.data.key.size.placeholder"));

        Button generateBtn = new Button(I18n.t("crypto.data.key.generate.button"), new Icon(VaadinIcon.KEY));
        generateBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);

        plaintextKeyArea = new TextArea();
        plaintextKeyArea.setHeight("100px");
        plaintextKeyArea.setReadOnly(true);

        ciphertextKeyArea = new TextArea();
        ciphertextKeyArea.setHeight("100px");
        ciphertextKeyArea.setReadOnly(true);

        generateBtn.addClickListener(e -> generateDataKey());

        add(keySpecCombo, keySizeField, generateBtn,
                CryptoPanelUtils.createLabelledTextArea(I18n.t("crypto.data.key.plaintext"), plaintextKeyArea),
                CryptoPanelUtils.createLabelledTextArea(I18n.t("crypto.data.key.ciphertext"), ciphertextKeyArea));
    }

    public void setKeyInfo(String keyId, IEnumKeyUsage.Types keyUsage) {
        // nothing to update dynamically for this panel, but we keep the method for consistency
    }

    private void generateDataKey() {
        String keyId = keyIdSupplier.get();
        if (keyId == null) {
            notifyWarning(I18n.t("crypto.data.key.select.key.first"));
            return;
        }
        if (keyUsageSupplier.get() != IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
            notifyWarning(I18n.t("crypto.data.key.key.not.supported"));
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
                notifySuccess(I18n.t("crypto.data.key.generate.success"));
            } else {
                notifyError(I18n.t("crypto.data.key.generate.failed"));
            }
        } catch (FeignException ex) {
            notifyError(I18n.t("crypto.data.key.generate.error", ((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage())));
        } catch (Exception ex) {
            notifyError(I18n.t("crypto.data.key.generate.error", ex.getMessage()));
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