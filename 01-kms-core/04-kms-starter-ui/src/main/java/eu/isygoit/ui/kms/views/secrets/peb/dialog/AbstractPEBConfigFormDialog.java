package eu.isygoit.ui.kms.views.secrets.peb.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.PEBConfigDto;
import eu.isygoit.enums.IEnumAlgoPEBConfig;
import eu.isygoit.enums.IEnumIvGenerator;
import eu.isygoit.enums.IEnumSaltGenerator;
import eu.isygoit.enums.IEnumStringOutputType;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PEBConfigService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsActionDialog;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;
import eu.isygoit.ui.kms.views.secrets.SecretsDialogSupport;
import eu.isygoit.ui.kms.views.secrets.SecretsDialogSupport.ProviderFields;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Shared form of the PEB configuration create/update dialogs: every
 * {@link PEBConfigDto} field is declared here once. {@code id} is never
 * shown; {@code code} and {@code tenant} are read-only (assigned by the
 * server). Subclasses only decide how the DTO is built and sent.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
abstract class AbstractPEBConfigFormDialog extends KmsActionDialog {

    final PEBConfigService configService;
    private final String successKey;
    private final String statusFailedKey;
    private final String errorKey;

    TextField codeField;
    TextField tenantField;
    ComboBox<IEnumAlgoPEBConfig.Types> algorithmCombo;
    IntegerField iterationsField;
    ComboBox<IEnumSaltGenerator.Types> saltGeneratorCombo;
    ComboBox<IEnumIvGenerator.Types> ivGeneratorCombo;
    ProviderFields providerFields;
    IntegerField poolSizeField;
    ComboBox<IEnumStringOutputType.Types> outputTypeCombo;

    AbstractPEBConfigFormDialog(String title, Runnable onSuccess, PEBConfigService configService,
                                String successKey, String statusFailedKey, String errorKey) {
        super(title, onSuccess);
        this.configService = configService;
        this.successKey = successKey;
        this.statusFailedKey = statusFailedKey;
        this.errorKey = errorKey;
    }

    /** The DTO that receives the form values on save. */
    abstract PEBConfigDto target();

