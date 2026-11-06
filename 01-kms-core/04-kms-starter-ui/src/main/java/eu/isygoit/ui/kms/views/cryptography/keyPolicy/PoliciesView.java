package eu.isygoit.ui.kms.views.cryptography.keyPolicy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.FloatingRefreshButton;
import eu.isygoit.ui.common.component.PageHeader;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.common.KmsConfirmationDialog;
import eu.isygoit.ui.kms.views.cryptography.keyPolicy.dialog.PolicyBuilderDialog;
import eu.isygoit.ui.kms.views.cryptography.keyPolicy.dialog.PolicyEvaluationDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SpringComponent
@RouteScope
@Route(value = "kms/policies", layout = KmsMainLayout.class)
@PermitAll
public class PoliciesView extends ManagementVerticalView {
    private final KmsApiService kmsApiService;
    private final ObjectMapper objectMapper;
    // UI components
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>(I18n.t("kms.policy.view.select.key"));
    private final TextArea policyEditor = new TextArea(I18n.t("kms.policy.view.policy.editor.label"));
    private final ProgressBar loadingBar = new ProgressBar();
    private final Button loadButton = new Button(I18n.t("kms.policy.view.load.button"), new Icon(VaadinIcon.DOWNLOAD));
    private final Button saveButton = new Button(I18n.t("kms.policy.view.save.button"), new Icon(VaadinIcon.UPLOAD));
    private final Button formatButton = new Button(I18n.t("kms.policy.view.format.button"), new Icon(VaadinIcon.CODE));
    private final Button copyButton = new Button(new Icon(VaadinIcon.COPY));
    private final Button builderButton = new Button(I18n.t("kms.policy.view.builder.button"), new Icon(VaadinIcon.PUZZLE_PIECE));
    private final TextField actionField = new TextField(I18n.t("kms.policy.view.check.action"));
    private final Button checkActionButton = new Button(I18n.t("kms.policy.view.check.action"), new Icon(VaadinIcon.SEARCH));
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

