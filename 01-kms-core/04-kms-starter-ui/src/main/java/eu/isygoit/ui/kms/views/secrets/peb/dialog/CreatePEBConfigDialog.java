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
import org.springframework.util.StringUtils;

public class CreatePEBConfigDialog extends BaseActionDialog {

    private final PEBConfigService configService;

    private TextField codeField;
    private ComboBox<IEnumAlgoPEBConfig.Types> algorithmCombo;
    private IntegerField iterationsField;
    private ComboBox<IEnumSaltGenerator.Types> saltGeneratorCombo;
    private ComboBox<IEnumIvGenerator.Types> ivGeneratorCombo;
    private TextField providerClassField;
    private TextField providerNameField;
    private IntegerField poolSizeField;
    private ComboBox<IEnumStringOutputType.Types> outputTypeCombo;

    public CreatePEBConfigDialog(PEBConfigService configService, Runnable onSuccess) {
        super("Create PEB Configuration", onSuccess);
        this.configService = configService;
        setOkButtonText("Create");
        setWidth("700px");
        buildForm();
        addContent(createFormLayout());
    }

    private void buildForm() {
        codeField = new TextField("Code");
        codeField.setRequired(true);
        codeField.setRequiredIndicatorVisible(true);
        codeField.setPlaceholder("e.g., PEB_PROD");
        codeField.setWidthFull();

        algorithmCombo = new ComboBox<>("Algorithm");
        algorithmCombo.setItems(IEnumAlgoPEBConfig.Types.values());
        algorithmCombo.setRequired(true);
        algorithmCombo.setRequiredIndicatorVisible(true);
        algorithmCombo.setWidthFull();

        iterationsField = new IntegerField("Iterations");
        iterationsField.setValue(10000);
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
        providerClassField.setPlaceholder("e.g., org.bouncycastle.jce.provider.BouncyCastleProvider");
        providerClassField.setWidthFull();

        providerNameField = new TextField("Provider name");
        providerNameField.setPlaceholder("e.g., BC");
        providerNameField.setWidthFull();

        poolSizeField = new IntegerField("Pool size");
        poolSizeField.setValue(10);
        poolSizeField.setMin(1);
        poolSizeField.setWidthFull();

        outputTypeCombo = new ComboBox<>("Output type");
        outputTypeCombo.setItems(IEnumStringOutputType.Types.values());
        outputTypeCombo.setValue(IEnumStringOutputType.Types.Base64);
        outputTypeCombo.setWidthFull();
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
        String code = codeField.getValue();
        if (!StringUtils.hasText(code)) {
            append("Code is required");
            return false;
        }
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

        PEBConfigDto dto = PEBConfigDto.builder()
                .code(code)
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
            ResponseEntity<PEBConfigDto> response = configService.create(dto);
            if (response.getStatusCode().is2xxSuccessful()) {
                append("Configuration created successfully");
                return true;
            } else {
                append("Creation failed: " + response.getStatusCode());
                return false;
            }
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
            return false;
        } catch (Exception ex) {
            append("Creation failed: " + ex.getMessage());
            return false;
        }
    }
}