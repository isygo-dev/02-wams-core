package eu.isygoit.ui.views.incremental;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Route(value = "incremental-key", layout = MainLayout.class)
@PageTitle("Incremental Key")
@Component
public class IncrementalKeyView extends Composite<VerticalLayout> {

    private final KmsIncrementalKeyService incrementalKeyService;

    @Autowired
    public IncrementalKeyView(KmsIncrementalKeyService incrementalKeyService) {
        this.incrementalKeyService = incrementalKeyService;
        buildUI();
    }

    private void buildUI() {
        VerticalLayout layout = getContent();
        layout.setPadding(true);
        layout.setSpacing(true);

        // ================= SUBSCRIPTION PANEL =================
        H2 subscribeTitle = new H2("Subscribe New Incremental Key Generator");
        FormLayout subscribeForm = new FormLayout();

        TextField entityField = new TextField("Entity");
        entityField.setRequired(true);
        TextField attributeField = new TextField("Attribute");
        attributeField.setRequired(true);
        TextField prefixField = new TextField("Prefix");
        TextField suffixField = new TextField("Suffix");
        IntegerField valueLengthField = new IntegerField("Value Length");
        valueLengthField.setValue(6);
        valueLengthField.setStepButtonsVisible(true);
        valueLengthField.setMin(1);
        IntegerField incrementField = new IntegerField("Increment");
        incrementField.setValue(1);
        incrementField.setStepButtonsVisible(true);
        incrementField.setMin(1);
        IntegerField startValueField = new IntegerField("Start Code Value");
        startValueField.setValue(0);
        startValueField.setStepButtonsVisible(true);
        startValueField.setMin(0);

        Button subscribeButton = new Button("Subscribe", event -> {
            String entity = entityField.getValue();
            String attribute = attributeField.getValue();

            if (entity == null || entity.isBlank() || attribute == null || attribute.isBlank()) {
                Notification.show("Entity and Attribute are required", 3000, Notification.Position.MIDDLE);
                return;
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
                ResponseEntity<String> response = incrementalKeyService.subscribeNextCode(dto);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Notification.show("Subscription successful", 3000, Notification.Position.MIDDLE);
                } else {
                    Notification.show("Subscription failed: " + response.getStatusCode(), 5000, Notification.Position.MIDDLE);
                }
            } catch (Exception e) {
                Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });

        subscribeForm.add(entityField, attributeField, prefixField, suffixField,
                valueLengthField, incrementField, startValueField, subscribeButton);
        subscribeForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        subscribeForm.setColspan(entityField, 1);
        subscribeForm.setColspan(attributeField, 1);
        subscribeForm.setColspan(prefixField, 1);
        subscribeForm.setColspan(suffixField, 1);
        subscribeForm.setColspan(valueLengthField, 1);
        subscribeForm.setColspan(incrementField, 1);
        subscribeForm.setColspan(startValueField, 1);
        subscribeForm.setColspan(subscribeButton, 2);

        // ================= CODE GENERATION PANEL =================
        H2 generateTitle = new H2("Generate Next Code");
        FormLayout generateForm = new FormLayout();

        TextField genEntityField = new TextField("Entity");
        genEntityField.setRequired(true);
        TextField genAttributeField = new TextField("Attribute");
        genAttributeField.setRequired(true);

        Button generateButton = new Button("Generate Code", event -> {
            String entity = genEntityField.getValue();
            String attribute = genAttributeField.getValue();

            if (entity == null || entity.isBlank() || attribute == null || attribute.isBlank()) {
                Notification.show("Entity and Attribute are required", 3000, Notification.Position.MIDDLE);
                return;
            }

            try {
                ResponseEntity<String> response = incrementalKeyService.generateNextCode(entity, attribute);
                if (response.getStatusCode().is2xxSuccessful()) {
                    String generatedCode = response.getBody();
                    Notification.show("Generated code: " + generatedCode, 5000, Notification.Position.MIDDLE);
                } else {
                    Notification.show("Generation failed: " + response.getStatusCode(), 5000, Notification.Position.MIDDLE);
                }
            } catch (Exception e) {
                Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });

        generateForm.add(genEntityField, genAttributeField, generateButton);
        generateForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        generateForm.setColspan(genEntityField, 1);
        generateForm.setColspan(genAttributeField, 1);
        generateForm.setColspan(generateButton, 2);

        layout.add(subscribeTitle, subscribeForm, generateTitle, generateForm);
    }
}