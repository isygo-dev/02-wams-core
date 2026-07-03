package eu.isygoit.ui.kms.views.cryptography.keyPolicy.dialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.KeyPolicy;
import eu.isygoit.enums.IKmsActionType;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.BaseActionDialog;

import java.util.*;
import java.util.function.Consumer;

public class PolicyStatementEditorDialog extends BaseActionDialog {

    private final ObjectMapper objectMapper;
    private final Consumer<KeyPolicy.Statement> onDone;
    private final String keyArnPlaceholder = "wrn:wams:kms:us-east-1:123456789012:key/";
    private final String aliasArnPlaceholder = "wrn:wams:kms:us-east-1:123456789012:alias/";

    // Form fields
    private final TextField sidField = new TextField(I18n.t("kms.policy.statement.field.sid"));
    private final ComboBox<String> effectCombo = new ComboBox<>(I18n.t("kms.policy.statement.field.effect"));
    private final TextField principalField = new TextField(I18n.t("kms.policy.statement.field.principal"));
    private final TextArea actionsArea = new TextArea(I18n.t("kms.policy.statement.field.actions"));
    private final TextArea resourcesArea = new TextArea(I18n.t("kms.policy.statement.field.resources"));
    private final TextArea conditionArea = new TextArea(I18n.t("kms.policy.statement.field.condition"));
    private final Span resourcesPreview = new Span();

    // Shortcut buttons
    private final HorizontalLayout actionShortcuts = new HorizontalLayout();
    private final HorizontalLayout resourceShortcuts = new HorizontalLayout();

    private final KeyPolicy.Statement statement;

    public PolicyStatementEditorDialog(ObjectMapper objectMapper, KeyPolicy.Statement existing, Consumer<KeyPolicy.Statement> onDone) {
        super(existing == null ? I18n.t("kms.policy.statement.dialog.add.title") : I18n.t("kms.policy.statement.dialog.edit.title"), null);
        this.objectMapper = objectMapper;
        this.onDone = onDone;
        this.statement = (existing != null) ? deepCopyStatement(existing) : createEmptyStatement();

        setOkButtonText(I18n.t("kms.policy.statement.dialog.save"));
        setWidth("850px");
        setMaxWidth("95%");
        setResizable(true);

        buildForm();
        bindData();
    }

    private KeyPolicy.Statement createEmptyStatement() {
        return KeyPolicy.Statement.builder()
                .sid(null)
                .effect("Allow")
                .principal("*")
                .action(Collections.singletonList("kms:*"))
                .resource("*")
                .condition(null)
                .build();
    }

    private KeyPolicy.Statement deepCopyStatement(KeyPolicy.Statement original) {
        try {
            String json = objectMapper.writeValueAsString(original);
            return objectMapper.readValue(json, KeyPolicy.Statement.class);
        } catch (Exception e) {
            return KeyPolicy.Statement.builder()
                    .sid(original.getSid())
                    .effect(original.getEffect())
                    .principal(original.getPrincipal())
                    .action(original.getAction())
                    .resource(original.getResource())
                    .condition(original.getCondition())
                    .build();
        }
    }

    private void buildForm() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.setWidthFull();

        sidField.setWidthFull();
        sidField.setHelperText(I18n.t("kms.policy.statement.field.sid.helper"));

        effectCombo.setItems("Allow", "Deny");
        effectCombo.setWidthFull();

        principalField.setWidthFull();
        principalField.setHelperText(I18n.t("kms.policy.statement.field.principal.helper"));

        // ---- Actions section with shortcuts ----
        actionsArea.setWidthFull();
        actionsArea.setHeight("120px");
        actionsArea.setHelperText(I18n.t("kms.policy.statement.field.actions.helper"));

        List<IKmsActionType.Types> importantActions = Arrays.asList(
                IKmsActionType.Types.ENCRYPT,
                IKmsActionType.Types.DECRYPT,
                IKmsActionType.Types.RE_ENCRYPT,
                IKmsActionType.Types.GENERATE_DATA_KEY,
                IKmsActionType.Types.GENERATE_DATA_KEY_WITHOUT_PLAINTEXT,
                IKmsActionType.Types.SIGN,
                IKmsActionType.Types.VERIFY,
                IKmsActionType.Types.GENERATE_MAC,
                IKmsActionType.Types.VERIFY_MAC,
                IKmsActionType.Types.DESCRIBE_KEY,
                IKmsActionType.Types.GET_PUBLIC_KEY
        );
        actionShortcuts.setWidthFull();
        actionShortcuts.setSpacing(true);
        actionShortcuts.addClassName("policy-statement-shortcuts");
        for (IKmsActionType.Types action : importantActions) {
            String actionMeaning = action.meaning();
            Button btn = new Button(actionMeaning);
            btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            btn.addClickListener(e -> addToActionsArea("kms:" + actionMeaning));
            actionShortcuts.add(btn);
        }
        Button allActionsBtn = new Button(I18n.t("kms.policy.statement.shortcut.all.actions"));
        allActionsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
        allActionsBtn.addClickListener(e -> actionsArea.setValue("kms:*"));
        actionShortcuts.add(allActionsBtn);

