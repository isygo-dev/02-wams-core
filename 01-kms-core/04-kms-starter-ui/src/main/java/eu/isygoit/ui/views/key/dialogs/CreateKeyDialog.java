package eu.isygoit.ui.views.key.dialogs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import eu.isygoit.dto.KmsDtos.CreateKeyRequest;
import eu.isygoit.dto.KmsDtos.CreateKeyResponse;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dialog for creating a new KMS key.
 * Encapsulates all UI fields and creation logic.
 */
public class CreateKeyDialog extends Dialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;
    private final ObjectMapper objectMapper;

    // Form fields
    private ComboBox<String> aliasCombo;
    private TextField newAliasField;
    private TextArea descriptionField;
    private ComboBox<IEnumKeySpec.Types> keySpecCombo;
    private ComboBox<IEnumKeyUsage.Types> keyUsageCombo;
    private ComboBox<IEnumKeyOrigin.Types> originCombo;
    private Checkbox multiRegionCheckbox;
    private TextField primaryRegionField;
    private TextField replicaRegionsField;
    private Checkbox bypassPolicyCheckbox;
    private TextArea policyField;
    private VerticalLayout tagsContainer;
    private List<HorizontalLayout> tagRows;
    private HorizontalLayout tagsHeader;  // <-- store header with label + button
    private Span errorSpan;

    public CreateKeyDialog(KeyManagementView parentView, KmsApiService kmsApiService, ObjectMapper objectMapper) {
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;

        setHeaderTitle("Create new KMS key");
        setWidth("700px");
        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);

        buildForm();
        buildFooter();

        add(createFormLayout());
    }

    private void buildForm() {
        // Alias section (unchanged)
        aliasCombo = new ComboBox<>("Alias (optional)");
        aliasCombo.setItems(parentView.existingAliases);
        aliasCombo.setPlaceholder("Select existing or type new");
        aliasCombo.setAllowCustomValue(true);
        newAliasField = new TextField("New alias name");
        newAliasField.setVisible(false);
        newAliasField.setPlaceholder("alias/my-new-alias");
        aliasCombo.addCustomValueSetListener(e -> {
            newAliasField.setVisible(true);
            newAliasField.setValue(e.getDetail());
        });
        aliasCombo.addValueChangeListener(e -> {
            if (e.getValue() != null && !e.getValue().isEmpty() && !parentView.existingAliases.contains(e.getValue())) {
                newAliasField.setVisible(true);
                newAliasField.setValue(e.getValue());
            } else {
                newAliasField.setVisible(false);
                newAliasField.clear();
            }
        });

        descriptionField = new TextArea("Description");
        descriptionField.setMaxLength(500);

        keySpecCombo = new ComboBox<>("Key specification");
        keySpecCombo.setItems(IEnumKeySpec.Types.values());
        keySpecCombo.setValue(IEnumKeySpec.Types.SYMMETRIC_DEFAULT);
        keySpecCombo.setRequiredIndicatorVisible(true);

        keyUsageCombo = new ComboBox<>("Key usage");
        keyUsageCombo.setItems(IEnumKeyUsage.Types.values());
        keyUsageCombo.setValue(IEnumKeyUsage.Types.ENCRYPT_DECRYPT);
        keyUsageCombo.setRequiredIndicatorVisible(true);

        originCombo = new ComboBox<>("Origin");
        originCombo.setItems(IEnumKeyOrigin.Types.values());
        originCombo.setValue(IEnumKeyOrigin.Types.WAMS_KMS);
        originCombo.setRequiredIndicatorVisible(true);

        // Multi‑region fields
        multiRegionCheckbox = new Checkbox("Multi-region key");
        primaryRegionField = new TextField("Primary region");
        primaryRegionField.setPlaceholder("e.g., us-east-1");
        primaryRegionField.setValue("us-east-1");
        primaryRegionField.setVisible(false);
        replicaRegionsField = new TextField("Replica regions (comma‑separated)");
        replicaRegionsField.setPlaceholder("e.g., eu-west-1,ap-southeast-1");
        replicaRegionsField.setVisible(false);
        multiRegionCheckbox.addValueChangeListener(e -> {
            boolean visible = e.getValue();
            primaryRegionField.setVisible(visible);
            replicaRegionsField.setVisible(visible);
            if (!visible) {
                primaryRegionField.clear();
                replicaRegionsField.clear();
            }
        });

        bypassPolicyCheckbox = new Checkbox("Bypass policy lockout safety check");
        policyField = new TextArea("Policy (JSON)");
        policyField.setPlaceholder("{\n  \"Version\": \"2012-10-17\",\n  \"Statement\": [...]\n}");
        policyField.setWidthFull();
        policyField.setHeight("150px");

        // Tag editor
        tagsContainer = new VerticalLayout();
        tagsContainer.setSpacing(true);
        tagsContainer.setPadding(false);
        tagRows = new ArrayList<>();

        // Create the header with label and add button
        Button addTagButton = new Button("Add tag", new Icon(VaadinIcon.PLUS));
        addTagButton.addClickListener(e -> addTagRow(null, null));
        tagsHeader = new HorizontalLayout(new Span("Tags (random key + value)"), addTagButton);
        tagsHeader.setAlignItems(FlexComponent.Alignment.BASELINE);
        tagsHeader.setSpacing(true);
    }

    private void addTagRow(String existingKey, String existingValue) {
        String randomKey = (existingKey != null) ? existingKey : "tag-" + UUID.randomUUID().toString().substring(0, 8);
        TextField keyField = new TextField();
        keyField.setValue(randomKey);
        keyField.setReadOnly(true);
        keyField.setWidth("150px");
        TextField valueField = new TextField();
        valueField.setValue(existingValue != null ? existingValue : "");
        valueField.setPlaceholder("Tag value");
        valueField.setWidth("250px");
        Button removeBtn = new Button(new Icon(VaadinIcon.TRASH));
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        HorizontalLayout row = new HorizontalLayout(keyField, valueField, removeBtn);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        tagRows.add(row);
        tagsContainer.add(row);
        removeBtn.addClickListener(e -> {
            tagsContainer.remove(row);
            tagRows.remove(row);
        });
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.add(aliasCombo, newAliasField, descriptionField,
                keySpecCombo, keyUsageCombo, originCombo,
                multiRegionCheckbox, primaryRegionField, replicaRegionsField,
                bypassPolicyCheckbox, policyField,
                tagsHeader, tagsContainer);   // <-- add both header and container
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }

    private void buildFooter() {
        errorSpan = new Span();
        errorSpan.getStyle().set("color", "var(--lumo-error-text-color)");
        errorSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        errorSpan.getStyle().set("margin-right", "auto");
        errorSpan.setVisible(false);

        Button createBtn = new Button("Create", e -> onCreate());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> close());

        HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setWidthFull();
        footerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        footerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        footerLayout.add(errorSpan);
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelBtn, createBtn);
        buttonLayout.setSpacing(true);
        footerLayout.add(buttonLayout);

        getFooter().removeAll();
        getFooter().add(footerLayout);
    }

    private void onCreate() {
        errorSpan.setText("");
        errorSpan.setVisible(false);

        String newAlias = null;
        String existingSelectedAlias = null;
        if (newAliasField.isVisible() && !newAliasField.getValue().isBlank()) {
            newAlias = newAliasField.getValue();
        } else if (aliasCombo.getValue() != null && !aliasCombo.getValue().isBlank()) {
            existingSelectedAlias = aliasCombo.getValue();
        }

        List<CreateKeyRequest.Tag> tags = new ArrayList<>();
        for (HorizontalLayout row : tagRows) {
            TextField keyField = (TextField) row.getComponentAt(0);
            TextField valueField = (TextField) row.getComponentAt(1);
            if (!valueField.getValue().isBlank()) {
                tags.add(CreateKeyRequest.Tag.builder()
                        .tagKey(keyField.getValue())
                        .tagValue(valueField.getValue())
                        .build());
            }
        }

        Map<String, Object> policyMap = null;
        if (!policyField.getValue().isBlank()) {
            try {
                policyMap = objectMapper.readValue(policyField.getValue(), new TypeReference<>() {});
            } catch (Exception ex) {
                String errorMsg = "Invalid JSON in policy field: " + ex.getMessage();
                errorSpan.setText(errorMsg);
                errorSpan.setVisible(true);
                Notification.show(errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
        }

        try {
            CreateKeyRequest request = CreateKeyRequest.builder()
                    .keyAlias(StringUtils.hasText(newAlias) ? newAlias : existingSelectedAlias)
                    .description(descriptionField.getValue())
                    .keySpec(keySpecCombo.getValue())
                    .keyUsage(keyUsageCombo.getValue())
                    .origin(originCombo.getValue())
                    .multiRegion(multiRegionCheckbox.getValue())
                    .bypassPolicyLockoutSafetyCheck(bypassPolicyCheckbox.getValue())
                    .policy(policyMap)
                    .tags(tags.isEmpty() ? null : tags)
                    .primaryRegion(primaryRegionField.getValue())
                    .replicaRegions(replicaRegionsField.getValue())
                    .build();

            ResponseEntity<CreateKeyResponse> response = kmsApiService.createKey(request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Key creation failed: " + (response.getBody() != null ? response.getBody().toString() : "unknown error");
                errorSpan.setText(errorMsg);
                errorSpan.setVisible(true);
                Notification.show(errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            close();
            Notification.show("Key created successfully", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            parentView.loadAliases();
            parentView.loadKeys();

        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            errorSpan.setText(errorMsg);
            errorSpan.setVisible(true);
            Notification.show("Creation error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            errorSpan.setText(errorMsg);
            errorSpan.setVisible(true);
            Notification.show("Error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}