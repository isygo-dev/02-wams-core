package eu.isygoit.ui.kms.views.cryptography.keyPolicy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Pre;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.cryptography.keyPolicy.dialog.PolicyBuilderDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@VaadinSessionScope
@Route(value = "kms/policies", layout = KmsMainLayout.class)
@PageTitle("Key Policies")
@PermitAll
public class PoliciesView extends VerticalLayout implements BeforeEnterObserver {

    private final KmsApiService kmsApiService;
    private final ObjectMapper objectMapper;

    // UI components
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>("Select KMS Key");
    private final TextArea policyEditor = new TextArea("Policy Document (JSON)");
    private final ProgressBar loadingBar = new ProgressBar();

    private final Button loadButton = new Button("Load", new Icon(VaadinIcon.DOWNLOAD));
    private final Button saveButton = new Button("Save", new Icon(VaadinIcon.UPLOAD));
    private final Button formatButton = new Button("Format", new Icon(VaadinIcon.CODE));
    private final Button copyButton = new Button(new Icon(VaadinIcon.COPY));
    private final Button builderButton = new Button("Policy Builder", new Icon(VaadinIcon.PUZZLE_PIECE));
    private final TextField actionField = new TextField("Action to check");
    private final Button checkActionButton = new Button("Check Action", new Icon(VaadinIcon.SEARCH));

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
        buildActionChecker();
        buildLoadingIndicator();
        attachResponsiveStyles();

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
        configureButton(copyButton, "Copy policy JSON to clipboard", ButtonVariant.LUMO_TERTIARY_INLINE);
        configureButton(builderButton, "Open graphical policy builder", ButtonVariant.LUMO_CONTRAST);

        loadButton.addClickListener(e -> loadPolicy(false));
        saveButton.addClickListener(e -> savePolicy());
        formatButton.addClickListener(e -> formatPolicy());
        copyButton.addClickListener(e -> copyPolicyToClipboard());
        builderButton.addClickListener(e -> openPolicyBuilder());

        actionBar.add(loadButton, saveButton, formatButton, copyButton, builderButton);
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

    private void buildActionChecker() {
        HorizontalLayout checkerLayout = new HorizontalLayout();
        checkerLayout.setWidthFull();
        checkerLayout.setAlignItems(FlexComponent.Alignment.END);
        checkerLayout.setSpacing(true);
        checkerLayout.getStyle().set("flex-wrap", "wrap");

        actionField.setPlaceholder("e.g., kms:Decrypt, kms:Encrypt, *");
        actionField.setWidth("300px");
        actionField.setClearButtonVisible(true);

        checkActionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        checkActionButton.setTooltipText("Evaluate if this action is allowed by the current policy");
        checkActionButton.addClickListener(e -> checkActionAgainstPolicy());

        checkerLayout.add(actionField, checkActionButton);
        add(checkerLayout);
    }

