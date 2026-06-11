package eu.isygoit.ui.views.kms.tokenizer.builder.dialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.ui.views.common.dialog.NoActionDialog;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClaimsBuilderDialog extends NoActionDialog {

    private final ObjectMapper objectMapper;
    private final String existingClaimsJson;
    private final ClaimsCallback callback;

    private final VerticalLayout rowsContainer = new VerticalLayout();
    private final List<ClaimsRow> rows = new ArrayList<>();
    private Span validationHint;
    private Span claimCounter;

    public ClaimsBuilderDialog(ObjectMapper objectMapper, String existingClaimsJson, ClaimsCallback callback) {
        super("Build Custom Claims");
        this.objectMapper = objectMapper;
        this.existingClaimsJson = existingClaimsJson;
        this.callback = callback;

        setWidth("750px");
        setResizable(true);
        setDraggable(true);

        buildUI();
    }

    private void buildUI() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setPadding(true);
        mainLayout.setMargin(false);

        // Header
        Span titleHint = new Span("Add extra claims to the JWT payload (standard claims like iss, sub, exp are generated automatically).");
        titleHint.addClassName(LumoUtility.TextColor.SECONDARY);
        titleHint.addClassName(LumoUtility.FontSize.SMALL);
        mainLayout.add(titleHint);

        // Row container
        rowsContainer.setSpacing(true);
        rowsContainer.setPadding(false);
        rowsContainer.setWidthFull();

        // Claim counter (compact)
        claimCounter = new Span();
        claimCounter.addClassName(LumoUtility.FontSize.XSMALL);
        claimCounter.addClassName(LumoUtility.TextColor.TERTIARY);
        claimCounter.getStyle().set("margin-top", "0");

        // Validation hint (init later but before loading)
        validationHint = new Span();
        validationHint.addClassName(LumoUtility.FontSize.XSMALL);
        validationHint.addClassName(LumoUtility.TextColor.ERROR);
        validationHint.setVisible(false);

        // Load existing claims (creates rows)
        loadExistingClaims();

        // Add row button
        Button addRowButton = new Button(new Icon(VaadinIcon.PLUS));
        addRowButton.setText("Add claim");
        addRowButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        addRowButton.addClickListener(e -> addNewRow());

        HorizontalLayout headerBar = new HorizontalLayout(claimCounter, addRowButton);
        headerBar.setWidthFull();
        headerBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerBar.setAlignItems(FlexComponent.Alignment.CENTER);
        headerBar.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        // Action buttons
        Button applyButton = new Button("Apply & Close", new Icon(VaadinIcon.CHECK));
        applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyButton.addClickListener(e -> applyClaims());

        Button cancelButton = new Button("Cancel", e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttonBar = new HorizontalLayout(applyButton, cancelButton);
        buttonBar.setWidthFull();
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.setSpacing(true);

        mainLayout.add(headerBar, rowsContainer, validationHint, buttonBar);
        add(mainLayout);
    }

    private void loadExistingClaims() {
        rows.clear();
        rowsContainer.removeAll();

        if (StringUtils.hasText(existingClaimsJson)) {
            try {
                JsonNode existingJson = objectMapper.readTree(existingClaimsJson);
                if (existingJson.isObject()) {
                    existingJson.fields().forEachRemaining(entry -> {
                        String valueStr = entry.getValue().isTextual() ? entry.getValue().asText() : entry.getValue().toString();
                        addNewRow(entry.getKey(), valueStr);
                    });
                }
            } catch (Exception ignored) {
                // start empty
            }
        }

        if (rows.isEmpty()) {
            addNewRow("", "");
        }
        updateCounter();
    }

    private void addNewRow() {
        addNewRow("", "");
    }

    private void addNewRow(String key, String value) {
        ClaimsRow row = new ClaimsRow(rowsContainer, rows, key, value, this::validateAndUpdate);
        rows.add(row);
        rowsContainer.add(row.getLayout());
        validateAndUpdate();
    }

    private void validateAndUpdate() {
        validateAll();
        updateCounter();
    }

    private void validateAll() {
        boolean hasErrors = false;
        Map<String, ClaimsRow> keyMap = new LinkedHashMap<>();

        for (ClaimsRow row : rows) {
            String key = row.getKey();
            if (StringUtils.hasText(key)) {
                if (keyMap.containsKey(key)) {
                    row.setKeyError("Duplicate claim name");
                    keyMap.get(key).setKeyError("Duplicate claim name");
                    hasErrors = true;
                } else {
                    keyMap.put(key, row);
                    row.setKeyError(null);
                }
            } else {
                row.setKeyError(null);
            }

            String value = row.getValue();
            if (StringUtils.hasText(value)) {
                String trimmed = value.trim();
                if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                        (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                    try {
                        objectMapper.readTree(trimmed);
                        row.setValueError(null);
                    } catch (JsonProcessingException e) {
                        row.setValueError("Invalid JSON syntax");
                        hasErrors = true;
                    }
                } else {
                    row.setValueError(null);
                }
            } else {
                row.setValueError(null);
            }
        }

        if (hasErrors) {
            validationHint.setText("⚠️ Please fix the errors above before applying.");
            validationHint.setVisible(true);
        } else {
            validationHint.setVisible(false);
        }
    }

    private void updateCounter() {
        int nonEmpty = (int) rows.stream().filter(r -> StringUtils.hasText(r.getKey())).count();
        claimCounter.setText(nonEmpty + " custom claim" + (nonEmpty == 1 ? "" : "s"));
    }

    private void applyClaims() {
        validateAll();
        if (validationHint.isVisible()) {
            Notification.show("Please fix validation errors first", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        Map<String, Object> claimsMap = new LinkedHashMap<>();
        for (ClaimsRow row : rows) {
            String key = row.getKey();
            String value = row.getValue();
            if (StringUtils.hasText(key)) {
                try {
                    claimsMap.put(key, objectMapper.readTree(value));
                } catch (Exception ex) {
                    claimsMap.put(key, value);
                }
            }
        }

        try {
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(claimsMap);
            callback.onClaimsBuilt(prettyJson);
            close();
            Notification.show("Claims applied", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show("Failed to generate JSON: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    public interface ClaimsCallback {
        void onClaimsBuilt(String prettyJson);
    }

    private static class ClaimsRow {
        private final HorizontalLayout layout;
        private final TextField keyField;
        private final TextArea valueArea;
        private final Span keyErrorLabel;
        private final Span valueErrorLabel;
        private final Runnable onUpdate;

        public ClaimsRow(VerticalLayout container, List<ClaimsRow> allRows, String initialKey, String initialValue, Runnable onUpdate) {
            this.onUpdate = onUpdate;

            layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.START);
            layout.setSpacing(true);
            layout.setWidthFull();
            layout.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
            layout.getStyle().set("padding-bottom", "var(--lumo-space-xs)");
            layout.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

            // Key field (30%)
            keyField = new TextField();
            keyField.setPlaceholder("Claim name");
            keyField.setValue(initialKey);
            keyField.setWidthFull();
            keyField.addValueChangeListener(e -> onUpdate.run());
            keyField.setTooltipText("Unique claim name (e.g., role, department).");

            // Value area (60%)
            valueArea = new TextArea();
            valueArea.setPlaceholder("Value – string, number, true/false, JSON object/array");
            valueArea.setValue(initialValue);
            valueArea.setHeight("60px");
            valueArea.setWidthFull();
            valueArea.addValueChangeListener(e -> onUpdate.run());
            valueArea.setTooltipText("Any JSON value. Use quotes for strings, no quotes for numbers/booleans, or a JSON object/array.");

            // Remove button (10%)
            Button removeButton = new Button(new Icon(VaadinIcon.TRASH));
            removeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            removeButton.addClickListener(e -> {
                container.remove(layout);
                allRows.remove(this);
                onUpdate.run();
            });
            removeButton.setTooltipText("Remove this claim");

            // Error labels (compact, only appear when needed)
            keyErrorLabel = new Span();
            keyErrorLabel.addClassName(LumoUtility.FontSize.XSMALL);
            keyErrorLabel.addClassName(LumoUtility.TextColor.ERROR);
            keyErrorLabel.setVisible(false);
            VerticalLayout keyWrapper = new VerticalLayout(keyField, keyErrorLabel);
            keyWrapper.setPadding(false);
            keyWrapper.setSpacing(false);
            keyWrapper.setWidth("30%");

            valueErrorLabel = new Span();
            valueErrorLabel.addClassName(LumoUtility.FontSize.XSMALL);
            valueErrorLabel.addClassName(LumoUtility.TextColor.ERROR);
            valueErrorLabel.setVisible(false);
            VerticalLayout valueWrapper = new VerticalLayout(valueArea, valueErrorLabel);
            valueWrapper.setPadding(false);
            valueWrapper.setSpacing(false);
            valueWrapper.setWidth("60%");

            layout.add(keyWrapper, valueWrapper, removeButton);
        }

        public HorizontalLayout getLayout() {
            return layout;
        }

        public String getKey() {
            return keyField.getValue();
        }

        public String getValue() {
            return valueArea.getValue();
        }

        public void setKeyError(String error) {
            if (error == null) {
                keyErrorLabel.setVisible(false);
                keyErrorLabel.setText("");
            } else {
                keyErrorLabel.setText(error);
                keyErrorLabel.setVisible(true);
            }
        }

        public void setValueError(String error) {
            if (error == null) {
                valueErrorLabel.setVisible(false);
                valueErrorLabel.setText("");
            } else {
                valueErrorLabel.setText(error);
                valueErrorLabel.setVisible(true);
            }
        }
    }
}