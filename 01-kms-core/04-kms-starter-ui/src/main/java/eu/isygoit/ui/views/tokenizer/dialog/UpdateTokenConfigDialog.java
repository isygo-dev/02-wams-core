package eu.isygoit.ui.views.tokenizer.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.ui.views.BaseActionDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;

public class UpdateTokenConfigDialog extends BaseActionDialog {

    private final KmsTokenConfigService tokenConfigService;
    private final TokenConfigDto original;

    private TextField codeField;
    private ComboBox<IEnumToken.Types> tokenTypeCombo;
    private TextField issuerField;
    private TextField audienceField;
    private ComboBox<String> signatureAlgorithmCombo;

    // HMAC fields
    private TextField secretKeyField;

    // Asymmetric fields
    private TextArea privateKeyArea;
    private TextArea publicKeyArea;   // read-only, like Create dialog
    private Button generateKeyPairButton;

    private FormLayout formLayout;

    private static final List<String> HMAC_ALGORITHMS = List.of("HS256", "HS384", "HS512");
    private static final List<String> ASYMMETRIC_ALGORITHMS = List.of(
            "RS256", "RS384", "RS512",
            "PS256", "PS384", "PS512",
            "ES256", "ES384", "ES512",
            "EdDSA"
    );
    private static final List<String> SUPPORTED_ALGORITHMS = List.of(
            "HS256", "HS384", "HS512",
            "RS256", "RS384", "RS512",
            "PS256", "PS384", "PS512",
            "ES256", "ES384", "ES512",
            "EdDSA"
    );

    public UpdateTokenConfigDialog(KmsTokenConfigService tokenConfigService, TokenConfigDto dto, Runnable onSuccess) {
        super("Edit Token Configuration", onSuccess);
        this.tokenConfigService = tokenConfigService;
        this.original = dto;

        setOkButtonText("Save");
        setWidth("700px");

        buildForm();
        bindData();
        add(formLayout);
        setupAlgorithmChangeListener();
        updateFieldsForAlgorithm(original.getSignatureAlgorithm());
    }

    private void buildForm() {
        codeField = new TextField("Code");
        codeField.setReadOnly(true);
        codeField.setWidthFull();

        tokenTypeCombo = new ComboBox<>("Token type");
        tokenTypeCombo.setItems(IEnumToken.Types.values());
        tokenTypeCombo.setRequired(true);
        tokenTypeCombo.setRequiredIndicatorVisible(true);
        tokenTypeCombo.setReadOnly(true); // token type cannot be changed
        tokenTypeCombo.setWidthFull();

        issuerField = new TextField("Issuer");
        issuerField.setPlaceholder("e.g., https://kms.isygoit.eu");

        audienceField = new TextField("Audience");
        audienceField.setPlaceholder("e.g., kms-console");

        signatureAlgorithmCombo = new ComboBox<>("Signature algorithm");
        signatureAlgorithmCombo.setItems(SUPPORTED_ALGORITHMS);
        signatureAlgorithmCombo.setRequired(true);
        signatureAlgorithmCombo.setRequiredIndicatorVisible(true);

        // HMAC field
        secretKeyField = new TextField("Secret key (Base64)");
        secretKeyField.setRequired(true);
        secretKeyField.setRequiredIndicatorVisible(true);
        secretKeyField.setHelperText("For HS256 at least 32 bytes, HS384 → 48 bytes, HS512 → 64 bytes (Base64 encoded)");
        secretKeyField.setWidthFull();

        // Asymmetric fields
        privateKeyArea = new TextArea("Private key (PEM)");
        privateKeyArea.setRequired(true);
        privateKeyArea.setRequiredIndicatorVisible(true);
        privateKeyArea.setWidthFull();
        privateKeyArea.setHeight("150px");
        privateKeyArea.setPlaceholder("-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----");

        publicKeyArea = new TextArea("Public key (PEM) - for reference only");
        publicKeyArea.setReadOnly(true);
        publicKeyArea.setWidthFull();
        publicKeyArea.setHeight("100px");
        publicKeyArea.setPlaceholder("Public key will appear after generating a new key pair");

        generateKeyPairButton = new Button("Generate New Key Pair", event -> generateKeyPair());

        formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        // Code and token type on the same row
        formLayout.add(codeField, tokenTypeCombo);
        formLayout.add(issuerField, 2);
        formLayout.add(audienceField, 2);
        formLayout.add(signatureAlgorithmCombo, 1);
        // Dynamic fields will be added later
    }

    private void bindData() {
        codeField.setValue(original.getCode());
        tokenTypeCombo.setValue(original.getTokenType());
        issuerField.setValue(original.getIssuer() != null ? original.getIssuer() : "");
        audienceField.setValue(original.getAudience() != null ? original.getAudience() : "");
        signatureAlgorithmCombo.setValue(original.getSignatureAlgorithm());

        String storedKey = original.getSecretKey();
        if (HMAC_ALGORITHMS.contains(original.getSignatureAlgorithm())) {
            secretKeyField.setValue(storedKey != null ? storedKey : "");
        } else if (ASYMMETRIC_ALGORITHMS.contains(original.getSignatureAlgorithm())) {
            privateKeyArea.setValue(storedKey != null ? storedKey : "");
            // Public key is not stored in DTO, so we cannot restore it.
            publicKeyArea.setValue("(Not stored – generate new pair to see public key)");
        }
    }

