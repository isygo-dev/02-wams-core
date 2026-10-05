package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import elemental.json.JsonObject;
import eu.isygoit.i18n.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Tag("auth-otp-input")
public class OtpInput extends Component implements HasComponents {

    private final List<TextField> digitFields = new ArrayList<>();
    private int length = 6;
    private boolean distributingPaste;
    private Consumer<String> completeListener = ignored -> {
    };
    private Consumer<Boolean> valueChangeListener = ignored -> {
    };

    public OtpInput() {
        getElement().setAttribute("dir", "ltr");
        buildFields(length);
    }

    public boolean setLength(int requestedLength) {
        if (requestedLength < 4 || requestedLength > 8) {
            return false;
        }
        if (requestedLength != length) {
            length = requestedLength;
            buildFields(length);
        }
        return true;
    }

    public String getValue() {
        return digitFields.stream().map(TextField::getValue).collect(Collectors.joining());
    }

    public boolean isComplete() {
        return digitFields.size() == length
                && digitFields.stream().allMatch(field -> field.getValue().matches("[0-9]"));
    }

    public void clear() {
        digitFields.forEach(TextField::clear);
    }

    public void focusFirst() {
        if (!digitFields.isEmpty()) {
            digitFields.get(0).focus();
        }
    }

    public void focusFirstIncomplete() {
        digitFields.stream()
                .filter(field -> field.getValue().isEmpty())
                .findFirst()
                .ifPresent(TextField::focus);
    }

    public void setEnabled(boolean enabled) {
        digitFields.forEach(field -> field.setEnabled(enabled));
    }

    public void setCompleteListener(Consumer<String> listener) {
        completeListener = listener;
    }

    public void setValueChangeListener(Consumer<Boolean> listener) {
        valueChangeListener = listener;
    }

    public void setErrorDescriptionId(String id) {
        digitFields.forEach(field -> field.getElement().setAttribute("aria-describedby", id));
    }

    private void buildFields(int requestedLength) {
        getElement().removeAllChildren();
        digitFields.clear();
        getElement().setAttribute("data-length", Integer.toString(requestedLength));

        for (int index = 0; index < requestedLength; index++) {
            TextField field = new TextField();
            field.setLabel(I18n.t("auth.otp.field.digit.label", index + 1, requestedLength));
            field.setRequiredIndicatorVisible(true);
            field.setMaxLength(index == 0 ? requestedLength : 1);
            field.setPattern(index == 0 ? "[0-9]{1," + requestedLength + "}" : "[0-9]");
            field.setValueChangeMode(ValueChangeMode.EAGER);
            field.getElement().setAttribute("inputmode", "numeric");
            field.getElement().setAttribute("aria-describedby", AuthErrorBanner.ID);
            if (index == 0) {
                field.getElement().setAttribute("autocomplete", "one-time-code");
            }

            int fieldIndex = index;
            field.addValueChangeListener(event -> {
                if (fieldIndex == 0 && event.getValue().length() > 1) {
                    distributePaste(event.getValue());
                    return;
                }
                if (!event.getValue().isEmpty() && fieldIndex < digitFields.size() - 1) {
                    digitFields.get(fieldIndex + 1).focus();
                }
                valueChangeListener.accept(isComplete());
                submitWhenComplete();
            });
            field.addKeyDownListener(event -> {
                if (event.getKey().equals(Key.BACKSPACE)
                        && field.getValue().isEmpty() && fieldIndex > 0) {
                    digitFields.get(fieldIndex - 1).focus();
                } else if (event.getKey().equals(Key.ARROW_LEFT) && fieldIndex > 0) {
                    digitFields.get(fieldIndex - 1).focus();
                } else if (event.getKey().equals(Key.ARROW_RIGHT)
                        && fieldIndex < digitFields.size() - 1) {
                    digitFields.get(fieldIndex + 1).focus();
                }
            });
            field.getElement().addEventListener("paste", event -> {
                JsonObject eventData = event.getEventData();
                String pastedValue = eventData.getString("event.clipboardData.getData('text')");
                distributePaste(pastedValue);
            }).addEventData("event.clipboardData.getData('text')");

            digitFields.add(field);
            add(field);
        }
    }

    private void distributePaste(String pastedValue) {
        if (pastedValue == null || pastedValue.isBlank()) {
            return;
        }
        String digits = pastedValue.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return;
        }

        distributingPaste = true;
        clear();
        int count = Math.min(length, digits.length());
        for (int index = 0; index < count; index++) {
            digitFields.get(index).setValue(digits.substring(index, index + 1));
        }
        distributingPaste = false;

        if (isComplete()) {
            valueChangeListener.accept(true);
            completeListener.accept(getValue());
        } else if (count < digitFields.size()) {
            valueChangeListener.accept(false);
            digitFields.get(count).focus();
        }
    }

    private void submitWhenComplete() {
        if (!distributingPaste && isComplete()) {
            completeListener.accept(getValue());
        }
    }

    @Override
    public void add(Component... components) {
        for (Component component : components) {
            getElement().appendChild(component.getElement());
        }
    }
}
