package eu.isygoit.ui.views.tokenizer.config.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public class CreateTokenConfigDialog extends TokenConfigDialogBase {

    private final KmsTokenConfigService tokenConfigService;

    public CreateTokenConfigDialog(KmsTokenConfigService tokenConfigService, KmsApiService kmsApiService, Runnable onSuccess) {
        super("Create Token Configuration", onSuccess, kmsApiService);
        this.tokenConfigService = tokenConfigService;
        setOkButtonText("Create");
        initUI();
        bindData();
    }

    @Override
    protected void bindData() {
        tokenTypeCombo.setValue(IEnumToken.Types.ACCESS);
        issuerField.clear();
        setAudienceList(List.of());
        signatureAlgorithmCombo.setValue("HS256");
        secretKeyField.clear();
        privateKeyArea.clear();
        publicKeyArea.clear();
        lifeTimeValueField.setValue(1);
        lifeTimeUnitCombo.setValue("Hours");
        keySourceGroup.setValue("Define custom key");
        kmsKeyCombo.clear();
    }

    @Override
    protected boolean onOk() {
        IEnumToken.Types tokenType = tokenTypeCombo.getValue();
        if (tokenType == null) {
            append("Token type is required");
            return false;
        }

        Integer lifeTime = getLifeTimeInMs();
        if (lifeTime == null) return false;

        String generatedCode = "TC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        TokenConfigDto.TokenConfigDtoBuilder builder = TokenConfigDto.builder()
                .code(generatedCode)
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

        TokenConfigDto dto = builder.build();

        try {
            ResponseEntity<TokenConfigDto> response = tokenConfigService.create(dto);
            if (response.getStatusCode().is2xxSuccessful()) {
                onSaveSuccess();
                return true;
            } else {
                append("Creation failed with status: " + response.getStatusCode());
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
        Notification.show("Configuration created successfully", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}