    /** Sends the DTO to the service. */
    abstract ResponseEntity<PEBConfigDto> send(PEBConfigDto dto);

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_M);

        VerticalLayout root = DialogLayout.stack();
        root.add(buildIdentitySection(), buildCryptoSection(), buildProviderSection());
        add(root);
    }

    private VerticalLayout buildIdentitySection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.peb.details.section.identity"), VaadinIcon.TAG);
        FormLayout form = DialogLayout.responsiveForm();

        codeField = SecretsDialogSupport.readOnlyField(I18n.t("kms.peb.dialog.field.code"));
        tenantField = SecretsDialogSupport.readOnlyField(I18n.t("kms.peb.details.field.tenant"));

        form.add(codeField, tenantField);
        section.add(form);
        return section;
    }

    private VerticalLayout buildCryptoSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.peb.details.section.crypto"), VaadinIcon.COG);
        FormLayout form = DialogLayout.responsiveForm();

        algorithmCombo = new ComboBox<>(I18n.t("kms.peb.dialog.field.algorithm"));
        algorithmCombo.setItems(IEnumAlgoPEBConfig.Types.values());
        KmsEnumTag.useTagRenderer(algorithmCombo, "kms.enum");
        algorithmCombo.setRequired(true);
        algorithmCombo.setRequiredIndicatorVisible(true);
        algorithmCombo.setWidthFull();

        iterationsField = new IntegerField(I18n.t("kms.peb.dialog.field.iterations"));
        iterationsField.setMin(1);
        iterationsField.setRequired(true);
        iterationsField.setRequiredIndicatorVisible(true);
        iterationsField.setWidthFull();

        saltGeneratorCombo = new ComboBox<>(I18n.t("kms.peb.dialog.field.salt.generator"));
        saltGeneratorCombo.setItems(IEnumSaltGenerator.Types.values());
        KmsEnumTag.useTagRenderer(saltGeneratorCombo, "kms.enum");
        saltGeneratorCombo.setRequired(true);
        saltGeneratorCombo.setRequiredIndicatorVisible(true);
        saltGeneratorCombo.setWidthFull();

        ivGeneratorCombo = new ComboBox<>(I18n.t("kms.peb.dialog.field.iv.generator"));
        ivGeneratorCombo.setItems(IEnumIvGenerator.Types.values());
        KmsEnumTag.useTagRenderer(ivGeneratorCombo, "kms.enum");
        ivGeneratorCombo.setRequired(true);
        ivGeneratorCombo.setRequiredIndicatorVisible(true);
        ivGeneratorCombo.setWidthFull();

        outputTypeCombo = new ComboBox<>(I18n.t("kms.peb.dialog.field.output.type"));
        outputTypeCombo.setItems(IEnumStringOutputType.Types.values());
        KmsEnumTag.useTagRenderer(outputTypeCombo, "kms.enum");
        outputTypeCombo.setWidthFull();

        form.add(algorithmCombo, iterationsField, saltGeneratorCombo, ivGeneratorCombo, outputTypeCombo);
        section.add(form);
        return section;
    }

    private VerticalLayout buildProviderSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.peb.details.section.advanced"), VaadinIcon.SERVER);
        FormLayout form = DialogLayout.responsiveForm();

        providerFields = new ProviderFields(
                I18n.t("kms.peb.dialog.field.provider.class"),
                I18n.t("kms.peb.dialog.field.provider.class.placeholder"),
                I18n.t("kms.peb.dialog.field.provider.name"),
                I18n.t("kms.peb.dialog.field.provider.name.placeholder"));

        poolSizeField = new IntegerField(I18n.t("kms.peb.dialog.field.pool.size"));
        poolSizeField.setMin(1);
        poolSizeField.setWidthFull();

        form.add(providerFields.classCombo(), providerFields.nameCombo(), poolSizeField);
        form.setColspan(providerFields.classCombo(), 2);
        section.add(form);
        return section;
    }

    /** Fills the form from an existing configuration. */
    final void fillFrom(PEBConfigDto dto) {
        SecretsDialogSupport.setText(codeField, dto.getCode());
        SecretsDialogSupport.setText(tenantField, dto.getTenant());
        algorithmCombo.setValue(dto.getAlgorithm());
        iterationsField.setValue(dto.getKeyObtentionIterations());
        saltGeneratorCombo.setValue(dto.getSaltGenerator());
        ivGeneratorCombo.setValue(dto.getIvGenerator());
        providerFields.classCombo().setValue(dto.getProviderClassName());
        providerFields.nameCombo().setValue(dto.getProviderName());
        poolSizeField.setValue(dto.getPoolSize());
        outputTypeCombo.setValue(dto.getStringOutputType());
    }

    /**
     * Copies the editable form values into the DTO; {@code id}, {@code code},
     * {@code tenant} and the audit fields are left untouched.
     */
    private void applyTo(PEBConfigDto dto) {
        dto.setAlgorithm(algorithmCombo.getValue());
        dto.setKeyObtentionIterations(iterationsField.getValue());
        dto.setSaltGenerator(saltGeneratorCombo.getValue());
        dto.setIvGenerator(ivGeneratorCombo.getValue());
        dto.setProviderClassName(providerFields.classCombo().getValue());
        dto.setProviderName(providerFields.nameCombo().getValue());
        dto.setPoolSize(poolSizeField.getValue());
        dto.setStringOutputType(outputTypeCombo.getValue());
    }

    private boolean isValid() {
        if (algorithmCombo.getValue() == null) {
            append(I18n.t("kms.peb.dialog.field.algorithm.required"));
            return false;
        }
        Integer iterations = iterationsField.getValue();
        if (iterations == null || iterations <= 0) {
            append(I18n.t("kms.peb.dialog.field.iterations.required"));
            return false;
        }
        if (saltGeneratorCombo.getValue() == null) {
            append(I18n.t("kms.peb.dialog.field.salt.generator.required"));
            return false;
        }
        if (ivGeneratorCombo.getValue() == null) {
            append(I18n.t("kms.peb.dialog.field.iv.generator.required"));
            return false;
        }
        return true;
    }

    @Override
    protected final boolean onOk() {
        if (!isValid()) {
            return false;
        }

        PEBConfigDto dto = target();
        applyTo(dto);

        try {
            ResponseEntity<PEBConfigDto> response = send(dto);
            if (response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t(successKey));
                return true;
            }
            append(I18n.t(statusFailedKey, response.getStatusCode()));
            return false;
        } catch (FeignException ex) {
            append(SecretsDialogSupport.feignMessage(ex));
            return false;
        } catch (Exception ex) {
            append(I18n.t(errorKey, ex.getMessage()));
            return false;
        }
    }
}
