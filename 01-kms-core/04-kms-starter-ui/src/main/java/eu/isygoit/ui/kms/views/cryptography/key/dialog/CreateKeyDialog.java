package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.CreateKeyRequest;
import eu.isygoit.dto.KmsDtos.CreateKeyResponse;
import eu.isygoit.enums.IEnumKeyExpirationModel;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateKeyDialog extends KeyDialogBase {

    private final ObjectMapper objectMapper;

    private ComboBox<IEnumKeyUsage.Types> keyUsageCombo;
    private ComboBox<IEnumKeySpec.Types> keySpecCombo;
    private ComboBox<IEnumKeyOrigin.Types> originCombo;
    private com.vaadin.flow.component.checkbox.Checkbox multiRegionCheckbox;
    private TextField primaryRegionField;
    private TextField replicaRegionsField;
    private com.vaadin.flow.component.checkbox.Checkbox bypassPolicyCheckbox;
    private ComboBox<IEnumKeyExpirationModel.Types> expirationModelCombo;
    private DatePicker validToPicker;
    private TextArea policyField;

    public CreateKeyDialog(KeyManagementView parentView,
                           KmsApiService kmsApiService,
                           ObjectMapper objectMapper,
                           Runnable onSuccess) {
        super("Create new KMS key", parentView, kmsApiService, onSuccess);
        this.objectMapper = objectMapper;
        setOkButtonText("Create");
        buildCommonForm();
        buildCreateSpecificForm();
        add(createFullFormLayout());
        prefillData();
    }

    private void buildCreateSpecificForm() {
        keyUsageCombo = new ComboBox<>("Key usage");
        keyUsageCombo.setItems(IEnumKeyUsage.Types.values());
        keyUsageCombo.setValue(IEnumKeyUsage.Types.ENCRYPT_DECRYPT);
        keyUsageCombo.setRequiredIndicatorVisible(true);

        keySpecCombo = new ComboBox<>("Key specification");
        keySpecCombo.setRequiredIndicatorVisible(true);
        updateKeySpecOptions(IEnumKeyUsage.Types.ENCRYPT_DECRYPT);
        keyUsageCombo.addValueChangeListener(e -> {
            IEnumKeyUsage.Types selected = e.getValue();
            if (selected != null) updateKeySpecOptions(selected);
            else keySpecCombo.setItems();
        });

        originCombo = new ComboBox<>("Origin");
        originCombo.setItems(IEnumKeyOrigin.Types.values());
        originCombo.setValue(IEnumKeyOrigin.Types.WAMS_KMS);
        originCombo.setRequiredIndicatorVisible(true);

        multiRegionCheckbox = new com.vaadin.flow.component.checkbox.Checkbox("Multi-region key");
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

        bypassPolicyCheckbox = new com.vaadin.flow.component.checkbox.Checkbox("Bypass policy lockout safety check");

        expirationModelCombo = new ComboBox<>("Expiration model (for imported key)");
        expirationModelCombo.setItems(IEnumKeyExpirationModel.Types.values());
        expirationModelCombo.setValue(IEnumKeyExpirationModel.Types.KEY_MATERIAL_DOES_NOT_EXPIRE);
        expirationModelCombo.setHelperText("Only for origin = EXTERNAL");
        expirationModelCombo.setVisible(false);

        validToPicker = new DatePicker("Valid until (date)");
        validToPicker.setPlaceholder("YYYY-MM-DD");
        validToPicker.setVisible(false);
        validToPicker.setHelperText("Date after which key material expires");

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
    }

    private void updateKeySpecOptions(IEnumKeyUsage.Types usage) {
        List<IEnumKeySpec.Types> compatible = new ArrayList<>();
        for (IEnumKeySpec.Types spec : IEnumKeySpec.Types.values()) {
            if (spec.allowedUsages().contains(usage)) compatible.add(spec);
        }
        keySpecCombo.setItems(compatible);
        if (!compatible.contains(keySpecCombo.getValue())) {
            if (compatible.contains(IEnumKeySpec.Types.SYMMETRIC_DEFAULT))
                keySpecCombo.setValue(IEnumKeySpec.Types.SYMMETRIC_DEFAULT);
            else if (!compatible.isEmpty()) keySpecCombo.setValue(compatible.get(0));
            else keySpecCombo.clear();
        }
    }

    private FormLayout createFullFormLayout() {
        FormLayout fullForm = new FormLayout();
        fullForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        fullForm.add(aliasField, descriptionField,
                keyUsageCombo, keySpecCombo, originCombo,
                multiRegionCheckbox, primaryRegionField, replicaRegionsField,
                bypassPolicyCheckbox,
                rotationEnabledCheckbox, rotationPeriodField,
                expirationModelCombo, validToPicker,
                policyField);

        // Tags section
        Button addTagButton = new Button("Add tag", new com.vaadin.flow.component.icon.Icon(VaadinIcon.PLUS));
        addTagButton.addClickListener(e -> addTagRow(null, null));
        HorizontalLayout tagsHeader = new HorizontalLayout(new Span("Tags"), addTagButton);
        tagsHeader.setAlignItems(FlexComponent.Alignment.BASELINE);
        tagsHeader.setSpacing(true);
        VerticalLayout tagsSection = new VerticalLayout(tagsHeader, tagsContainer);
        tagsSection.setPadding(false);
        tagsSection.setSpacing(false);
        fullForm.add(tagsSection);

        return fullForm;
    }

    @Override
    protected void prefillData() {
        if (tagRows.isEmpty()) addTagRow(null, null);
        rotationEnabledCheckbox.setValue(false);
        rotationPeriodField.setValue(365);
    }

    @Override
    protected boolean onOk() {
        String alias = getAliasOrNull();
        if (alias == null && !aliasField.getValue().isBlank()) {
            return false;
        }

        List<CreateKeyRequest.Tag> tags = getTagsFromRows();

        Map<String, Object> policyMap = null;
        if (StringUtils.hasText(policyField.getValue())) {
            try {
                policyMap = objectMapper.readValue(policyField.getValue(), new TypeReference<>() {});
            } catch (Exception ex) {
                append("Invalid JSON in policy");
                return false;
            }
        }

        IEnumKeyExpirationModel.Types expirationModel = expirationModelCombo.getValue();
        LocalDateTime validTo = null;
        if (expirationModel == IEnumKeyExpirationModel.Types.KEY_MATERIAL_EXPIRES && validToPicker.getValue() != null) {
            validTo = validToPicker.getValue().atTime(LocalTime.MAX);
        }

        parentView.showLoading(true);
        try {
            CreateKeyRequest request = CreateKeyRequest.builder()
                    .keyAlias(alias)
                    .description(getDescriptionOrNull())
                    .keyUsage(keyUsageCombo.getValue())
                    .keySpec(keySpecCombo.getValue())
                    .origin(originCombo.getValue())
                    .multiRegion(multiRegionCheckbox.getValue())
                    .bypassPolicyLockoutSafetyCheck(bypassPolicyCheckbox.getValue())
                    .rotationEnabled(rotationEnabledCheckbox.getValue())
                    .rotationPeriodInDays(getRotationPeriodOrNull())
                    .expirationModel(expirationModel)
                    .validTo(validTo)
                    .policy(policyMap)
                    .tags(tags.isEmpty() ? null : tags)
                    .primaryRegion(primaryRegionField.getValue())
                    .replicaRegions(replicaRegionsField.getValue())
                    .build();

            ResponseEntity<CreateKeyResponse> response = kmsApiService.createKey(request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append("Key creation failed: " + (response.getBody() != null ? response.getBody() : "unknown error"));
                return false;
            }

            append("Key created successfully");
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append("Failed operation: " + e.getMessage());
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}