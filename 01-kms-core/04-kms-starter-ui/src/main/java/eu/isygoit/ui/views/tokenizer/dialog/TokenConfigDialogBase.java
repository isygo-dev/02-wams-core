package eu.isygoit.ui.views.tokenizer.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.ui.views.BaseActionDialog;
import feign.FeignException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

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

    // Common UI components
    protected ComboBox<IEnumToken.Types> tokenTypeCombo;
    protected TextField issuerField;
    protected AudienceInput audienceInput;          // replaces simple TextField
    protected ComboBox<String> signatureAlgorithmCombo;

    // HMAC field
    protected TextField secretKeyField;

    // Asymmetric fields
    protected TextArea privateKeyArea;
    protected TextArea publicKeyArea;
    protected Button generateKeyPairButton;
    protected Button copyPublicKeyButton;
    protected VerticalLayout publicKeyComponent;

    protected FormLayout formLayout;

    protected TokenConfigDialogBase(String title, Runnable onSuccess) {
        super(title, onSuccess);
        setWidth("750px");
    }

    protected void buildCommonForm() {
        tokenTypeCombo = new ComboBox<>("Token type");
        tokenTypeCombo.setItems(IEnumToken.Types.values());
        tokenTypeCombo.setRequired(true);
        tokenTypeCombo.setRequiredIndicatorVisible(true);
        tokenTypeCombo.setValue(IEnumToken.Types.ACCESS);

        issuerField = new TextField("Issuer");
        issuerField.setPlaceholder("e.g., https://kms.isygoit.eu");
        issuerField.setWidthFull();

        audienceInput = new AudienceInput();
        audienceInput.setWidthFull();

        signatureAlgorithmCombo = new ComboBox<>("Signature algorithm");
        signatureAlgorithmCombo.setItems(SUPPORTED_ALGORITHMS);
        signatureAlgorithmCombo.setRequired(true);
        signatureAlgorithmCombo.setRequiredIndicatorVisible(true);

        // HMAC field
        secretKeyField = new TextField("Secret key (Base64)");
        secretKeyField.setRequired(true);
        secretKeyField.setRequiredIndicatorVisible(true);
        secretKeyField.setWidthFull();

        // Asymmetric fields
        privateKeyArea = new TextArea("Private key (PEM)");
        privateKeyArea.setRequired(true);
        privateKeyArea.setRequiredIndicatorVisible(true);
        privateKeyArea.setWidthFull();
        privateKeyArea.setHeight("150px");
        privateKeyArea.setPlaceholder("-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----");
        privateKeyArea.addClassName("no-copy");

        publicKeyArea = new TextArea();
        publicKeyArea.setReadOnly(true);
        publicKeyArea.setWidthFull();
        publicKeyArea.setHeight("100px");
        publicKeyArea.setPlaceholder("Generate a key pair to see the public key");

        publicKeyComponent = createPublicKeyComponent();

        generateKeyPairButton = new Button("Generate Key Pair", event -> generateKeyPair());

        formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
    }

    private VerticalLayout createPublicKeyComponent() {
        HorizontalLayout header = new HorizontalLayout();
        Span label = new Span("Public key (PEM) - for reference only");
        label.getStyle().set("font-weight", "bold");
        label.getStyle().set("margin-right", "auto");

        copyPublicKeyButton = new Button(new Icon(VaadinIcon.COPY));
        copyPublicKeyButton.addClickListener(e -> copyToClipboard(publicKeyArea.getValue()));
        copyPublicKeyButton.setTooltipText("Copy public key");
        copyPublicKeyButton.getStyle().set("margin-left", "auto");

        header.add(label, copyPublicKeyButton);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        VerticalLayout wrapper = new VerticalLayout(header, publicKeyArea);
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.setWidthFull();
        return wrapper;
    }

    protected void addCommonFieldsToLayout() {
        formLayout.add(tokenTypeCombo, signatureAlgorithmCombo);
        formLayout.add(issuerField, 2);
        formLayout.add(audienceInput, 2);
    }

    protected void setupAlgorithmChangeListener() {
        signatureAlgorithmCombo.addValueChangeListener(event -> {
            String alg = event.getValue();
            if (alg != null) {
                updateFieldsForAlgorithm(alg);
            }
        });
    }

    protected void updateFieldsForAlgorithm(String algorithm) {
        formLayout.remove(secretKeyField, privateKeyArea, publicKeyComponent, generateKeyPairButton);

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
            publicKeyComponent.setVisible(true);
            generateKeyPairButton.setVisible(true);
            formLayout.add(privateKeyArea, 2);
            formLayout.add(publicKeyComponent, 2);
            formLayout.add(generateKeyPairButton, 2);
        }
    }

    protected void generateKeyPair() {
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

            Notification.show("Key pair generated successfully", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Failed to generate key pair: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    protected void copyToClipboard(String text) {
        if (text == null || text.isBlank()) {
            Notification.show("Nothing to copy", 2000, Notification.Position.MIDDLE)
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

    protected void showError(String message) {
        Notification.show(message, 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    protected void handleFeignException(FeignException ex) {
        String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
        this.append(errorMsg);
        Notification.show("Error: " + errorMsg, 6000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    protected void handleGenericException(Exception ex) {
        String errorMsg = ex.getMessage();
        this.append(errorMsg);
        Notification.show("Error: " + errorMsg, 6000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    // Methods for audience list management
    protected List<String> getAudienceList() {
        return audienceInput.getAudiences();
    }

    protected void setAudienceList(List<String> audiences) {
        audienceInput.setAudiences(audiences);
    }

    // Abstract methods
    protected abstract void bindData();

    protected abstract void onSaveSuccess();

    // ================================
    // Inner class: AudienceInput (chips)
    // ================================
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

        private void addAudience() {
            String value = inputField.getValue();
            if (value == null || value.isBlank()) {
                Notification.show("Audience cannot be empty", 2000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            value = value.trim();
            if (getAudiences().contains(value)) {
                Notification.show("Audience already added", 2000, Notification.Position.MIDDLE)
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
                    // Remove the close icon character (✕) if present
                    text = text.replace("✕", "").trim();
                    if (!text.isEmpty()) {
                        list.add(text);
                    }
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