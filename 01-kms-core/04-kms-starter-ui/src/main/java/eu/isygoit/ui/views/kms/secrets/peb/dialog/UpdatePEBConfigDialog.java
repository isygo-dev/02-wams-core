package eu.isygoit.ui.views.kms.secrets.peb.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.PEBConfigDto;
import eu.isygoit.enums.IEnumAlgoPEBConfig;
import eu.isygoit.enums.IEnumIvGenerator;
import eu.isygoit.enums.IEnumSaltGenerator;
import eu.isygoit.enums.IEnumStringOutputType;
import eu.isygoit.remote.kms.PEBConfigService;
import eu.isygoit.ui.views.common.dialog.BaseActionDialog;
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
        super("Edit PEB Configuration", onSuccess);
        this.configService = configService;
        this.original = dto;
        setOkButtonText("Save");
        setWidth("700px");
        buildForm();
        add(createFormLayout());
        bindData();
    }

    private void buildForm() {
        codeField = new TextField("Code");
        codeField.setReadOnly(true);
        codeField.setWidthFull();

        algorithmCombo = new ComboBox<>("Algorithm");
        algorithmCombo.setItems(IEnumAlgoPEBConfig.Types.values());
        algorithmCombo.setRequired(true);

        iterationsField = new IntegerField("Iterations");
        iterationsField.setRequired(true);
        iterationsField.setMin(1);

        saltGeneratorCombo = new ComboBox<>("Salt generator");
        saltGeneratorCombo.setItems(IEnumSaltGenerator.Types.values());
        saltGeneratorCombo.setRequired(true);

        ivGeneratorCombo = new ComboBox<>("IV generator");
        ivGeneratorCombo.setItems(IEnumIvGenerator.Types.values());
        ivGeneratorCombo.setRequired(true);

        providerClassField = new TextField("Provider class");
        providerNameField = new TextField("Provider name");
        poolSizeField = new IntegerField("Pool size");
        poolSizeField.setMin(1);

        outputTypeCombo = new ComboBox<>("Output type");
        outputTypeCombo.setItems(IEnumStringOutputType.Types.values());
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
        PEBConfigDto updated = PEBConfigDto.builder()
                .id(original.getId())
                .code(original.getCode())
                .algorithm(algorithmCombo.getValue())
                .keyObtentionIterations(iterationsField.getValue())
                .saltGenerator(saltGeneratorCombo.getValue())
                .ivGenerator(ivGeneratorCombo.getValue())
                .providerClassName(providerClassField.getValue())
                .providerName(providerNameField.getValue())
                .poolSize(poolSizeField.getValue())
                .stringOutputType(outputTypeCombo.getValue())
                .build();

        try {
            ResponseEntity<PEBConfigDto> response = configService.update(original.getId(), updated);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Configuration updated successfully", 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                return true;
            } else {
                this.append("Update failed: " + response.getStatusCode());
                return false;
            }
        } catch (FeignException ex) {
            handleFeignException(ex);
            return false;
        } catch (Exception ex) {
            handleGenericException(ex);
            return false;
        }
    }

    private void handleFeignException(FeignException ex) {
        String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
        this.append(errorMsg);
    }

    private void handleGenericException(Exception ex) {
        String errorMsg = "Update failed: " + ex.getMessage();
        this.append(errorMsg);
    }
}