        updateButtonsState();
        loadKeyOptions();
    }

    @Override
    public String getPageTitle() {
        return I18n.t("kms.policy.view.title");
    }

    private void buildHeader() {
        add(new PageHeader("kms.policy.view.title", "kms.policy.view.subtitle"));
        add(new Hr());
    }

    private void buildKeySelector() {
        HorizontalLayout keyLayout = new HorizontalLayout();
        keyLayout.setWidthFull();
        keyLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        keyLayout.setSpacing(true);
        keyLayout.addClassName("policies-key-layout");

        keyCombo.setPlaceholder(I18n.t("kms.policy.view.select.key"));
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("400px");
        keyCombo.addValueChangeListener(e -> {
            selectedKeyId = e.getValue() != null ? e.getValue().getKeyId() : null;
            if (selectedKeyId == null) {
                policyEditor.clear();
            }
            updateButtonsState();
        });

        keyLayout.add(keyCombo);
        add(keyLayout);

        // Floating refresh button (reloads key list; keeps selected key and editor content)
        add(new FloatingRefreshButton(this::loadKeyOptions));
    }

    private void buildActionBar() {
        HorizontalLayout actionBar = new HorizontalLayout();
        actionBar.setSpacing(true);
        actionBar.addClassName("policies-action-bar");

        configureButton(loadButton, I18n.t("kms.policy.view.load.policy.tooltip"), ButtonVariant.LUMO_PRIMARY);
        configureButton(saveButton, I18n.t("kms.policy.view.save.policy.tooltip"), ButtonVariant.LUMO_SUCCESS);
        configureButton(formatButton, I18n.t("kms.policy.view.format.tooltip"), ButtonVariant.LUMO_TERTIARY);
        configureButton(copyButton, I18n.t("kms.policy.view.copy.tooltip"), ButtonVariant.LUMO_TERTIARY_INLINE);
        configureButton(builderButton, I18n.t("kms.policy.view.builder.tooltip"), ButtonVariant.LUMO_CONTRAST);

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
        policyEditor.setPlaceholder(I18n.t("kms.policy.view.policy.placeholder"));
        policyEditor.addClassName("policies-policy-editor");
        policyEditor.addValueChangeListener(e -> updateButtonsState());
        add(policyEditor);
    }

    private void buildActionChecker() {
        HorizontalLayout checkerLayout = new HorizontalLayout();
        checkerLayout.setWidthFull();
        checkerLayout.setAlignItems(FlexComponent.Alignment.END);
        checkerLayout.setSpacing(true);
        checkerLayout.addClassName("policies-action-checker");

        actionField.setPlaceholder(I18n.t("kms.policy.view.action.placeholder"));
        actionField.setWidth("300px");
        actionField.setClearButtonVisible(true);

        checkActionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        checkActionButton.setTooltipText(I18n.t("kms.policy.view.action.check.tooltip"));
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
            showError(I18n.t("kms.policy.view.load.keys.error", errorMsg));
            log.error("Failed to load keys: {}", errorMsg);
        } catch (Exception e) {
            showError(I18n.t("kms.policy.view.load.keys.error", e.getMessage()));
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
            if (!silentFailure) showWarning(I18n.t("kms.policy.view.select.key.first"));
            return;
        }
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.GetKeyPolicyResponse> response = kmsApiService.getKeyPolicy(selectedKeyId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String policy = objectMapper.writeValueAsString(response.getBody().getPolicy());
                if (StringUtils.hasText(policy)) {
                    policyEditor.setValue(prettyPrintJson(policy));
                    if (!silentFailure) showSuccess(I18n.t("kms.policy.view.policy.loaded"));
                } else {
                    policyEditor.clear();
                    if (!silentFailure) showWarning(I18n.t("kms.policy.view.no.policy"));
                }
            } else {
                policyEditor.clear();
                if (!silentFailure) showError(I18n.t("kms.policy.view.load.policy.error", response.getStatusCode()));
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            if (!silentFailure) showError(I18n.t("kms.policy.view.load.policy.error", errorMsg));
            log.error("Failed to load policy for key {}: {}", selectedKeyId, errorMsg);
        } catch (Exception e) {
            if (!silentFailure) showError(I18n.t("kms.policy.view.load.policy.error", e.getMessage()));
            log.error("Failed to load policy for key {}: {}", selectedKeyId, e.getMessage());
        } finally {
            showLoading(false);
            updateButtonsState();
        }
    }

    private void savePolicy() {
        if (selectedKeyId == null) {
            showWarning(I18n.t("kms.policy.view.select.key.first"));
            return;
        }
        String policyText = policyEditor.getValue();

        if (!StringUtils.hasText(policyText)) {
            new KmsConfirmationDialog(
                    I18n.t("kms.policy.view.erase.confirm.header"),
                    I18n.t("kms.policy.view.erase.confirm.message"),
                    I18n.t("kms.policy.view.erase.confirm.button"),
                    I18n.t("kms.policy.view.erase.cancel.button"),
                    () -> {
                        Map<String, Object> emptyPolicy = new HashMap<>();
                        performSave(emptyPolicy, true);
                    },
                    ButtonVariant.LUMO_ERROR,
                    ButtonVariant.LUMO_PRIMARY).open();
            return;
        }

        Map<String, Object> policyMap;
        try {
            policyMap = objectMapper.readValue(policyText, Map.class);
        } catch (Exception e) {
            showError(I18n.t("kms.policy.view.invalid.json", e.getMessage()));
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
                showSuccess(I18n.t("kms.policy.view.policy.saved"));
                loadPolicy(true);
            } else {
                showError(I18n.t("kms.policy.view.save.policy.error", response.getStatusCode()));
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showError(I18n.t("kms.policy.view.save.policy.error", errorMsg));
            log.error("Failed to save policy for key {}: {}", selectedKeyId, errorMsg);
        } catch (Exception e) {
            showError(I18n.t("kms.policy.view.save.policy.error", e.getMessage()));
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
            showSuccess(I18n.t("kms.policy.view.policy.formatted"));
        } catch (Exception e) {
            showError(I18n.t("kms.policy.view.invalid.json", e.getMessage()));
        }
    }

    private void copyPolicyToClipboard() {
        String content = policyEditor.getValue();
        if (!StringUtils.hasText(content)) {
            showWarning(I18n.t("kms.policy.view.nothing.to.copy"));
            return;
        }
        UI.getCurrent().getPage().executeJs(
                "const showToast = (message, tone, duration) => {" +
                        "  const notification = document.createElement('div'); " +
                        "  notification.className = 'kms-copy-toast kms-copy-toast--' + tone; " +
                        "  notification.textContent = message; " +
                        "  document.body.appendChild(notification); " +
                        "  setTimeout(() => notification.remove(), duration); " +
                        "}; " +
                        "navigator.clipboard.writeText($0) " +
                        "  .then(() => showToast($1, 'success', 2000)) " +
                        "  .catch(() => showToast($2, 'error', 3000));",
                content, I18n.t("kms.policy.view.policy.copied"), I18n.t("kms.policy.view.copy.failed")
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
                        I18n.t("kms.policy.view.current.content.invalid"),
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
                showSuccess(I18n.t("kms.policy.view.policy.built"));
                updateButtonsState();
            } catch (Exception ex) {
                showError(I18n.t("kms.policy.view.invalid.json", ex.getMessage()));
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
            showWarning(I18n.t("kms.policy.view.action.check.enter"));
            return;
        }

        String policyText = policyEditor.getValue();
        if (!StringUtils.hasText(policyText)) {
            showWarning(I18n.t("kms.policy.view.action.check.no.policy"));
            return;
        }

        Map<String, Object> policyMap;
        try {
            policyMap = objectMapper.readValue(policyText, Map.class);
        } catch (Exception e) {
            showError(I18n.t("kms.policy.view.action.check.invalid.policy", e.getMessage()));
            return;
        }

        String keyArn = null;
        if (selectedKeyId != null) {
            keyArn = "wrn:wams:kms:*:*:key/" + selectedKeyId;
        }

        EvaluationResult result = evaluateAction(action, policyMap, keyArn);

        new PolicyEvaluationDialog(
                action,
                result.decision,
                result.reason,
                result.sid,
                result.principal,
                selectedKeyId,
                keyArn,
                result.matchedStatement
        ).open();
    }

    private EvaluationResult evaluateAction(String action, Map<String, Object> policyMap, String keyArn) {
        boolean explicitlyAllowed = false;
        String matchedStatementJson = null;
        String matchedSid = null;
        String matchedPrincipal = null;
        String reason = I18n.t("kms.policy.eval.reason.no.match");

        Object statementsObj = policyMap.get("Statement");
        if (!(statementsObj instanceof Collection<?> statements)) {
            return new EvaluationResult("DENIED", I18n.t("kms.policy.eval.reason.no.valid.statement.array"), null, null, null);
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
                return new EvaluationResult("DENIED", I18n.t("kms.policy.eval.reason.deny"), statementStr, sid, principal);
            } else if ("Allow".equals(effect)) {
                explicitlyAllowed = true;
                matchedStatementJson = statementStr;
                matchedSid = sid;
                matchedPrincipal = principal;
                reason = I18n.t("kms.policy.eval.reason.allow");
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
}