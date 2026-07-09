package eu.isygoit.ui.kms.views.tokenizer.builder.dialog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lets a user compose custom JWT claims as key/value rows.
 *
 * <p>Extends {@link BaseActionDialog} (not {@code NoActionDialog}) because it
 * has real action semantics — Apply commits the claims back to the caller via
 * {@link ClaimsCallback}, Cancel discards — so it shares the same Ok/Cancel +
 * error-span footer contract as every other action dialog in the app instead
 * of building its own ad-hoc button bar in the content area.
 */
public class ClaimsBuilderDialog extends BaseActionDialog {

    private final ObjectMapper objectMapper;
    private final String existingClaimsJson;
    private final ClaimsCallback callback;

    private final VerticalLayout rowsContainer = new VerticalLayout();
    private final List<ClaimsRow> rows = new ArrayList<>();
    private Span validationHint;
    private Span claimCounter;

    public ClaimsBuilderDialog(ObjectMapper objectMapper, String existingClaimsJson, ClaimsCallback callback) {
        super(I18n.t("kms.claims.builder.title"));
        this.objectMapper = objectMapper;
        this.existingClaimsJson = existingClaimsJson;
        this.callback = callback;

        addClassName("claims-builder-dialog");
        setWidth("750px");
        setMaxWidth("95%");
        setResizable(true);
        setDraggable(true);
        setOkButtonText(I18n.t("kms.claims.builder.apply.close"));

        buildUI();
    }

    private void buildUI() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setPadding(true);
        mainLayout.setWidthFull();

        // Header
        Span titleHint = new Span(I18n.t("kms.claims.builder.hint"));
        titleHint.addClassName(LumoUtility.TextColor.SECONDARY);
        titleHint.addClassName(LumoUtility.FontSize.SMALL);
        mainLayout.add(titleHint);

        // Row container
        rowsContainer.setSpacing(true);
        rowsContainer.setPadding(false);
        rowsContainer.setWidthFull();

        // Claim counter
        claimCounter = new Span();
        claimCounter.addClassName(LumoUtility.FontSize.XSMALL);
        claimCounter.addClassName(LumoUtility.TextColor.TERTIARY);
        claimCounter.addClassName("claim-counter");

        // Validation hint
        validationHint = new Span();
        validationHint.addClassName(LumoUtility.FontSize.XSMALL);
        validationHint.addClassName(LumoUtility.TextColor.ERROR);
        validationHint.setVisible(false);

        // Load existing claims
        loadExistingClaims();

        // Add row button
        Button addRowButton = new Button(new Icon(VaadinIcon.PLUS));
        addRowButton.setText(I18n.t("kms.claims.builder.add.claim"));
        addRowButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        addRowButton.addClickListener(e -> addNewRow());

        HorizontalLayout headerBar = new HorizontalLayout(claimCounter, addRowButton);
        headerBar.setWidthFull();
        headerBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerBar.setAlignItems(FlexComponent.Alignment.CENTER);
        headerBar.addClassName("header-bar");

        mainLayout.add(headerBar, rowsContainer, validationHint);
        addContent(mainLayout);
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
                    row.setKeyError(I18n.t("kms.claims.builder.duplicate.claim"));
                    keyMap.get(key).setKeyError(I18n.t("kms.claims.builder.duplicate.claim"));
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
                        row.setValueError(I18n.t("kms.claims.builder.invalid.json"));
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
            validationHint.setText("⚠️ " + I18n.t("kms.claims.builder.validation.error"));
            validationHint.setVisible(true);
        } else {
            validationHint.setVisible(false);
        }
    }

    private void updateCounter() {
        int nonEmpty = (int) rows.stream().filter(r -> StringUtils.hasText(r.getKey())).count();
        String counterText = nonEmpty == 1 ?
                I18n.t("kms.claims.builder.claim.count", nonEmpty) :
                I18n.t("kms.claims.builder.claim.count.plural", nonEmpty);
        claimCounter.setText(counterText);
    }

    @Override
    protected boolean onOk() {
        validateAll();
        if (validationHint.isVisible()) {
            append(I18n.t("kms.claims.builder.fix.errors"));
            return false;
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
            append(I18n.t("kms.claims.builder.claims.applied"));
            return true;
        } catch (Exception ex) {
            append(I18n.t("kms.claims.builder.json.failed", ex.getMessage()));
            return false;
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
            layout.addClassName("claim-row");

            // Key field (30%)
            keyField = new TextField();
            keyField.setPlaceholder(I18n.t("kms.claims.builder.claim.name.placeholder"));
            keyField.setValue(initialKey);
            keyField.setWidthFull();
            keyField.addValueChangeListener(e -> onUpdate.run());
            keyField.setTooltipText(I18n.t("kms.claims.builder.claim.name.tooltip"));

            // Value area (60%)
            valueArea = new TextArea();
            valueArea.setPlaceholder(I18n.t("kms.claims.builder.claim.value.placeholder"));
            valueArea.setValue(initialValue);
            valueArea.setHeight("60px");
            valueArea.setWidthFull();
            valueArea.addValueChangeListener(e -> onUpdate.run());
            valueArea.setTooltipText(I18n.t("kms.claims.builder.claim.value.tooltip"));

            // Remove button (10%)
            Button removeButton = new Button(new Icon(VaadinIcon.TRASH));
            removeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            removeButton.addClickListener(e -> {
                container.remove(layout);
                allRows.remove(this);
                onUpdate.run();
            });
            removeButton.setTooltipText(I18n.t("kms.claims.builder.remove.claim"));

            // Error labels
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