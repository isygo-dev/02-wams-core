package eu.isygoit.ui.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
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
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainLayout;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "byok", layout = MainLayout.class)
@PageTitle("BYOK - Bring Your Own Key")
@PermitAll
public class ByokView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>("KMS Key (origin = EXTERNAL)");
    private final Button refreshKeysButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Button getParamsButton = new Button("Get Import Parameters", new Icon(VaadinIcon.DOWNLOAD));
    private final TextArea wrappedKeyDisplay = new TextArea("Wrapping Key (Public Key)");
    private final TextArea importTokenDisplay = new TextArea("Import Token");
    private final TextArea encryptedMaterialField = new TextArea("Encrypted Key Material (Base64)");
    private final DatePicker expirationDatePicker = new DatePicker("Expiration Date (optional)");
    private final Button importButton = new Button("Import Key Material", new Icon(VaadinIcon.UPLOAD));
    private final Button deleteMaterialButton = new Button("Delete Imported Material", new Icon(VaadinIcon.TRASH));
    private final ProgressBar loadingBar = new ProgressBar();

    private String selectedKeyId = null;
    private String currentImportToken = null;
    private List<KeyOption> keyOptions = new ArrayList<>();

    @Autowired
    public ByokView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-byok-view");

        H2 header = new H2("Bring Your Own Key (BYOK)");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        // Responsive key selection toolbar
        HorizontalLayout keyLayout = new HorizontalLayout();
        keyLayout.setWidthFull();
        keyLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        keyLayout.setSpacing(true);
        keyLayout.getStyle().set("flex-wrap", "wrap");
        keyLayout.addClassName("byok-key-layout");

        keyCombo.setPlaceholder("Select an EXTERNAL key...");
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("400px");
        keyCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                selectedKeyId = e.getValue().getKeyId();
                clearParameters();
            } else {
                selectedKeyId = null;
                clearParameters();
            }
        });

        refreshKeysButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshKeysButton.setTooltipText("Refresh key list");
        refreshKeysButton.addClickListener(e -> loadKeyOptions());

        keyLayout.add(keyCombo, refreshKeysButton);
        add(keyLayout);

        // Responsive parameters buttons row
        HorizontalLayout paramLayout = new HorizontalLayout(getParamsButton, deleteMaterialButton);
        paramLayout.setSpacing(true);
        paramLayout.getStyle().set("flex-wrap", "wrap");
        paramLayout.addClassName("byok-param-layout");
        getParamsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        deleteMaterialButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteMaterialButton.addClickListener(e -> deleteImportedMaterial());
        add(paramLayout);

        // Display of wrapping key and import token
        wrappedKeyDisplay.setWidthFull();
        wrappedKeyDisplay.setHeight("150px");
        wrappedKeyDisplay.setReadOnly(true);
        wrappedKeyDisplay.setPlaceholder("Wrapping key will appear here after generating parameters...");

        importTokenDisplay.setWidthFull();
        importTokenDisplay.setHeight("100px");
        importTokenDisplay.setReadOnly(true);
        importTokenDisplay.setPlaceholder("Import token will appear here...");

        add(wrappedKeyDisplay, importTokenDisplay);

        // Import material section
        encryptedMaterialField.setWidthFull();
        encryptedMaterialField.setHeight("150px");
        encryptedMaterialField.setPlaceholder("Paste the Base64-encoded encrypted key material here");
        encryptedMaterialField.setHelperText("Encrypt your key material using the public wrapping key above, then paste the result.");

        expirationDatePicker.setPlaceholder("YYYY-MM-DD");
        expirationDatePicker.setHelperText("If set, the key material will expire on this date.");

        add(encryptedMaterialField, expirationDatePicker, importButton);

        // Loading indicator
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        // Event listeners
        getParamsButton.addClickListener(e -> generateParameters());
        importButton.addClickListener(e -> importKeyMaterial());

        // Inject responsive CSS using JavaScript (fixes URL encoding issues)
        injectResponsiveStyles();

        // Initial load
        loadKeyOptions();
    }

    private void injectResponsiveStyles() {
        String css = """
                    .byok-key-layout,
                    .byok-param-layout {
                        display: flex;
                        flex-wrap: wrap;
                        gap: var(--lumo-space-s);
                        align-items: center;
                    }
                    @media (max-width: 768px) {
                        .byok-key-layout,
                        .byok-param-layout {
                            flex-direction: column;
                            align-items: stretch;
                        }
                        .byok-key-layout > *,
                        .byok-param-layout > * {
                            width: 100% !important;
                        }
                        .byok-key-layout > .vaadin-combo-box {
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

    private void loadKeyOptions() {
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.ListKeysResponse> response = kmsApiService.listKeys(100, null);
            KmsDtos.ListKeysResponse keys = response.getBody();
            if (keys != null && keys.getKeys() != null) {
                keyOptions = keys.getKeys().stream()
                        .map(entry -> fetchAliasAndOrigin(entry.getKeyId()))
                        .filter(opt -> opt.isExternal) // only external keys
                        .collect(Collectors.toList());
                keyCombo.setItems(keyOptions);
            } else {
                keyOptions = new ArrayList<>();
                keyCombo.setItems(keyOptions);
            }
            if (selectedKeyId != null && keyOptions.stream().noneMatch(opt -> opt.getKeyId().equals(selectedKeyId))) {
                selectedKeyId = null;
                keyCombo.clear();
                clearParameters();
            }
        } catch (Exception e) {
            Notification.show("Failed to load keys: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
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

    private void generateParameters() {
        if (selectedKeyId == null) {
            Notification.show("Please select a key first", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
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
                Notification.show("Parameters generated (valid until " + params.getValidTo() + ")", 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show("Failed to get import parameters", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void importKeyMaterial() {
        if (selectedKeyId == null) {
            Notification.show("Please select a key first", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        if (!StringUtils.hasText(encryptedMaterialField.getValue())) {
            Notification.show("Encrypted key material is required", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        if (currentImportToken == null) {
            Notification.show("Please generate import parameters first (they expire after 24h)", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        showLoading(true);
        try {
            KmsDtos.ImportKeyMaterialRequest request = KmsDtos.ImportKeyMaterialRequest.builder()
                    .keyId(selectedKeyId)
                    .importToken(currentImportToken)
                    .encryptedKeyMaterial(encryptedMaterialField.getValue())
                    .build();

            // Handle expiration
            if (expirationDatePicker.getValue() != null) {
                LocalDateTime validTo = expirationDatePicker.getValue().atTime(LocalTime.MAX);
                request.setValidTo(validTo);
                request.setExpirationModel(IEnumKeyExpirationModel.Types.KEY_MATERIAL_EXPIRES);
            } else {
                request.setExpirationModel(IEnumKeyExpirationModel.Types.KEY_MATERIAL_DOES_NOT_EXPIRE);
            }

            ResponseEntity<KmsDtos.ImportKeyMaterialResponse> response = kmsApiService.importKeyMaterial(selectedKeyId, request);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Key material imported successfully", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                clearParameters();
            } else {
                Notification.show("Import failed", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void deleteImportedMaterial() {
        if (selectedKeyId == null) {
            Notification.show("Please select a key first", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.DeleteImportedKeyMaterialResponse> response =
                    kmsApiService.deleteImportedKeyMaterial(selectedKeyId);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Imported key material deleted", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                clearParameters();
            } else {
                Notification.show("Deletion failed", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void clearParameters() {
        wrappedKeyDisplay.clear();
        importTokenDisplay.clear();
        encryptedMaterialField.clear();
        expirationDatePicker.clear();
        currentImportToken = null;
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

    // Helper class for key selection
    private static class KeyOption {
        private final String keyId;
        private final String displayName;
        private final boolean isExternal;

        KeyOption(String keyId, String aliasOrId) {
            this(keyId, aliasOrId, false);
        }

        KeyOption(String keyId, String aliasOrId, boolean isExternal) {
            this.keyId = keyId;
            this.displayName = aliasOrId != null ? aliasOrId + " (" + keyId + ")" : keyId;
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