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
import eu.isygoit.enums.IEnumSignatureAlgorithm;
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

public class SignVerifyPanel extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final Supplier<String> keyIdSupplier;
    private final Supplier<IEnumKeySpec.Types> keySpecSupplier;
    private final Supplier<IEnumKeyUsage.Types> keyUsageSupplier;

    private ComboBox<String> signAlgoCombo;
    private TextArea messageArea;
    private TextArea signatureArea;

    public SignVerifyPanel(KmsApiService kmsApiService,
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

        signatureArea = new TextArea();
        signatureArea.setHeight("150px");

        signAlgoCombo = new ComboBox<>(I18n.t("kms.crypto.sign.verify.algorithm"));
        signAlgoCombo.setEnabled(false);
        signAlgoCombo.setPlaceholder(I18n.t("kms.crypto.view.select.key"));

        Button signBtn = new Button(I18n.t("kms.crypto.sign.verify.sign.button"), new Icon(VaadinIcon.PENCIL));
        Button verifyBtn = new Button(I18n.t("kms.crypto.sign.verify.verify.button"), new Icon(VaadinIcon.CHECK));
        signBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
        verifyBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout buttonRow = new HorizontalLayout(signBtn, verifyBtn);
        buttonRow.setSpacing(true);
        buttonRow.addClassName("crypto-button-row");

        signBtn.addClickListener(e -> sign());
        verifyBtn.addClickListener(e -> verify());

        add(CryptoPanelUtils.createLabelledTextArea(I18n.t("kms.crypto.sign.verify.message"), messageArea),
                CryptoPanelUtils.createLabelledTextArea(I18n.t("kms.crypto.sign.verify.signature"), signatureArea),
                signAlgoCombo, buttonRow);
    }

    public void setKeyInfo(String keyId, IEnumKeySpec.Types keySpec, IEnumKeyUsage.Types keyUsage) {
        updateAlgorithmCombo(keySpec, keyUsage);
    }

    private void updateAlgorithmCombo(IEnumKeySpec.Types keySpec, IEnumKeyUsage.Types keyUsage) {
        signAlgoCombo.clear();
        if (keyUsage == null || keySpec == null || keyUsage != IEnumKeyUsage.Types.SIGN_VERIFY) {
            signAlgoCombo.setEnabled(false);
            signAlgoCombo.setPlaceholder(I18n.t("kms.crypto.view.select.key"));
            return;
        }
        List<String> algorithms = AlgorithmMapper.keySpecToSigningAlgo(keySpec).stream()
                .map(IEnumSignatureAlgorithm::name)
                .collect(Collectors.toList());
        if (algorithms.isEmpty()) {
            signAlgoCombo.setEnabled(false);
            signAlgoCombo.setPlaceholder(I18n.t("kms.crypto.sign.verify.no.algorithm"));
            return;
        }
        signAlgoCombo.setItems(algorithms);
        String defaultAlgo = AlgorithmMapper.getDefaultAlgorithm(keySpec, keyUsage);
        if (defaultAlgo != null && algorithms.contains(defaultAlgo)) {
            signAlgoCombo.setValue(defaultAlgo);
            signAlgoCombo.setEnabled(false);
        } else {
            signAlgoCombo.setValue(algorithms.get(0));
            signAlgoCombo.setEnabled(true);
        }
    }

    private void sign() {
        String keyId = keyIdSupplier.get();
        if (keyId == null) {
            notifyWarning(I18n.t("kms.crypto.sign.verify.select.key.first"));
            return;
        }
        if (keyUsageSupplier.get() != IEnumKeyUsage.Types.SIGN_VERIFY) {
            notifyWarning(I18n.t("kms.crypto.sign.verify.key.not.supported"));
            return;
        }
        String algo = signAlgoCombo.getValue();
        if (algo == null) {
            notifyWarning(I18n.t("kms.crypto.sign.verify.no.algorithm"));
            return;
        }
        String message = messageArea.getValue();
        if (!StringUtils.hasText(message)) {
            notifyWarning(I18n.t("kms.crypto.sign.verify.message.required"));
            return;
        }
        try {
            String msgB64 = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
            KmsDtos.SignRequest request = KmsDtos.SignRequest.builder()
                    .keyId(keyId)
                    .message(msgB64)
                    .messageType("RAW")
                    .signingAlgorithm(IEnumSignatureAlgorithm.valueOf(algo))
                    .build();
            ResponseEntity<KmsDtos.SignResponse> response = kmsApiService.sign(request);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                signatureArea.setValue(response.getBody().getSignature());
                notifySuccess(I18n.t("kms.crypto.sign.verify.signature.generated"));
            } else {
                notifyError(I18n.t("kms.crypto.sign.verify.sign.failed"));
            }
        } catch (FeignException ex) {
            notifyError(I18n.t("kms.crypto.sign.verify.sign.error", ((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage())));
        } catch (Exception ex) {
            notifyError(I18n.t("kms.crypto.sign.verify.sign.error", ex.getMessage()));
        }
    }

    private void verify() {
        String keyId = keyIdSupplier.get();
        if (keyId == null) {
            notifyWarning(I18n.t("kms.crypto.sign.verify.select.key.first"));
            return;
        }
        String message = messageArea.getValue();
        String signature = signatureArea.getValue();
        if (!StringUtils.hasText(message) || !StringUtils.hasText(signature)) {
            notifyWarning(I18n.t("kms.crypto.sign.verify.both.required"));
            return;
        }
        String algo = signAlgoCombo.getValue();
        if (algo == null) {
            notifyWarning(I18n.t("kms.crypto.sign.verify.no.algorithm.selected"));
            return;
        }
        try {
            String msgB64 = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
            KmsDtos.VerifyRequest request = KmsDtos.VerifyRequest.builder()
                    .keyId(keyId)
                    .message(msgB64)
                    .messageType("RAW")
                    .signature(signature)
                    .signingAlgorithm(IEnumSignatureAlgorithm.valueOf(algo))
                    .build();
            ResponseEntity<KmsDtos.VerifyResponse> response = kmsApiService.verify(request);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                if (response.getBody().isValid()) {
                    notifySuccess(I18n.t("kms.crypto.sign.verify.valid"));
                } else {
                    notifyError(I18n.t("kms.crypto.sign.verify.invalid"));
                }
            } else {
                notifyError(I18n.t("kms.crypto.sign.verify.verify.failed"));
            }
        } catch (FeignException ex) {
            notifyError(I18n.t("kms.crypto.sign.verify.verify.error", ((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage())));
        } catch (Exception ex) {
            notifyError(I18n.t("kms.crypto.sign.verify.verify.error", ex.getMessage()));
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