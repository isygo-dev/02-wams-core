package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.CreateKeyRequest;
import eu.isygoit.dto.KmsDtos.CreateKeyResponse;
import eu.isygoit.enums.IEnumKeyExpirationModel;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;
import feign.FeignException;
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
    private Checkbox multiRegionCheckbox;
    private TextField primaryRegionField;
    private TextField replicaRegionsField;
    private Checkbox bypassPolicyCheckbox;
    private ComboBox<IEnumKeyExpirationModel.Types> expirationModelCombo;
    private DatePicker validToPicker;
    private TextArea policyField;

    public CreateKeyDialog(KeyManagementView parentView,
                           KmsApiService kmsApiService,
                           ObjectMapper objectMapper,
                           Runnable onSuccess) {
        super(I18n.t("kms.key.dialog.create.title"), parentView, kmsApiService, onSuccess);
        this.objectMapper = objectMapper;
        setOkButtonText(I18n.t("kms.key.dialog.create.button"));
        buildCommonForm();
        buildCreateSpecificForm();
        addFormSections();
        prefillData();
    }

    private void buildCreateSpecificForm() {
        keyUsageCombo = new ComboBox<>(I18n.t("kms.key.dialog.create.field.key.usage"));
        keyUsageCombo.setItems(IEnumKeyUsage.Types.values());
        KmsEnumTag.useTagRenderer(keyUsageCombo, "kms.enum");
        keyUsageCombo.setValue(IEnumKeyUsage.Types.ENCRYPT_DECRYPT);
        keyUsageCombo.setRequiredIndicatorVisible(true);

        keySpecCombo = new ComboBox<>(I18n.t("kms.key.dialog.create.field.key.spec"));
        KmsEnumTag.useTagRenderer(keySpecCombo, "kms.enum");
        keySpecCombo.setRequiredIndicatorVisible(true);
        updateKeySpecOptions(IEnumKeyUsage.Types.ENCRYPT_DECRYPT);
        keyUsageCombo.addValueChangeListener(e -> {
            IEnumKeyUsage.Types selected = e.getValue();
            if (selected != null) updateKeySpecOptions(selected);
            else keySpecCombo.setItems();
        });

        originCombo = new ComboBox<>(I18n.t("kms.key.dialog.create.field.origin"));
        originCombo.setItems(IEnumKeyOrigin.Types.values());
        KmsEnumTag.useTagRenderer(originCombo, "kms.enum");
        originCombo.setValue(IEnumKeyOrigin.Types.WAMS_KMS);
        originCombo.setRequiredIndicatorVisible(true);

        multiRegionCheckbox = new Checkbox(I18n.t("kms.key.dialog.create.field.multi.region"));
        primaryRegionField = new TextField(I18n.t("kms.key.dialog.create.field.primary.region"));
        primaryRegionField.setPlaceholder(I18n.t("kms.key.dialog.create.field.primary.region.placeholder"));
        primaryRegionField.setValue("us-east-1");
        primaryRegionField.setVisible(false);
        replicaRegionsField = new TextField(I18n.t("kms.key.dialog.create.field.replica.regions"));
        replicaRegionsField.setPlaceholder(I18n.t("kms.key.dialog.create.field.replica.regions.placeholder"));
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

        bypassPolicyCheckbox = new Checkbox(I18n.t("kms.key.dialog.create.field.bypass.policy"));

        expirationModelCombo = new ComboBox<>(I18n.t("kms.key.dialog.create.field.expiration.model"));
        expirationModelCombo.setItems(IEnumKeyExpirationModel.Types.values());
        KmsEnumTag.useTagRenderer(expirationModelCombo, "kms.enum");
        expirationModelCombo.setValue(IEnumKeyExpirationModel.Types.KEY_MATERIAL_DOES_NOT_EXPIRE);
        expirationModelCombo.setHelperText(I18n.t("kms.key.dialog.create.field.expiration.model.helper"));
        expirationModelCombo.setVisible(false);

        validToPicker = new DatePicker(I18n.t("kms.key.dialog.create.field.valid.until"));
        validToPicker.setPlaceholder(I18n.t("kms.key.dialog.create.field.valid.until.placeholder"));
        validToPicker.setVisible(false);
        validToPicker.setHelperText(I18n.t("kms.key.dialog.create.field.valid.until.helper"));

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

        policyField = DialogLayout.tall(new TextArea(I18n.t("kms.key.dialog.create.field.policy")));
        policyField.setPlaceholder(I18n.t("kms.key.dialog.create.field.policy.placeholder"));
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

    private void addFormSections() {
        // Cryptography: usage, spec, origin and (conditional) external key material expiry
        VerticalLayout cryptoSection = DialogLayout.section(
                I18n.t("kms.key.dialog.describe.section.crypto"), VaadinIcon.LOCK);
        FormLayout cryptoForm = DialogLayout.responsiveForm();
        cryptoForm.add(keyUsageCombo, keySpecCombo, originCombo, expirationModelCombo, validToPicker);
        cryptoSection.add(cryptoForm);

        // Multi-region
        VerticalLayout regionSection = DialogLayout.section(
                I18n.t("kms.key.dialog.create.section.region"), VaadinIcon.GLOBE);
        FormLayout regionForm = DialogLayout.responsiveForm();
        regionForm.add(multiRegionCheckbox, primaryRegionField, replicaRegionsField);
        regionForm.setColspan(multiRegionCheckbox, 2);
        regionSection.add(regionForm);

        // Policy
        VerticalLayout policySection = DialogLayout.section(
                I18n.t("kms.key.dialog.create.section.policy"), VaadinIcon.SHIELD);
        FormLayout policyForm = DialogLayout.responsiveForm();
        policyForm.add(bypassPolicyCheckbox, policyField);
        policyForm.setColspan(policyField, 2);
        policySection.add(policyForm);

        addSections(buildIdentitySection(), cryptoSection, regionSection,
                buildRotationSection(), policySection, buildTagsSection());
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
                policyMap = objectMapper.readValue(policyField.getValue(), new TypeReference<>() {
                });
            } catch (Exception ex) {
                append(I18n.t("kms.key.dialog.create.invalid.policy"));
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
                append(I18n.t("kms.key.dialog.create.failed", (response.getBody() != null ? response.getBody() : "unknown error")));
                return false;
            }

            append(I18n.t("kms.key.dialog.create.success"));
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("kms.key.dialog.create.failed", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}