package eu.isygoit.ui.views.key.dialog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.CreateKeyRequest;
import eu.isygoit.dto.KmsDtos.CreateKeyResponse;
import eu.isygoit.enums.IEnumKeyExpirationModel;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.BaseActionDialog;
import eu.isygoit.ui.views.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CreateKeyDialog extends BaseActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;
    private final Runnable onSuccess;

    private final ObjectMapper objectMapper;

    // Form fields
    private ComboBox<String> aliasCombo;
    private TextField newAliasField;
    private TextArea descriptionField;
    private ComboBox<IEnumKeyUsage.Types> keyUsageCombo;
    private ComboBox<IEnumKeySpec.Types> keySpecCombo;
    private ComboBox<IEnumKeyOrigin.Types> originCombo;
    private Checkbox multiRegionCheckbox;
    private TextField primaryRegionField;
    private TextField replicaRegionsField;
    private Checkbox bypassPolicyCheckbox;
    private Checkbox rotationEnabledCheckbox;
    private IntegerField rotationPeriodField;
    private ComboBox<IEnumKeyExpirationModel.Types> expirationModelCombo;
    private DatePicker validToPicker;
    private TextArea policyField;
    private VerticalLayout tagsContainer;
    private List<HorizontalLayout> tagRows;
    private HorizontalLayout tagsHeader;

    public CreateKeyDialog(KeyManagementView parentView,
                           KmsApiService kmsApiService,
                           Runnable onSuccess,
                           ObjectMapper objectMapper) {
        super("Create new KMS key", onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.onSuccess = onSuccess;
        this.objectMapper = objectMapper;

        setOkButtonText("Create");
        setWidth("700px");

        buildForm();
        add(createFormLayout());
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);

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
                policyMap = objectMapper.readValue(policyField.getValue(), new TypeReference<>() {
                });
            } catch (Exception ex) {
                String errorMsg = "Invalid JSON in policy field: " + ex.getMessage();
                this.append(errorMsg);
                Notification.show(errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false;
            }
        }

        IEnumKeyExpirationModel.Types expirationModel = expirationModelCombo.getValue();
        LocalDateTime validTo = null;
        if (expirationModel == IEnumKeyExpirationModel.Types.KEY_MATERIAL_EXPIRES && validToPicker.getValue() != null) {
            validTo = validToPicker.getValue().atTime(LocalTime.MAX);
        }

        try {
            CreateKeyRequest request = CreateKeyRequest.builder()
                    .keyAlias(StringUtils.hasText(newAlias) ? newAlias : existingSelectedAlias)
                    .description(descriptionField.getValue())
                    .keyUsage(keyUsageCombo.getValue())
                    .keySpec(keySpecCombo.getValue())
                    .origin(originCombo.getValue())
                    .multiRegion(multiRegionCheckbox.getValue())
                    .bypassPolicyLockoutSafetyCheck(bypassPolicyCheckbox.getValue())
                    .rotationEnabled(rotationEnabledCheckbox.getValue())
                    .rotationPeriodInDays(rotationEnabledCheckbox.getValue() ? rotationPeriodField.getValue() : null)
                    .expirationModel(expirationModel)
                    .validTo(validTo)
                    .policy(policyMap)
                    .tags(tags.isEmpty() ? null : tags)
                    .primaryRegion(primaryRegionField.getValue())
                    .replicaRegions(replicaRegionsField.getValue())
                    .build();

            ResponseEntity<CreateKeyResponse> response = kmsApiService.createKey(request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Key creation failed: " + (response.getBody() != null ? response.getBody().toString() : "unknown error");
                this.append(errorMsg);
                Notification.show(errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false;
            }

            close();
            Notification.show("Key created successfully", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
            Notification.show("Creation error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            this.append(errorMsg);
            Notification.show("Creation error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildForm() {
        // Alias section
        aliasCombo = new ComboBox<>("Alias (optional)");
        aliasCombo.setItems(parentView.existingAliases);
        aliasCombo.setPlaceholder("Select existing or type new");
        aliasCombo.setAllowCustomValue(true);
        newAliasField = new TextField("New alias name");
        newAliasField.setVisible(false);
        newAliasField.setPlaceholder("alias:my-new-alias");
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

        // Key usage first (so it can filter key spec)
        keyUsageCombo = new ComboBox<>("Key usage");
        keyUsageCombo.setItems(IEnumKeyUsage.Types.values());
        keyUsageCombo.setValue(IEnumKeyUsage.Types.ENCRYPT_DECRYPT);
        keyUsageCombo.setRequiredIndicatorVisible(true);

        // Key spec will be filtered based on selected usage
        keySpecCombo = new ComboBox<>("Key specification");
        keySpecCombo.setRequiredIndicatorVisible(true);
        updateKeySpecOptions(IEnumKeyUsage.Types.ENCRYPT_DECRYPT);
        keyUsageCombo.addValueChangeListener(e -> {
            IEnumKeyUsage.Types selectedUsage = e.getValue();
            if (selectedUsage != null) {
                updateKeySpecOptions(selectedUsage);
            } else {
                keySpecCombo.setItems();
            }
        });

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

        // Rotation settings
        rotationEnabledCheckbox = new Checkbox("Enable automatic rotation");
        rotationPeriodField = new IntegerField("Rotation period (days)");
        rotationPeriodField.setMin(90);
        rotationPeriodField.setMax(3650);
        rotationPeriodField.setValue(365);
        rotationPeriodField.setVisible(false);
        rotationPeriodField.setHelperText("Default 365 days, min 90, max 3650");
        rotationEnabledCheckbox.addValueChangeListener(e -> {
            boolean enabled = e.getValue();
            rotationPeriodField.setVisible(enabled);
            if (!enabled) {
                rotationPeriodField.clear();
            }
        });

        // Expiration model for imported key material (BYOK) – initially hidden
        expirationModelCombo = new ComboBox<>("Expiration model (for imported key)");
        expirationModelCombo.setItems(IEnumKeyExpirationModel.Types.values());
        expirationModelCombo.setValue(IEnumKeyExpirationModel.Types.KEY_MATERIAL_DOES_NOT_EXPIRE);
        expirationModelCombo.setHelperText("Select how the imported key material should expire (only for origin = EXTERNAL)");
        expirationModelCombo.setVisible(false);

        validToPicker = new DatePicker("Valid until (date)");
        validToPicker.setPlaceholder("YYYY-MM-DD");
        validToPicker.setVisible(false);
        validToPicker.setHelperText("Date after which the key material becomes unusable");

        // Listeners to toggle expiration fields based on origin
        originCombo.addValueChangeListener(e -> {
            boolean isExternal = e.getValue() == IEnumKeyOrigin.Types.EXTERNAL;
            expirationModelCombo.setVisible(isExternal);
            if (isExternal) {
                boolean expires = expirationModelCombo.getValue() == IEnumKeyExpirationModel.Types.KEY_MATERIAL_EXPIRES;
                validToPicker.setVisible(expires);
            } else {
                validToPicker.setVisible(false);
                expirationModelCombo.setValue(IEnumKeyExpirationModel.Types.KEY_MATERIAL_DOES_NOT_EXPIRE);
                validToPicker.clear();
            }
        });

        expirationModelCombo.addValueChangeListener(e -> {
            boolean isExternal = originCombo.getValue() == IEnumKeyOrigin.Types.EXTERNAL;
            if (isExternal) {
                validToPicker.setVisible(e.getValue() == IEnumKeyExpirationModel.Types.KEY_MATERIAL_EXPIRES);
            } else {
                validToPicker.setVisible(false);
            }
        });

        policyField = new TextArea("Policy (JSON)");
        policyField.setPlaceholder("{\n  \"Version\": \"2012-10-17\",\n  \"Statement\": [...]\n}");
        policyField.setWidthFull();
        policyField.setHeight("150px");

        // Tag editor
        tagsContainer = new VerticalLayout();
        tagsContainer.setSpacing(true);
        tagsContainer.setPadding(false);
        tagRows = new ArrayList<>();
        Button addTagButton = new Button("Add tag", new Icon(VaadinIcon.PLUS));
        addTagButton.addClickListener(e -> addTagRow(null, null));
        tagsHeader = new HorizontalLayout(new Span("Tags (random key + value)"), addTagButton);
        tagsHeader.setAlignItems(FlexComponent.Alignment.BASELINE);
        tagsHeader.setSpacing(true);
    }

    private void updateKeySpecOptions(IEnumKeyUsage.Types usage) {
        List<IEnumKeySpec.Types> compatibleSpecs = new ArrayList<>();
        for (IEnumKeySpec.Types spec : IEnumKeySpec.Types.values()) {
            if (spec.allowedUsages().contains(usage)) {
                compatibleSpecs.add(spec);
            }
        }
        keySpecCombo.setItems(compatibleSpecs);
        if (!compatibleSpecs.contains(keySpecCombo.getValue())) {
            if (compatibleSpecs.contains(IEnumKeySpec.Types.SYMMETRIC_DEFAULT)) {
                keySpecCombo.setValue(IEnumKeySpec.Types.SYMMETRIC_DEFAULT);
            } else if (!compatibleSpecs.isEmpty()) {
                keySpecCombo.setValue(compatibleSpecs.get(0));
            } else {
                keySpecCombo.clear();
            }
        }
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
                keyUsageCombo, keySpecCombo, originCombo,
                multiRegionCheckbox, primaryRegionField, replicaRegionsField,
                bypassPolicyCheckbox,
                rotationEnabledCheckbox, rotationPeriodField,
                expirationModelCombo, validToPicker,
                policyField,
                tagsHeader, tagsContainer);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }
}