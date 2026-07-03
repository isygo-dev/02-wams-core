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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
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

    private void buildHeader() {
        H2 header = new H2(I18n.t("kms.policy.view.title"));
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

        Button refreshBtn = new Button(new Icon(VaadinIcon.REFRESH));
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshBtn.setTooltipText(I18n.t("kms.policy.view.refresh.tooltip"));
        refreshBtn.addClickListener(e -> loadKeyOptions());

        keyLayout.add(keyCombo, refreshBtn);
        add(keyLayout);
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
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(I18n.t("kms.policy.view.erase.confirm.header"));
            confirmDialog.setText(I18n.t("kms.policy.view.erase.confirm.message"));
            confirmDialog.setConfirmText(I18n.t("kms.policy.view.erase.confirm.button"));
            confirmDialog.setCancelText(I18n.t("kms.policy.view.erase.cancel.button"));
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
                "navigator.clipboard.writeText($0).then(() => { " +
                        "  const notification = document.createElement('div'); " +
                        "  notification.textContent = $1; " +
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
                        "  notification.textContent = $2; " +
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

        // Build refined dialog
        Dialog resultDialog = new Dialog();
        resultDialog.setHeaderTitle(I18n.t("kms.policy.eval.title", action));
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
        String decisionModifierClass;
        if ("ALLOWED".equals(result.decision)) {
            decisionIcon = VaadinIcon.CHECK_CIRCLE.create();
            decisionText = I18n.t("kms.policy.eval.decision.allowed");
            decisionModifierClass = "policy-eval-decision-span--allowed";
        } else if ("DENIED".equals(result.decision)) {
            decisionIcon = VaadinIcon.CLOSE_CIRCLE.create();
            decisionText = I18n.t("kms.policy.eval.decision.denied");
            decisionModifierClass = "policy-eval-decision-span--denied";
        } else {
            decisionIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
            decisionText = I18n.t("kms.policy.eval.decision.uncertain");
            decisionModifierClass = "policy-eval-decision-span--uncertain";
        }
        decisionIcon.addClassName(decisionModifierClass);

        Span decisionSpan = new Span(decisionText);
        decisionSpan.addClassName("policy-eval-decision-span");
        decisionSpan.addClassName(decisionModifierClass);

        decisionLayout.add(decisionIcon, decisionSpan);
        content.add(decisionLayout);

        // Reason
        Span reasonSpan = new Span(I18n.t("kms.policy.eval.reason", result.reason));
        reasonSpan.addClassName("policy-eval-reason-span");
        content.add(reasonSpan);

        // Sid and Principal
        if (result.sid != null && !result.sid.isEmpty()) {
            HorizontalLayout sidLayout = new HorizontalLayout();
            sidLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            sidLayout.setSpacing(true);
            Span sidLabel = new Span("🆔 " + I18n.t("kms.policy.eval.sid"));
            sidLabel.addClassName(LumoUtility.FontWeight.BOLD);
            Span sidValue = new Span(result.sid);
            sidLayout.add(sidLabel, sidValue);
            content.add(sidLayout);
        }

        if (result.principal != null && !result.principal.isEmpty()) {
            HorizontalLayout principalLayout = new HorizontalLayout();
            principalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            principalLayout.setSpacing(true);
            Span principalLabel = new Span("👤 " + I18n.t("kms.policy.eval.principal"));
            principalLabel.addClassName(LumoUtility.FontWeight.BOLD);
            Span principalValue = new Span(result.principal);
            principalLayout.add(principalLabel, principalValue);
            content.add(principalLayout);
        }

        // Key context
        if (selectedKeyId == null) {
            Span warningSpan = new Span(I18n.t("kms.policy.eval.no.key.selected"));
            warningSpan.addClassName("policy-eval-warning-span");
            content.add(warningSpan);
        } else {
            Span resourceSpan = new Span(I18n.t("kms.policy.eval.key.arn", keyArn));
            resourceSpan.addClassName("policy-eval-muted-span");
            content.add(resourceSpan);
        }

        // Matched statement (full JSON)
        if (result.matchedStatement != null && !result.matchedStatement.isEmpty()) {
            Span matchedHeader = new Span(I18n.t("kms.policy.eval.matched.statement"));
            matchedHeader.addClassName("policy-eval-matched-header");
            content.add(matchedHeader);

            Pre pre = new Pre();
            pre.setText(result.matchedStatement);
            pre.addClassName("policy-eval-matched-pre");
            pre.setWidthFull();
            content.add(pre);
        }

        // Help tip
        Span helpSpan = new Span(I18n.t("kms.policy.eval.tip"));
        helpSpan.addClassName("policy-eval-help-span");
        content.add(helpSpan);

        Button closeBtn = new Button(I18n.t("kms.policy.eval.close"), e -> resultDialog.close());
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