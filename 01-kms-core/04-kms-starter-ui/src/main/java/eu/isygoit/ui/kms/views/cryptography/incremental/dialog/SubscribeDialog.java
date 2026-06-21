package eu.isygoit.ui.kms.views.cryptography.incremental.dialog;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
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
        addContent(createFormLayout());
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

    private VerticalLayout createFormLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();

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
        form.setWidthFull();

        layout.add(form);
        return layout;
    }

    @Override
    protected boolean onOk() {
        String entity = entityField.getValue();
        String attribute = attributeField.getValue();

        if (entity == null || entity.isBlank() || attribute == null || attribute.isBlank()) {
            append("Entity and Attribute are required");
            return false;
        }

        try {
            NextCodeDto dto = NextCodeDto.builder()
                    .entity(entity)
                    .attribute(attribute)
                    .prefix(prefixField.getValue())
                    .suffix(suffixField.getValue())
                    .valueLength(valueLengthField.getValue() != null ? valueLengthField.getValue().longValue() : 6L)
                    .increment(incrementField.getValue())
                    .codeValue(startValueField.getValue() != null ? startValueField.getValue().longValue() : 0L)
                    .build();

            ResponseEntity<NextCodeDto> response = nextCodeService.create(dto);
            if (response.getStatusCode().is2xxSuccessful()) {
                append("Subscription successful");
                return true;
            } else {
                append("Subscription failed: " + response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            append("Error: " + e.getMessage());
            return false;
        }
    }
}