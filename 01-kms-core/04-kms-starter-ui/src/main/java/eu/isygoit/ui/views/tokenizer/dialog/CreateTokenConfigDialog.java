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
import io.jsonwebtoken.Jwts;
import org.springframework.http.ResponseEntity;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class CreateTokenConfigDialog extends BaseActionDialog {

    private final KmsTokenConfigService tokenConfigService;

    private ComboBox<IEnumToken.Types> tokenTypeCombo;
    private TextField issuerField;
    private TextField audienceField;
    private ComboBox<String> signatureAlgorithmCombo;

    // HMAC fields
    private TextField secretKeyField;

    // Asymmetric fields
    private TextArea privateKeyArea;
    private TextArea publicKeyArea;   // read-only, for information
    private Button generateKeyPairButton;

    private FormLayout formLayout;

    // Algorithm groups
    private static final List<String> HMAC_ALGORITHMS = List.of("HS256", "HS384", "HS512");
    private static final List<String> ASYMMETRIC_ALGORITHMS = List.of(
            "RS256", "RS384", "RS512",
            "PS256", "PS384", "PS512",
            "ES256", "ES384", "ES512",
            "EdDSA"
    );

    // All supported algorithms from Jwts.SIG (excluding "none")
    private static final List<String> SUPPORTED_ALGORITHMS = List.of(
            "HS256", "HS384", "HS512",
            "RS256", "RS384", "RS512",
            "PS256", "PS384", "PS512",
            "ES256", "ES384", "ES512",
            "EdDSA"
    );

    public CreateTokenConfigDialog(KmsTokenConfigService tokenConfigService, Runnable onSuccess) {
        super("Create Token Configuration", onSuccess);
        this.tokenConfigService = tokenConfigService;

        setOkButtonText("Create");
        setWidth("700px");

        buildForm();
        add(formLayout);
        setupAlgorithmChangeListener();
        // Default to HS256 (HMAC)
        updateFieldsForAlgorithm("HS256");
    }

    private void buildForm() {
        tokenTypeCombo = new ComboBox<>("Token type");
        tokenTypeCombo.setItems(IEnumToken.Types.values());
        tokenTypeCombo.setRequired(true);
        tokenTypeCombo.setRequiredIndicatorVisible(true);
        tokenTypeCombo.setValue(IEnumToken.Types.ACCESS);

        issuerField = new TextField("Issuer");
        issuerField.setPlaceholder("e.g., https://kms.isygoit.eu");

        audienceField = new TextField("Audience");
        audienceField.setPlaceholder("e.g., kms-console");

        signatureAlgorithmCombo = new ComboBox<>("Signature algorithm");
        signatureAlgorithmCombo.setItems(SUPPORTED_ALGORITHMS);
        signatureAlgorithmCombo.setRequired(true);
        signatureAlgorithmCombo.setRequiredIndicatorVisible(true);
        signatureAlgorithmCombo.setValue("HS256");

        // HMAC field
        secretKeyField = new TextField("Secret key (Base64)");
        secretKeyField.setRequired(true);
        secretKeyField.setRequiredIndicatorVisible(true);
        secretKeyField.setHelperText("For HS256 at least 32 bytes, HS384 → 48 bytes, HS512 → 64 bytes (Base64 encoded)");

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

        generateKeyPairButton = new Button("Generate Key Pair", event -> generateKeyPair());

        formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        formLayout.setColspan(tokenTypeCombo, 1);
        formLayout.setColspan(issuerField, 2);
        formLayout.setColspan(audienceField, 2);
        formLayout.setColspan(signatureAlgorithmCombo, 1);
        // Start with HMAC field (will be replaced by listener if needed)
        formLayout.add(tokenTypeCombo, issuerField, audienceField, signatureAlgorithmCombo, secretKeyField);
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
        } else if (ASYMMETRIC_ALGORITHMS.contains(algorithm)) {
            privateKeyArea.clear();
            publicKeyArea.clear();
            privateKeyArea.setRequired(true);
            privateKeyArea.setVisible(true);
            publicKeyArea.setVisible(true);
            generateKeyPairButton.setVisible(true);
            formLayout.add(privateKeyArea, 2);
            formLayout.add(publicKeyArea, 2);
            formLayout.add(generateKeyPairButton, 1);
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

            // Map JJWT algorithm name to Java security parameters
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
                case "EdDSA": jcaAlgorithm = "Ed25519"; break; // Needs Java 15+ or Bouncy Castle
                default:
                    throw new IllegalArgumentException("Unsupported asymmetric algorithm: " + algorithm);
            }

            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(jcaAlgorithm);
            if (keySize > 0) {
                keyPairGen.initialize(keySize);
            } else if (ecCurve != null) {
                keyPairGen.initialize(new ECGenParameterSpec(ecCurve));
            } else {
                // EdDSA: use key size hint (Ed25519)
                keyPairGen.initialize(255);
            }
            keyPair = keyPairGen.generateKeyPair();

            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            // Simple PEM encoding (Base64 with line breaks)
            String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                    Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(privateKey.getEncoded()) +
                    "\n-----END PRIVATE KEY-----";
            String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
                    Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(publicKey.getEncoded()) +
                    "\n-----END PUBLIC KEY-----";

            privateKeyArea.setValue(privateKeyPem);
            publicKeyArea.setValue(publicKeyPem);

            Notification.show("Key pair generated successfully", 3000, Notification.Position.MIDDLE)
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

        String generatedCode = "TC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        TokenConfigDto dto = TokenConfigDto.builder()
                .code(generatedCode)
                .tokenType(tokenType)
                .issuer(issuerField.getValue())
                .audience(audienceField.getValue())
                .signatureAlgorithm(signatureAlgorithm)
                .secretKey(secretOrPrivateKey)
                .build();

        try {
            ResponseEntity<TokenConfigDto> response = tokenConfigService.create(dto);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Configuration created successfully", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                return true;
            } else {
                String errorMsg = "Creation failed: " + response.getStatusCode();
                showError(errorMsg);
                return false;
            }
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg); // Assumes BaseActionDialog has append() – otherwise comment or log
            Notification.show("Creation error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            this.append(errorMsg);
            Notification.show("Creation error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }
    }
}