    private void buildLoadingIndicator() {
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);
    }

    private void attachResponsiveStyles() {
        String css = """
                .policies-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
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
        builderButton.setEnabled(true);
        checkActionButton.setEnabled(hasContent);
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
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showError("Failed to load keys: " + errorMsg);
            log.error("Failed to load keys: {}", errorMsg);
        } catch (Exception e) {
            showError("Failed to load keys: " + e.getMessage());
            log.error("Failed to load keys: {}", e.getMessage());
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

    private void loadPolicy(boolean silentFailure) {
        if (selectedKeyId == null) {
            if (!silentFailure) showWarning("Please select a key first");
            return;
        }
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.GetKeyPolicyResponse> response = kmsApiService.getKeyPolicy(selectedKeyId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String policy = objectMapper.writeValueAsString(response.getBody().getPolicy());
                if (StringUtils.hasText(policy)) {
                    policyEditor.setValue(prettyPrintJson(policy));
                    if (!silentFailure) showSuccess("Policy loaded");
                } else {
                    policyEditor.clear();
                    if (!silentFailure) showWarning("Key has no policy");
                }
            } else {
                policyEditor.clear();
                if (!silentFailure) showError("Failed to load policy: " + response.getStatusCode());
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            if (!silentFailure) showError("Error loading policy: " + errorMsg);
            log.error("Failed to load policy for key {}: {}", selectedKeyId, errorMsg);
        } catch (Exception e) {
            if (!silentFailure) showError("Error loading policy: " + e.getMessage());
            log.error("Failed to load policy for key {}: {}", selectedKeyId, e.getMessage());
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
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader("Erase policy");
            confirmDialog.setText(
                    "The policy editor is empty.\n" +
                            "Saving will erase the current policy from the key.\n" +
                            "Are you sure you want to proceed?"
            );
            confirmDialog.setConfirmText("Erase policy");
            confirmDialog.setCancelText("Cancel");
            confirmDialog.setConfirmButtonTheme("error primary");
            confirmDialog.addConfirmListener(event -> {
                Map<String, Object> emptyPolicy = new HashMap<>();
                performSave(emptyPolicy, true);
            });
            confirmDialog.open();
            return;
        }

        Map<String, Object> policyMap;
        try {
            policyMap = objectMapper.readValue(policyText, Map.class);
        } catch (Exception e) {
            showError("Invalid JSON: " + e.getMessage());
            return;
        }
        performSave(policyMap, false);
    }

    private void performSave(Map<String, Object> policyMap, boolean bypassSafetyCheck) {
        showLoading(true);
        try {
            KmsDtos.PutKeyPolicyRequest request = KmsDtos.PutKeyPolicyRequest.builder()
                    .keyId(selectedKeyId)
                    .policy(policyMap)
                    .bypassPolicyLockoutSafetyCheck(bypassSafetyCheck)
                    .build();
            ResponseEntity<KmsDtos.PutKeyPolicyResponse> response = kmsApiService.putKeyPolicy(selectedKeyId, request);
            if (response.getStatusCode().is2xxSuccessful()) {
                showSuccess("Policy saved successfully");
                loadPolicy(true);
            } else {
                showError("Save failed: " + response.getStatusCode());
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showError("Error saving policy: " + errorMsg);
            log.error("Failed to save policy for key {}: {}", selectedKeyId, errorMsg);
        } catch (Exception e) {
            showError("Error saving policy: " + e.getMessage());
            log.error("Failed to save policy for key {}: {}", selectedKeyId, e.getMessage());
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

    private void copyPolicyToClipboard() {
        String content = policyEditor.getValue();
        if (!StringUtils.hasText(content)) {
            showWarning("Nothing to copy – editor is empty");
            return;
        }
        UI.getCurrent().getPage().executeJs(
                "navigator.clipboard.writeText($0).then(() => { " +
                        "  const notification = document.createElement('div'); " +
                        "  notification.textContent = 'Policy JSON copied to clipboard'; " +
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
                        "  notification.textContent = 'Failed to copy. Check clipboard permissions.'; " +
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
                        "});",
                content
        );
    }

    private void openPolicyBuilder() {
        KmsDtos.KeyPolicy existingPolicy = null;
        String currentText = policyEditor.getValue();
        if (StringUtils.hasText(currentText)) {
            try {
                existingPolicy = objectMapper.readValue(currentText, KmsDtos.KeyPolicy.class);
            } catch (Exception e) {
                Notification.show(
                        "Current editor content is not a valid KeyPolicy JSON.\nStarting with an empty policy.",
                        5000,
                        Notification.Position.BOTTOM_END
                ).addThemeVariants(NotificationVariant.LUMO_WARNING);
                existingPolicy = null;
                return;
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

    private String prettyPrintJson(String json) throws Exception {
        Object obj = objectMapper.readValue(json, Object.class);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    private void checkActionAgainstPolicy() {
        String action = actionField.getValue();
        if (!StringUtils.hasText(action)) {
            showWarning("Please enter an action to check (e.g., kms:Decrypt)");
            return;
        }

        String policyText = policyEditor.getValue();
        if (!StringUtils.hasText(policyText)) {
            showWarning("No policy loaded or editor is empty. Load or write a policy first.");
            return;
        }

        Map<String, Object> policyMap;
        try {
            policyMap = objectMapper.readValue(policyText, Map.class);
        } catch (Exception e) {
            showError("Invalid policy JSON: " + e.getMessage());
            return;
        }

        String keyArn = null;
        if (selectedKeyId != null) {
            keyArn = "wrn:wams:kms:*:*:key/" + selectedKeyId;
        }

        EvaluationResult result = evaluateAction(action, policyMap, keyArn);

        // Build refined dialog
        Dialog resultDialog = new Dialog();
        resultDialog.setHeaderTitle("Action Evaluation: " + action);
        resultDialog.setWidth("600px");
        resultDialog.setResizable(true);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        // Decision icon and text
        HorizontalLayout decisionLayout = new HorizontalLayout();
        decisionLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        decisionLayout.setSpacing(true);

        Icon decisionIcon;
        String decisionText;
        String decisionColor;
        if ("ALLOWED".equals(result.decision)) {
            decisionIcon = VaadinIcon.CHECK_CIRCLE.create();
            decisionIcon.setColor("#2e7d32");
            decisionText = "ALLOWED";
            decisionColor = "#2e7d32";
        } else if ("DENIED".equals(result.decision)) {
            decisionIcon = VaadinIcon.CLOSE_CIRCLE.create();
            decisionIcon.setColor("#c62828");
            decisionText = "DENIED";
            decisionColor = "#c62828";
        } else {
            decisionIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
            decisionIcon.setColor("#ef6c00");
            decisionText = "UNCERTAIN";
            decisionColor = "#ef6c00";
        }

        Span decisionSpan = new Span(decisionText);
        decisionSpan.getStyle().set("font-weight", "bold");
        decisionSpan.getStyle().set("color", decisionColor);
        decisionSpan.getStyle().set("font-size", "1.2em");

        decisionLayout.add(decisionIcon, decisionSpan);
        content.add(decisionLayout);

        // Reason
        Span reasonSpan = new Span("Reason: " + result.reason);
        reasonSpan.getStyle().set("font-style", "italic");
        content.add(reasonSpan);

        // Sid and Principal
        if (result.sid != null && !result.sid.isEmpty()) {
            HorizontalLayout sidLayout = new HorizontalLayout();
            sidLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            sidLayout.setSpacing(true);
            Span sidLabel = new Span("🆔 Sid:");
            sidLabel.getStyle().set("font-weight", "bold");
            Span sidValue = new Span(result.sid);
            sidLayout.add(sidLabel, sidValue);
            content.add(sidLayout);
        }

        if (result.principal != null && !result.principal.isEmpty()) {
            HorizontalLayout principalLayout = new HorizontalLayout();
            principalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            principalLayout.setSpacing(true);
            Span principalLabel = new Span("👤 Principal:");
            principalLabel.getStyle().set("font-weight", "bold");
            Span principalValue = new Span(result.principal);
            principalLayout.add(principalLabel, principalValue);
            content.add(principalLayout);
        }

        // Key context
        if (selectedKeyId == null) {
            Span warningSpan = new Span("⚠️ No KMS key selected. Resource matching assumed '*' (any key).");
            warningSpan.getStyle().set("color", "#ef6c00");
            warningSpan.getStyle().set("font-size", "0.9em");
            content.add(warningSpan);
        } else {
            Span resourceSpan = new Span("🔑 Evaluated with key ARN: " + keyArn);
            resourceSpan.getStyle().set("font-size", "0.9em");
            content.add(resourceSpan);
        }

        // Matched statement (full JSON)
        if (result.matchedStatement != null && !result.matchedStatement.isEmpty()) {
            Span matchedHeader = new Span("📄 Matched statement:");
            matchedHeader.getStyle().set("font-weight", "bold");
            content.add(matchedHeader);

            Pre pre = new Pre();
            pre.setText(result.matchedStatement);
            pre.getStyle().set("background-color", "#f5f5f5");
            pre.getStyle().set("padding", "8px");
            pre.getStyle().set("border-radius", "4px");
            pre.getStyle().set("font-size", "12px");
            pre.getStyle().set("overflow-x", "auto");
            pre.setWidthFull();
            content.add(pre);
        }

        // Help tip
        Span helpSpan = new Span("💡 Tip: Explicit Deny overrides any Allow. If no Allow matches, the action is Denied by default.");
        helpSpan.getStyle().set("font-size", "0.85em");
        helpSpan.getStyle().set("color", "#666");
        content.add(helpSpan);

        Button closeBtn = new Button("Close", e -> resultDialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        resultDialog.getFooter().add(closeBtn);
        resultDialog.add(content);
        resultDialog.open();
    }

    private EvaluationResult evaluateAction(String action, Map<String, Object> policyMap, String keyArn) {
        boolean explicitlyAllowed = false;
        String matchedStatementJson = null;
        String matchedSid = null;
        String matchedPrincipal = null;
        String reason = "No matching statement allowed the action.";

        Object statementsObj = policyMap.get("Statement");
        if (!(statementsObj instanceof Collection<?> statements)) {
            return new EvaluationResult("DENIED", "Policy has no valid Statement array.", null, null, null);
        }

        for (Object stmtObj : statements) {
            if (!(stmtObj instanceof Map<?, ?> stmt)) continue;

            String effect = stmt.get("Effect") != null ? stmt.get("Effect").toString() : null;
            if (!"Allow".equals(effect) && !"Deny".equals(effect)) continue;

            Object actionObj = stmt.get("Action");
            boolean actionMatches = actionMatches(actionObj, action);
            if (!actionMatches) continue;

            Object resourceObj = stmt.get("Resource");
            boolean resourceMatches = resourceMatches(resourceObj, keyArn);
            if (resourceObj != null && !resourceMatches) continue;

            // Extract Sid and Principal
            String sid = stmt.get("Sid") != null ? stmt.get("Sid").toString() : null;
            Object principalObj = stmt.get("Principal");
            String principal = null;
            if (principalObj != null) {
                if (principalObj instanceof String) {
                    principal = (String) principalObj;
                } else if (principalObj instanceof Map) {
                    try {
                        principal = objectMapper.writeValueAsString(principalObj);
                    } catch (Exception e) {
                        principal = principalObj.toString();
                    }
                } else {
                    principal = principalObj.toString();
                }
            }

            String statementStr = statementToString(stmt);

            if ("Deny".equals(effect)) {
                return new EvaluationResult("DENIED", "Explicit Deny statement matches.", statementStr, sid, principal);
            } else if ("Allow".equals(effect)) {
                explicitlyAllowed = true;
                matchedStatementJson = statementStr;
                matchedSid = sid;
                matchedPrincipal = principal;
                reason = "Explicit Allow statement matches.";
            }
        }

        if (explicitlyAllowed) {
            return new EvaluationResult("ALLOWED", reason, matchedStatementJson, matchedSid, matchedPrincipal);
        } else {
            return new EvaluationResult("DENIED", reason, null, null, null);
        }
    }

    private boolean actionMatches(Object actionObj, String action) {
        if (actionObj == null) return false;
        if (actionObj instanceof String pattern) {
            return matchActionPattern(pattern, action);
        }
        if (actionObj instanceof Collection<?> actions) {
            for (Object a : actions) {
                if (a instanceof String && matchActionPattern((String) a, action)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchActionPattern(String pattern, String action) {
        if (pattern.equals("*")) return true;
        if (pattern.equals("kms:*") && action.startsWith("kms:")) return true;
        return pattern.equals(action);
    }

    private boolean resourceMatches(Object resourceObj, String keyArn) {
        if (keyArn == null) {
            return resourceObj == null || "*".equals(resourceObj);
        }
        if (resourceObj == null) return true;
        if (resourceObj instanceof String res) {
            if (res.equals("*")) return true;
            if (selectedKeyId != null && res.contains(selectedKeyId)) return true;
            return res.equals(keyArn);
        }
        if (resourceObj instanceof Collection<?> resources) {
            for (Object r : resources) {
                if (r instanceof String && resourceMatches(r, keyArn)) return true;
            }
        }
        return false;
    }

    private String statementToString(Map<?, ?> stmt) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(stmt);
        } catch (Exception e) {
            return stmt.toString();
        }
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
            checkActionButton.setEnabled(false);
            policyEditor.setEnabled(false);
        } else {
            policyEditor.setEnabled(true);
            updateButtonsState();
        }
    }

    private void showSuccess(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showWarning(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END)
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

    private record EvaluationResult(String decision, String reason, String matchedStatement, String sid,
                                    String principal) {
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("user") == null) {
            String currentPath = event.getLocation().getPath();
            event.forwardTo("login?redirect=" + currentPath);
        }
    }
}