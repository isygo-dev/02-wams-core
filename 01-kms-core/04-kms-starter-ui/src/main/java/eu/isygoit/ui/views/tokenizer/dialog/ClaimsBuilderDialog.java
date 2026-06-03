package eu.isygoit.ui.views.tokenizer.dialog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.ui.views.NoActionDialog;
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

    public ClaimsBuilderDialog(ObjectMapper objectMapper, String existingClaimsJson, ClaimsCallback callback) {
        super("Build Custom Claims");
        this.objectMapper = objectMapper;
        this.existingClaimsJson = existingClaimsJson;
        this.callback = callback;

        setWidth("700px");
        setResizable(true);

        buildUI();
    }

    private void buildUI() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        rowsContainer.setSpacing(true);
        rowsContainer.setPadding(false);

        // Load existing claims if any
        if (StringUtils.hasText(existingClaimsJson)) {
            try {
                JsonNode existingJson = objectMapper.readTree(existingClaimsJson);
                if (existingJson.isObject()) {
                    existingJson.fields().forEachRemaining(entry -> {
                        String valueStr = entry.getValue().isTextual() ? entry.getValue().asText() : entry.getValue().toString();
                        rows.add(new ClaimsRow(rowsContainer, entry.getKey(), valueStr));
                    });
                }
            } catch (Exception ignored) {
            }
        }
        if (rows.isEmpty()) {
            rows.add(new ClaimsRow(rowsContainer, "", ""));
        }

        Button addRowButton = new Button("Add Claim", new Icon(VaadinIcon.PLUS));
        addRowButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addRowButton.addClickListener(e -> rows.add(new ClaimsRow(rowsContainer, "", "")));

        Button applyButton = new Button("Apply to Claims", new Icon(VaadinIcon.CHECK));
        applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyButton.addClickListener(e -> applyClaims());

        Button cancelButton = new Button("Cancel", e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        content.add(rowsContainer, addRowButton, new HorizontalLayout(applyButton, cancelButton));
        add(content);
    }

    private void applyClaims() {
        Map<String, Object> claimsMap = new LinkedHashMap<>();
        for (ClaimsRow row : rows) {
            String key = row.getKey();
            String value = row.getValue();
            if (StringUtils.hasText(key)) {
                try {
                    JsonNode node = objectMapper.readTree(value);
                    claimsMap.put(key, node);
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

        public ClaimsRow(VerticalLayout container, String initialKey, String initialValue) {
            layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.START);
            layout.setSpacing(true);
            layout.setWidthFull();

            keyField = new TextField();
            keyField.setPlaceholder("Claim name (e.g., role)");
            keyField.setValue(initialKey);
            keyField.setWidth("30%");

            valueArea = new TextArea();
            valueArea.setPlaceholder("Value (string, number, JSON object/array)");
            valueArea.setValue(initialValue);
            valueArea.setHeight("70px");
            valueArea.setWidth("60%");

            Button removeButton = new Button(new Icon(VaadinIcon.TRASH));
            removeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            removeButton.addClickListener(e -> container.remove(layout));
            removeButton.setWidth("10%");

            layout.add(keyField, valueArea, removeButton);
            container.add(layout);
        }

        public String getKey() {
            return keyField.getValue();
        }

        public String getValue() {
            return valueArea.getValue();
        }
    }
}