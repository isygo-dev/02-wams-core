package eu.isygoit.ui.kms.views.secrets.digest.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.enums.IEnumAlgoDigestConfig;
import eu.isygoit.enums.IEnumProviderClassName;
import eu.isygoit.enums.IEnumSaltGenerator;
import eu.isygoit.enums.IEnumStringOutputType;
import eu.isygoit.i18n.I18n;
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
    private final Map<String, String> classToProviderNameMap = new HashMap<>();
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

    public CreateDigestConfigDialog(DigestConfigService configService, Runnable onSuccess) {
        super(I18n.t("digest.dialog.create.title"), onSuccess);
        this.configService = configService;
        setOkButtonText(I18n.t("digest.dialog.create.button"));
        setWidth("700px");
        buildForm();
        addContent(createFormLayout());
    }

    private void buildForm() {
        codeField = new TextField(I18n.t("digest.dialog.field.code"));
        codeField.setRequired(true);
        codeField.setRequiredIndicatorVisible(true);
        codeField.setPlaceholder(I18n.t("digest.dialog.field.code.placeholder"));
        codeField.setWidthFull();

        algorithmCombo = new ComboBox<>(I18n.t("digest.dialog.field.algorithm"));
        algorithmCombo.setItems(IEnumAlgoDigestConfig.Types.values());
        algorithmCombo.setRequired(true);
        algorithmCombo.setWidthFull();

        iterationsField = new IntegerField(I18n.t("digest.dialog.field.iterations"));
        iterationsField.setValue(1);
        iterationsField.setMin(1);
        iterationsField.setRequired(true);
        iterationsField.setWidthFull();

        saltSizeField = new IntegerField(I18n.t("digest.dialog.field.salt.size"));
        saltSizeField.setValue(16);
        saltSizeField.setMin(0);
        saltSizeField.setHelperText(I18n.t("digest.dialog.field.salt.size.helper"));
        saltSizeField.setWidthFull();

        saltGeneratorCombo = new ComboBox<>(I18n.t("digest.dialog.field.salt.generator"));
        saltGeneratorCombo.setItems(IEnumSaltGenerator.Types.values());
        saltGeneratorCombo.setValue(IEnumSaltGenerator.Types.RandomSaltGenerator);
        saltGeneratorCombo.setWidthFull();

        for (IEnumProviderClassName.Types type : IEnumProviderClassName.Types.values()) {
            classToProviderNameMap.put(type.getClassPath(), type.getProviderName());
        }

        providerClassCombo = new ComboBox<>(I18n.t("digest.dialog.field.provider.class"));
        providerClassCombo.setAllowCustomValue(true);
        providerClassCombo.setItems(
                Arrays.stream(IEnumProviderClassName.Types.values())
                        .map(IEnumProviderClassName.Types::getClassPath)
                        .collect(Collectors.toList())
        );
        providerClassCombo.setPlaceholder(I18n.t("digest.dialog.field.provider.class.placeholder"));
        providerClassCombo.setClearButtonVisible(true);
        providerClassCombo.setWidthFull();
        providerClassCombo.addValueChangeListener(e -> {
            String selectedClass = e.getValue();
            if (selectedClass != null && classToProviderNameMap.containsKey(selectedClass)) {
                providerNameCombo.setValue(classToProviderNameMap.get(selectedClass));
            }
        });

        providerNameCombo = new ComboBox<>(I18n.t("digest.dialog.field.provider.name"));
        providerNameCombo.setAllowCustomValue(true);
        providerNameCombo.setItems(
                Arrays.stream(IEnumProviderClassName.Types.values())
                        .map(IEnumProviderClassName.Types::getProviderName)
                        .collect(Collectors.toList())
        );
        providerNameCombo.setPlaceholder(I18n.t("digest.dialog.field.provider.name.placeholder"));
        providerNameCombo.setClearButtonVisible(true);
        providerNameCombo.setWidthFull();

        invertSaltPositionCheckbox = new Checkbox(I18n.t("digest.dialog.field.invert.salt.position"));
        invertPlainSaltCheckbox = new Checkbox(I18n.t("digest.dialog.field.invert.plain.salt"));
        lenientSaltCheckbox = new Checkbox(I18n.t("digest.dialog.field.lenient.salt"));

        poolSizeField = new IntegerField(I18n.t("digest.dialog.field.pool.size"));
        poolSizeField.setValue(10);
        poolSizeField.setMin(1);
        poolSizeField.setWidthFull();

        unicodeIgnoreCheckbox = new Checkbox(I18n.t("digest.dialog.field.ignore.unicode"));

        outputTypeCombo = new ComboBox<>(I18n.t("digest.dialog.field.output.type"));
        outputTypeCombo.setItems(IEnumStringOutputType.Types.values());
        outputTypeCombo.setValue(IEnumStringOutputType.Types.Base64);
        outputTypeCombo.setWidthFull();

        prefixField = new TextField(I18n.t("digest.dialog.field.prefix"));
        prefixField.setWidthFull();

        suffixField = new TextField(I18n.t("digest.dialog.field.suffix"));
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
            append(I18n.t("digest.dialog.field.code.required"));
            return false;
        }
        IEnumAlgoDigestConfig.Types algo = algorithmCombo.getValue();
        if (algo == null) {
            append(I18n.t("digest.dialog.field.algorithm.required"));
            return false;
        }
        Integer iterations = iterationsField.getValue();
        if (iterations == null || iterations < 1) {
            append(I18n.t("digest.dialog.field.iterations.required"));
            return false;
        }
        Integer saltSize = saltSizeField.getValue();
        if (saltSize == null || saltSize < 0) {
            append(I18n.t("digest.dialog.field.salt.size.required"));
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
                append(I18n.t("digest.dialog.create.success"));
                return true;
            } else {
                append(I18n.t("digest.dialog.create.failed", response.getStatusCode()));
                return false;
            }
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
            return false;
        } catch (Exception ex) {
            append(I18n.t("digest.dialog.creation.failed", ex.getMessage()));
            return false;
        }
    }
}