        // ---- Resources section with shortcuts ----
        resourcesArea.setWidthFull();
        resourcesArea.setHeight("120px");
        resourcesArea.setHelperText(I18n.t("kms.policy.statement.field.resources.helper"));

        resourceShortcuts.setWidthFull();
        resourceShortcuts.setSpacing(true);
        resourceShortcuts.addClassName("policy-statement-shortcuts");

        Button allResourcesBtn = new Button(I18n.t("kms.policy.statement.shortcut.all.resources"));
        allResourcesBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        allResourcesBtn.addClickListener(e -> addToResourcesArea("*"));
        resourceShortcuts.add(allResourcesBtn);

        Button keyArnBtn = new Button(I18n.t("kms.policy.statement.shortcut.key.wrn"));
        keyArnBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        keyArnBtn.addClickListener(e -> addToResourcesArea(keyArnPlaceholder + "<key-id>"));
        resourceShortcuts.add(keyArnBtn);

        Button aliasArnBtn = new Button(I18n.t("kms.policy.statement.shortcut.alias.wrn"));
        aliasArnBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        aliasArnBtn.addClickListener(e -> addToResourcesArea(aliasArnPlaceholder + "<alias-name>"));
        resourceShortcuts.add(aliasArnBtn);

        Button accountRootBtn = new Button(I18n.t("kms.policy.statement.shortcut.account.root"));
        accountRootBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        accountRootBtn.addClickListener(e -> addToResourcesArea("wrn:wams:iam::*:root"));
        resourceShortcuts.add(accountRootBtn);

        resourcesPreview.addClassName("policy-statement-resources-preview");
        resourcesPreview.setText(I18n.t("kms.policy.statement.preview.stored.as", "[\"*\"]"));
        resourcesArea.addValueChangeListener(e -> updatePreview());

        conditionArea.setWidthFull();
        conditionArea.setHeight("120px");
        conditionArea.setHelperText(I18n.t("kms.policy.statement.field.condition.helper"));

