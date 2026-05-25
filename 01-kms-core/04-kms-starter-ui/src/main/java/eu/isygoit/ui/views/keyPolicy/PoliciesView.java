package eu.isygoit.ui.views.keyPolicy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
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
import eu.isygoit.ui.MainView;
import eu.isygoit.ui.views.keyPolicy.dialog.PolicyBuilderDialog;
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

    // UI components
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>("Select KMS Key");
    private final TextArea policyEditor = new TextArea("Policy Document (JSON)");
    private final ProgressBar loadingBar = new ProgressBar();

    private final Button loadButton = new Button("Load", new Icon(VaadinIcon.DOWNLOAD));
    private final Button saveButton = new Button("Save", new Icon(VaadinIcon.UPLOAD));
    private final Button formatButton = new Button("Format", new Icon(VaadinIcon.CODE));
    private final Button copyButton = MainView.createCopyButton(VaadinIcon.COPY, "", "Copy policy JSON to clipboard");
    private final Button builderButton = new Button("Policy Builder", new Icon(VaadinIcon.PUZZLE_PIECE));
    private final Button loadFromEditorButton = new Button("Edit in Builder", new Icon(VaadinIcon.REFRESH));

    private String selectedKeyId = null;
    private List<KeyOption> keyOptions = new ArrayList<>();

    @Autowired
    public PoliciesView(KmsApiService kmsApiService, ObjectMapper objectMapper) {
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("policies-view");

        buildHeader();
        buildKeySelector();
        buildActionBar();
        buildPolicyEditor();
        buildLoadingIndicator();
        attachResponsiveStyles();

        // Initial button state: disabled (no key selected)
        updateButtonsState();
        loadKeyOptions();
    }

    private void buildHeader() {
        H2 header = new H2("Key Policies");
        header.addClassNames(
                LumoUtility.FontSize.XXLARGE,
                LumoUtility.Margin.Bottom.NONE,
                LumoUtility.Margin.Top.NONE
        );
        add(header);
        add(new Hr());
    }

    private void buildKeySelector() {
        HorizontalLayout keyLayout = new HorizontalLayout();
        keyLayout.setWidthFull();
        keyLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        keyLayout.setSpacing(true);
        keyLayout.getStyle().set("flex-wrap", "wrap");

        keyCombo.setPlaceholder("Select a KMS key...");
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("400px");
        keyCombo.addValueChangeListener(e -> {
            selectedKeyId = e.getValue() != null ? e.getValue().getKeyId() : null;
            if (selectedKeyId == null) {
                policyEditor.clear();
            }
            updateButtonsState();
        });

        Button refreshBtn = new Button(new Icon(VaadinIcon.REFRESH));
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshBtn.setTooltipText("Refresh key list");
        refreshBtn.addClickListener(e -> loadKeyOptions());

        keyLayout.add(keyCombo, refreshBtn);
        add(keyLayout);
    }

    private void buildActionBar() {
        HorizontalLayout actionBar = new HorizontalLayout();
        actionBar.setSpacing(true);
        actionBar.getStyle().set("flex-wrap", "wrap");
        actionBar.addClassName("policies-action-bar");

        configureButton(loadButton, "Load policy from selected key", ButtonVariant.LUMO_PRIMARY);
        configureButton(saveButton, "Save policy to selected key", ButtonVariant.LUMO_SUCCESS);
        configureButton(formatButton, "Pretty-format JSON", ButtonVariant.LUMO_TERTIARY);
        copyButton.setTooltipText("Copy policy JSON to clipboard");
        copyButton.addClickListener(e -> {
            String content = policyEditor.getValue();
            if (StringUtils.hasText(content)) {
                MainView.copyToClipboard(content);
            } else {
                showWarning("Nothing to copy – editor is empty");
            }
        });
        configureButton(builderButton, "Open graphical policy builder", ButtonVariant.LUMO_CONTRAST);
        configureButton(loadFromEditorButton, "Load current JSON into builder", ButtonVariant.LUMO_TERTIARY);

        loadButton.addClickListener(e -> loadPolicy());
        saveButton.addClickListener(e -> savePolicy());
        formatButton.addClickListener(e -> formatPolicy());
        builderButton.addClickListener(e -> openPolicyBuilder());
        loadFromEditorButton.addClickListener(e -> loadFromEditorIntoBuilder());

        actionBar.add(loadButton, saveButton, formatButton, copyButton, builderButton, loadFromEditorButton);
        add(actionBar);
    }

    private void configureButton(Button button, String tooltip, ButtonVariant variant) {
        button.setTooltipText(tooltip);
        button.addThemeVariants(variant);
    }

    private void buildPolicyEditor() {
        policyEditor.setWidthFull();
        policyEditor.setHeight("500px");
        policyEditor.setPlaceholder("Policy JSON will appear here after loading...");
        policyEditor.getStyle()
                .set("font-family", "monospace")
                .set("font-size", "13px");
        policyEditor.addValueChangeListener(e -> updateButtonsState());
        add(policyEditor);
    }

    private void buildLoadingIndicator() {
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);
    }

    private void attachResponsiveStyles() {
        String css = """
                .policies-view .policies-action-bar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    align-items: center;
                }
                @media (max-width: 768px) {
                    .policies-view .policies-action-bar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .policies-view .policies-action-bar > * {
                        width: 100%;
                    }
                    .policies-view .vaadin-combo-box {
                        width: 100%;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    private void updateButtonsState() {
        boolean hasKey = selectedKeyId != null;
        boolean hasContent = StringUtils.hasText(policyEditor.getValue());
        loadButton.setEnabled(hasKey);
        saveButton.setEnabled(hasKey);
        formatButton.setEnabled(hasContent);
        copyButton.setEnabled(hasContent);
        builderButton.setEnabled(hasContent);
        loadFromEditorButton.setEnabled(hasKey && hasContent);
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
                keyOptions.clear();
                keyCombo.setItems(keyOptions);
            }
            if (selectedKeyId != null && keyOptions.stream().noneMatch(k -> k.getKeyId().equals(selectedKeyId))) {
                selectedKeyId = null;
                keyCombo.clear();
                policyEditor.clear();
            }
            updateButtonsState();
        } catch (Exception e) {
            showError("Failed to load keys: " + e.getMessage());
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
        } catch (Exception ignored) {
        }
        return keyId;
    }

    private void loadPolicy() {
        if (selectedKeyId == null) {
            showWarning("Please select a key first");
            return;
        }
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.GetKeyPolicyResponse> response = kmsApiService.getKeyPolicy(selectedKeyId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String policy = objectMapper.writeValueAsString(response.getBody().getPolicy());
                if (StringUtils.hasText(policy)) {
                    policyEditor.setValue(prettyPrintJson(policy));
                    showSuccess("Policy loaded");
                } else {
                    policyEditor.clear();
                    showWarning("Key has no policy");
                }
            } else {
                policyEditor.clear();
                showError("Failed to load policy: " + response.getStatusCode());
            }
        } catch (Exception e) {
            showError("Error loading policy: " + e.getMessage());
        } finally {
            showLoading(false);
            updateButtonsState();
        }
    }

    private void savePolicy() {
        if (selectedKeyId == null) {
            showWarning("Please select a key first");
            return;
        }
        String policyText = policyEditor.getValue();
        if (!StringUtils.hasText(policyText)) {
            showWarning("Policy cannot be empty");
            return;
        }
        Map<String, Object> policyMap;
        try {
            policyMap = objectMapper.readValue(policyText, Map.class);
        } catch (Exception e) {
            showError("Invalid JSON: " + e.getMessage());
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
                showSuccess("Policy saved successfully");
                loadPolicy();
            } else {
                showError("Save failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            showError("Error saving policy: " + e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private void formatPolicy() {
        String text = policyEditor.getValue();
        if (!StringUtils.hasText(text)) return;
        try {
            policyEditor.setValue(prettyPrintJson(text));
            showSuccess("Formatted");
        } catch (Exception e) {
            showError("Invalid JSON: " + e.getMessage());
        }
    }

    private void openPolicyBuilder() {
        KmsDtos.KeyPolicy existingPolicy = null;
        String currentText = policyEditor.getValue();
        if (StringUtils.hasText(currentText)) {
            try {
                existingPolicy = objectMapper.readValue(currentText, KmsDtos.KeyPolicy.class);
            } catch (Exception e) {
                showWarning("Current policy is not valid JSON – starting with empty policy");
            }
        }
        PolicyBuilderDialog dialog = new PolicyBuilderDialog(objectMapper, existingPolicy, newPolicy -> {
            try {
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(newPolicy);
                policyEditor.setValue(json);
                showSuccess("Policy built and inserted into editor");
                updateButtonsState();
            } catch (Exception ex) {
                showError("Failed to generate JSON: " + ex.getMessage());
            }
        });
        dialog.open();
    }

    private void loadFromEditorIntoBuilder() {
        String currentText = policyEditor.getValue();
        if (!StringUtils.hasText(currentText)) {
            showWarning("Editor is empty");
            return;
        }
        try {
            KmsDtos.KeyPolicy policy = objectMapper.readValue(currentText, KmsDtos.KeyPolicy.class);
            PolicyBuilderDialog dialog = new PolicyBuilderDialog(objectMapper, policy, newPolicy -> {
                try {
                    String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(newPolicy);
                    policyEditor.setValue(json);
                    showSuccess("Policy updated from builder");
                    updateButtonsState();
                } catch (Exception ex) {
                    showError("Error generating JSON: " + ex.getMessage());
                }
            });
            dialog.open();
        } catch (Exception e) {
            showError("Invalid JSON in editor: " + e.getMessage());
        }
    }

    private String prettyPrintJson(String json) throws Exception {
        Object obj = objectMapper.readValue(json, Object.class);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        keyCombo.setEnabled(!show);
        if (show) {
            loadButton.setEnabled(false);
            saveButton.setEnabled(false);
            formatButton.setEnabled(false);
            copyButton.setEnabled(false);
            builderButton.setEnabled(false);
            loadFromEditorButton.setEnabled(false);
            policyEditor.setEnabled(false);
        } else {
            policyEditor.setEnabled(true);
            updateButtonsState();
        }
    }

    private void showSuccess(String msg) {
        Notification.show(msg, 3000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String msg) {
        Notification.show(msg, 5000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showWarning(String msg) {
        Notification.show(msg, 3000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

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