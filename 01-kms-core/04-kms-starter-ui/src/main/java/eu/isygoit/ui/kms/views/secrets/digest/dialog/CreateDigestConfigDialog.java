package eu.isygoit.ui.kms.views.secrets.digest.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.enums.IEnumAlgoDigestConfig;
import eu.isygoit.enums.IEnumProviderClassName;
import eu.isygoit.enums.IEnumSaltGenerator;
import eu.isygoit.enums.IEnumStringOutputType;
import eu.isygoit.remote.kms.DigestConfigService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateDigestConfigDialog extends BaseActionDialog {

    private final DigestConfigService configService;

    private TextField codeField;
    private ComboBox<IEnumAlgoDigestConfig.Types> algorithmCombo;
    private IntegerField iterationsField;
    private IntegerField saltSizeField;
    private ComboBox<IEnumSaltGenerator.Types> saltGeneratorCombo;
    private ComboBox<String> providerClassCombo;
    private ComboBox<String> providerNameCombo;
    private Checkbox invertSaltPositionCheckbox;
    private Checkbox invertPlainSaltCheckbox;
    private Checkbox lenientSaltCheckbox;
    private IntegerField poolSizeField;
    private Checkbox unicodeIgnoreCheckbox;
    private ComboBox<IEnumStringOutputType.Types> outputTypeCombo;
    private TextField prefixField;
    private TextField suffixField;

    private final Map<String, String> classToProviderNameMap = new HashMap<>();

    public CreateDigestConfigDialog(DigestConfigService configService, Runnable onSuccess) {
        super("Create Digest Configuration", onSuccess);
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
        codeField.setPlaceholder("e.g., DIGEST_PROD");
        codeField.setWidthFull();

        algorithmCombo = new ComboBox<>("Algorithm");
        algorithmCombo.setItems(IEnumAlgoDigestConfig.Types.values());
        algorithmCombo.setRequired(true);
        algorithmCombo.setWidthFull();

        iterationsField = new IntegerField("Iterations");
        iterationsField.setValue(1);
        iterationsField.setMin(1);
        iterationsField.setRequired(true);
        iterationsField.setWidthFull();

        saltSizeField = new IntegerField("Salt size (bytes)");
        saltSizeField.setValue(16);
        saltSizeField.setMin(0);
        saltSizeField.setHelperText("0 = no salt");
        saltSizeField.setWidthFull();

        saltGeneratorCombo = new ComboBox<>("Salt generator");
        saltGeneratorCombo.setItems(IEnumSaltGenerator.Types.values());
        saltGeneratorCombo.setValue(IEnumSaltGenerator.Types.RandomSaltGenerator);
        saltGeneratorCombo.setWidthFull();

        for (IEnumProviderClassName.Types type : IEnumProviderClassName.Types.values()) {
            classToProviderNameMap.put(type.getClassPath(), type.getProviderName());
        }

        providerClassCombo = new ComboBox<>("Provider class");
        providerClassCombo.setAllowCustomValue(true);
        providerClassCombo.setItems(
                Arrays.stream(IEnumProviderClassName.Types.values())
                        .map(IEnumProviderClassName.Types::getClassPath)
                        .collect(Collectors.toList())
        );
        providerClassCombo.setPlaceholder("Select or type a provider class");
        providerClassCombo.setClearButtonVisible(true);
        providerClassCombo.setWidthFull();
        providerClassCombo.addValueChangeListener(e -> {
            String selectedClass = e.getValue();
            if (selectedClass != null && classToProviderNameMap.containsKey(selectedClass)) {
                providerNameCombo.setValue(classToProviderNameMap.get(selectedClass));
            }
        });

        providerNameCombo = new ComboBox<>("Provider name");
        providerNameCombo.setAllowCustomValue(true);
        providerNameCombo.setItems(
                Arrays.stream(IEnumProviderClassName.Types.values())
                        .map(IEnumProviderClassName.Types::getProviderName)
                        .collect(Collectors.toList())
        );
        providerNameCombo.setPlaceholder("Select or type a provider name (e.g., BC, SunJCE)");
        providerNameCombo.setClearButtonVisible(true);
        providerNameCombo.setWidthFull();

        invertSaltPositionCheckbox = new Checkbox("Invert position of salt in message before digesting");
        invertPlainSaltCheckbox = new Checkbox("Invert position of plain salt in encryption results");
        lenientSaltCheckbox = new Checkbox("Use lenient salt size check");

        poolSizeField = new IntegerField("Pool size");
        poolSizeField.setValue(10);
        poolSizeField.setMin(1);
        poolSizeField.setWidthFull();

        unicodeIgnoreCheckbox = new Checkbox("Ignore Unicode normalization");

        outputTypeCombo = new ComboBox<>("Output type");
        outputTypeCombo.setItems(IEnumStringOutputType.Types.values());
        outputTypeCombo.setValue(IEnumStringOutputType.Types.Base64);
        outputTypeCombo.setWidthFull();

        prefixField = new TextField("Prefix");
        prefixField.setWidthFull();

        suffixField = new TextField("Suffix");
        suffixField.setWidthFull();
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(codeField, algorithmCombo, iterationsField, saltSizeField, saltGeneratorCombo,
                providerClassCombo, providerNameCombo,
                invertSaltPositionCheckbox, invertPlainSaltCheckbox,
                lenientSaltCheckbox, poolSizeField, unicodeIgnoreCheckbox,
                outputTypeCombo, prefixField, suffixField);
        return form;
    }

    @Override
    protected boolean onOk() {
        String code = codeField.getValue();
        if (!StringUtils.hasText(code)) {
            append("Code is required");
            return false;
        }
        IEnumAlgoDigestConfig.Types algo = algorithmCombo.getValue();
        if (algo == null) {
            append("Algorithm is required");
            return false;
        }
        Integer iterations = iterationsField.getValue();
        if (iterations == null || iterations < 1) {
            append("Iterations must be at least 1");
            return false;
        }
        Integer saltSize = saltSizeField.getValue();
        if (saltSize == null || saltSize < 0) {
            append("Salt size must be >= 0");
            return false;
        }

        DigestConfigDto dto = DigestConfigDto.builder()
                .code(code)
                .algorithm(algo)
                .iterations(iterations)
                .saltSizeBytes(saltSize)
                .saltGenerator(saltGeneratorCombo.getValue())
                .providerClassName(providerClassCombo.getValue())
                .providerName(providerNameCombo.getValue())
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