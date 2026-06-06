package eu.isygoit.ui.views.tokenizer.config.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.List;

public class UpdateTokenConfigDialog extends TokenConfigDialogBase {

    private final KmsTokenConfigService tokenConfigService;
    private final TokenConfigDto original;
    private TextField codeField;

    public UpdateTokenConfigDialog(KmsTokenConfigService tokenConfigService, KmsApiService kmsApiService, TokenConfigDto dto, Runnable onSuccess) {
        super("Edit Token Configuration", onSuccess, kmsApiService);
        this.tokenConfigService = tokenConfigService;
        this.original = dto;
        setOkButtonText("Save");
        initUI();
        addCodeFieldToMetadataCard();
        bindData();
    }

    private void addCodeFieldToMetadataCard() {
        codeField = new TextField("Code");
        codeField.setReadOnly(true);
        codeField.setWidthFull();
        VerticalLayout metaForm = (VerticalLayout) metadataCard.getChildren().toArray()[1];
        metaForm.addComponentAsFirst(codeField);
    }

    @Override
    protected void bindData() {
        codeField.setValue(original.getCode());
        tokenTypeCombo.setValue(original.getTokenType());
        tokenTypeCombo.setReadOnly(true);
        issuerField.setValue(original.getIssuer() != null ? original.getIssuer() : "");
        if (original.getAudience() != null && !original.getAudience().isEmpty()) {
            setAudienceList(original.getAudience());
        } else {
            setAudienceList(List.of());
        }
        setLifeTimeFromMs(original.getLifeTimeInMs());

        // Handle key source selection
        if (StringUtils.hasText(original.getKmsKeyId())) {
            keySourceGroup.setValue("Use existing KMS key");
            if (availableKeyOptions != null) {
                KeyOption selected = availableKeyOptions.stream()
                        .filter(opt -> opt.getKeyId().equals(original.getKmsKeyId()))
                        .findFirst()
                        .orElse(null);
                kmsKeyCombo.setValue(selected);
            }
            // No custom key fields to populate
        } else {
            keySourceGroup.setValue("Define custom key");
            signatureAlgorithmCombo.setValue(original.getSignatureAlgorithm());
            String storedKey = original.getSecretKey();
            if (HMAC_ALGORITHMS.contains(original.getSignatureAlgorithm())) {
                secretKeyField.setValue(storedKey != null ? storedKey : "");
                updateCryptographySection(original.getSignatureAlgorithm());
            } else if (ASYMMETRIC_ALGORITHMS.contains(original.getSignatureAlgorithm())) {
                privateKeyArea.setValue(storedKey != null ? storedKey : "");
                publicKeyArea.setValue(original.getPublicKey() != null ? original.getPublicKey() :
                        "(Not stored – generate new pair to see public key)");
                updateCryptographySection(original.getSignatureAlgorithm());
            }
        }
    }

    @Override
    protected boolean onOk() {
        IEnumToken.Types tokenType = tokenTypeCombo.getValue();
        if (tokenType == null) {
            append("Token type is required");
            return false;
        }

        Integer lifeTime = getLifeTimeInMs();

        TokenConfigDto.TokenConfigDtoBuilder builder = TokenConfigDto.builder()
                .id(original.getId())
                .code(original.getCode())
                .tokenType(tokenType)
                .issuer(issuerField.getValue())
                .audience(getAudienceList())
                .lifeTimeInMs(lifeTime);

        boolean useKmsKey = "Use existing KMS key".equals(keySourceGroup.getValue());
        if (useKmsKey) {
            KeyOption selected = kmsKeyCombo.getValue();
            if (selected == null) {
                append("Please select a KMS key");
                return false;
            }
            builder.kmsKeyId(selected.getKeyId())
                    .secretKey(null)
                    .publicKey(null)
                    .signatureAlgorithm(null);
        } else {
            String signatureAlgorithm = signatureAlgorithmCombo.getValue();
            if (signatureAlgorithm == null || signatureAlgorithm.isBlank()) {
                append("Signature algorithm is required");
                return false;
            }
            builder.signatureAlgorithm(signatureAlgorithm);
            String secretOrPrivateKey;
            if (HMAC_ALGORITHMS.contains(signatureAlgorithm)) {
                String secretKey = secretKeyField.getValue();
                if (secretKey == null || secretKey.isBlank()) {
                    append("Secret key is required for " + signatureAlgorithm);
                    return false;
                }
                if (!validateHmacKey(signatureAlgorithm, secretKey)) return false;
                secretOrPrivateKey = secretKey;
                builder.publicKey(null);
            } else if (ASYMMETRIC_ALGORITHMS.contains(signatureAlgorithm)) {
                String privateKey = privateKeyArea.getValue();
                if (privateKey == null || privateKey.isBlank()) {
                    append("Private key is required for " + signatureAlgorithm);
                    return false;
                }
                secretOrPrivateKey = privateKey;
                builder.publicKey(publicKeyArea.getValue());
            } else {
                append("Unsupported signature algorithm: " + signatureAlgorithm);
                return false;
            }
            builder.secretKey(secretOrPrivateKey)
                    .kmsKeyId(null);
        }

        TokenConfigDto updated = builder.build();

        try {
            ResponseEntity<TokenConfigDto> response = tokenConfigService.update(original.getId(), updated);
            if (response.getStatusCode().is2xxSuccessful()) {
                onSaveSuccess();
                return true;
            } else {
                return false;
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed operation: " + e.getMessage();
            this.append(errorMsg);
        }

        return false;
    }

    @Override
    protected void onSaveSuccess() {
        Notification.show("Configuration updated successfully", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}