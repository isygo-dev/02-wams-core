package eu.isygoit.ui.views.byok;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.*;
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
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumKeyExpirationModel;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainLayout;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "byok", layout = MainLayout.class)
@PageTitle("BYOK - Bring Your Own Key")
@PermitAll
public class ByokView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>("External KMS Key");
    private final Button refreshKeysButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Button getParamsButton = new Button("Generate Import Parameters", new Icon(VaadinIcon.DOWNLOAD));
    private final TextArea wrappedKeyDisplay = new TextArea("Public Wrapping Key (PEM / Base64)");
    private final TextArea importTokenDisplay = new TextArea("Import Token");
    private final TextArea encryptedMaterialField = new TextArea("Encrypted Key Material (Base64)");
    private final DatePicker expirationDatePicker = new DatePicker("Expiration Date (optional)");
    private final Button importButton = new Button("Import Key Material", new Icon(VaadinIcon.UPLOAD));
    private final Button deleteMaterialButton = new Button("Delete Imported Material", new Icon(VaadinIcon.TRASH));
    private final ProgressBar loadingBar = new ProgressBar();
    private final Span paramsExpiryInfo = new Span();
    private final Span keyStatusInfo = new Span();

    private String selectedKeyId = null;
    private String currentImportToken = null;
    private LocalDateTime parametersValidUntil = null;
    private List<KeyOption> keyOptions = new ArrayList<>();

    @Autowired
    public ByokView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-byok-view");

        // Header
        H2 header = new H2("Bring Your Own Key (BYOK)");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        add(createHelpText());

        // Key selection section
        HorizontalLayout keyLayout = buildKeySelectionSection();
        add(keyLayout);

        // Key material status
        add(keyStatusInfo);

        // Parameters section
        HorizontalLayout paramButtons = buildParamButtons();
        add(paramButtons);
        add(wrappedKeyDisplay, importTokenDisplay);
        add(paramsExpiryInfo);

        // Import section
        HorizontalLayout importLayout = buildImportSection();
        add(importLayout);
        add(encryptedMaterialField, expirationDatePicker, importButton);

        // Configure text areas for full width and larger height
        configureTextAreas();

        // Loading indicator
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        // Event handlers
        getParamsButton.addClickListener(e -> generateParameters());
        importButton.addClickListener(e -> importKeyMaterial());
        deleteMaterialButton.addClickListener(e -> deleteImportedMaterial());
        refreshKeysButton.addClickListener(e -> loadKeyOptions());
        keyCombo.addValueChangeListener(e -> {
            selectedKeyId = e.getValue() != null ? e.getValue().getKeyId() : null;
            clearParameters();
            updateKeyStatus();
        });

        injectResponsiveStyles();
        loadKeyOptions();
    }

    private void configureTextAreas() {
        // Wrapping key – full width, readable height
        wrappedKeyDisplay.setWidthFull();
        wrappedKeyDisplay.setHeight("200px");
        wrappedKeyDisplay.setReadOnly(true);
        wrappedKeyDisplay.setPlaceholder("Wrapping key will appear here after generating parameters...");

        // Import token – full width, compact height
        importTokenDisplay.setWidthFull();
        importTokenDisplay.setHeight("100px");
        importTokenDisplay.setReadOnly(true);
        importTokenDisplay.setPlaceholder("Import token will appear here...");

        // Encrypted material – full width, large editable area
        encryptedMaterialField.setWidthFull();
        encryptedMaterialField.setHeight("180px");
        encryptedMaterialField.setPlaceholder("Paste the Base64-encoded encrypted key material here");
        encryptedMaterialField.setHelperText("Encrypt your key material using the public wrapping key above, then paste the result.");
    }

    private HorizontalLayout buildKeySelectionSection() {
        HorizontalLayout keyLayout = new HorizontalLayout();
        keyLayout.setWidthFull();
        keyLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        keyLayout.setSpacing(true);
        keyLayout.getStyle().set("flex-wrap", "wrap");
        keyLayout.addClassName("byok-key-layout");

        keyCombo.setPlaceholder("Select a key created with origin = EXTERNAL...");
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("400px");
        keyCombo.setTooltipText("Only keys with origin 'EXTERNAL' can be used for BYOK");

        refreshKeysButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshKeysButton.setTooltipText("Refresh key list");

        keyLayout.add(keyCombo, refreshKeysButton);
        return keyLayout;
    }

    private HorizontalLayout buildParamButtons() {
        HorizontalLayout paramLayout = new HorizontalLayout(getParamsButton, deleteMaterialButton);
        paramLayout.setSpacing(true);
        paramLayout.getStyle().set("flex-wrap", "wrap");
        paramLayout.addClassName("byok-param-layout");

        getParamsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getParamsButton.setTooltipText("Generate a new wrapping key and import token (valid for 24 hours)");

        deleteMaterialButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteMaterialButton.setTooltipText("Delete previously imported key material (the key becomes unusable)");

        return paramLayout;
    }

    private HorizontalLayout buildImportSection() {
        HorizontalLayout importLayout = new HorizontalLayout();
        importLayout.setWidthFull();
        importLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        importButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        importButton.setTooltipText("Import the encrypted key material using the current import token");
        return importLayout;
    }

    private Paragraph createHelpText() {
        Paragraph help = new Paragraph(
                "Bring Your Own Key (BYOK) allows you to import your own key material into a KMS key.\n" +
                        "Step 1: Create a KMS key with origin = EXTERNAL.\n" +
                        "Step 2: Generate import parameters (wrapping key + import token).\n" +
                        "Step 3: Encrypt your key material with the wrapping key (using OpenSSL or similar).\n" +
                        "Step 4: Paste the Base64-encoded encrypted material and import it.\n" +
                        "The import token expires after 24 hours."
        );
        help.addClassName(LumoUtility.FontSize.SMALL);
        help.addClassName(LumoUtility.TextColor.SECONDARY);
        help.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        return help;
    }

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
        } catch (Exception e) {
            showErrorNotification("Failed to load keys: " + e.getMessage());
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
        } catch (Exception e) {
            // ignore
        }
        return new KeyOption(keyId, keyId, false);
    }

    private void updateKeyStatus() {
        if (selectedKeyId == null) {
            keyStatusInfo.setText("");
            return;
        }
        try {
            ResponseEntity<KmsDtos.DescribeKeyResponse> response = kmsApiService.describeKey(selectedKeyId);
            KmsDtos.DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null) {
                boolean hasImportedMaterial = desc.getKeyMetadata().getOrigin() == IEnumKeyOrigin.Types.EXTERNAL &&
                        desc.getKeyMetadata().getKeyStatus() != IEnumKeyStatus.Types.PENDING_IMPORT;
                String statusText = hasImportedMaterial ?
                        "✅ Key material imported" :
                        "⚠️ No imported key material – ready for import";
                keyStatusInfo.setText(statusText);
                keyStatusInfo.addClassName(LumoUtility.FontSize.SMALL);
                keyStatusInfo.getStyle().set("margin-top", "var(--lumo-space-xs)");
            }
        } catch (Exception e) {
            keyStatusInfo.setText("");
        }
    }

    private void generateParameters() {
        if (selectedKeyId == null) {
            showWarningNotification("Please select an external KMS key first");
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
                wrappedKeyDisplay.setValue(params.getPublicKey());
                importTokenDisplay.setValue(params.getImportToken());
                currentImportToken = params.getImportToken();
                parametersValidUntil = params.getValidTo();

                String expiryMsg = parametersValidUntil != null ?
                        "Parameters valid until: " + parametersValidUntil.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) :
                        "Parameters generated (no expiry provided)";
                paramsExpiryInfo.setText(expiryMsg);
                paramsExpiryInfo.addClassName(LumoUtility.FontSize.XSMALL);
                paramsExpiryInfo.addClassName(LumoUtility.TextColor.TERTIARY);

                showSuccessNotification("Import parameters generated – valid for 24 hours");
            } else {
                showErrorNotification("Failed to generate import parameters");
            }
        } catch (Exception e) {
            showErrorNotification("Error: " + e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private void importKeyMaterial() {
        if (selectedKeyId == null) {
            showWarningNotification("Please select a key first");
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
        } catch (Exception e) {
            showErrorNotification("Error: " + e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private void deleteImportedMaterial() {
        if (selectedKeyId == null) {
            showWarningNotification("Please select a key first");
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
        } catch (Exception e) {
            showErrorNotification("Error: " + e.getMessage());
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
        encryptedMaterialField.clear();
        expirationDatePicker.clear();
        paramsExpiryInfo.setText("");
        currentImportToken = null;
        parametersValidUntil = null;
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        keyCombo.setEnabled(!show);
        refreshKeysButton.setEnabled(!show);
        getParamsButton.setEnabled(!show);
        importButton.setEnabled(!show);
        deleteMaterialButton.setEnabled(!show);
        encryptedMaterialField.setEnabled(!show);
        expirationDatePicker.setEnabled(!show);
    }

    private void showSuccessNotification(String msg) {
        Notification.show(msg, 6000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(String msg) {
        Notification.show(msg, 6000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showWarningNotification(String msg) {
        Notification.show(msg, 6000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    private void injectResponsiveStyles() {
        String css = """
                    .byok-key-layout, .byok-param-layout {
                        display: flex;
                        flex-wrap: wrap;
                        gap: var(--lumo-space-s);
                        align-items: center;
                    }
                    @media (max-width: 768px) {
                        .byok-key-layout, .byok-param-layout {
                            flex-direction: column;
                            align-items: stretch;
                        }
                        .byok-key-layout > *, .byok-param-layout > * {
                            width: 100% !important;
                        }
                        textarea {
                            font-size: 14px;
                        }
                    }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    // Helper class
    private static class KeyOption {
        private final String keyId;
        private final String displayName;
        private final boolean isExternal;

        KeyOption(String keyId, String aliasOrId, boolean isExternal) {
            this.keyId = keyId;
            this.displayName = (aliasOrId != null && !aliasOrId.equals(keyId)) ? aliasOrId + " (" + keyId + ")" : keyId;
            this.isExternal = isExternal;
        }

        String getKeyId() { return keyId; }
        String getDisplayName() { return displayName; }
        boolean isExternal() { return isExternal; }
    }
}