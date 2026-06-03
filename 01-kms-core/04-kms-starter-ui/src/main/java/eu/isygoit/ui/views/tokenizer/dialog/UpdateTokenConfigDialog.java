package eu.isygoit.ui.views.tokenizer.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class UpdateTokenConfigDialog extends TokenConfigDialogBase {

    private final KmsTokenConfigService tokenConfigService;
    private final TokenConfigDto original;
    private TextField codeField;

    public UpdateTokenConfigDialog(KmsTokenConfigService tokenConfigService, TokenConfigDto dto, Runnable onSuccess) {
        super("Edit Token Configuration", onSuccess);
        this.tokenConfigService = tokenConfigService;
        this.original = dto;

        setOkButtonText("Save");
        buildCommonForm();
        addUpdateSpecificFields();
        addCommonFieldsToLayout();
        add(formLayout);
        setupAlgorithmChangeListener();
        bindData();
        updateFieldsForAlgorithm(original.getSignatureAlgorithm());
    }

    private void addUpdateSpecificFields() {
        codeField = new TextField("Code");
        codeField.setReadOnly(true);
        codeField.setWidthFull();
        formLayout.add(codeField, 2);
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

        signatureAlgorithmCombo.setValue(original.getSignatureAlgorithm());

        Integer lifetimeMs = original.getLifeTimeInMs();
        setLifeTimeFromMs(lifetimeMs);

        String storedKey = original.getSecretKey();
        if (HMAC_ALGORITHMS.contains(original.getSignatureAlgorithm())) {
            secretKeyField.setValue(storedKey != null ? storedKey : "");
        } else if (ASYMMETRIC_ALGORITHMS.contains(original.getSignatureAlgorithm())) {
            privateKeyArea.setValue(storedKey != null ? storedKey : "");
            publicKeyArea.setValue(original.getPublicKey() != null ? original.getPublicKey() :
                    "(Not stored – generate new pair to see public key)");
        }
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

        Integer lifeTime = getLifeTimeInMs();
        if (lifeTime == null) return false;

        TokenConfigDto updated = TokenConfigDto.builder()
                .id(original.getId())
                .code(original.getCode())
                .tokenType(tokenType)
                .issuer(issuerField.getValue())
                .audience(getAudienceList())
                .signatureAlgorithm(signatureAlgorithm)
                .secretKey(secretOrPrivateKey)
                .publicKey(publicKeyArea.getValue())
                .lifeTimeInMs(lifeTime)
                .build();

        try {
            ResponseEntity<TokenConfigDto> response = tokenConfigService.update(original.getId(), updated);
            if (response.getStatusCode().is2xxSuccessful()) {
                onSaveSuccess();
                return true;
            } else {
                showError("Update failed: " + response.getStatusCode());
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
        Notification.show("Configuration updated successfully", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}