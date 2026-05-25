package eu.isygoit.ui.views.keyPolicy.dialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.KeyPolicy;
import eu.isygoit.enums.IKmsActionType;

import java.util.*;
import java.util.function.Consumer;

public class PolicyStatementEditorDialog extends Dialog {

    private final ObjectMapper objectMapper;
    private final Consumer<KeyPolicy.Statement> onDone;
    private final String keyArnPlaceholder = "wrn:wams:kms:us-east-1:123456789012:key/";
    private final String aliasArnPlaceholder = "wrn:wams:kms:us-east-1:123456789012:alias/";
    private final TextField sidField = new TextField("SID (Statement ID)");
    private final ComboBox<String> effectCombo = new ComboBox<>("Effect");
    private final TextField principalField = new TextField("Principal");
    private final TextArea actionsArea = new TextArea("Actions");
    private final TextArea resourcesArea = new TextArea("Resources");
    private final TextArea conditionArea = new TextArea("Condition (JSON)");
    private final Span resourcesPreview = new Span();
    // Shortcut buttons
    private final HorizontalLayout actionShortcuts = new HorizontalLayout();
    private final HorizontalLayout resourceShortcuts = new HorizontalLayout();
    private KeyPolicy.Statement statement;

    public PolicyStatementEditorDialog(ObjectMapper objectMapper, KeyPolicy.Statement existing, Consumer<KeyPolicy.Statement> onDone) {
        this.objectMapper = objectMapper;
        this.onDone = onDone;
        this.statement = (existing != null) ? deepCopyStatement(existing) : createEmptyStatement();

        setHeaderTitle(existing == null ? "Add Statement" : "Edit Statement");
        setWidth("850px");
        setResizable(true);
        setCloseOnEsc(true);

        buildForm();
        bindData();
        getFooter().add(createFooterButtons());
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
        sidField.setHelperText("Optional unique identifier (e.g., 'AllowKeyAdmin')");

        effectCombo.setItems("Allow", "Deny");
        effectCombo.setWidthFull();

        principalField.setWidthFull();
        principalField.setHelperText("Examples: '*' (everyone), 'wrn:wams:iam::123456789012:root', or JSON like {\"WAMS\": \"wrn:wams:iam::123456789012:role/MyRole\"}");

        // ---- Actions section with shortcuts ----
        actionsArea.setWidthFull();
        actionsArea.setHeight("120px");
        actionsArea.setHelperText("Enter actions separated by new lines or commas. Click shortcuts to add.");

        // Build action shortcut buttons from IKmsActionType.Types (most important ones)
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
        actionShortcuts.getStyle().set("flex-wrap", "wrap");
        for (IKmsActionType.Types action : importantActions) {
            String actionMeaning = action.meaning(); // e.g., "Encrypt"
            Button btn = new Button(actionMeaning);
            btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            btn.addClickListener(e -> addToActionsArea("kms:" + actionMeaning));
            actionShortcuts.add(btn);
        }
        // Add "All actions" button
        Button allActionsBtn = new Button("All (kms:*)");
        allActionsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
        allActionsBtn.addClickListener(e -> actionsArea.setValue("kms:*"));
        actionShortcuts.add(allActionsBtn);

        // ---- Resources section with shortcuts ----
        resourcesArea.setWidthFull();
        resourcesArea.setHeight("120px");
        resourcesArea.setHelperText("Enter resource ARNs one per line or separated by commas. Use '*' for all resources.");

        resourceShortcuts.setWidthFull();
        resourceShortcuts.setSpacing(true);
        resourceShortcuts.getStyle().set("flex-wrap", "wrap");

        Button allResourcesBtn = new Button("All resources (*)");
        allResourcesBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        allResourcesBtn.addClickListener(e -> addToResourcesArea("*"));
        resourceShortcuts.add(allResourcesBtn);

        Button keyArnBtn = new Button("Key WRN");
        keyArnBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        keyArnBtn.addClickListener(e -> addToResourcesArea(keyArnPlaceholder + "<key-id>"));
        resourceShortcuts.add(keyArnBtn);

        Button aliasArnBtn = new Button("Alias WRN");
        aliasArnBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        aliasArnBtn.addClickListener(e -> addToResourcesArea(aliasArnPlaceholder + "<alias-name>"));
        resourceShortcuts.add(aliasArnBtn);

        Button accountRootBtn = new Button("Account root");
        accountRootBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        accountRootBtn.addClickListener(e -> addToResourcesArea("wrn:wams:iam::*:root"));
        resourceShortcuts.add(accountRootBtn);

        // Preview of how resources will be stored
        resourcesPreview.getStyle().set("font-family", "monospace").set("font-size", "small");
        resourcesPreview.getStyle().set("color", "var(--lumo-secondary-text-color)");
        resourcesPreview.setText("→ Will be stored as: [\"*\"]");
        resourcesArea.addValueChangeListener(e -> updatePreview());

        // Condition
        conditionArea.setWidthFull();
        conditionArea.setHeight("120px");
        conditionArea.setHelperText("Optional JSON condition. Example:\n{\n  \"Bool\": {\n    \"wams:MultiFactorAuthPresent\": \"true\"\n  }\n}");

        // Assemble layout
        layout.add(sidField, effectCombo, principalField,
                new Span("Actions"), actionsArea, actionShortcuts,
                new Span("Resources"), resourcesArea, resourceShortcuts, resourcesPreview,
                new Span("Condition"), conditionArea);
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
            resourcesPreview.setText("→ Will be stored as: [\"*\"]");
            return;
        }
        List<String> items = new ArrayList<>(parseMultilineOrComma(text));
        if (items.isEmpty()) items = new ArrayList<>(Collections.singletonList("*"));
        try {
            String json = objectMapper.writeValueAsString(items);
            resourcesPreview.setText("→ Will be stored as: " + json);
        } catch (JsonProcessingException e) {
            resourcesPreview.setText("→ Invalid format");
        }
    }

    private void bindData() {
        sidField.setValue(statement.getSid() != null ? statement.getSid() : "");
        effectCombo.setValue(statement.getEffect());

        // Principal
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

        // Actions
        Object action = statement.getAction();
        if (action instanceof String) {
            actionsArea.setValue((String) action);
        } else if (action instanceof List) {
            actionsArea.setValue(String.join("\n", (List<String>) action));
        } else {
            actionsArea.setValue("");
        }

        // Resources
        Object resource = statement.getResource();
        if (resource instanceof String) {
            resourcesArea.setValue((String) resource);
        } else if (resource instanceof List) {
            resourcesArea.setValue(String.join("\n", (List<String>) resource));
        } else {
            resourcesArea.setValue("");
        }
        updatePreview();

        // Condition
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

    private HorizontalLayout createFooterButtons() {
        Button saveBtn = new Button("Save Statement", e -> save());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> close());
        return new HorizontalLayout(cancelBtn, saveBtn);
    }

    private void save() {
        KeyPolicy.Statement.StatementBuilder builder = KeyPolicy.Statement.builder();

        String sid = sidField.getValue().trim();
        builder.sid(sid.isEmpty() ? null : sid);
        builder.effect(effectCombo.getValue());

        // Principal
        String principalStr = principalField.getValue().trim();
        if (principalStr.isEmpty()) principalStr = "*";
        if (principalStr.startsWith("{") && principalStr.endsWith("}")) {
            try {
                Map<String, Object> principalMap = objectMapper.readValue(principalStr, Map.class);
                builder.principal(principalMap);
            } catch (Exception e) {
                showError("Invalid Principal JSON: " + e.getMessage());
                return;
            }
        } else {
            builder.principal(principalStr);
        }

        // Actions
        String actionsText = actionsArea.getValue().trim();
        if (actionsText.isEmpty()) actionsText = "kms:*";
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
        if (resourcesText.isEmpty()) resourcesText = "*";
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
                showError("Invalid Condition JSON: " + e.getMessage());
                return;
            }
        } else {
            builder.condition(null);
        }

        onDone.accept(builder.build());
        close();
    }

    private void showError(String msg) {
        Notification.show(msg, 5000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}