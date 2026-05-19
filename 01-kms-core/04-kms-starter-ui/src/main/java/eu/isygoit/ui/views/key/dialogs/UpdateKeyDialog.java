package eu.isygoit.ui.views.key.dialogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
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

import java.util.ArrayList;
import java.util.List;

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
    private ComboBox<String> aliasCombo;
    private TextField newAliasField;
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
                           Integer currentRotationPeriodInDays) {
        super("Edit key");
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
    protected void onOk() {
        clearError();

        String newAlias = null;
        if (newAliasField.isVisible() && !newAliasField.getValue().isBlank()) {
            newAlias = newAliasField.getValue();
        } else if (aliasCombo.getValue() != null && !aliasCombo.getValue().isBlank()) {
            newAlias = aliasCombo.getValue();
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
                .keyAlias(newAlias)
                .description(newDescription)
                .rotationEnabled(newRotationEnabled)
                .rotationPeriodInDays(newRotationPeriod)
                .tags(newTags.isEmpty() ? null : newTags)
                .build();

        try {
            ResponseEntity<UpdateKeyDescriptionResponse> response = kmsApiService.updateKeyDescription(keyId, request);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = "Update failed: " + (response.getBody() != null ? response.getBody().toString() : "unknown error");
                showError(errorMsg);
                Notification.show(errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            close();
            Notification.show("Key updated successfully", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            parentView.loadAliases();
            parentView.loadKeys();
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            showError(errorMsg);
            Notification.show("Update error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            showError(errorMsg);
            Notification.show("Error: " + errorMsg, 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void buildForm() {
        // Alias section
        aliasCombo = new ComboBox<>("Alias");
        aliasCombo.setItems(parentView.existingAliases);
        aliasCombo.setPlaceholder("Select existing alias");
        aliasCombo.setAllowCustomValue(true);
        newAliasField = new TextField("New alias name");
        newAliasField.setVisible(false);
        newAliasField.setPlaceholder("alias:my-new-alias");

        aliasCombo.setValue(currentAlias.isEmpty() ? null : currentAlias);

        aliasCombo.addCustomValueSetListener(e -> {
            newAliasField.setVisible(true);
            newAliasField.setValue(e.getDetail());
        });
        aliasCombo.addValueChangeListener(e -> {
            if (e.getValue() != null && !e.getValue().isEmpty() && !parentView.existingAliases.contains(e.getValue())) {
                newAliasField.setVisible(true);
                newAliasField.setValue(e.getValue());
            } else {
                newAliasField.setVisible(false);
                newAliasField.clear();
            }
        });

        // Description
        descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();
        descriptionField.setMaxLength(500);
        descriptionField.setValue(currentDesc);

        // Rotation settings
        rotationEnabledCheckbox = new Checkbox("Enable automatic rotation");
        rotationPeriodField = new IntegerField("Rotation period (days)");
        rotationPeriodField.setMin(90);
        rotationPeriodField.setMax(3650);
        rotationPeriodField.setHelperText("Default 365 days, min 90, max 3650");

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

        // Tags editor
        tagsContainer = new VerticalLayout();
        tagsContainer.setSpacing(true);
        tagsContainer.setPadding(false);
        tagRows = new ArrayList<>();
        for (ListResourceTagsResponse.Tag tag : currentTags) {
            parentView.addTagRow(tagsContainer, tagRows, tag.getTagKey(), tag.getTagValue());
        }
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();

        // Build the tags header (label + add button) inline, so no scope issues
        Button addTagButton = new Button("Add tag", new Icon(VaadinIcon.PLUS));
        addTagButton.addClickListener(e -> parentView.addTagRow(tagsContainer, tagRows, null, null));
        HorizontalLayout tagsHeader = new HorizontalLayout(new Span("Tags"), addTagButton);
        tagsHeader.setAlignItems(FlexComponent.Alignment.BASELINE);
        tagsHeader.setSpacing(true);

        // Combine header and container into one vertical section
        VerticalLayout tagsSection = new VerticalLayout(tagsHeader, tagsContainer);
        tagsSection.setPadding(false);
        tagsSection.setSpacing(false);

        form.add(aliasCombo, newAliasField, descriptionField,
                rotationEnabledCheckbox, rotationPeriodField,
                tagsSection);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }
}