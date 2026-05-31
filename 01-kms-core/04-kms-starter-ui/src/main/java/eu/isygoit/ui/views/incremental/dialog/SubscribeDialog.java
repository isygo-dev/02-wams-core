package eu.isygoit.ui.views.incremental.dialog;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.ui.views.BaseActionDialog;
import org.springframework.http.ResponseEntity;

public class SubscribeDialog extends BaseActionDialog {

    private final KmsAppNextCodeService nextCodeService;

    private TextField entityField;
    private TextField attributeField;
    private TextField prefixField;
    private TextField suffixField;
    private IntegerField valueLengthField;
    private IntegerField incrementField;
    private IntegerField startValueField;

    public SubscribeDialog(KmsAppNextCodeService nextCodeService, Runnable onSuccess) {
        super("Subscribe New Incremental Key Generator", onSuccess);
        this.nextCodeService = nextCodeService;
        setOkButtonText("Subscribe");
        setWidth("600px");
        buildForm();
        add(createFormLayout());
    }

    private void buildForm() {
        entityField = new TextField("Entity");
        entityField.setRequired(true);
        entityField.setRequiredIndicatorVisible(true);
        entityField.setPlaceholder("e.g., User");

        attributeField = new TextField("Attribute");
        attributeField.setRequired(true);
        attributeField.setRequiredIndicatorVisible(true);
        attributeField.setPlaceholder("e.g., code");

        prefixField = new TextField("Prefix");
        prefixField.setPlaceholder("e.g., USR-");

        suffixField = new TextField("Suffix");
        suffixField.setPlaceholder("e.g., -2025");

        valueLengthField = new IntegerField("Value Length");
        valueLengthField.setValue(6);
        valueLengthField.setStepButtonsVisible(true);
        valueLengthField.setMin(1);
        valueLengthField.setMax(20);
        valueLengthField.setHelperText("Number of digits (zero‑padded)");

        incrementField = new IntegerField("Increment");
        incrementField.setValue(1);
        incrementField.setStepButtonsVisible(true);
        incrementField.setMin(1);
        incrementField.setMax(1000);
        incrementField.setHelperText("Step size for each generation");

        startValueField = new IntegerField("Start Code Value");
        startValueField.setValue(0);
        startValueField.setStepButtonsVisible(true);
        startValueField.setMin(0);
        startValueField.setHelperText("Initial numeric value");
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.add(entityField, attributeField,
                prefixField, suffixField,
                valueLengthField, incrementField,
                startValueField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(entityField, 1);
        form.setColspan(attributeField, 1);
        form.setColspan(prefixField, 1);
        form.setColspan(suffixField, 1);
        form.setColspan(valueLengthField, 1);
        form.setColspan(incrementField, 1);
        form.setColspan(startValueField, 2);
        return form;
    }

    @Override
    protected boolean onOk() {
        String entity = entityField.getValue();
        String attribute = attributeField.getValue();

        if (entity == null || entity.isBlank() || attribute == null || attribute.isBlank()) {
            Notification.show("Entity and Attribute are required", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        NextCodeDto dto = NextCodeDto.builder()
                .entity(entity)
                .attribute(attribute)
                .prefix(prefixField.getValue())
                .suffix(suffixField.getValue())
                .valueLength(valueLengthField.getValue() != null ? valueLengthField.getValue().longValue() : 6L)
                .increment(incrementField.getValue())
                .codeValue(startValueField.getValue() != null ? startValueField.getValue().longValue() : 0L)
                .build();

        try {
            ResponseEntity<NextCodeDto> response = nextCodeService.create(dto);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Subscription successful", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                return true;
            } else {
                String errorMsg = "Subscription failed: " + response.getStatusCode();
                showError(errorMsg);
                Notification.show(errorMsg, 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false;
            }
        } catch (Exception e) {
            String errorMsg = "Error: " + e.getMessage();
            showError(errorMsg);
            Notification.show(errorMsg, 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }
    }
}