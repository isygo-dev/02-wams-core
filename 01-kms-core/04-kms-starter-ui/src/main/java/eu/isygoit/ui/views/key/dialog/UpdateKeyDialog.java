package eu.isygoit.ui.views.key.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.CreateKeyRequest;
import eu.isygoit.dto.KmsDtos.ListResourceTagsResponse;
import eu.isygoit.dto.KmsDtos.UpdateKeyDescriptionRequest;
import eu.isygoit.dto.KmsDtos.UpdateKeyDescriptionResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.BaseActionDialog;
import eu.isygoit.ui.views.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UpdateKeyDialog extends BaseActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;


    private final ObjectMapper objectMapper;
    private final String keyId;
    private final String currentAlias;
    private final String currentDesc;
    private final List<ListResourceTagsResponse.Tag> currentTags;
    private final Boolean currentRotationEnabled;
    private final Integer currentRotationPeriodInDays;

    // UI fields
    private TextField aliasField;
    private TextArea descriptionField;
    private Checkbox rotationEnabledCheckbox;
    private IntegerField rotationPeriodField;
    private VerticalLayout tagsContainer;
    private List<HorizontalLayout> tagRows;

    public UpdateKeyDialog(KeyManagementView parentView,
                           KmsApiService kmsApiService,
                           ObjectMapper objectMapper,
                           String keyId,
                           String currentAlias,
                           String currentDesc,
                           List<ListResourceTagsResponse.Tag> currentTags,
                           Boolean currentRotationEnabled,
                           Integer currentRotationPeriodInDays,
                           Runnable onSuccess) {
        super("Edit key", onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.currentAlias = currentAlias != null ? currentAlias : "";
        this.currentDesc = currentDesc != null ? currentDesc : "";
        this.currentTags = currentTags != null ? currentTags : new ArrayList<>();
        this.currentRotationEnabled = currentRotationEnabled != null ? currentRotationEnabled : false;
        this.currentRotationPeriodInDays = currentRotationPeriodInDays;

        setOkButtonText("Save");
        setWidth("700px");

        buildForm();
        add(createFormLayout());
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);

        String newAlias = aliasField.getValue();
        if (StringUtils.hasText(newAlias) && !newAlias.startsWith("alias:")) {
            String errorMsg = "Alias must start with 'alias:' (e.g., alias:my-key)";
            this.append(errorMsg);
            Notification.show(errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            parentView.showLoading(false);
            return false;
        }

        String newDescription = descriptionField.getValue();

        boolean newRotationEnabled = rotationEnabledCheckbox.getValue();
        Integer newRotationPeriod = newRotationEnabled ? rotationPeriodField.getValue() : null;

        List<CreateKeyRequest.Tag> newTags = new ArrayList<>();
        for (HorizontalLayout row : tagRows) {
            TextField keyField = (TextField) row.getComponentAt(0);
            TextField valueField = (TextField) row.getComponentAt(1);
            if (!valueField.getValue().isBlank()) {
                newTags.add(CreateKeyRequest.Tag.builder()
                        .tagKey(keyField.getValue())
                        .tagValue(valueField.getValue())
                        .build());
            }
        }

        UpdateKeyDescriptionRequest request = UpdateKeyDescriptionRequest.builder()
                .keyId(keyId)
                .keyAlias(StringUtils.hasText(newAlias) ? newAlias : null)
                .description(newDescription)
                .rotationEnabled(newRotationEnabled)
                .rotationPeriodInDays(newRotationPeriod)
                .tags(newTags.isEmpty() ? null : newTags)
                .build();

        try {
            ResponseEntity<UpdateKeyDescriptionResponse> response = kmsApiService.updateKeyDescription(keyId, request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Update failed: " + (response.getBody() != null ? response.getBody().toString() : "unknown error");
                this.append(errorMsg);
                Notification.show(errorMsg, 6000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                parentView.showLoading(false);
                return false;
            }

            close();
            Notification.show("Key updated successfully", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            return true;
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
            Notification.show("Update error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            this.append(errorMsg);
            Notification.show("Error: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildForm() {
        // Alias field (simple text, no dropdown)
        aliasField = new TextField("Alias (optional)");
        aliasField.setPlaceholder("alias:my-key");
        aliasField.setHelperText("Must start with 'alias:' if provided.");
        aliasField.setValue(currentAlias);

        // Description
        descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();
        descriptionField.setMaxLength(500);
        descriptionField.setValue(currentDesc);

        // Rotation settings
        rotationEnabledCheckbox = new Checkbox("Enable automatic rotation");
        rotationPeriodField = new IntegerField("Rotation period (days)");
        rotationPeriodField.setMin(90);
        rotationPeriodField.setMax(365);
        rotationPeriodField.setHelperText("Default 365 days, min 90, max 365");

        rotationEnabledCheckbox.setValue(currentRotationEnabled);
        if (currentRotationEnabled && currentRotationPeriodInDays != null) {
            rotationPeriodField.setValue(currentRotationPeriodInDays);
        }
        rotationPeriodField.setVisible(currentRotationEnabled);

        rotationEnabledCheckbox.addValueChangeListener(e -> {
            rotationPeriodField.setVisible(e.getValue());
            if (!e.getValue()) {
                rotationPeriodField.clear();
            }
        });

        // Tags editor (self-contained)
        tagsContainer = new VerticalLayout();
        tagsContainer.setSpacing(true);
        tagsContainer.setPadding(false);
        tagRows = new ArrayList<>();
        for (ListResourceTagsResponse.Tag tag : currentTags) {
            addTagRow(tag.getTagKey(), tag.getTagValue());
        }
    }

    private void addTagRow(String existingKey, String existingValue) {
        String randomKey = (existingKey != null) ? existingKey : "tag-" + UUID.randomUUID().toString().substring(0, 8);
        TextField keyField = new TextField();
        keyField.setValue(randomKey);
        keyField.setReadOnly(true);
        keyField.setWidth("150px");
        TextField valueField = new TextField();
        valueField.setValue(existingValue != null ? existingValue : "");
        valueField.setPlaceholder("Tag value");
        valueField.setWidth("250px");
        Button removeBtn = new Button(new Icon(VaadinIcon.TRASH));
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        HorizontalLayout row = new HorizontalLayout(keyField, valueField, removeBtn);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        tagRows.add(row);
        tagsContainer.add(row);
        removeBtn.addClickListener(e -> {
            tagsContainer.remove(row);
            tagRows.remove(row);
        });
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();

        // Tags header with add button
        Button addTagButton = new Button("Add tag", new Icon(VaadinIcon.PLUS));
        addTagButton.addClickListener(e -> addTagRow(null, null));
        HorizontalLayout tagsHeader = new HorizontalLayout(new Span("Tags"), addTagButton);
        tagsHeader.setAlignItems(FlexComponent.Alignment.BASELINE);
        tagsHeader.setSpacing(true);

        VerticalLayout tagsSection = new VerticalLayout(tagsHeader, tagsContainer);
        tagsSection.setPadding(false);
        tagsSection.setSpacing(false);

        form.add(aliasField, descriptionField,
                rotationEnabledCheckbox, rotationPeriodField,
                tagsSection);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }
}