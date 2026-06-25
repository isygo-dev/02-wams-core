package eu.isygoit.ui.kms.views.cryptography.crypto.panel;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.enums.IEnumMacAlgorithm;
import eu.isygoit.i18n.I18n;
import eu.isygoit.mapper.AlgorithmMapper;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.kms.views.cryptography.crypto.CryptoPanelUtils;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MacPanel extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final Supplier<String> keyIdSupplier;
    private final Supplier<IEnumKeySpec.Types> keySpecSupplier;
    private final Supplier<IEnumKeyUsage.Types> keyUsageSupplier;

    private ComboBox<String> macAlgoCombo;
    private TextArea messageArea;
    private TextArea macArea;

    public MacPanel(KmsApiService kmsApiService,
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
        messageArea = new TextArea();
        messageArea.setHeight("150px");

        macArea = new TextArea();
        macArea.setHeight("100px");

        macAlgoCombo = new ComboBox<>(I18n.t("crypto.mac.algorithm"));
        macAlgoCombo.setEnabled(false);
        macAlgoCombo.setPlaceholder(I18n.t("crypto.mac.hmac.key.placeholder"));

        Button generateBtn = new Button(I18n.t("crypto.mac.generate.button"), new Icon(VaadinIcon.SIGNAL));
        Button verifyBtn = new Button(I18n.t("crypto.mac.verify.button"), new Icon(VaadinIcon.CHECK));
        generateBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
        verifyBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout buttonRow = new HorizontalLayout(generateBtn, verifyBtn);
        buttonRow.setSpacing(true);
        buttonRow.addClassName("crypto-button-row");

        generateBtn.addClickListener(e -> generateMac());
        verifyBtn.addClickListener(e -> verifyMac());

        add(CryptoPanelUtils.createLabelledTextArea(I18n.t("crypto.mac.message"), messageArea),
                CryptoPanelUtils.createLabelledTextArea(I18n.t("crypto.mac.mac"), macArea),
                macAlgoCombo, buttonRow);
    }

    public void setKeyInfo(String keyId, IEnumKeySpec.Types keySpec, IEnumKeyUsage.Types keyUsage) {
        updateAlgorithmCombo(keySpec, keyUsage);
    }

    private void updateAlgorithmCombo(IEnumKeySpec.Types keySpec, IEnumKeyUsage.Types keyUsage) {
        macAlgoCombo.clear();
        if (keyUsage == null || keySpec == null || keyUsage != IEnumKeyUsage.Types.GENERATE_VERIFY_MAC) {
            macAlgoCombo.setEnabled(false);
            macAlgoCombo.setPlaceholder(I18n.t("crypto.mac.hmac.key.placeholder"));
            return;
        }
        List<String> algorithms = AlgorithmMapper.keySpecToMacAlgo(keySpec).stream()
                .map(IEnumMacAlgorithm::name)
                .collect(Collectors.toList());
        if (algorithms.isEmpty()) {
            macAlgoCombo.setEnabled(false);
            macAlgoCombo.setPlaceholder(I18n.t("crypto.mac.no.algorithm"));
            return;
        }
        macAlgoCombo.setItems(algorithms);
        String defaultAlgo = AlgorithmMapper.getDefaultAlgorithm(keySpec, keyUsage);
        if (defaultAlgo != null && algorithms.contains(defaultAlgo)) {
            macAlgoCombo.setValue(defaultAlgo);
            macAlgoCombo.setEnabled(false);
        } else {
            macAlgoCombo.setValue(algorithms.get(0));
            macAlgoCombo.setEnabled(true);
        }
    }

    private void generateMac() {
        String keyId = keyIdSupplier.get();
        if (keyId == null) {
            notifyWarning(I18n.t("crypto.mac.select.key.first"));
            return;
        }
        IEnumKeyUsage.Types keyUsage = keyUsageSupplier.get();
        if (keyUsage != IEnumKeyUsage.Types.GENERATE_VERIFY_MAC) {
            notifyWarning(I18n.t("crypto.mac.key.not.supported"));
            return;
        }
        String algo = macAlgoCombo.getValue();
        if (algo == null) {
            notifyWarning(I18n.t("crypto.mac.no.algorithm"));
            return;
        }
        String message = messageArea.getValue();
        if (!StringUtils.hasText(message)) {
            notifyWarning(I18n.t("crypto.mac.message.required"));
            return;
        }
        try {
            String msgB64 = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
            KmsDtos.GenerateMacRequest request = KmsDtos.GenerateMacRequest.builder()
                    .keyId(keyId)
                    .message(msgB64)
                    .macAlgorithm(IEnumMacAlgorithm.valueOf(algo))
                    .build();
            ResponseEntity<KmsDtos.GenerateMacResponse> response = kmsApiService.generateMac(request);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                macArea.setValue(response.getBody().getMac());
                notifySuccess(I18n.t("crypto.mac.generated"));
            } else {
                notifyError(I18n.t("crypto.mac.generate.failed"));
            }
        } catch (FeignException ex) {
            notifyError(I18n.t("crypto.mac.generate.error", ((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage())));
        } catch (Exception ex) {
            notifyError(I18n.t("crypto.mac.generate.error", ex.getMessage()));
        }
    }

    private void verifyMac() {
        String keyId = keyIdSupplier.get();
        if (keyId == null) {
            notifyWarning(I18n.t("crypto.mac.select.key.first"));
            return;
        }
        String message = messageArea.getValue();
        String mac = macArea.getValue();
        if (!StringUtils.hasText(message) || !StringUtils.hasText(mac)) {
            notifyWarning(I18n.t("crypto.mac.both.required"));
            return;
        }
        String algo = macAlgoCombo.getValue();
        if (algo == null) {
            notifyWarning(I18n.t("crypto.mac.no.algorithm.selected"));
            return;
        }
        try {
            String msgB64 = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
            KmsDtos.VerifyMacRequest request = KmsDtos.VerifyMacRequest.builder()
                    .keyId(keyId)
                    .message(msgB64)
                    .mac(mac)
                    .macAlgorithm(IEnumMacAlgorithm.valueOf(algo))
                    .build();
            ResponseEntity<KmsDtos.VerifyMacResponse> response = kmsApiService.verifyMac(request);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                if (response.getBody().getMacValid()) {
                    notifySuccess(I18n.t("crypto.mac.valid"));
                } else {
                    notifyError(I18n.t("crypto.mac.invalid"));
                }
            } else {
                notifyError(I18n.t("crypto.mac.verify.failed"));
            }
        } catch (FeignException ex) {
            notifyError(I18n.t("crypto.mac.verify.error", ((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage())));
        } catch (Exception ex) {
            notifyError(I18n.t("crypto.mac.verify.error", ex.getMessage()));
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