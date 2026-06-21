package eu.isygoit.ui.kms.views.cryptography.byok;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumKeyExpirationModel;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.util.RsaEncryptionUtil;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@VaadinSessionScope //(or UIScope)
@Route(value = "kms/byok", layout = KmsMainLayout.class)
@PageTitle("BYOK - Bring Your Own Key")
@PermitAll
public class ByokView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>("External KMS Key");
    private final Button refreshKeysButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Button getParamsButton = new Button("Generate Import Parameters", new Icon(VaadinIcon.DOWNLOAD));
    private final TextArea wrappedKeyDisplay = new TextArea();
    private final TextArea importTokenDisplay = new TextArea();
    private final TextArea plainKeyMaterialField = new TextArea();
    private final Button encryptNowButton = new Button("Encrypt Now", new Icon(VaadinIcon.LOCK));
    private final TextArea encryptedMaterialField = new TextArea();
    private final DatePicker expirationDatePicker = new DatePicker("Expiration Date (optional)");
    private final Button importButton = new Button("Import Key Material", new Icon(VaadinIcon.UPLOAD));
    private final Button deleteMaterialButton = new Button("Delete Imported Material", new Icon(VaadinIcon.TRASH));
    private final ProgressBar loadingBar = new ProgressBar();
    private final Span paramsExpiryInfo = new Span();
    private final Span keyStatusInfo = new Span();

    private String selectedKeyId = null;
    private String currentImportToken = null;
    private LocalDateTime parametersValidUntil = null;
    private String currentPublicKey = null;
    private List<KeyOption> keyOptions = new ArrayList<>();
    private boolean hasImportedMaterial = false;

    @Autowired
    public ByokView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-byok-view");
        setAlignItems(FlexComponent.Alignment.STRETCH);

        // Header
        H2 header = new H2("Bring Your Own Key (BYOK)");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.SMALL);
        header.addClassName(LumoUtility.Margin.Top.NONE);
        add(header);

        // Step 1: Key selection + status + delete button
        add(createStepCard("1. Select External KMS Key",
                "Only keys with origin = EXTERNAL can be used",
                buildKeySelectionSection(),
                buildStatusAndDeleteRow()));

        // Step 2: Generate parameters
        VerticalLayout step2Content = new VerticalLayout();
        step2Content.setSpacing(false);
        step2Content.setPadding(false);
        step2Content.add(buildParamButtons());
        step2Content.add(createFieldRow("Public Wrapping Key (Base64)", wrappedKeyDisplay, "Copy public wrapping key"));
        step2Content.add(createFieldRow("Import Token", importTokenDisplay, "Copy import token"));
        step2Content.add(paramsExpiryInfo);
        add(createStepCard("2. Generate Import Parameters",
                "These parameters are valid for 24 hours. Generate them before encrypting your key material.",
                step2Content));

        // Step 3: Encrypt key material
        VerticalLayout step3Content = new VerticalLayout();
        step3Content.setSpacing(false);
        step3Content.setPadding(false);
        step3Content.add(createEncryptionSection());
        step3Content.add(createFieldRow("Encrypted Key Material (Base64)", encryptedMaterialField, "Copy encrypted material"));
        add(createStepCard("3. Encrypt Your Key Material",
                "Paste your plain key material (Base64) and click 'Encrypt Now'.",
                step3Content));

        // Step 4: Import
        VerticalLayout step4Content = new VerticalLayout();
        step4Content.setSpacing(false);
        step4Content.setPadding(false);
        step4Content.add(expirationDatePicker);
        step4Content.add(buildImportSection());
        add(createStepCard("4. Import Encrypted Material",
                "The import token must be valid (not expired). Optionally set an expiration date for the key material.",
                step4Content));

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        // Event handlers
        getParamsButton.addClickListener(e -> generateParameters());
        encryptNowButton.addClickListener(e -> encryptWithPublicKey());
        importButton.addClickListener(e -> importKeyMaterial());
        deleteMaterialButton.addClickListener(e -> deleteImportedMaterial());
        refreshKeysButton.addClickListener(e -> loadKeyOptions());
        keyCombo.addValueChangeListener(e -> {
            selectedKeyId = e.getValue() != null ? e.getValue().getKeyId() : null;
            clearParameters();
            updateKeyStatus();
        });

        configureComponents();
        injectResponsiveStyles();
        loadKeyOptions();
    }

    // ---------- UI Helpers ----------
    private VerticalLayout createStepCard(String title, String hint, Component... components) {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.addClassName(LumoUtility.Border.ALL);
        card.addClassName(LumoUtility.BorderRadius.LARGE);
        card.addClassName(LumoUtility.Padding.MEDIUM);
        card.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        card.getStyle().set("background-color", "var(--lumo-base-color)");

        H3 stepTitle = new H3(title);
        stepTitle.addClassName(LumoUtility.FontSize.MEDIUM);
        stepTitle.addClassName(LumoUtility.Margin.Top.NONE);
        stepTitle.addClassName(LumoUtility.Margin.Bottom.XSMALL);
        card.add(stepTitle);

        if (StringUtils.hasText(hint)) {
            Span hintSpan = new Span(hint);
            hintSpan.addClassName(LumoUtility.FontSize.XSMALL);
            hintSpan.addClassName(LumoUtility.TextColor.TERTIARY);
            hintSpan.getStyle().set("margin-bottom", "var(--lumo-space-s)");
            card.add(hintSpan);
        }

        for (Component comp : components) {
            card.add(comp);
        }
        return card;
    }

    private HorizontalLayout buildKeySelectionSection() {
        HorizontalLayout keyLayout = new HorizontalLayout();
        keyLayout.setWidthFull();
        keyLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        keyLayout.setSpacing(true);
        keyLayout.getStyle().set("flex-wrap", "wrap");

        keyCombo.setPlaceholder("Select a key created with origin = EXTERNAL...");
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("400px");
        keyCombo.setTooltipText("Only keys with origin 'EXTERNAL' can be used for BYOK");

        refreshKeysButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshKeysButton.setTooltipText("Refresh key list");

        keyLayout.add(keyCombo, refreshKeysButton);
        return keyLayout;
    }

    private HorizontalLayout buildStatusAndDeleteRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.getStyle().set("flex-wrap", "wrap");

        keyStatusInfo.addClassName(LumoUtility.FontSize.SMALL);
        keyStatusInfo.getStyle().set("margin-top", "var(--lumo-space-xs)");

        deleteMaterialButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteMaterialButton.setTooltipText("Delete imported key material");
        deleteMaterialButton.setVisible(false); // initially hidden

        row.add(keyStatusInfo, deleteMaterialButton);
        return row;
    }

    private HorizontalLayout buildParamButtons() {
        HorizontalLayout paramLayout = new HorizontalLayout(getParamsButton);
        paramLayout.setSpacing(true);
        getParamsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getParamsButton.setTooltipText("Generate a new wrapping key and import token (valid for 24 hours)");
        return paramLayout;
    }

    private HorizontalLayout buildImportSection() {
        HorizontalLayout importLayout = new HorizontalLayout(importButton);
        importLayout.setWidthFull();
        importLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        importButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        importButton.setTooltipText("Import the encrypted key material using the current import token");
        return importLayout;
    }

    private VerticalLayout createEncryptionSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);

        layout.add(createPlainMaterialRow());

        HorizontalLayout buttonRow = new HorizontalLayout(encryptNowButton);
        buttonRow.setWidthFull();
        buttonRow.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        encryptNowButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        encryptNowButton.setTooltipText("Encrypt your plain key material using the public wrapping key");
        layout.add(buttonRow);

        return layout;
    }

    private VerticalLayout createPlainMaterialRow() {
        VerticalLayout row = new VerticalLayout();
        row.setWidthFull();
        row.setSpacing(false);
        row.setPadding(false);

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setSpacing(true);
        Span labelSpan = new Span("Your Key Material (Base64)");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);
        headerRow.add(labelSpan);
        headerRow.getStyle().set("flex-wrap", "wrap");

        plainKeyMaterialField.setWidthFull();
        plainKeyMaterialField.setPlaceholder("Paste your plain key material as Base64 (e.g., `openssl rand -base64 32`)");
        plainKeyMaterialField.setHelperText("The plaintext must be ≤ 190 bytes (RSA‑2048 limit).");

        row.add(headerRow, plainKeyMaterialField);
        return row;
    }

    private VerticalLayout createFieldRow(String label, TextArea textArea, String copyTooltip) {
        VerticalLayout row = new VerticalLayout();
        row.setWidthFull();
        row.setSpacing(false);
        row.setPadding(false);

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setSpacing(true);

        Span labelSpan = new Span(label);
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);

        Button copyBtn = new Button(new Icon(VaadinIcon.COPY));
        copyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        copyBtn.setTooltipText(copyTooltip);
        copyBtn.setWidth("32px");
        copyBtn.addClickListener(e -> {
            String value = textArea.getValue();
            if (StringUtils.hasText(value)) {
                copyToClipboard(value);
                showSuccessNotification("Copied to clipboard");
            } else {
                showWarningNotification("Nothing to copy");
            }
        });

        headerRow.add(labelSpan, copyBtn);
        headerRow.getStyle().set("flex-wrap", "wrap");

        textArea.setWidthFull();
        textArea.setHeight("100px");

        row.add(headerRow, textArea);
        return row;
    }

    private void copyToClipboard(String text) {
        UI.getCurrent().getPage().executeJs(
                "navigator.clipboard.writeText($0).then(() => { " +
                        "  const notification = document.createElement('div'); " +
                        "  notification.textContent = 'Copied!'; " +
                        "  notification.style.position = 'fixed'; " +
                        "  notification.style.bottom = '20px'; " +
                        "  notification.style.right = '20px'; " +
                        "  notification.style.backgroundColor = '#4caf50'; " +
                        "  notification.style.color = 'white'; " +
                        "  notification.style.padding = '10px 20px'; " +
                        "  notification.style.borderRadius = '4px'; " +
                        "  notification.style.zIndex = '1000'; " +
                        "  document.body.appendChild(notification); " +
                        "  setTimeout(() => notification.remove(), 2000); " +
                        "}).catch(() => { " +
                        "  const notification = document.createElement('div'); " +
                        "  notification.textContent = 'Copy failed. Check permissions.'; " +
                        "  notification.style.position = 'fixed'; " +
                        "  notification.style.bottom = '20px'; " +
                        "  notification.style.right = '20px'; " +
                        "  notification.style.backgroundColor = '#f44336'; " +
                        "  notification.style.color = 'white'; " +
                        "  notification.style.padding = '10px 20px'; " +
                        "  notification.style.borderRadius = '4px'; " +
                        "  notification.style.zIndex = '1000'; " +
                        "  document.body.appendChild(notification); " +
                        "  setTimeout(() => notification.remove(), 3000); " +
                        "});", text);
    }

    private void configureComponents() {
        int textAreaHeight = 100;
        wrappedKeyDisplay.setHeight(textAreaHeight + "px");
        wrappedKeyDisplay.setReadOnly(true);
        wrappedKeyDisplay.setPlaceholder("Wrapping key will appear here after generating parameters...");

        importTokenDisplay.setHeight(textAreaHeight + "px");
        importTokenDisplay.setReadOnly(true);
        importTokenDisplay.setPlaceholder("Import token will appear here...");

        encryptedMaterialField.setHeight(textAreaHeight + "px");
        encryptedMaterialField.setPlaceholder("Encrypted material will appear here after encryption");
        encryptedMaterialField.setHelperText("This is the material to import.");

        expirationDatePicker.setWidth("250px");
        expirationDatePicker.setPlaceholder("YYYY-MM-DD");
        expirationDatePicker.setHelperText("If set, key material expires on this date.");
    }

    // ---------- Data & API ----------
    private void loadKeyOptions() {
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.ListKeysResponse> response = kmsApiService.listKeys(100, null);
            KmsDtos.ListKeysResponse keys = response.getBody();
            if (keys != null && keys.getKeys() != null) {
                keyOptions = keys.getKeys().stream()
                        .map(entry -> fetchAliasAndOrigin(entry.getKeyId()))
                        .filter(KeyOption::isExternal)
                        .collect(Collectors.toList());
                keyCombo.setItems(keyOptions);
            } else {
                keyOptions.clear();
                keyCombo.setItems(keyOptions);
            }
            if (selectedKeyId != null && keyOptions.stream().noneMatch(k -> k.getKeyId().equals(selectedKeyId))) {
                selectedKeyId = null;
                keyCombo.clear();
                clearParameters();
            }
            updateKeyStatus();
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showErrorNotification("Failed to load keys: " + errorMsg);
            log.error("Failed to load keys: {}", errorMsg);
        } catch (Exception e) {
            showErrorNotification("Failed to load keys: " + e.getMessage());
            log.error("Failed to load keys: {}", e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private KeyOption fetchAliasAndOrigin(String keyId) {
        try {
            ResponseEntity<KmsDtos.DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
            KmsDtos.DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null) {
                String alias = desc.getKeyMetadata().getKeyAlias();
                IEnumKeyOrigin.Types origin = desc.getKeyMetadata().getOrigin();
                boolean isExternal = origin == IEnumKeyOrigin.Types.EXTERNAL;
                return new KeyOption(keyId, alias, isExternal);
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showErrorNotification("Error fetching key details: " + errorMsg);
            log.error("Error fetching key details for {}: {}", keyId, errorMsg);
        } catch (Exception e) {
            showErrorNotification("Error fetching key details for " + keyId + ": " + e.getMessage());
            log.error("Error fetching key details for {}: {}", keyId, e.getMessage());
        }
        return new KeyOption(keyId, keyId, false);
    }

    private void updateKeyStatus() {
        if (selectedKeyId == null) {
            keyStatusInfo.setText("");
            deleteMaterialButton.setVisible(false);
            hasImportedMaterial = false;
            return;
        }
        try {
            ResponseEntity<KmsDtos.DescribeKeyResponse> response = kmsApiService.describeKey(selectedKeyId);
            KmsDtos.DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null) {
                hasImportedMaterial = desc.getKeyMetadata().getOrigin() == IEnumKeyOrigin.Types.EXTERNAL &&
                        desc.getKeyMetadata().getKeyStatus() != IEnumKeyStatus.Types.PENDING_IMPORT;
                String statusText = hasImportedMaterial ?
                        "✅ Key material imported. The key is ready to use. To replace or generate new import parameters, delete the existing material first." :
                        "⚠️ No key material imported. Follow the steps above to import your own key material.";
                keyStatusInfo.setText(statusText);
                deleteMaterialButton.setVisible(hasImportedMaterial);
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showErrorNotification("Error fetching key details: " + errorMsg);
            log.error("Error fetching key details for {}: {}", selectedKeyId, ex.getMessage());
        } catch (Exception e) {
            showErrorNotification("Error fetching key details for " + selectedKeyId + ": " + e.getMessage());
            log.error("Error fetching key details for {}: {}", selectedKeyId, e.getMessage());
            keyStatusInfo.setText("");
            deleteMaterialButton.setVisible(false);
            hasImportedMaterial = false;
        }
    }

    private void generateParameters() {
        if (selectedKeyId == null) {
            showWarningNotification("Please select an external KMS key first");
            return;
        }
        if (hasImportedMaterial) {
            showWarningNotification("Key already has imported material. Delete it first before generating new parameters.");
            return;
        }
        showLoading(true);
        try {
            KmsDtos.GetParametersForImportRequest request = KmsDtos.GetParametersForImportRequest.builder()
                    .wrappingAlgorithm("RSAES_OAEP_SHA_256")
                    .wrappingKeySpec("RSA_2048")
                    .build();
            ResponseEntity<KmsDtos.GetParametersForImportResponse> response =
                    kmsApiService.getParametersForImport(selectedKeyId, request);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                KmsDtos.GetParametersForImportResponse params = response.getBody();
                currentPublicKey = params.getPublicKey();
                wrappedKeyDisplay.setValue(currentPublicKey);
                importTokenDisplay.setValue(params.getImportToken());
                currentImportToken = params.getImportToken();
                parametersValidUntil = params.getValidTo();

                String expiryMsg = parametersValidUntil != null ?
                        "Parameters valid until: " + DateHelper.formatToHumanReadable(parametersValidUntil) :
                        "Parameters generated (no expiry provided)";
                paramsExpiryInfo.setText(expiryMsg);
                paramsExpiryInfo.addClassName(LumoUtility.FontSize.XSMALL);
                paramsExpiryInfo.addClassName(LumoUtility.TextColor.TERTIARY);

                showSuccessNotification("Import parameters generated – valid for 24 hours");
            } else {
                showErrorNotification("Failed to generate import parameters");
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showErrorNotification("Error: " + errorMsg);
            log.error("Failed to generate import parameters for key {}: {}", selectedKeyId, errorMsg);
        } catch (Exception e) {
            showErrorNotification("Error: " + e.getMessage());
            log.error("Failed to generate import parameters for key {}: {}", selectedKeyId, e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private void encryptWithPublicKey() {
        String publicKey = wrappedKeyDisplay.getValue();
        if (!StringUtils.hasText(publicKey)) {
            showWarningNotification("Please generate import parameters first");
            return;
        }
        String plainBase64 = plainKeyMaterialField.getValue();
        if (!StringUtils.hasText(plainBase64)) {
            showWarningNotification("Please enter your plain key material (Base64)");
            return;
        }
        if (!isValidBase64(plainBase64)) {
            showErrorNotification("Invalid Base64 format for plain material");
            return;
        }

        showLoading(true);
        try {
            String encryptedBase64 = RsaEncryptionUtil.encryptWithPublicKey(publicKey, plainBase64);
            encryptedMaterialField.setValue(encryptedBase64);
            showSuccessNotification("Encryption completed");
        } catch (Exception e) {
            showErrorNotification("Encryption failed: " + e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private void importKeyMaterial() {
        if (selectedKeyId == null) {
            showWarningNotification("Please select a key first");
            return;
        }
        if (hasImportedMaterial) {
            showWarningNotification("Key already has imported material. Delete it first.");
            return;
        }
        String encrypted = encryptedMaterialField.getValue();
        if (!StringUtils.hasText(encrypted)) {
            showWarningNotification("Encrypted key material is required");
            return;
        }
        if (!isValidBase64(encrypted)) {
            showErrorNotification("Invalid Base64 format – please check your encrypted material");
            return;
        }
        if (currentImportToken == null) {
            showWarningNotification("Please generate import parameters first (they expire after 24h)");
            return;
        }
        if (parametersValidUntil != null && parametersValidUntil.isBefore(LocalDateTime.now())) {
            showErrorNotification("Import token has expired. Please generate new parameters.");
            return;
        }

        showLoading(true);
        try {
            KmsDtos.ImportKeyMaterialRequest request = KmsDtos.ImportKeyMaterialRequest.builder()
                    .keyId(selectedKeyId)
                    .importToken(currentImportToken)
                    .encryptedKeyMaterial(encrypted)
                    .build();
            if (expirationDatePicker.getValue() != null) {
                LocalDateTime validTo = expirationDatePicker.getValue().atTime(LocalTime.MAX);
                request.setValidTo(validTo);
                request.setExpirationModel(IEnumKeyExpirationModel.Types.KEY_MATERIAL_EXPIRES);
            } else {
                request.setExpirationModel(IEnumKeyExpirationModel.Types.KEY_MATERIAL_DOES_NOT_EXPIRE);
            }
            ResponseEntity<KmsDtos.ImportKeyMaterialResponse> response = kmsApiService.importKeyMaterial(selectedKeyId, request);
            if (response.getStatusCode().is2xxSuccessful()) {
                showSuccessNotification("Key material imported successfully");
                clearParameters();
                updateKeyStatus();
            } else {
                showErrorNotification("Import failed – check token validity and material format");
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showErrorNotification("Import key material failed: " + errorMsg);
            log.error("Import key material failed for {}: {}", selectedKeyId, errorMsg);
        } catch (Exception e) {
            showErrorNotification("Error: " + e.getMessage());
            log.error("Import key material failed for {}: {}", selectedKeyId, e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private void deleteImportedMaterial() {
        if (selectedKeyId == null) {
            showWarningNotification("Please select a key first");
            return;
        }
        if (!hasImportedMaterial) {
            showWarningNotification("No imported material to delete");
            return;
        }
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.DeleteImportedKeyMaterialResponse> response =
                    kmsApiService.deleteImportedKeyMaterial(selectedKeyId);
            if (response.getStatusCode().is2xxSuccessful()) {
                showSuccessNotification("Imported key material deleted");
                clearParameters();
                updateKeyStatus();
            } else {
                showErrorNotification("Deletion failed – key may not have imported material");
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showErrorNotification("Error: " + errorMsg);
            log.error("Failed to delete imported key material for {}: {}", selectedKeyId, errorMsg);
        } catch (Exception e) {
            showErrorNotification("Error: " + e.getMessage());
            log.error("Failed to delete imported key material for {}: {}", selectedKeyId, e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private boolean isValidBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void clearParameters() {
        wrappedKeyDisplay.clear();
        importTokenDisplay.clear();
        plainKeyMaterialField.clear();
        encryptedMaterialField.clear();
        expirationDatePicker.clear();
        paramsExpiryInfo.setText("");
        currentImportToken = null;
        parametersValidUntil = null;
        currentPublicKey = null;
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        keyCombo.setEnabled(!show);
        refreshKeysButton.setEnabled(!show);
        getParamsButton.setEnabled(!show);
        encryptNowButton.setEnabled(!show);
        importButton.setEnabled(!show);
        deleteMaterialButton.setEnabled(!show); // keep enabled even when hidden, but visibility toggles
        encryptedMaterialField.setEnabled(!show);
        plainKeyMaterialField.setEnabled(!show);
        expirationDatePicker.setEnabled(!show);
    }

    private void showSuccessNotification(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showWarningNotification(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    private void injectResponsiveStyles() {
        String css = """
                .kms-byok-view {
                    max-width: 1200px;
                    margin: 0 auto;
                }
                @media (max-width: 768px) {
                    .kms-byok-view .byok-field-row,
                    .kms-byok-view .byok-header-row {
                        flex-direction: column;
                        align-items: flex-start;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    private static class KeyOption {
        private final String keyId;
        private final String displayName;
        private final boolean isExternal;

        KeyOption(String keyId, String aliasOrId, boolean isExternal) {
            this.keyId = keyId;
            this.displayName = (aliasOrId != null && !aliasOrId.equals(keyId)) ? aliasOrId + " (" + keyId + ")" : keyId;
            this.isExternal = isExternal;
        }

        String getKeyId() {
            return keyId;
        }

        String getDisplayName() {
            return displayName;
        }

        boolean isExternal() {
            return isExternal;
        }
    }
}