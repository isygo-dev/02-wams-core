package eu.isygoit.ui.kms.views.secrets.digest.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateDigestConfigDialog extends BaseActionDialog {

    private final DigestConfigService configService;
    private final DigestConfigDto original;

    private TextField codeField;
    private ComboBox<IEnumAlgoDigestConfig.Types> algorithmCombo;
    private IntegerField iterationsField;
    private IntegerField saltSizeField;
    private ComboBox<IEnumSaltGenerator.Types> saltGeneratorCombo;
    private ComboBox<String> providerClassCombo;       // free-text ComboBox
    private ComboBox<String> providerNameCombo;         // free-text ComboBox
    private Checkbox invertSaltPositionCheckbox;
    private Checkbox invertPlainSaltCheckbox;
    private Checkbox lenientSaltCheckbox;
    private IntegerField poolSizeField;
    private Checkbox unicodeIgnoreCheckbox;
    private ComboBox<IEnumStringOutputType.Types> outputTypeCombo;
    private TextField prefixField;
    private TextField suffixField;

    // Map to quickly look up provider name from class path
    private Map<String, String> classToProviderNameMap = new HashMap<>();

    public UpdateDigestConfigDialog(DigestConfigService configService, DigestConfigDto dto, Runnable onSuccess) {
        super("Edit Digest Configuration", onSuccess);
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
        algorithmCombo.setItems(IEnumAlgoDigestConfig.Types.values());
        algorithmCombo.setRequired(true);

        iterationsField = new IntegerField("Iterations");
        iterationsField.setRequired(true);
        iterationsField.setMin(1);

        saltSizeField = new IntegerField("Salt size (bytes)");
        saltSizeField.setMin(0);

        saltGeneratorCombo = new ComboBox<>("Salt generator");
        saltGeneratorCombo.setItems(IEnumSaltGenerator.Types.values());

        // Build mapping from class path to provider name
        for (IEnumProviderClassName.Types type : IEnumProviderClassName.Types.values()) {
            classToProviderNameMap.put(type.getClassPath(), type.getProviderName());
        }

        // Provider class – ComboBox with free-text input
        providerClassCombo = new ComboBox<>("Provider class");
        providerClassCombo.setAllowCustomValue(true);
        providerClassCombo.setItems(
                Arrays.stream(IEnumProviderClassName.Types.values())
                        .map(IEnumProviderClassName.Types::getClassPath)
                        .collect(Collectors.toList())
        );
        providerClassCombo.setPlaceholder("Select or type a provider class");
        providerClassCombo.setClearButtonVisible(true);
        // Auto-populate provider name when a class is selected
        providerClassCombo.addValueChangeListener(e -> {
            String selectedClass = e.getValue();
            if (selectedClass != null && classToProviderNameMap.containsKey(selectedClass)) {
                providerNameCombo.setValue(classToProviderNameMap.get(selectedClass));
            }
        });

        // Provider name – ComboBox with free-text input
        providerNameCombo = new ComboBox<>("Provider name");
        providerNameCombo.setAllowCustomValue(true);
        providerNameCombo.setItems(
                Arrays.stream(IEnumProviderClassName.Types.values())
                        .map(IEnumProviderClassName.Types::getProviderName)
                        .collect(Collectors.toList())
        );
        providerNameCombo.setPlaceholder("Select or type a provider name (e.g., BC, SunJCE)");
        providerNameCombo.setClearButtonVisible(true);

        invertSaltPositionCheckbox = new Checkbox("Invert position of salt in message before digesting");
        invertPlainSaltCheckbox = new Checkbox("Invert position of plain salt in encryption results");
        lenientSaltCheckbox = new Checkbox("Use lenient salt size check");

        poolSizeField = new IntegerField("Pool size");
        poolSizeField.setMin(1);

        unicodeIgnoreCheckbox = new Checkbox("Ignore Unicode normalization");

        outputTypeCombo = new ComboBox<>("Output type");
        outputTypeCombo.setItems(IEnumStringOutputType.Types.values());

        prefixField = new TextField("Prefix");
        suffixField = new TextField("Suffix");
    }

    private void bindData() {
        codeField.setValue(original.getCode());
        algorithmCombo.setValue(original.getAlgorithm());
        iterationsField.setValue(original.getIterations());
        saltSizeField.setValue(original.getSaltSizeBytes());
        saltGeneratorCombo.setValue(original.getSaltGenerator());
        providerClassCombo.setValue(original.getProviderClassName());
        providerNameCombo.setValue(original.getProviderName());
        invertSaltPositionCheckbox.setValue(original.getInvertPositionOfSaltInMessageBeforeDigesting());
        invertPlainSaltCheckbox.setValue(original.getInvertPositionOfPlainSaltInEncryptionResults());
        lenientSaltCheckbox.setValue(original.getUseLenientSaltSizeCheck());
        poolSizeField.setValue(original.getPoolSize());
        unicodeIgnoreCheckbox.setValue(original.getUnicodeNormalizationIgnored());
        outputTypeCombo.setValue(original.getStringOutputType());
        prefixField.setValue(original.getPrefix());
        suffixField.setValue(original.getSuffix());
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
        DigestConfigDto updated = DigestConfigDto.builder()
                .id(original.getId())
                .code(original.getCode())
                .algorithm(algorithmCombo.getValue())
                .iterations(iterationsField.getValue())
                .saltSizeBytes(saltSizeField.getValue())
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
            ResponseEntity<DigestConfigDto> response = configService.update(original.getId(), updated);
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