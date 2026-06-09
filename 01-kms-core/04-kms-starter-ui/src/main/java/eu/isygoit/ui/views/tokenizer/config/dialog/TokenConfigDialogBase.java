package eu.isygoit.ui.views.tokenizer.config.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.common.dialog.BaseActionDialog;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class TokenConfigDialogBase extends BaseActionDialog {

    // Algorithm groups
    protected static final List<String> HMAC_ALGORITHMS = List.of("HS256", "HS384", "HS512");
    protected static final List<String> ASYMMETRIC_ALGORITHMS = List.of(
            "RS256", "RS384", "RS512",
            "PS256", "PS384", "PS512",
            "ES256", "ES384", "ES512",
            "EdDSA"
    );
    protected static final List<String> SUPPORTED_ALGORITHMS = List.of(
            "HS256", "HS384", "HS512",
            "RS256", "RS384", "RS512",
            "PS256", "PS384", "PS512",
            "ES256", "ES384", "ES512",
            "EdDSA"
    );

    // Services
    protected final KmsApiService kmsApiService;
    protected List<KeyOption> availableKeyOptions = new ArrayList<>();

    // UI components
    protected RadioButtonGroup<String> keySourceGroup;
    protected ComboBox<KeyOption> kmsKeyCombo;
    protected VerticalLayout kmsKeyLayout;
    protected VerticalLayout customKeyLayout;
    protected Card metadataCard;
    protected Card cryptoCard;
    protected VerticalLayout cryptoCardContent;

    // Metadata fields
    protected ComboBox<IEnumToken.Types> tokenTypeCombo;
    protected TextField issuerField;
    protected AudienceInput audienceInput;
    protected IntegerField lifeTimeValueField;
    protected ComboBox<String> lifeTimeUnitCombo;
    protected Checkbox noExpirationCheckbox;
    protected HorizontalLayout lifetimeRow;

    // Custom key fields
    protected ComboBox<String> signatureAlgorithmCombo;
    protected TextField secretKeyField;
    protected TextArea privateKeyArea;
    protected TextArea publicKeyArea;
    protected Button generateKeyPairButton;
    protected Button copyPublicKeyButton;
    protected VerticalLayout publicKeyComponent;

    protected TokenConfigDialogBase(String title, Runnable onSuccess, KmsApiService kmsApiService) {
        super(title, onSuccess);
        this.kmsApiService = kmsApiService;
        setWidth("850px");
    }

    protected void initUI() {
        buildComponents();
        VerticalLayout mainLayout = new VerticalLayout(metadataCard, cryptoCard);
        mainLayout.setSpacing(true);
        mainLayout.setPadding(false);
        add(mainLayout);
        setupKeySourceListener();
        setupAlgorithmChangeListener();
        loadKmsKeys();
    }

    private void buildComponents() {
        // ---------- Metadata Card ----------
        metadataCard = new Card();
        metadataCard.addClassName("config-metadata-card");
        metadataCard.setWidthFull();
        Span metaTitle = new Span("Token Metadata");
        metaTitle.addClassName(LumoUtility.FontWeight.BOLD);
        metaTitle.addClassName(LumoUtility.FontSize.MEDIUM);

        // Token type
        tokenTypeCombo = new ComboBox<>("Token type");
        tokenTypeCombo.setItems(IEnumToken.Types.values());
        tokenTypeCombo.setRequired(true);
        tokenTypeCombo.setRequiredIndicatorVisible(true);
        tokenTypeCombo.setValue(IEnumToken.Types.ACCESS);
        tokenTypeCombo.setWidthFull();
        tokenTypeCombo.setTooltipText("Type of token: ACCESS (short-lived) or REFRESH (longer-lived).");

        // Issuer
        issuerField = new TextField("Issuer");
        issuerField.setPlaceholder("e.g., https://kms.isygoit.eu");
        issuerField.setWidthFull();
        issuerField.setTooltipText("The 'iss' claim – typically the base URL of your token issuer.");

        // Audience input (custom component)
        audienceInput = new AudienceInput();
        audienceInput.setWidthFull();
        audienceInput.setTooltipText("The intended recipient(s) of the token ('aud' claim). Add one or more URLs or identifiers.");

        // Lifetime row with "No expiration" checkbox
        noExpirationCheckbox = new Checkbox("No expiration");
        noExpirationCheckbox.setTooltipText("If checked, the token will never expire (lifeTimeInMs = null).");
        noExpirationCheckbox.addValueChangeListener(e -> {
            boolean noExp = e.getValue();
            lifeTimeValueField.setEnabled(!noExp);
            lifeTimeUnitCombo.setEnabled(!noExp);
            if (noExp) {
                lifeTimeValueField.setValue(1);
                lifeTimeUnitCombo.setValue("Hours");
            }
        });

        lifeTimeValueField = new IntegerField();
        lifeTimeValueField.setPlaceholder("e.g., 1");
        lifeTimeValueField.setValue(1);
        lifeTimeValueField.setWidth("50%");
        lifeTimeValueField.setStepButtonsVisible(true);
        lifeTimeValueField.setMin(1);
        lifeTimeValueField.setEnabled(true);
        lifeTimeValueField.setTooltipText("Numeric value of the lifetime.");

        lifeTimeUnitCombo = new ComboBox<>();
        lifeTimeUnitCombo.setItems("Seconds", "Minutes", "Hours", "Days");
        lifeTimeUnitCombo.setValue("Hours");
        lifeTimeUnitCombo.setWidth("30%");
        lifeTimeUnitCombo.setEnabled(true);
        lifeTimeUnitCombo.setTooltipText("Unit of the lifetime value.");

        Span lifetimeLabel = new Span("Lifetime:");
        lifetimeLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        lifetimeLabel.getStyle().set("margin-right", "var(--lumo-space-s)");

        lifetimeRow = new HorizontalLayout(lifetimeLabel, lifeTimeValueField, lifeTimeUnitCombo, noExpirationCheckbox);
        lifetimeRow.setWidthFull();
        lifetimeRow.setAlignItems(FlexComponent.Alignment.CENTER);
        lifetimeRow.setFlexGrow(1, lifeTimeValueField);
        lifetimeRow.setSpacing(true);

        VerticalLayout metaForm = new VerticalLayout();
        metaForm.setSpacing(true);
        metaForm.setPadding(false);
        metaForm.add(tokenTypeCombo, issuerField, audienceInput, lifetimeRow);
        metadataCard.add(metaTitle, metaForm);

        // ---------- Crypto Card ----------
        cryptoCard = new Card();
        cryptoCard.addClassName("config-crypto-card");
        cryptoCard.setWidthFull();
        Span cryptoTitle = new Span("Cryptographic Material");
        cryptoTitle.addClassName(LumoUtility.FontWeight.BOLD);
        cryptoTitle.addClassName(LumoUtility.FontSize.MEDIUM);
        cryptoCard.add(cryptoTitle);

        cryptoCardContent = new VerticalLayout();
        cryptoCardContent.setPadding(false);
        cryptoCardContent.setSpacing(true);
        cryptoCard.add(cryptoCardContent);

        // Key source selection
        keySourceGroup = new RadioButtonGroup<>();
        keySourceGroup.setLabel("Key source");
        keySourceGroup.setItems("Use existing KMS key", "Define custom key");
        keySourceGroup.setValue("Define custom key");
        keySourceGroup.setWidthFull();
        keySourceGroup.setTooltipText("Choose whether to reference an existing KMS key or provide custom cryptographic material.");
        cryptoCardContent.add(keySourceGroup);

        // KMS key selection layout
        kmsKeyLayout = new VerticalLayout();
        kmsKeyLayout.setPadding(false);
        kmsKeyLayout.setSpacing(true);
        kmsKeyLayout.setVisible(false);
        Span kmsKeyLabel = new Span("Select KMS key:");
        kmsKeyLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        kmsKeyCombo = new ComboBox<>();
        kmsKeyCombo.setPlaceholder("Choose a KMS key...");
        kmsKeyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        kmsKeyCombo.setWidthFull();
        kmsKeyCombo.setRequired(true);
        kmsKeyCombo.setTooltipText("Existing key stored in the KMS. The token will be signed using that key.");
        kmsKeyLayout.add(kmsKeyLabel, kmsKeyCombo);
        cryptoCardContent.add(kmsKeyLayout);

        // Custom key layout (dynamic)
        customKeyLayout = new VerticalLayout();
        customKeyLayout.setPadding(false);
        customKeyLayout.setSpacing(true);

        signatureAlgorithmCombo = new ComboBox<>("Signature algorithm");
        signatureAlgorithmCombo.setItems(SUPPORTED_ALGORITHMS);
        signatureAlgorithmCombo.setRequired(true);
        signatureAlgorithmCombo.setRequiredIndicatorVisible(true);
        signatureAlgorithmCombo.setValue("HS256");
        signatureAlgorithmCombo.setWidthFull();
        signatureAlgorithmCombo.setTooltipText("JWS algorithm used to sign the token. HS* = HMAC (shared secret), RS*/PS*/ES*/EdDSA = asymmetric key pair.");
        customKeyLayout.add(signatureAlgorithmCombo);

        secretKeyField = new TextField("Secret key (Base64)");
        secretKeyField.setRequired(true);
        secretKeyField.setRequiredIndicatorVisible(true);
        secretKeyField.setWidthFull();
        secretKeyField.setTooltipText("Base64-encoded secret key for HMAC algorithms. Minimum length depends on algorithm: HS256=32B, HS384=48B, HS512=64B.");

        privateKeyArea = new TextArea("Private key (PEM)");
        privateKeyArea.setRequired(true);
        privateKeyArea.setRequiredIndicatorVisible(true);
        privateKeyArea.setWidthFull();
        privateKeyArea.setHeight("150px");
        privateKeyArea.setPlaceholder("-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----");
        privateKeyArea.addClassName("no-copy");
        privateKeyArea.setTooltipText("PEM-encoded private key for asymmetric algorithms. Can be generated with the 'Generate Key Pair' button.");

        publicKeyArea = new TextArea();
        publicKeyArea.setReadOnly(true);
        publicKeyArea.setWidthFull();
        publicKeyArea.setHeight("100px");
        publicKeyArea.setPlaceholder("Generate a key pair to see the public key");
        publicKeyArea.setTooltipText("Corresponding public key (PEM). Not stored on the server – used for verification only.");

        generateKeyPairButton = new Button("Generate Key Pair", event -> generateKeyPair());
        generateKeyPairButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        generateKeyPairButton.setWidthFull();
        generateKeyPairButton.setTooltipText("Generate a new RSA/EC/EdDSA key pair matching the selected algorithm. Private key will be filled; public key is displayed for reference.");

        publicKeyComponent = createPublicKeyComponent();

        customKeyLayout.add(secretKeyField); // initially HMAC (HS256)
        cryptoCardContent.add(customKeyLayout);
    }

    private VerticalLayout createPublicKeyComponent() {
        HorizontalLayout header = new HorizontalLayout();
        Span label = new Span("Public key (PEM) - for reference only");
        label.getStyle().set("font-weight", "bold");
        label.getStyle().set("margin-right", "auto");

        copyPublicKeyButton = new Button(new Icon(VaadinIcon.COPY));
        copyPublicKeyButton.addClickListener(e -> copyToClipboard(publicKeyArea.getValue()));
        copyPublicKeyButton.setTooltipText("Copy public key to clipboard");
        copyPublicKeyButton.getStyle().set("margin-left", "auto");
        copyPublicKeyButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        header.add(label, copyPublicKeyButton);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        VerticalLayout wrapper = new VerticalLayout(header, publicKeyArea);
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.setWidthFull();
        return wrapper;
    }

    private void setupKeySourceListener() {
        keySourceGroup.addValueChangeListener(event -> {
            String value = event.getValue();
            if ("Use existing KMS key".equals(value)) {
                kmsKeyLayout.setVisible(true);
                customKeyLayout.setVisible(false);
                secretKeyField.clear();
                privateKeyArea.clear();
                publicKeyArea.clear();
                signatureAlgorithmCombo.setRequired(false);
            } else {
                kmsKeyLayout.setVisible(false);
                customKeyLayout.setVisible(true);
                kmsKeyCombo.clear();
                signatureAlgorithmCombo.setRequired(true);
                updateCryptographySection(signatureAlgorithmCombo.getValue());
            }
        });
    }

    protected void loadKmsKeys() {
        if (kmsApiService == null) return;
        try {
            ResponseEntity<KmsDtos.ListKeysResponse> response = kmsApiService.listKeys(100, null);
            KmsDtos.ListKeysResponse keys = response.getBody();
            if (keys != null && keys.getKeys() != null) {
                availableKeyOptions = keys.getKeys().stream()
                        .map(entry -> new KeyOption(entry.getKeyId(), fetchAlias(entry.getKeyId())))
                        .collect(Collectors.toList());
                kmsKeyCombo.setItems(availableKeyOptions);
            } else {
                availableKeyOptions = new ArrayList<>();
                kmsKeyCombo.setItems(availableKeyOptions);
            }
        } catch (FeignException ex) {
            log.error("Failed to load KMS keys: {}", ex.getMessage());
            Notification.show("Could not load KMS keys: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            log.error("Failed to load KMS keys", e);
        }
    }

    private String fetchAlias(String keyId) {
        try {
            ResponseEntity<KmsDtos.DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
            KmsDtos.DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null && desc.getKeyMetadata().getKeyAlias() != null) {
                return desc.getKeyMetadata().getKeyAlias();
            }
        } catch (Exception e) {
            // ignore
        }
        return keyId;
    }

    protected void setupAlgorithmChangeListener() {
        signatureAlgorithmCombo.addValueChangeListener(event -> {
            String alg = event.getValue();
            if (alg != null && customKeyLayout.isVisible()) {
                updateCryptographySection(alg);
            }
        });
    }

    protected void updateCryptographySection(String algorithm) {
        customKeyLayout.remove(secretKeyField, privateKeyArea, publicKeyComponent, generateKeyPairButton);

        if (HMAC_ALGORITHMS.contains(algorithm)) {
            int minBytes = switch (algorithm) {
                case "HS256" -> 32;
                case "HS384" -> 48;
                case "HS512" -> 64;
                default -> 32;
            };
            secretKeyField.setHelperText("For " + algorithm + " the secret key must be at least " + minBytes + " bytes (Base64 encoded)");
            customKeyLayout.add(secretKeyField);
        } else if (ASYMMETRIC_ALGORITHMS.contains(algorithm)) {
            privateKeyArea.clear();
            publicKeyArea.clear();
            customKeyLayout.add(privateKeyArea);
            customKeyLayout.add(publicKeyComponent);
            customKeyLayout.add(generateKeyPairButton);
        }
    }

    protected void generateKeyPair() {
        String algorithm = signatureAlgorithmCombo.getValue();
        if (algorithm == null || !ASYMMETRIC_ALGORITHMS.contains(algorithm)) {
            Notification.show("Select an asymmetric algorithm first", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        try {
            KeyPair keyPair;
            String jcaAlgorithm;
            int keySize = 0;
            String ecCurve = null;

            switch (algorithm) {
                case "RS256":
                    jcaAlgorithm = "RSA";
                    keySize = 2048;
                    break;
                case "RS384":
                    jcaAlgorithm = "RSA";
                    keySize = 3072;
                    break;
                case "RS512":
                    jcaAlgorithm = "RSA";
                    keySize = 4096;
                    break;
                case "PS256":
                    jcaAlgorithm = "RSASSA-PSS";
                    keySize = 2048;
                    break;
                case "PS384":
                    jcaAlgorithm = "RSASSA-PSS";
                    keySize = 3072;
                    break;
                case "PS512":
                    jcaAlgorithm = "RSASSA-PSS";
                    keySize = 4096;
                    break;
                case "ES256":
                    jcaAlgorithm = "EC";
                    ecCurve = "secp256r1";
                    break;
                case "ES384":
                    jcaAlgorithm = "EC";
                    ecCurve = "secp384r1";
                    break;
                case "ES512":
                    jcaAlgorithm = "EC";
                    ecCurve = "secp521r1";
                    break;
                case "EdDSA":
                    jcaAlgorithm = "Ed25519";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported asymmetric algorithm: " + algorithm);
            }

            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(jcaAlgorithm);
            if (keySize > 0) keyPairGen.initialize(keySize);
            else if (ecCurve != null) keyPairGen.initialize(new ECGenParameterSpec(ecCurve));
            else keyPairGen.initialize(255);

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

            Notification.show("Key pair generated successfully", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Failed to generate key pair: " + e.getMessage(), 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // ---------- Utility Methods ----------
    protected void copyToClipboard(String text) {
        if (text == null || text.isBlank()) {
            Notification.show("Nothing to copy", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        UI.getCurrent().getPage().executeJs(
                "navigator.clipboard.writeText($0).then(() => {" +
                        "  const notification = document.createElement('div');" +
                        "  notification.textContent = 'Copied to clipboard';" +
                        "  notification.style.position = 'fixed';" +
                        "  notification.style.bottom = '20px';" +
                        "  notification.style.left = '50%';" +
                        "  notification.style.transform = 'translateX(-50%)';" +
                        "  notification.style.backgroundColor = '#4caf50';" +
                        "  notification.style.color = 'white';" +
                        "  notification.style.padding = '8px 16px';" +
                        "  notification.style.borderRadius = '4px';" +
                        "  notification.style.zIndex = '10000';" +
                        "  document.body.appendChild(notification);" +
                        "  setTimeout(() => notification.remove(), 2000);" +
                        "});",
                text);
    }

    protected boolean validateHmacKey(String algorithm, String secretKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            int requiredMinBytes = switch (algorithm) {
                case "HS256" -> 32;
                case "HS384" -> 48;
                case "HS512" -> 64;
                default -> 32;
            };
            if (keyBytes.length < requiredMinBytes) {
                showError("Secret key for " + algorithm + " must be at least " + requiredMinBytes + " bytes (Base64 decoded length)");
                return false;
            }
            return true;
        } catch (IllegalArgumentException e) {
            showError("Secret key must be valid Base64");
            return false;
        }
    }

    protected void handleFeignException(FeignException ex) {
        String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
        this.append(errorMsg);
    }

    protected void handleGenericException(Exception ex) {
        this.append(ex.getMessage());
    }

    // ---------- Audience Management ----------
    protected List<String> getAudienceList() {
        return audienceInput.getAudiences();
    }

    protected void setAudienceList(List<String> audiences) {
        audienceInput.setAudiences(audiences);
    }

    // ---------- Lifetime Management (with optional expiration) ----------
    protected Integer getLifeTimeInMs() {
        if (noExpirationCheckbox.getValue()) {
            return null;   // no expiration
        }
        Integer value = lifeTimeValueField.getValue();
        if (value == null || value <= 0) {
            showError("Lifetime value must be a positive number");
            return null;
        }
        String unit = lifeTimeUnitCombo.getValue();
        if (unit == null) {
            showError("Please select a time unit");
            return null;
        }
        int ms;
        switch (unit) {
            case "Seconds":
                ms = value * 1000;
                break;
            case "Minutes":
                ms = value * 60 * 1000;
                break;
            case "Hours":
                ms = value * 60 * 60 * 1000;
                break;
            case "Days":
                ms = value * 24 * 60 * 60 * 1000;
                break;
            default:
                throw new IllegalStateException("Unknown unit: " + unit);
        }
        return ms;
    }

    protected void setLifeTimeFromMs(Integer ms) {
        if (ms == null || ms <= 0) {
            // No expiration
            noExpirationCheckbox.setValue(true);
            lifeTimeValueField.setEnabled(false);
            lifeTimeUnitCombo.setEnabled(false);
            // Placeholder values (won't be used)
            lifeTimeValueField.setValue(1);
            lifeTimeUnitCombo.setValue("Hours");
            return;
        }
        noExpirationCheckbox.setValue(false);
        lifeTimeValueField.setEnabled(true);
        lifeTimeUnitCombo.setEnabled(true);

        // Determine best unit
        if (ms % (24 * 60 * 60 * 1000) == 0 && ms >= (24 * 60 * 60 * 1000)) {
            lifeTimeValueField.setValue(ms / (24 * 60 * 60 * 1000));
            lifeTimeUnitCombo.setValue("Days");
        } else if (ms % (60 * 60 * 1000) == 0) {
            lifeTimeValueField.setValue(ms / (60 * 60 * 1000));
            lifeTimeUnitCombo.setValue("Hours");
        } else if (ms % (60 * 1000) == 0) {
            lifeTimeValueField.setValue(ms / (60 * 1000));
            lifeTimeUnitCombo.setValue("Minutes");
        } else {
            lifeTimeValueField.setValue(ms / 1000);
            lifeTimeUnitCombo.setValue("Seconds");
            if (ms % 1000 != 0) {
                Notification.show("Lifetime millisecond precision lost, rounded to seconds", 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
        }
    }

    // ---------- Abstract methods to be implemented by concrete dialogs ----------
    protected abstract void bindData();

    protected abstract void onSaveSuccess();

    // ---------- Inner classes ----------
    protected static class KeyOption {
        private final String keyId;
        private final String displayName;

        KeyOption(String keyId, String aliasOrId) {
            this.keyId = keyId;
            this.displayName = (aliasOrId != null && !aliasOrId.equals(keyId)) ? aliasOrId + " (" + keyId + ")" : keyId;
        }

        public String getKeyId() {
            return keyId;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Custom audience input component (unchanged, but added tooltip support)
    protected static class AudienceInput extends VerticalLayout {
        private final TextField inputField;
        private final Button addButton;
        private final HorizontalLayout chipsContainer;

        public AudienceInput() {
            setPadding(false);
            setSpacing(false);

            inputField = new TextField();
            inputField.setPlaceholder("Enter audience (e.g., https://api.example.com)");
            inputField.setWidthFull();

            addButton = new Button("Add", new Icon(VaadinIcon.PLUS));
            addButton.addClickListener(e -> addAudience());

            HorizontalLayout inputRow = new HorizontalLayout(inputField, addButton);
            inputRow.setWidthFull();
            inputRow.setFlexGrow(1, inputField);

            chipsContainer = new HorizontalLayout();
            chipsContainer.setSpacing(true);
            chipsContainer.setWidthFull();
            chipsContainer.getStyle().set("flex-wrap", "wrap");

            add(inputRow, chipsContainer);
        }

        public void setTooltipText(String tooltip) {
            inputField.setTooltipText(tooltip);
        }

        private void addAudience() {
            String value = inputField.getValue();
            if (value == null || value.isBlank()) {
                Notification.show("Audience cannot be empty", 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            value = value.trim();
            if (getAudiences().contains(value)) {
                Notification.show("Audience already added", 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            addChip(value);
            inputField.clear();
        }

        private void addChip(String audience) {
            Span chip = new Span(audience);
            chip.getStyle()
                    .set("background-color", "#e0e0e0")
                    .set("border-radius", "16px")
                    .set("padding", "4px 12px")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("display", "inline-flex")
                    .set("align-items", "center")
                    .set("gap", "8px");

            Button removeButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
            removeButton.addThemeName("tertiary-inline");
            removeButton.addClickListener(e -> chipsContainer.remove(chip));
            chip.add(removeButton);
            chipsContainer.add(chip);
        }

        public List<String> getAudiences() {
            List<String> list = new ArrayList<>();
            for (Component component : chipsContainer.getChildren().collect(Collectors.toList())) {
                if (component instanceof Span) {
                    String text = ((Span) component).getText();
                    text = text.replace("✕", "").trim();
                    if (!text.isEmpty()) list.add(text);
                }
            }
            return list;
        }

        public void setAudiences(List<String> audiences) {
            chipsContainer.removeAll();
            if (audiences != null) {
                audiences.forEach(this::addChip);
            }
        }
    }
}