    private void setupAlgorithmChangeListener() {
        signatureAlgorithmCombo.addValueChangeListener(event -> {
            String alg = event.getValue();
            if (alg != null) {
                updateFieldsForAlgorithm(alg);
            }
        });
    }

    private void updateFieldsForAlgorithm(String algorithm) {
        // Remove dynamic fields
        formLayout.remove(secretKeyField, privateKeyArea, publicKeyArea, generateKeyPairButton);

        if (HMAC_ALGORITHMS.contains(algorithm)) {
            int minBytes = switch (algorithm) {
                case "HS256" -> 32;
                case "HS384" -> 48;
                case "HS512" -> 64;
                default -> 32;
            };
            secretKeyField.setHelperText("For " + algorithm + " the secret key must be at least " + minBytes + " bytes (Base64 encoded)");
            secretKeyField.setVisible(true);
            secretKeyField.setRequired(true);
            formLayout.add(secretKeyField, 2);
            // Restore existing value if it was from HMAC
            if (original.getSecretKey() != null && HMAC_ALGORITHMS.contains(original.getSignatureAlgorithm())) {
                secretKeyField.setValue(original.getSecretKey());
            }
        } else if (ASYMMETRIC_ALGORITHMS.contains(algorithm)) {
            privateKeyArea.setRequired(true);
            privateKeyArea.setVisible(true);
            publicKeyArea.setVisible(true);
            generateKeyPairButton.setVisible(true);
            formLayout.add(privateKeyArea, 2);
            formLayout.add(publicKeyArea, 2);
            formLayout.add(generateKeyPairButton, 1);
            // Restore existing private key if any
            if (original.getSecretKey() != null && ASYMMETRIC_ALGORITHMS.contains(original.getSignatureAlgorithm())) {
                privateKeyArea.setValue(original.getSecretKey());
                // Public key not stored – show placeholder
                publicKeyArea.setValue("(Not stored – generate new pair to see public key)");
            } else {
                privateKeyArea.clear();
                publicKeyArea.clear();
            }
        }
    }

    private void generateKeyPair() {
        String algorithm = signatureAlgorithmCombo.getValue();
        if (algorithm == null || !ASYMMETRIC_ALGORITHMS.contains(algorithm)) {
            Notification.show("Select an asymmetric algorithm first", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        try {
            KeyPair keyPair;
            String jcaAlgorithm;
            int keySize = 0;
            String ecCurve = null;

            switch (algorithm) {
                case "RS256": jcaAlgorithm = "RSA"; keySize = 2048; break;
                case "RS384": jcaAlgorithm = "RSA"; keySize = 3072; break;
                case "RS512": jcaAlgorithm = "RSA"; keySize = 4096; break;
                case "PS256": jcaAlgorithm = "RSASSA-PSS"; keySize = 2048; break;
                case "PS384": jcaAlgorithm = "RSASSA-PSS"; keySize = 3072; break;
                case "PS512": jcaAlgorithm = "RSASSA-PSS"; keySize = 4096; break;
                case "ES256": jcaAlgorithm = "EC"; ecCurve = "secp256r1"; break;
                case "ES384": jcaAlgorithm = "EC"; ecCurve = "secp384r1"; break;
                case "ES512": jcaAlgorithm = "EC"; ecCurve = "secp521r1"; break;
                case "EdDSA": jcaAlgorithm = "Ed25519"; break;
                default:
                    throw new IllegalArgumentException("Unsupported asymmetric algorithm: " + algorithm);
            }

            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(jcaAlgorithm);
            if (keySize > 0) {
                keyPairGen.initialize(keySize);
            } else if (ecCurve != null) {
                keyPairGen.initialize(new ECGenParameterSpec(ecCurve));
            } else {
                keyPairGen.initialize(255);
            }
            keyPair = keyPairGen.generateKeyPair();

            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                    Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(privateKey.getEncoded()) +
                    "\n-----END PRIVATE KEY-----";
            String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
                    Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(publicKey.getEncoded()) +
                    "\n-----END PUBLIC KEY-----";

            privateKeyArea.setValue(privateKeyPem);
            publicKeyArea.setValue(publicKeyPem);

            Notification.show("New key pair generated. Private key updated.", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Failed to generate key pair: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
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
            // Validate Base64 and minimum length
            try {
                byte[] keyBytes = Base64.getDecoder().decode(secretKey);
                int requiredMinBytes = switch (signatureAlgorithm) {
                    case "HS256" -> 32;
                    case "HS384" -> 48;
                    case "HS512" -> 64;
                    default -> 32;
                };
                if (keyBytes.length < requiredMinBytes) {
                    showError("Secret key for " + signatureAlgorithm + " must be at least " + requiredMinBytes + " bytes (Base64 decoded length)");
                    return false;
                }
            } catch (IllegalArgumentException e) {
                showError("Secret key must be valid Base64");
                return false;
            }
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

        TokenConfigDto updated = TokenConfigDto.builder()
                .id(original.getId())
                .code(original.getCode())
                .tokenType(tokenType)
                .issuer(issuerField.getValue())
                .audience(audienceField.getValue())
                .signatureAlgorithm(signatureAlgorithm)
                .secretKey(secretOrPrivateKey)
                .build();

        try {
            ResponseEntity<TokenConfigDto> response = tokenConfigService.update(original.getId(), updated);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Configuration updated successfully", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                return true;
            } else {
                String errorMsg = "Update failed: " + response.getStatusCode();
                showError(errorMsg);
                return false;
            }
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
            Notification.show("Update error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            this.append(errorMsg);
            Notification.show("Update error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }
    }
}