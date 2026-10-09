package eu.isygoit.ui.kms.views.secrets.digest.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.enums.IEnumAlgoDigestConfig;
import eu.isygoit.enums.IEnumSaltGenerator;
import eu.isygoit.enums.IEnumStringOutputType;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.DigestConfigService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsActionDialog;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;
import eu.isygoit.ui.kms.views.secrets.SecretsDialogSupport;
import eu.isygoit.ui.kms.views.secrets.SecretsDialogSupport.ProviderFields;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Shared form of the digest configuration create/update dialogs: every
 * {@link DigestConfigDto} field is declared here once. {@code id} is never
 * shown; {@code code} and {@code tenant} are read-only (assigned by the
 * server). Subclasses only decide how the DTO is built and sent.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
abstract class AbstractDigestConfigFormDialog extends KmsActionDialog {

    final DigestConfigService configService;
    private final String successKey;
    private final String statusFailedKey;
    private final String errorKey;

    TextField tenantField;
    TextField codeField;
    ComboBox<IEnumAlgoDigestConfig.Types> algorithmCombo;
    IntegerField iterationsField;
    IntegerField saltSizeField;
    ComboBox<IEnumSaltGenerator.Types> saltGeneratorCombo;
    ProviderFields providerFields;
    Checkbox invertSaltPositionCheckbox;
    Checkbox invertPlainSaltCheckbox;
    Checkbox lenientSaltCheckbox;
    IntegerField poolSizeField;
    Checkbox unicodeIgnoreCheckbox;
    ComboBox<IEnumStringOutputType.Types> outputTypeCombo;
    TextField prefixField;
    TextField suffixField;

    AbstractDigestConfigFormDialog(String title, Runnable onSuccess, DigestConfigService configService,
                                   String successKey, String statusFailedKey, String errorKey) {
        super(title, onSuccess);
        this.configService = configService;
        this.successKey = successKey;
        this.statusFailedKey = statusFailedKey;
        this.errorKey = errorKey;
    }

    /** The DTO that receives the form values on save. */
    abstract DigestConfigDto target();

