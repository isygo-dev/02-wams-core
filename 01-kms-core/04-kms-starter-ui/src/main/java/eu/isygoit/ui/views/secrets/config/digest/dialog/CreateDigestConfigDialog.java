package eu.isygoit.ui.views.secrets.config.digest.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.enums.IEnumAlgoDigestConfig;
import eu.isygoit.enums.IEnumSaltGenerator;
import eu.isygoit.enums.IEnumStringOutputType;
import eu.isygoit.remote.kms.DigestConfigService;
import eu.isygoit.ui.views.common.dialog.BaseActionDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

public class CreateDigestConfigDialog extends BaseActionDialog {

    private final DigestConfigService configService;

    private TextField codeField;
    private ComboBox<IEnumAlgoDigestConfig.Types> algorithmCombo;
    private IntegerField iterationsField;
    private IntegerField saltSizeField;
    private ComboBox<IEnumSaltGenerator.Types> saltGeneratorCombo;
    private TextField providerClassField;
    private TextField providerNameField;
    private Checkbox invertSaltPositionCheckbox;
    private Checkbox invertPlainSaltCheckbox;
    private Checkbox lenientSaltCheckbox;
    private IntegerField poolSizeField;
    private Checkbox unicodeIgnoreCheckbox;
    private ComboBox<IEnumStringOutputType.Types> outputTypeCombo;
    private TextField prefixField;
    private TextField suffixField;

    public CreateDigestConfigDialog(DigestConfigService configService, Runnable onSuccess) {
        super("Create Digest Configuration", onSuccess);
        this.configService = configService;
        setOkButtonText("Create");
        setWidth("700px");
        buildForm();
        add(createFormLayout());
    }

    private void buildForm() {
        codeField = new TextField("Code");
        codeField.setRequired(true);
        codeField.setRequiredIndicatorVisible(true);
        codeField.setPlaceholder("e.g., DIGEST_PROD");

        algorithmCombo = new ComboBox<>("Algorithm");
        algorithmCombo.setItems(IEnumAlgoDigestConfig.Types.values());
        algorithmCombo.setRequired(true);

        iterationsField = new IntegerField("Iterations");
        iterationsField.setValue(1);
        iterationsField.setMin(1);
        iterationsField.setRequired(true);

        saltSizeField = new IntegerField("Salt size (bytes)");
        saltSizeField.setValue(16);
        saltSizeField.setMin(0);
        saltSizeField.setHelperText("0 = no salt");

        saltGeneratorCombo = new ComboBox<>("Salt generator");
        saltGeneratorCombo.setItems(IEnumSaltGenerator.Types.values());
        saltGeneratorCombo.setValue(IEnumSaltGenerator.Types.RandomSaltGenerator);

        providerClassField = new TextField("Provider class");
        providerNameField = new TextField("Provider name");

        invertSaltPositionCheckbox = new Checkbox("Invert position of salt in message before digesting");
        invertPlainSaltCheckbox = new Checkbox("Invert position of plain salt in encryption results");
        lenientSaltCheckbox = new Checkbox("Use lenient salt size check");

        poolSizeField = new IntegerField("Pool size");
        poolSizeField.setValue(10);
        poolSizeField.setMin(1);

        unicodeIgnoreCheckbox = new Checkbox("Ignore Unicode normalization");

        outputTypeCombo = new ComboBox<>("Output type");
        outputTypeCombo.setItems(IEnumStringOutputType.Types.values());
        outputTypeCombo.setValue(IEnumStringOutputType.Types.Base64);

        prefixField = new TextField("Prefix");
        suffixField = new TextField("Suffix");
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(codeField, algorithmCombo, iterationsField, saltSizeField, saltGeneratorCombo,
                providerClassField, providerNameField, invertSaltPositionCheckbox, invertPlainSaltCheckbox,
                lenientSaltCheckbox, poolSizeField, unicodeIgnoreCheckbox, outputTypeCombo,
                prefixField, suffixField);
        return form;
    }

    @Override
    protected boolean onOk() {
        String code = codeField.getValue();
        if (!StringUtils.hasText(code)) {
            showError("Code is required");
            return false;
        }
        IEnumAlgoDigestConfig.Types algo = algorithmCombo.getValue();
        if (algo == null) {
            showError("Algorithm is required");
            return false;
        }
        Integer iterations = iterationsField.getValue();
        if (iterations == null || iterations < 1) {
            showError("Iterations must be at least 1");
            return false;
        }
        Integer saltSize = saltSizeField.getValue();
        if (saltSize == null || saltSize < 0) {
            showError("Salt size must be >= 0");
            return false;
        }

        DigestConfigDto dto = DigestConfigDto.builder()
                .code(code)
                .algorithm(algo)
                .iterations(iterations)
                .saltSizeBytes(saltSize)
                .saltGenerator(saltGeneratorCombo.getValue())
                .providerClassName(providerClassField.getValue())
                .providerName(providerNameField.getValue())
                .invertPositionOfSaltInMessageBeforeDigesting(invertSaltPositionCheckbox.getValue())
                .invertPositionOfPlainSaltInEncryptionResults(invertPlainSaltCheckbox.getValue())
                .useLenientSaltSizeCheck(lenientSaltCheckbox.getValue())
                .poolSize(poolSizeField.getValue())
                .unicodeNormalizationIgnored(unicodeIgnoreCheckbox.getValue())
                .stringOutputType(outputTypeCombo.getValue())
                .prefix(prefixField.getValue())
                .suffix(suffixField.getValue())
                .build();

        try {
            ResponseEntity<DigestConfigDto> response = configService.create(dto);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Configuration created successfully", 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                return true;
            } else {
                this.append("Creation failed: " + response.getStatusCode());
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
        String errorMsg = "Creation failed: " + ex.getMessage();
        this.append(errorMsg);
    }
}