        layout.add(sidField, effectCombo, principalField,
                new Span(I18n.t("kms.policy.statement.field.actions")), actionsArea, actionShortcuts,
                new Span(I18n.t("kms.policy.statement.field.resources")), resourcesArea, resourceShortcuts, resourcesPreview,
                new Span(I18n.t("kms.policy.statement.field.condition")), conditionArea);
        add(layout);
    }

    private void addToActionsArea(String action) {
        String current = actionsArea.getValue();
        if (current == null || current.isBlank()) {
            actionsArea.setValue(action);
        } else if (!current.contains(action)) {
            actionsArea.setValue(current + "\n" + action);
        }
    }

    private void addToResourcesArea(String resource) {
        String current = resourcesArea.getValue();
        if (current == null || current.isBlank()) {
            resourcesArea.setValue(resource);
        } else if (!current.contains(resource)) {
            resourcesArea.setValue(current + "\n" + resource);
        }
    }

    private void updatePreview() {
        String text = resourcesArea.getValue();
        if (text == null || text.trim().isEmpty()) {
            resourcesPreview.setText(I18n.t("kms.policy.statement.preview.stored.as", "[\"*\"]"));
            return;
        }
        List<String> items = new ArrayList<>(parseMultilineOrComma(text));
        if (items.isEmpty()) items = new ArrayList<>(Collections.singletonList("*"));
        try {
            String json = objectMapper.writeValueAsString(items);
            resourcesPreview.setText(I18n.t("kms.policy.statement.preview.stored.as", json));
        } catch (JsonProcessingException e) {
            resourcesPreview.setText(I18n.t("kms.policy.statement.preview.invalid.format"));
        }
    }

    private void bindData() {
        sidField.setValue(statement.getSid() != null ? statement.getSid() : "");
        effectCombo.setValue(statement.getEffect());

        Object principal = statement.getPrincipal();
        if (principal instanceof String) {
            principalField.setValue((String) principal);
        } else if (principal instanceof Map) {
            try {
                principalField.setValue(objectMapper.writeValueAsString(principal));
            } catch (JsonProcessingException e) {
                principalField.setValue("{}");
            }
        } else {
            principalField.setValue("*");
        }

        Object action = statement.getAction();
        if (action instanceof String) {
            actionsArea.setValue((String) action);
        } else if (action instanceof List) {
            actionsArea.setValue(String.join("\n", (List<String>) action));
        } else {
            actionsArea.setValue("");
        }

        Object resource = statement.getResource();
        if (resource instanceof String) {
            resourcesArea.setValue((String) resource);
        } else if (resource instanceof List) {
            resourcesArea.setValue(String.join("\n", (List<String>) resource));
        } else {
            resourcesArea.setValue("");
        }
        updatePreview();

        if (statement.getCondition() != null) {
            try {
                conditionArea.setValue(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(statement.getCondition()));
            } catch (JsonProcessingException e) {
                conditionArea.setValue("");
            }
        } else {
            conditionArea.setValue("");
        }
    }

    private List<String> parseMultilineOrComma(String text) {
        if (text == null) return new ArrayList<>();
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return new ArrayList<>();
        if (trimmed.contains("\n")) {
            return Arrays.asList(trimmed.split("\\r?\\n"));
        } else if (trimmed.contains(",")) {
            return Arrays.asList(trimmed.split(","));
        } else {
            return Collections.singletonList(trimmed);
        }
    }

    @Override
    protected boolean onOk() {
        KeyPolicy.Statement.StatementBuilder builder = KeyPolicy.Statement.builder();

        String sid = sidField.getValue().trim();
        if (sid.isEmpty()) {
            sid = I18n.t("kms.policy.statement.sid.empty") + UUID.randomUUID();
        }
        builder.sid(sid);
        builder.effect(effectCombo.getValue());

        // Principal
        String principalStr = principalField.getValue().trim();
        if (principalStr.isEmpty()) principalStr = "*";
        if (principalStr.startsWith("{") && principalStr.endsWith("}")) {
            try {
                Map<String, Object> principalMap = objectMapper.readValue(principalStr, Map.class);
                builder.principal(principalMap);
            } catch (Exception e) {
                append(I18n.t("kms.policy.statement.invalid.principal.json", e.getMessage()));
                return false;
            }
        } else {
            builder.principal(principalStr);
        }

        // Actions
        String actionsText = actionsArea.getValue().trim();
        if (actionsText.isEmpty()) actionsText = I18n.t("kms.policy.statement.action.default");
        List<String> actions;
        if (actionsText.contains("\n")) {
            actions = new ArrayList<>(Arrays.asList(actionsText.split("\\r?\\n")));
        } else if (actionsText.contains(",")) {
            actions = new ArrayList<>(Arrays.asList(actionsText.split(",")));
        } else {
            actions = new ArrayList<>(Collections.singletonList(actionsText));
        }
        actions.removeIf(String::isBlank);
        if (actions.size() == 1) {
            builder.action(actions.get(0));
        } else {
            builder.action(actions);
        }

        // Resources
        String resourcesText = resourcesArea.getValue().trim();
        if (resourcesText.isEmpty()) resourcesText = I18n.t("kms.policy.statement.resource.default");
        List<String> resources = new ArrayList<>(parseMultilineOrComma(resourcesText));
        if (resources.isEmpty()) resources = new ArrayList<>(Collections.singletonList("*"));
        resources.replaceAll(String::trim);
        resources.removeIf(String::isEmpty);
        if (resources.size() == 1) {
            builder.resource(resources.get(0));
        } else {
            builder.resource(resources);
        }

        // Condition
        String conditionText = conditionArea.getValue().trim();
        if (!conditionText.isEmpty()) {
            try {
                Map<String, Map<String, String>> conditionMap = objectMapper.readValue(conditionText, Map.class);
                builder.condition(conditionMap);
            } catch (Exception e) {
                append(I18n.t("kms.policy.statement.invalid.condition.json", e.getMessage()));
                return false;
            }
        } else {
            builder.condition(null);
        }

        KeyPolicy.Statement newStatement = builder.build();
        if (onDone != null) {
            onDone.accept(newStatement);
        }
        return true;
    }
}