    /** Sends the DTO to the service. */
    abstract ResponseEntity<DigestConfigDto> send(DigestConfigDto dto);

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_M);

        VerticalLayout root = DialogLayout.stack();
        root.add(buildIdentitySection(), buildAlgorithmSection(), buildSaltSection(),
                buildProviderSection(), buildFramingSection());
        add(root);
    }

    private VerticalLayout buildIdentitySection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.digest.details.section.identity"), VaadinIcon.TAG);
        FormLayout form = DialogLayout.responsiveForm();

        codeField = SecretsDialogSupport.readOnlyField(I18n.t("kms.digest.dialog.field.code"));
        tenantField = SecretsDialogSupport.readOnlyField(I18n.t("kms.digest.details.field.tenant"));

        form.add(codeField, tenantField);
        section.add(form);
        return section;
    }

    private VerticalLayout buildAlgorithmSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.digest.details.section.algorithm"), VaadinIcon.COG);
        FormLayout form = DialogLayout.responsiveForm();

        algorithmCombo = new ComboBox<>(I18n.t("kms.digest.dialog.field.algorithm"));
        algorithmCombo.setItems(IEnumAlgoDigestConfig.Types.values());
        KmsEnumTag.useTagRenderer(algorithmCombo, "kms.enum");
        algorithmCombo.setRequired(true);
        algorithmCombo.setRequiredIndicatorVisible(true);
        algorithmCombo.setWidthFull();

        iterationsField = new IntegerField(I18n.t("kms.digest.dialog.field.iterations"));
        iterationsField.setMin(1);
        iterationsField.setRequired(true);
        iterationsField.setRequiredIndicatorVisible(true);
        iterationsField.setWidthFull();

        outputTypeCombo = new ComboBox<>(I18n.t("kms.digest.dialog.field.output.type"));
        outputTypeCombo.setItems(IEnumStringOutputType.Types.values());
        KmsEnumTag.useTagRenderer(outputTypeCombo, "kms.enum");
        outputTypeCombo.setWidthFull();

        form.add(algorithmCombo, iterationsField, outputTypeCombo);
        section.add(form);
        return section;
    }

    private VerticalLayout buildSaltSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.digest.dialog.section.salt"), VaadinIcon.DROP);
        FormLayout form = DialogLayout.responsiveForm();

        saltSizeField = new IntegerField(I18n.t("kms.digest.dialog.field.salt.size"));
        saltSizeField.setMin(0);
        saltSizeField.setHelperText(I18n.t("kms.digest.dialog.field.salt.size.helper"));
        saltSizeField.setWidthFull();

        saltGeneratorCombo = new ComboBox<>(I18n.t("kms.digest.dialog.field.salt.generator"));
        saltGeneratorCombo.setItems(IEnumSaltGenerator.Types.values());
        KmsEnumTag.useTagRenderer(saltGeneratorCombo, "kms.enum");
        saltGeneratorCombo.setWidthFull();

        invertSaltPositionCheckbox = new Checkbox(I18n.t("kms.digest.dialog.field.invert.salt.position"));
        invertPlainSaltCheckbox = new Checkbox(I18n.t("kms.digest.dialog.field.invert.plain.salt"));
        lenientSaltCheckbox = new Checkbox(I18n.t("kms.digest.dialog.field.lenient.salt"));

        form.add(saltSizeField, saltGeneratorCombo,
                invertSaltPositionCheckbox, invertPlainSaltCheckbox, lenientSaltCheckbox);
        section.add(form);
        return section;
    }

    private VerticalLayout buildProviderSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.digest.details.section.advanced"), VaadinIcon.SERVER);
        FormLayout form = DialogLayout.responsiveForm();

        providerFields = new ProviderFields(
                I18n.t("kms.digest.dialog.field.provider.class"),
                I18n.t("kms.digest.dialog.field.provider.class.placeholder"),
                I18n.t("kms.digest.dialog.field.provider.name"),
                I18n.t("kms.digest.dialog.field.provider.name.placeholder"));

        poolSizeField = new IntegerField(I18n.t("kms.digest.dialog.field.pool.size"));
        poolSizeField.setMin(1);
        poolSizeField.setWidthFull();

        unicodeIgnoreCheckbox = new Checkbox(I18n.t("kms.digest.dialog.field.ignore.unicode"));

        form.add(providerFields.classCombo(), providerFields.nameCombo(), poolSizeField, unicodeIgnoreCheckbox);
        form.setColspan(providerFields.classCombo(), 2);
        section.add(form);
        return section;
    }

    private VerticalLayout buildFramingSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.digest.dialog.section.framing"), VaadinIcon.TEXT_INPUT);
        FormLayout form = DialogLayout.responsiveForm();

        prefixField = new TextField(I18n.t("kms.digest.dialog.field.prefix"));
        prefixField.setWidthFull();

        suffixField = new TextField(I18n.t("kms.digest.dialog.field.suffix"));
        suffixField.setWidthFull();

        form.add(prefixField, suffixField);
        section.add(form);
        return section;
    }

    /** Fills the form from an existing configuration. */
    final void fillFrom(DigestConfigDto dto) {
        SecretsDialogSupport.setText(codeField, dto.getCode());
        SecretsDialogSupport.setText(tenantField, dto.getTenant());
        algorithmCombo.setValue(dto.getAlgorithm());
        iterationsField.setValue(dto.getIterations());
        saltSizeField.setValue(dto.getSaltSizeBytes());
        saltGeneratorCombo.setValue(dto.getSaltGenerator());
        providerFields.classCombo().setValue(dto.getProviderClassName());
        providerFields.nameCombo().setValue(dto.getProviderName());
        SecretsDialogSupport.setFlag(invertSaltPositionCheckbox, dto.getInvertPositionOfSaltInMessageBeforeDigesting());
        SecretsDialogSupport.setFlag(invertPlainSaltCheckbox, dto.getInvertPositionOfPlainSaltInEncryptionResults());
        SecretsDialogSupport.setFlag(lenientSaltCheckbox, dto.getUseLenientSaltSizeCheck());
        poolSizeField.setValue(dto.getPoolSize());
        SecretsDialogSupport.setFlag(unicodeIgnoreCheckbox, dto.getUnicodeNormalizationIgnored());
        outputTypeCombo.setValue(dto.getStringOutputType());
        SecretsDialogSupport.setText(prefixField, dto.getPrefix());
        SecretsDialogSupport.setText(suffixField, dto.getSuffix());
    }

    /**
     * Copies the editable form values into the DTO; {@code id}, {@code code},
     * {@code tenant} and the audit fields are left untouched.
     */
    private void applyTo(DigestConfigDto dto) {
        dto.setAlgorithm(algorithmCombo.getValue());
        dto.setIterations(iterationsField.getValue());
        dto.setSaltSizeBytes(saltSizeField.getValue());
        dto.setSaltGenerator(saltGeneratorCombo.getValue());
        dto.setProviderClassName(providerFields.classCombo().getValue());
        dto.setProviderName(providerFields.nameCombo().getValue());
        dto.setInvertPositionOfSaltInMessageBeforeDigesting(invertSaltPositionCheckbox.getValue());
        dto.setInvertPositionOfPlainSaltInEncryptionResults(invertPlainSaltCheckbox.getValue());
        dto.setUseLenientSaltSizeCheck(lenientSaltCheckbox.getValue());
        dto.setPoolSize(poolSizeField.getValue());
        dto.setUnicodeNormalizationIgnored(unicodeIgnoreCheckbox.getValue());
        dto.setStringOutputType(outputTypeCombo.getValue());
        dto.setPrefix(prefixField.getValue());
        dto.setSuffix(suffixField.getValue());
    }

    private boolean isValid() {
        if (algorithmCombo.getValue() == null) {
            append(I18n.t("kms.digest.dialog.field.algorithm.required"));
            return false;
        }
        Integer iterations = iterationsField.getValue();
        if (iterations == null || iterations < 1) {
            append(I18n.t("kms.digest.dialog.field.iterations.required"));
            return false;
        }
        Integer saltSize = saltSizeField.getValue();
        if (saltSize == null || saltSize < 0) {
            append(I18n.t("kms.digest.dialog.field.salt.size.required"));
            return false;
        }
        return true;
    }

    @Override
    protected final boolean onOk() {
        if (!isValid()) {
            return false;
        }

        DigestConfigDto dto = target();
        applyTo(dto);

        try {
            ResponseEntity<DigestConfigDto> response = send(dto);
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
