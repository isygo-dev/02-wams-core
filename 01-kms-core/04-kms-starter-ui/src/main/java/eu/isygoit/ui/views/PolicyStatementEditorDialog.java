package eu.isygoit.ui.views;

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

import java.util.*;
import java.util.function.Consumer;

public class PolicyStatementEditorDialog extends Dialog {

    private final ObjectMapper objectMapper;
    private final Consumer<KeyPolicy.Statement> onDone;
    private KeyPolicy.Statement statement;
    private final String keyArnPlaceholder = "arn:aws:kms:us-east-1:123456789012:key/";

    private final TextField sidField = new TextField("SID (Statement ID)");
    private final ComboBox<String> effectCombo = new ComboBox<>("Effect");
    private final TextField principalField = new TextField("Principal");
    private final TextArea actionsArea = new TextArea("Actions");
    private final TextArea resourcesArea = new TextArea("Resources");
    private final TextArea conditionArea = new TextArea("Condition (JSON)");
    private final Span resourcesPreview = new Span();

    public PolicyStatementEditorDialog(ObjectMapper objectMapper, KeyPolicy.Statement existing, Consumer<KeyPolicy.Statement> onDone) {
        this.objectMapper = objectMapper;
        this.onDone = onDone;
        this.statement = (existing != null) ? deepCopyStatement(existing) : createEmptyStatement();

        setHeaderTitle(existing == null ? "Add Statement" : "Edit Statement");
        setWidth("800px");
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

        // All fields set width full
        sidField.setWidthFull();
        sidField.setHelperText("Optional unique identifier (e.g., 'AllowKeyAdmin')");

        effectCombo.setItems("Allow", "Deny");
        effectCombo.setWidthFull();

        principalField.setWidthFull();
        principalField.setHelperText("Examples: '*' (everyone), 'arn:aws:iam::123456789012:root', or JSON like {\"AWS\": \"arn:aws:iam::123456789012:role/MyRole\"}");

        actionsArea.setWidthFull();
        actionsArea.setHeight("120px");
        actionsArea.setHelperText("Enter actions separated by new lines or commas. Examples:\nkms:Encrypt\nkms:Decrypt\nkms:*");

        // Quick add buttons for actions
        HorizontalLayout actionQuick = new HorizontalLayout();
        actionQuick.setWidthFull();
        Button addAllActions = new Button("All (kms:*)", e -> actionsArea.setValue("kms:*"));
        Button addEncryptDecrypt = new Button("Encrypt/Decrypt", e -> actionsArea.setValue("kms:Encrypt\nkms:Decrypt\nkms:GenerateDataKey"));
        addAllActions.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        addEncryptDecrypt.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        actionQuick.add(addAllActions, addEncryptDecrypt);
        actionQuick.setSpacing(true);

        resourcesArea.setWidthFull();
        resourcesArea.setHeight("120px");
        resourcesArea.setHelperText("Enter resource ARNs one per line or separated by commas. Use '*' for all resources under this key.");

        // Quick add buttons for resources
        HorizontalLayout resourceQuick = new HorizontalLayout();
        resourceQuick.setWidthFull();
        Button addAllResources = new Button("All resources (*)", e -> resourcesArea.setValue("*"));
        Button addKeyArn = new Button("Key ARN placeholder", e -> {
            String current = resourcesArea.getValue();
            String arn = keyArnPlaceholder + "<key-id>";
            resourcesArea.setValue(current.isEmpty() ? arn : current + "\n" + arn);
        });
        addAllResources.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        addKeyArn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        resourceQuick.add(addAllResources, addKeyArn);
        resourceQuick.setSpacing(true);

        // Preview of how resources will be stored
        resourcesPreview.getStyle().set("font-family", "monospace").set("font-size", "small");
        resourcesPreview.getStyle().set("color", "var(--lumo-secondary-text-color)");
        resourcesPreview.setText("→ Will be stored as: [\"*\"]");
        resourcesArea.addValueChangeListener(e -> updatePreview());

        conditionArea.setWidthFull();
        conditionArea.setHeight("120px");
        conditionArea.setHelperText("Optional JSON condition. Example:\n{\n  \"Bool\": {\n    \"aws:MultiFactorAuthPresent\": \"true\"\n  }\n}");

        // Add all components
        layout.add(sidField, effectCombo, principalField,
                new Span("Actions"), actionsArea, actionQuick,
                new Span("Resources"), resourcesArea, resourceQuick, resourcesPreview,
                new Span("Condition"), conditionArea);
        add(layout);
    }

    private void updatePreview() {
        String text = resourcesArea.getValue();
        if (text == null || text.trim().isEmpty()) {
            resourcesPreview.setText("→ Will be stored as: [\"*\"]");
            return;
        }
        List<String> items = parseMultilineOrComma(text);
        if (items.isEmpty()) items = Collections.singletonList("*");
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
        if (actionsText.contains("\n")) {
            List<String> actions = Arrays.asList(actionsText.split("\\r?\\n"));
            actions.removeIf(String::isBlank);
            builder.action(actions);
        } else if (actionsText.contains(",")) {
            List<String> actions = Arrays.asList(actionsText.split(","));
            actions.replaceAll(String::trim);
            actions.removeIf(String::isEmpty);
            builder.action(actions);
        } else {
            builder.action(actionsText);
        }

        // Resources
        String resourcesText = resourcesArea.getValue().trim();
        if (resourcesText.isEmpty()) resourcesText = "*";
        List<String> resources = parseMultilineOrComma(resourcesText);
        if (resources.isEmpty()) resources = Collections.singletonList("*");
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