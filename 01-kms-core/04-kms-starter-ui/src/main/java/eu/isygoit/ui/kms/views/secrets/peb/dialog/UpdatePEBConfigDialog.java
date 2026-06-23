package eu.isygoit.ui.kms.views.secrets.peb.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.PEBConfigDto;
import eu.isygoit.enums.IEnumAlgoPEBConfig;
import eu.isygoit.enums.IEnumIvGenerator;
import eu.isygoit.enums.IEnumSaltGenerator;
import eu.isygoit.enums.IEnumStringOutputType;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PEBConfigService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class UpdatePEBConfigDialog extends BaseActionDialog {

    private final PEBConfigService configService;
    private final PEBConfigDto original;

    private TextField codeField;
    private ComboBox<IEnumAlgoPEBConfig.Types> algorithmCombo;
    private IntegerField iterationsField;
    private ComboBox<IEnumSaltGenerator.Types> saltGeneratorCombo;
    private ComboBox<IEnumIvGenerator.Types> ivGeneratorCombo;
    private TextField providerClassField;
    private TextField providerNameField;
    private IntegerField poolSizeField;
    private ComboBox<IEnumStringOutputType.Types> outputTypeCombo;

    public UpdatePEBConfigDialog(PEBConfigService configService, PEBConfigDto dto, Runnable onSuccess) {
        super(I18n.t("peb.dialog.update.title"), onSuccess);
        this.configService = configService;
        this.original = dto;
        setOkButtonText(I18n.t("peb.dialog.update.button"));
        setWidth("700px");
        buildForm();
        addContent(createFormLayout());
        bindData();
    }

    private void buildForm() {
        codeField = new TextField(I18n.t("peb.dialog.field.code"));
        codeField.setReadOnly(true);
        codeField.setWidthFull();

        algorithmCombo = new ComboBox<>(I18n.t("peb.dialog.field.algorithm"));
        algorithmCombo.setItems(IEnumAlgoPEBConfig.Types.values());
        algorithmCombo.setRequired(true);
        algorithmCombo.setWidthFull();

        iterationsField = new IntegerField(I18n.t("peb.dialog.field.iterations"));
        iterationsField.setRequired(true);
        iterationsField.setMin(1);
        iterationsField.setWidthFull();

        saltGeneratorCombo = new ComboBox<>(I18n.t("peb.dialog.field.salt.generator"));
        saltGeneratorCombo.setItems(IEnumSaltGenerator.Types.values());
        saltGeneratorCombo.setRequired(true);
        saltGeneratorCombo.setWidthFull();

        ivGeneratorCombo = new ComboBox<>(I18n.t("peb.dialog.field.iv.generator"));
        ivGeneratorCombo.setItems(IEnumIvGenerator.Types.values());
        ivGeneratorCombo.setRequired(true);
        ivGeneratorCombo.setWidthFull();

        providerClassField = new TextField(I18n.t("peb.dialog.field.provider.class"));
        providerClassField.setWidthFull();

        providerNameField = new TextField(I18n.t("peb.dialog.field.provider.name"));
        providerNameField.setWidthFull();

        poolSizeField = new IntegerField(I18n.t("peb.dialog.field.pool.size"));
        poolSizeField.setMin(1);
        poolSizeField.setWidthFull();

        outputTypeCombo = new ComboBox<>(I18n.t("peb.dialog.field.output.type"));
        outputTypeCombo.setItems(IEnumStringOutputType.Types.values());
        outputTypeCombo.setWidthFull();
    }

    private void bindData() {
        codeField.setValue(original.getCode());
        algorithmCombo.setValue(original.getAlgorithm());
        iterationsField.setValue(original.getKeyObtentionIterations());
        saltGeneratorCombo.setValue(original.getSaltGenerator());
        ivGeneratorCombo.setValue(original.getIvGenerator());
        providerClassField.setValue(original.getProviderClassName());
        providerNameField.setValue(original.getProviderName());
        poolSizeField.setValue(original.getPoolSize());
        outputTypeCombo.setValue(original.getStringOutputType());
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(codeField, algorithmCombo, iterationsField, saltGeneratorCombo, ivGeneratorCombo,
                providerClassField, providerNameField, poolSizeField, outputTypeCombo);
        return form;
    }

    @Override
    protected boolean onOk() {
        IEnumAlgoPEBConfig.Types algo = algorithmCombo.getValue();
        if (algo == null) {
            append(I18n.t("peb.dialog.field.algorithm.required"));
            return false;
        }
        Integer iterations = iterationsField.getValue();
        if (iterations == null || iterations <= 0) {
            append(I18n.t("peb.dialog.field.iterations.required"));
            return false;
        }
        IEnumSaltGenerator.Types saltGen = saltGeneratorCombo.getValue();
        if (saltGen == null) {
            append(I18n.t("peb.dialog.field.salt.generator.required"));
            return false;
        }
        IEnumIvGenerator.Types ivGen = ivGeneratorCombo.getValue();
        if (ivGen == null) {
            append(I18n.t("peb.dialog.field.iv.generator.required"));
            return false;
        }

        PEBConfigDto updated = PEBConfigDto.builder()
                .id(original.getId())
                .code(original.getCode())
                .algorithm(algo)
                .keyObtentionIterations(iterations)
                .saltGenerator(saltGen)
                .ivGenerator(ivGen)
                .providerClassName(providerClassField.getValue())
                .providerName(providerNameField.getValue())
                .poolSize(poolSizeField.getValue())
                .stringOutputType(outputTypeCombo.getValue())
                .build();

        try {
            ResponseEntity<PEBConfigDto> response = configService.update(original.getId(), updated);
            if (response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("peb.dialog.update.success"));
                return true;
            } else {
                append(I18n.t("peb.dialog.update.failed.status", response.getStatusCode()));
                return false;
            }
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
            return false;
        } catch (Exception ex) {
            append(I18n.t("peb.dialog.update.failed", ex.getMessage()));
            return false;
        }
    }
}