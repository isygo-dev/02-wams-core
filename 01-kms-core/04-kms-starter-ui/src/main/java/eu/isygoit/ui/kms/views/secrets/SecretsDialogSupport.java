package eu.isygoit.ui.kms.views.secrets;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.enums.IEnumProviderClassName;
import feign.FeignException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Helpers shared by the digest, password and PEB configuration dialogs
 * (read-only identity fields, provider combos, null-safe form binding and
 * Feign error extraction).
 */
public final class SecretsDialogSupport {

    private SecretsDialogSupport() {
    }

    /** Full-width read-only text field (used for the server-assigned {@code code} and {@code tenant}). */
    public static TextField readOnlyField(String label) {
        TextField field = new TextField(label);
        field.setReadOnly(true);
        field.setWidthFull();
        return field;
    }

    /** Null-safe text binding. */
    public static void setText(TextField field, String value) {
        field.setValue(value != null ? value : "");
    }

    /** Null-safe checkbox binding (null means unchecked). */
    public static void setFlag(Checkbox checkbox, Boolean value) {
        checkbox.setValue(Boolean.TRUE.equals(value));
    }

    /** Message shown for a Feign failure: the server body for 400/500, the exception message otherwise. */
    public static String feignMessage(FeignException ex) {
        return (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
    }

    /**
     * Provider class / provider name combos: both accept custom values and
     * choosing a known class fills in the matching provider name.
     */
    public static final class ProviderFields {

        private final ComboBox<String> classCombo;
        private final ComboBox<String> nameCombo;

        public ProviderFields(String classLabel, String classPlaceholder,
                              String nameLabel, String namePlaceholder) {
            Map<String, String> classToName = new HashMap<>();
            for (IEnumProviderClassName.Types type : IEnumProviderClassName.Types.values()) {
                classToName.put(type.getClassPath(), type.getProviderName());
            }

            classCombo = new ComboBox<>(classLabel);
            classCombo.setAllowCustomValue(true);
            classCombo.setItems(Arrays.stream(IEnumProviderClassName.Types.values())
                    .map(IEnumProviderClassName.Types::getClassPath)
                    .toList());
            classCombo.setPlaceholder(classPlaceholder);
            classCombo.setClearButtonVisible(true);
            classCombo.setWidthFull();

            nameCombo = new ComboBox<>(nameLabel);
            nameCombo.setAllowCustomValue(true);
            nameCombo.setItems(Arrays.stream(IEnumProviderClassName.Types.values())
                    .map(IEnumProviderClassName.Types::getProviderName)
                    .toList());
            nameCombo.setPlaceholder(namePlaceholder);
            nameCombo.setClearButtonVisible(true);
            nameCombo.setWidthFull();

            // Custom values are kept as typed (the combo has no item to select for them).
            classCombo.addCustomValueSetListener(e -> classCombo.setValue(e.getDetail()));
            nameCombo.addCustomValueSetListener(e -> nameCombo.setValue(e.getDetail()));

            classCombo.addValueChangeListener(e -> {
                String selected = e.getValue();
                if (selected != null && classToName.containsKey(selected)) {
                    nameCombo.setValue(classToName.get(selected));
                }
            });
        }

        public ComboBox<String> classCombo() {
            return classCombo;
        }

        public ComboBox<String> nameCombo() {
            return nameCombo;
        }
    }
}
