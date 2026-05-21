package eu.isygoit.ui.views;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainLayout;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "policies", layout = MainLayout.class)
@PageTitle("Key Policies")
@PermitAll
public class PoliciesView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final ObjectMapper objectMapper;
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>("KMS Key");
    private final TextArea policyEditor = new TextArea("Policy (JSON)");
    private final Button loadButton = new Button("Load Policy", new Icon(VaadinIcon.DOWNLOAD));
    private final Button saveButton = new Button("Save Policy", new Icon(VaadinIcon.UPLOAD));
    private final Button formatButton = new Button("Format JSON", new Icon(VaadinIcon.CODE));
    private final ProgressBar loadingBar = new ProgressBar();

    private String selectedKeyId = null;
    private List<KeyOption> keyOptions = new ArrayList<>();

    @Autowired
    public PoliciesView(KmsApiService kmsApiService, ObjectMapper objectMapper) {
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-policies-view");

        H2 header = new H2("Key Policies");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        // Responsive key selection toolbar
        HorizontalLayout keyLayout = new HorizontalLayout();
        keyLayout.setWidthFull();
        keyLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        keyLayout.setSpacing(true);
        keyLayout.getStyle().set("flex-wrap", "wrap");
        keyLayout.addClassName("policies-key-layout");

        keyCombo.setPlaceholder("Select a KMS key...");
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("400px");
        keyCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                selectedKeyId = e.getValue().getKeyId();
            } else {
                selectedKeyId = null;
                policyEditor.clear();
            }
        });

        Button refreshKeysButton = new Button(new Icon(VaadinIcon.REFRESH));
        refreshKeysButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshKeysButton.setTooltipText("Refresh key list");
        refreshKeysButton.addClickListener(e -> loadKeyOptions());

        keyLayout.add(keyCombo, refreshKeysButton);
        add(keyLayout);

        // Responsive action bar
        HorizontalLayout actionBar = new HorizontalLayout(loadButton, saveButton, formatButton);
        actionBar.setSpacing(true);
        actionBar.getStyle().set("flex-wrap", "wrap");
        actionBar.addClassName("policies-action-bar");
        loadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        formatButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        add(actionBar);

        // Policy editor
        policyEditor.setWidthFull();
        policyEditor.setHeight("400px");
        policyEditor.setPlaceholder("Policy JSON will appear here after loading...");
        policyEditor.getStyle().set("font-family", "monospace");
        add(policyEditor);

        // Loading indicator
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        // Event listeners
        loadButton.addClickListener(e -> loadPolicy());
        saveButton.addClickListener(e -> savePolicy());
        formatButton.addClickListener(e -> formatPolicy());

        // Inject responsive CSS using JavaScript (fixes URL encoding issues)
        injectResponsiveStyles();

        // Initial load
        loadKeyOptions();
    }

    private void injectResponsiveStyles() {
        String css = """
                    .policies-key-layout,
                    .policies-action-bar {
                        display: flex;
                        flex-wrap: wrap;
                        gap: var(--lumo-space-s);
                        align-items: center;
                    }
                    @media (max-width: 768px) {
                        .policies-key-layout,
                        .policies-action-bar {
                            flex-direction: column;
                            align-items: stretch;
                        }
                        .policies-key-layout > *,
                        .policies-action-bar > * {
                            width: 100% !important;
                        }
                        .policies-key-layout > .vaadin-combo-box {
                            width: 100% !important;
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
                        .map(entry -> new KeyOption(entry.getKeyId(), fetchAlias(entry.getKeyId())))
                        .collect(Collectors.toList());
                keyCombo.setItems(keyOptions);
            } else {
                keyOptions = new ArrayList<>();
                keyCombo.setItems(keyOptions);
            }
            // Clear selection if current key no longer exists
            if (selectedKeyId != null && keyOptions.stream().noneMatch(opt -> opt.getKeyId().equals(selectedKeyId))) {
                selectedKeyId = null;
                keyCombo.clear();
                policyEditor.clear();
            }
        } catch (Exception e) {
            Notification.show("Failed to load keys: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private String fetchAlias(String keyId) {
        try {
            ResponseEntity<KmsDtos.DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
            KmsDtos.DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null && StringUtils.hasText(desc.getKeyMetadata().getKeyAlias())) {
                return desc.getKeyMetadata().getKeyAlias();
            }
        } catch (Exception e) {
            // ignore
        }
        return keyId;
    }

    private void loadPolicy() {
        if (selectedKeyId == null) {
            Notification.show("Please select a key first", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.GetKeyPolicyResponse> response = kmsApiService.getKeyPolicy(selectedKeyId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String policy = objectMapper.writeValueAsString(response.getBody().getPolicy());
                if (StringUtils.hasText(policy)) {
                    String prettyPolicy = prettyPrintJson(policy);
                    policyEditor.setValue(prettyPolicy);
                    Notification.show("Policy loaded", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    policyEditor.clear();
                    Notification.show("Key has no policy (default will be applied on save?)", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_WARNING);
                }
            } else {
                policyEditor.clear();
                Notification.show("Failed to load policy: " + response.getStatusCode(), 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification.show("Error loading policy: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void savePolicy() {
        if (selectedKeyId == null) {
            Notification.show("Please select a key first", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        String policyText = policyEditor.getValue();
        if (!StringUtils.hasText(policyText)) {
            Notification.show("Policy cannot be empty", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        // Validate JSON before sending
        Map<String, Object> policyMap;
        try {
            policyMap = objectMapper.readValue(policyText, Map.class);
        } catch (Exception e) {
            Notification.show("Invalid JSON format: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        showLoading(true);
        try {
            KmsDtos.PutKeyPolicyRequest request = KmsDtos.PutKeyPolicyRequest.builder()
                    .keyId(selectedKeyId)
                    .policy(policyMap)
                    .bypassPolicyLockoutSafetyCheck(false)
                    .build();
            ResponseEntity<KmsDtos.PutKeyPolicyResponse> response = kmsApiService.putKeyPolicy(selectedKeyId, request);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Policy saved successfully", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                // Re-load the policy to show the saved version (pretty-printed again)
                loadPolicy();
            } else {
                Notification.show("Failed to save policy: " + response.getStatusCode(), 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification.show("Error saving policy: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void formatPolicy() {
        String text = policyEditor.getValue();
        if (!StringUtils.hasText(text)) return;
        try {
            String pretty = prettyPrintJson(text);
            policyEditor.setValue(pretty);
            Notification.show("Policy formatted", 2000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Invalid JSON: " + e.getMessage(), 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private String prettyPrintJson(String json) throws Exception {
        Object jsonObj = objectMapper.readValue(json, Object.class);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        keyCombo.setEnabled(!show);
        loadButton.setEnabled(!show);
        saveButton.setEnabled(!show);
        formatButton.setEnabled(!show);
        policyEditor.setEnabled(!show);
    }

    // Helper class for key selection
    private static class KeyOption {
        private final String keyId;
        private final String displayName;

        KeyOption(String keyId, String aliasOrId) {
            this.keyId = keyId;
            this.displayName = aliasOrId != null ? aliasOrId + " (" + keyId + ")" : keyId;
        }

        String getKeyId() {
            return keyId;
        }

        String getDisplayName() {
            return displayName;
        }
    }
}