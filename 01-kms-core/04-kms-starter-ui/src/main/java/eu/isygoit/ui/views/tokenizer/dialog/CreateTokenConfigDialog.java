package eu.isygoit.ui.views.tokenizer.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public class CreateTokenConfigDialog extends TokenConfigDialogBase {

    private final KmsTokenConfigService tokenConfigService;

    public CreateTokenConfigDialog(KmsTokenConfigService tokenConfigService, Runnable onSuccess) {
        super("Create Token Configuration", onSuccess);
        this.tokenConfigService = tokenConfigService;

        setOkButtonText("Create");
        buildCommonForm();
        addCommonFieldsToLayout();
        add(formLayout);
        setupAlgorithmChangeListener();
        // Default to HMAC view
        updateFieldsForAlgorithm("HS256");
        bindData();
    }

    @Override
    protected void bindData() {
        tokenTypeCombo.setValue(IEnumToken.Types.ACCESS);
        issuerField.clear();
        setAudienceList(List.of());    // clear audiences
        signatureAlgorithmCombo.setValue("HS256");
        secretKeyField.clear();
        privateKeyArea.clear();
        publicKeyArea.clear();
    }

    @Override
    protected boolean onOk() {
        IEnumToken.Types tokenType = tokenTypeCombo.getValue();
        if (tokenType == null) {
            showError("Token type is required");
            return false;
        }

        String signatureAlgorithm = signatureAlgorithmCombo.getValue();
        if (signatureAlgorithm == null || signatureAlgorithm.isBlank()) {
            showError("Signature algorithm is required");
            return false;
        }

        String secretOrPrivateKey;
        if (HMAC_ALGORITHMS.contains(signatureAlgorithm)) {
            String secretKey = secretKeyField.getValue();
            if (secretKey == null || secretKey.isBlank()) {
                showError("Secret key is required for " + signatureAlgorithm);
                return false;
            }
            if (!validateHmacKey(signatureAlgorithm, secretKey)) return false;
            secretOrPrivateKey = secretKey;
        } else if (ASYMMETRIC_ALGORITHMS.contains(signatureAlgorithm)) {
            String privateKey = privateKeyArea.getValue();
            if (privateKey == null || privateKey.isBlank()) {
                showError("Private key is required for " + signatureAlgorithm);
                return false;
            }
            secretOrPrivateKey = privateKey;
        } else {
            showError("Unsupported signature algorithm: " + signatureAlgorithm);
            return false;
        }

        String generatedCode = "TC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        TokenConfigDto dto = TokenConfigDto.builder()
                .code(generatedCode)
                .tokenType(tokenType)
                .issuer(issuerField.getValue())
                .audience(getAudienceList())      // list of audiences
                .signatureAlgorithm(signatureAlgorithm)
                .secretKey(secretOrPrivateKey)
                .publicKey(publicKeyArea.getValue())
                .build();

        try {
            ResponseEntity<TokenConfigDto> response = tokenConfigService.create(dto);
            if (response.getStatusCode().is2xxSuccessful()) {
                onSaveSuccess();
                return true;
            } else {
                showError("Creation failed: " + response.getStatusCode());
                return false;
            }
        } catch (FeignException ex) {
            handleFeignException(ex);
            return false;
        } catch (Exception ex) {
            handleGenericException(ex);
            return false;
        }
    }

    @Override
    protected void onSaveSuccess() {
        Notification.show("Configuration created successfully", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}