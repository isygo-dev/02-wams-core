package eu.isygoit.ui.kms.views.tokenizer.config.dialog;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
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
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.exception.InvalidUnitException;
import eu.isygoit.exception.UnsupportedAsymmetricAlgorithmException;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsActionDialog;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;
import eu.isygoit.ui.kms.views.secrets.SecretsDialogSupport;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Shared form of the token configuration create/update dialogs.
 *
 * <p>Mapping of the form to {@link TokenConfigDto}:
 * <ul>
 *   <li>{@code code}, {@code tenant}: read-only (assigned by the server);</li>
 *   <li>{@code tokenType}, {@code issuer}, {@code audience}: type combo, issuer field, audience chips;</li>
 *   <li>{@code lifeTimeInMs}: UI representation = value + unit + "no expiration" checkbox
 *       (see {@link #getLifeTimeInMs()} / {@link #setLifeTimeFromMs(Integer)});</li>
 *   <li>{@code kmsKeyId}: KMS key combo, used when the key source is "KMS";</li>
 *   <li>{@code signatureAlgorithm}: algorithm combo, used when the key source is "custom";</li>
 *   <li>{@code secretKey}: UI representation = HMAC secret field, or the private key area
 *       for asymmetric algorithms;</li>
 *   <li>{@code publicKey}: read-only area filled by "generate key pair".</li>
 * </ul>
 * {@code id} is never shown. Subclasses decide how the DTO is built and sent.
 */
@Slf4j
public abstract class TokenConfigDialogBase extends KmsActionDialog {

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
    protected VerticalLayout customKeyLayout;

    // Identity / metadata fields
    protected TextField codeField;
    protected TextField tenantField;
    protected ComboBox<IEnumToken.Types> tokenTypeCombo;
    protected TextField issuerField;
    protected AudienceInput audienceInput;
    protected IntegerField lifeTimeValueField;
    protected ComboBox<String> lifeTimeUnitCombo;
    protected Checkbox noExpirationCheckbox;

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
        DialogLayout.size(this, DialogLayout.WIDTH_L);
    }

    protected void initUI() {
        buildComponents();

        VerticalLayout root = DialogLayout.stack();
        root.add(buildMetadataSection(), buildCryptoSection());
        add(root);

        setupKeySourceListener();
        setupAlgorithmChangeListener();
        loadKmsKeys();
    }

    private void buildComponents() {
        codeField = SecretsDialogSupport.readOnlyField(I18n.t("kms.dialog.token.code"));
        tenantField = SecretsDialogSupport.readOnlyField(I18n.t("kms.token.details.field.tenant"));

        // Token type
        tokenTypeCombo = new ComboBox<>(I18n.t("kms.dialog.token.token.type"));
        tokenTypeCombo.setItems(IEnumToken.Types.values());
        KmsEnumTag.useTagRenderer(tokenTypeCombo, "kms.enum");
        tokenTypeCombo.setRequired(true);
        tokenTypeCombo.setRequiredIndicatorVisible(true);
        tokenTypeCombo.setValue(IEnumToken.Types.ACCESS);
        tokenTypeCombo.setWidthFull();
        tokenTypeCombo.setTooltipText(I18n.t("kms.dialog.token.token.type.tooltip"));

        // Issuer
        issuerField = new TextField(I18n.t("kms.dialog.token.issuer"));
        issuerField.setPlaceholder(I18n.t("kms.dialog.token.issuer.placeholder"));
        issuerField.setWidthFull();
        issuerField.setTooltipText(I18n.t("kms.dialog.token.issuer.tooltip"));

        // Audience input (custom component)
        audienceInput = new AudienceInput();
        audienceInput.setWidthFull();
        audienceInput.setTooltipText(I18n.t("kms.dialog.token.audience.tooltip"));

        // Lifetime: value + unit, with an optional "No expiration"
        noExpirationCheckbox = new Checkbox(I18n.t("kms.dialog.token.no.expiration"));
        noExpirationCheckbox.setTooltipText(I18n.t("kms.dialog.token.no.expiration.tooltip"));
        noExpirationCheckbox.addValueChangeListener(e -> {
            boolean noExp = e.getValue();
            lifeTimeValueField.setEnabled(!noExp);
            lifeTimeUnitCombo.setEnabled(!noExp);
            if (noExp) {
                lifeTimeValueField.setValue(1);
                lifeTimeUnitCombo.setValue(I18n.t("kms.dialog.token.lifetime.unit.hours"));
            }
        });

        lifeTimeValueField = new IntegerField(I18n.t("kms.dialog.token.lifetime.field"));
        lifeTimeValueField.setPlaceholder(I18n.t("kms.dialog.token.lifetime.value.placeholder"));
        lifeTimeValueField.setValue(1);
        lifeTimeValueField.setWidthFull();
        lifeTimeValueField.setStepButtonsVisible(true);
        lifeTimeValueField.setMin(1);
        lifeTimeValueField.setEnabled(true);
        lifeTimeValueField.setTooltipText(I18n.t("kms.dialog.token.lifetime.tooltip"));

        lifeTimeUnitCombo = new ComboBox<>(I18n.t("kms.dialog.token.lifetime.unit.field"));
        lifeTimeUnitCombo.setItems(
                I18n.t("kms.dialog.token.lifetime.unit.seconds"),
                I18n.t("kms.dialog.token.lifetime.unit.minutes"),
                I18n.t("kms.dialog.token.lifetime.unit.hours"),
                I18n.t("kms.dialog.token.lifetime.unit.days")
        );
        lifeTimeUnitCombo.setValue(I18n.t("kms.dialog.token.lifetime.unit.hours"));
        lifeTimeUnitCombo.setWidthFull();
        lifeTimeUnitCombo.setEnabled(true);
        lifeTimeUnitCombo.setTooltipText(I18n.t("kms.dialog.token.lifetime.unit.tooltip"));

        // Key source selection
        keySourceGroup = new RadioButtonGroup<>();
        keySourceGroup.setLabel(I18n.t("kms.dialog.token.key.source"));
        keySourceGroup.setItems(I18n.t("kms.dialog.token.key.source.kms"), I18n.t("kms.dialog.token.key.source.custom"));
        keySourceGroup.setValue(I18n.t("kms.dialog.token.key.source.custom"));
        keySourceGroup.setWidthFull();
        keySourceGroup.setTooltipText(I18n.t("kms.dialog.token.key.source.tooltip"));

        // KMS key selection
        kmsKeyCombo = new ComboBox<>(I18n.t("kms.dialog.token.kms.key.field"));
        kmsKeyCombo.setPlaceholder(I18n.t("kms.dialog.token.choose.kms.key"));
        kmsKeyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        kmsKeyCombo.setWidthFull();
        kmsKeyCombo.setRequired(true);
        kmsKeyCombo.setRequiredIndicatorVisible(true);
        kmsKeyCombo.setVisible(false);
        kmsKeyCombo.setTooltipText(I18n.t("kms.dialog.token.kms.key.tooltip"));

        // Custom key layout (dynamic)
        customKeyLayout = new VerticalLayout();
        customKeyLayout.setPadding(false);
        customKeyLayout.setSpacing(true);
        customKeyLayout.setWidthFull();

        signatureAlgorithmCombo = new ComboBox<>(I18n.t("kms.dialog.token.signature.algorithm"));
        signatureAlgorithmCombo.setItems(SUPPORTED_ALGORITHMS);
        signatureAlgorithmCombo.setRequired(true);
        signatureAlgorithmCombo.setRequiredIndicatorVisible(true);
        signatureAlgorithmCombo.setValue("HS256");
        signatureAlgorithmCombo.setWidthFull();
        signatureAlgorithmCombo.setTooltipText(I18n.t("kms.dialog.token.signature.tooltip"));
        customKeyLayout.add(signatureAlgorithmCombo);

        secretKeyField = new TextField(I18n.t("kms.dialog.token.secret.key"));
        secretKeyField.setRequired(true);
        secretKeyField.setRequiredIndicatorVisible(true);
        secretKeyField.setWidthFull();
        secretKeyField.setTooltipText(I18n.t("kms.dialog.token.secret.tooltip"));

        privateKeyArea = DialogLayout.tall(new TextArea(I18n.t("kms.dialog.token.private.key")));
        privateKeyArea.setRequired(true);
        privateKeyArea.setRequiredIndicatorVisible(true);
        privateKeyArea.setPlaceholder(I18n.t("kms.dialog.token.private.key.placeholder"));
        privateKeyArea.setTooltipText(I18n.t("kms.dialog.token.private.tooltip"));

        publicKeyArea = DialogLayout.tall(new TextArea(I18n.t("kms.dialog.token.public.key")));
        publicKeyArea.setReadOnly(true);
        publicKeyArea.setPlaceholder(I18n.t("kms.dialog.token.public.key.placeholder"));
        publicKeyArea.setTooltipText(I18n.t("kms.dialog.token.public.tooltip"));

        generateKeyPairButton = new Button(I18n.t("kms.dialog.token.generate.key.pair"), event -> generateKeyPair());
        generateKeyPairButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        generateKeyPairButton.setWidthFull();
        generateKeyPairButton.setTooltipText(I18n.t("kms.dialog.token.generate.tooltip"));

        publicKeyComponent = createPublicKeyComponent();

        customKeyLayout.add(secretKeyField); // initially HMAC (HS256)
    }

    private VerticalLayout buildMetadataSection() {
        VerticalLayout section = DialogLayout.section(I18n.t("kms.dialog.token.metadata"), VaadinIcon.KEY);
        FormLayout form = DialogLayout.responsiveForm();

        form.add(codeField, tenantField, tokenTypeCombo, issuerField, audienceInput,
                lifeTimeValueField, lifeTimeUnitCombo, noExpirationCheckbox);
        form.setColspan(audienceInput, 2);
        form.setColspan(noExpirationCheckbox, 2);
        section.add(form);
        return section;
    }

    private VerticalLayout buildCryptoSection() {
        VerticalLayout section = DialogLayout.section(I18n.t("kms.dialog.token.crypto"), VaadinIcon.LOCK);
        section.add(keySourceGroup, kmsKeyCombo, customKeyLayout);
        return section;
    }

    private VerticalLayout createPublicKeyComponent() {
        copyPublicKeyButton = new Button(new Icon(VaadinIcon.COPY));
        copyPublicKeyButton.addClickListener(e -> copyToClipboard(publicKeyArea.getValue()));
        copyPublicKeyButton.setTooltipText(I18n.t("kms.dialog.token.copy.public.key"));
        copyPublicKeyButton.setAriaLabel(I18n.t("kms.dialog.token.copy.public.key"));
        copyPublicKeyButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(copyPublicKeyButton);
        actions.setWidthFull();
        actions.setPadding(false);
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout wrapper = new VerticalLayout(publicKeyArea, actions);
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.setWidthFull();
        return wrapper;
    }

    private void setupKeySourceListener() {
        keySourceGroup.addValueChangeListener(event -> {
            String value = event.getValue();
            if (I18n.t("kms.dialog.token.key.source.kms").equals(value)) {
                kmsKeyCombo.setVisible(true);
                customKeyLayout.setVisible(false);
                secretKeyField.clear();
                privateKeyArea.clear();
                publicKeyArea.clear();
                signatureAlgorithmCombo.setRequired(false);
            } else {
                kmsKeyCombo.setVisible(false);
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
            Notification.show(I18n.t("kms.dialog.token.load.failed", ex.getMessage()), 5000, Notification.Position.BOTTOM_END)
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
            secretKeyField.setHelperText(I18n.t("kms.dialog.token.secret.helper", algorithm, minBytes));
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
            Notification.show(I18n.t("kms.dialog.token.select.asymmetric.first"), 3000, Notification.Position.BOTTOM_END)
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
                    throw new UnsupportedAsymmetricAlgorithmException("Unsupported asymmetric algorithm: " + algorithm);
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

            Notification.show(I18n.t("kms.dialog.token.key.generated.success", algorithm), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show(I18n.t("kms.dialog.token.failed.generate.key", e.getMessage()), 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // ---------- Utility Methods ----------
    protected void copyToClipboard(String text) {
        if (text == null || text.isBlank()) {
            Notification.show(I18n.t("kms.dialog.token.nothing.to.copy"), 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        UI.getCurrent().getPage().executeJs(
                "const showToast = (message, tone) => {" +
                        "  const notification = document.createElement('div');" +
                        "  notification.className = 'kms-copy-toast kms-copy-toast--' + tone;" +
                        "  notification.textContent = message;" +
                        "  document.body.appendChild(notification);" +
                        "  setTimeout(() => notification.remove(), 2000);" +
                        "};" +
                        "navigator.clipboard.writeText($0)" +
                        "  .then(() => showToast($1, 'success'))" +
                        "  .catch(() => showToast($2, 'error'));",
                text, I18n.t("kms.dialog.token.copied"), I18n.t("kms.token.builder.copy.failed"));
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
                append(I18n.t("kms.dialog.token.secret.length", algorithm, requiredMinBytes));
                return false;
            }
            return true;
        } catch (IllegalArgumentException e) {
            append(I18n.t("kms.dialog.token.secret.base64"));
            return false;
        }
    }

    protected void handleFeignException(FeignException ex) {
        this.append(SecretsDialogSupport.feignMessage(ex));
    }

    protected void handleGenericException(Exception ex) {
        this.append(ex.getMessage());
    }

    // ---------- Form <-> DTO ----------

    /** Fills the read-only identity fields ({@code code}, {@code tenant}) from a loaded DTO. */
    protected final void bindIdentity(TokenConfigDto dto) {
        SecretsDialogSupport.setText(codeField, dto.getCode());
        SecretsDialogSupport.setText(tenantField, dto.getTenant());
    }

    /**
     * Validates the form and, only when everything is valid, copies the edited
     * values into {@code dto}. {@code id}, {@code code}, {@code tenant} and the
     * audit fields are left untouched, so an update keeps everything it does not edit.
     *
     * @return false (after appending an error message) when the form is invalid
     */
    protected final boolean collectInto(TokenConfigDto dto) {
        IEnumToken.Types tokenType = tokenTypeCombo.getValue();
        if (tokenType == null) {
            append(I18n.t("kms.dialog.token.type.required"));
            return false;
        }

        Integer lifeTime = getLifeTimeInMs();

        String kmsKeyId = null;
        String signatureAlgorithm = null;
        String secretOrPrivateKey = null;
        String publicKey = null;

        boolean useKmsKey = I18n.t("kms.dialog.token.key.source.kms").equals(keySourceGroup.getValue());
        if (useKmsKey) {
            KeyOption selected = kmsKeyCombo.getValue();
            if (selected == null) {
                append(I18n.t("kms.dialog.token.kms.select"));
                return false;
            }
            kmsKeyId = selected.getKeyId();
        } else {
            signatureAlgorithm = signatureAlgorithmCombo.getValue();
            if (signatureAlgorithm == null || signatureAlgorithm.isBlank()) {
                append(I18n.t("kms.dialog.token.algorithm.required"));
                return false;
            }
            if (HMAC_ALGORITHMS.contains(signatureAlgorithm)) {
                String secretKey = secretKeyField.getValue();
                if (secretKey == null || secretKey.isBlank()) {
                    append(I18n.t("kms.dialog.token.secret.required", signatureAlgorithm));
                    return false;
                }
                if (!validateHmacKey(signatureAlgorithm, secretKey)) return false;
                secretOrPrivateKey = secretKey;
            } else if (ASYMMETRIC_ALGORITHMS.contains(signatureAlgorithm)) {
                String privateKey = privateKeyArea.getValue();
                if (privateKey == null || privateKey.isBlank()) {
                    append(I18n.t("kms.dialog.token.private.required", signatureAlgorithm));
                    return false;
                }
                secretOrPrivateKey = privateKey;
                publicKey = publicKeyArea.getValue();
            } else {
                append(I18n.t("kms.dialog.token.unsupported.algorithm", signatureAlgorithm));
                return false;
            }
        }

        dto.setTokenType(tokenType);
        dto.setIssuer(issuerField.getValue());
        dto.setAudience(getAudienceList());
        dto.setLifeTimeInMs(lifeTime);
        dto.setKmsKeyId(kmsKeyId);
        dto.setSignatureAlgorithm(signatureAlgorithm);
        dto.setSecretKey(secretOrPrivateKey);
        dto.setPublicKey(publicKey);
        return true;
    }

    /**
     * Runs the service call and reports the outcome; returns true on a 2xx answer.
     *
     * @param statusFailedKey i18n key used when the service answers with a non-2xx status
     */
    protected final boolean send(Supplier<ResponseEntity<TokenConfigDto>> call, String statusFailedKey) {
        try {
            ResponseEntity<TokenConfigDto> response = call.get();
            if (response.getStatusCode().is2xxSuccessful()) {
                onSaveSuccess();
                return true;
            }
            append(I18n.t(statusFailedKey, response.getStatusCode()));
            return false;
        } catch (FeignException ex) {
            handleFeignException(ex);
            return false;
        } catch (Exception e) {
            handleGenericException(e);
            return false;
        }
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
            append(I18n.t("kms.dialog.token.lifetime.positive"));
            return null;
        }
        String unit = lifeTimeUnitCombo.getValue();
        if (unit == null) {
            append(I18n.t("kms.dialog.token.select.unit"));
            return null;
        }
        int ms;
        if (I18n.t("kms.dialog.token.lifetime.unit.seconds").equals(unit)) {
            ms = value * 1000;
        } else if (I18n.t("kms.dialog.token.lifetime.unit.minutes").equals(unit)) {
            ms = value * 60 * 1000;
        } else if (I18n.t("kms.dialog.token.lifetime.unit.hours").equals(unit)) {
            ms = value * 60 * 60 * 1000;
        } else if (I18n.t("kms.dialog.token.lifetime.unit.days").equals(unit)) {
            ms = value * 24 * 60 * 60 * 1000;
        } else {
            throw new InvalidUnitException("Unknown unit: " + unit);
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
            lifeTimeUnitCombo.setValue(I18n.t("kms.dialog.token.lifetime.unit.hours"));
            return;
        }
        noExpirationCheckbox.setValue(false);
        lifeTimeValueField.setEnabled(true);
        lifeTimeUnitCombo.setEnabled(true);

        // Determine best unit
        if (ms % (24 * 60 * 60 * 1000) == 0 && ms >= (24 * 60 * 60 * 1000)) {
            lifeTimeValueField.setValue(ms / (24 * 60 * 60 * 1000));
            lifeTimeUnitCombo.setValue(I18n.t("kms.dialog.token.lifetime.unit.days"));
        } else if (ms % (60 * 60 * 1000) == 0) {
            lifeTimeValueField.setValue(ms / (60 * 60 * 1000));
            lifeTimeUnitCombo.setValue(I18n.t("kms.dialog.token.lifetime.unit.hours"));
        } else if (ms % (60 * 1000) == 0) {
            lifeTimeValueField.setValue(ms / (60 * 1000));
            lifeTimeUnitCombo.setValue(I18n.t("kms.dialog.token.lifetime.unit.minutes"));
        } else {
            lifeTimeValueField.setValue(ms / 1000);
            lifeTimeUnitCombo.setValue(I18n.t("kms.dialog.token.lifetime.unit.seconds"));
            if (ms % 1000 != 0) {
                Notification.show(I18n.t("kms.dialog.token.lifetime.warning"), 3000, Notification.Position.BOTTOM_END)
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

    /** Audience editor: a text input and removable chips; the audience list is kept as a model. */
    protected static class AudienceInput extends VerticalLayout {
        private final List<String> audiences = new ArrayList<>();
        private final TextField inputField;
        private final HorizontalLayout chipsContainer;

        public AudienceInput() {
            setPadding(false);
            setSpacing(false);

            inputField = new TextField(I18n.t("kms.dialog.token.audience"));
            inputField.setPlaceholder(I18n.t("kms.dialog.token.audience.placeholder"));
            inputField.setWidthFull();

            Button addButton = new Button(I18n.t("kms.dialog.token.audience.add"), new Icon(VaadinIcon.PLUS));
            addButton.addClickListener(e -> addAudience());

            HorizontalLayout inputRow = new HorizontalLayout(inputField, addButton);
            inputRow.setWidthFull();
            inputRow.setPadding(false);
            inputRow.setAlignItems(FlexComponent.Alignment.END);
            inputRow.setFlexGrow(1, inputField);

            chipsContainer = new HorizontalLayout();
            chipsContainer.setSpacing(false);
            chipsContainer.setPadding(false);
            chipsContainer.setWidthFull();
            chipsContainer.addClassName(DialogLayout.CLASS_ROW);

            add(inputRow, chipsContainer);
        }

        public void setTooltipText(String tooltip) {
            inputField.setTooltipText(tooltip);
        }

        private void addAudience() {
            String value = inputField.getValue();
            if (value == null || value.isBlank()) {
                Notification.show(I18n.t("kms.dialog.token.audience.empty"), 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            value = value.trim();
            if (audiences.contains(value)) {
                Notification.show(I18n.t("kms.dialog.token.audience.exists"), 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            addChip(value);
            inputField.clear();
        }

        private void addChip(String audience) {
            audiences.add(audience);

            Button removeButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
            removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
            removeButton.setAriaLabel(I18n.t("kms.dialog.token.audience.remove", audience));

            Span chip = new Span(new Span(audience), removeButton);
            chip.getElement().getThemeList().add("badge");
            chip.addClassName(DialogLayout.CLASS_CHIP);

            removeButton.addClickListener(e -> {
                audiences.remove(audience);
                chipsContainer.remove(chip);
            });
            chipsContainer.add(chip);
        }

        public List<String> getAudiences() {
            return new ArrayList<>(audiences);
        }

        public void setAudiences(List<String> newAudiences) {
            audiences.clear();
            chipsContainer.removeAll();
            if (newAudiences != null) {
                newAudiences.forEach(this::addChip);
            }
        }
    }
}
