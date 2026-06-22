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
import eu.isygoit.remote.kms.PEBConfigService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;
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
        super("Edit PEB Configuration", onSuccess);
        this.configService = configService;
        this.original = dto;
        setOkButtonText("Save");
        setWidth("700px");
        buildForm();
        addContent(createFormLayout());
        bindData();
    }

    private void buildForm() {
        codeField = new TextField("Code");
        codeField.setReadOnly(true);
        codeField.setWidthFull();

        algorithmCombo = new ComboBox<>("Algorithm");
        algorithmCombo.setItems(IEnumAlgoPEBConfig.Types.values());
        algorithmCombo.setRequired(true);
        algorithmCombo.setWidthFull();

        iterationsField = new IntegerField("Iterations");
        iterationsField.setRequired(true);
        iterationsField.setMin(1);
        iterationsField.setWidthFull();

        saltGeneratorCombo = new ComboBox<>("Salt generator");
        saltGeneratorCombo.setItems(IEnumSaltGenerator.Types.values());
        saltGeneratorCombo.setRequired(true);
        saltGeneratorCombo.setWidthFull();

        ivGeneratorCombo = new ComboBox<>("IV generator");
        ivGeneratorCombo.setItems(IEnumIvGenerator.Types.values());
        ivGeneratorCombo.setRequired(true);
        ivGeneratorCombo.setWidthFull();

        providerClassField = new TextField("Provider class");
        providerClassField.setWidthFull();

        providerNameField = new TextField("Provider name");
        providerNameField.setWidthFull();

        poolSizeField = new IntegerField("Pool size");
        poolSizeField.setMin(1);
        poolSizeField.setWidthFull();

        outputTypeCombo = new ComboBox<>("Output type");
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
            append("Algorithm is required");
            return false;
        }
        Integer iterations = iterationsField.getValue();
        if (iterations == null || iterations <= 0) {
            append("Iterations must be a positive number");
            return false;
        }
        IEnumSaltGenerator.Types saltGen = saltGeneratorCombo.getValue();
        if (saltGen == null) {
            append("Salt generator is required");
            return false;
        }
        IEnumIvGenerator.Types ivGen = ivGeneratorCombo.getValue();
        if (ivGen == null) {
            append("IV generator is required");
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
                append("Configuration updated successfully");
                return true;
            } else {
                append("Update failed: " + response.getStatusCode());
                return false;
            }
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
            return false;
        } catch (Exception ex) {
            append("Update failed: " + ex.getMessage());
            return false;
        }
    }
}