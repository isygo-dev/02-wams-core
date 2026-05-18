package eu.isygoit.ui.views.key.dialogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
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
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.key.KeyManagementView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog for updating a KMS key's alias, description, tags, and rotation settings.
 */
public class UpdateKeyDialog extends Dialog {

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
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.currentAlias = currentAlias != null ? currentAlias : "";
        this.currentDesc = currentDesc != null ? currentDesc : "";
        this.currentTags = currentTags != null ? currentTags : new ArrayList<>();
        this.currentRotationEnabled = currentRotationEnabled != null ? currentRotationEnabled : false;
        this.currentRotationPeriodInDays = currentRotationPeriodInDays;

        setHeaderTitle("Edit key alias, description & tags");
        setWidth("700px");
        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);

        buildForm();
        buildFooter();

        add(createFormLayout());
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
        Button addTagButton = new Button("Add tag", new Icon(VaadinIcon.PLUS));
        addTagButton.addClickListener(e -> parentView.addTagRow(tagsContainer, tagRows, null, null));
        HorizontalLayout tagsHeader = new HorizontalLayout(new Span("Tags"), addTagButton);
        tagsHeader.setAlignItems(FlexComponent.Alignment.BASELINE);
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.add(aliasCombo, newAliasField, descriptionField,
                rotationEnabledCheckbox, rotationPeriodField,
                new HorizontalLayout(new Span("Tags"), tagsContainer));
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }

    private void buildFooter() {
        Button saveBtn = new Button("Save", e -> onSave());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> close());

        HorizontalLayout footerLayout = new HorizontalLayout(cancelBtn, saveBtn);
        footerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        getFooter().add(footerLayout);
    }

    private void onSave() {
        String newAlias = null;
        if (newAliasField.isVisible() && !newAliasField.getValue().isBlank()) {
            newAlias = newAliasField.getValue();
        } else if (aliasCombo.getValue() != null && !aliasCombo.getValue().isBlank()) {
            newAlias = aliasCombo.getValue();
        }
        String newDescription = descriptionField.getValue();

        boolean newRotationEnabled = rotationEnabledCheckbox.getValue();
        Integer newRotationPeriod = rotationEnabledCheckbox.getValue() ? rotationPeriodField.getValue() : null;

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

        try {
            // Update description if changed
            if (!newDescription.equals(currentDesc)) {
                UpdateKeyDescriptionRequest descRequest = UpdateKeyDescriptionRequest.builder()
                        .keyId(keyId)
                        .description(newDescription)
                        .build();
                kmsApiService.updateKeyDescription(keyId, descRequest);
            }

            // Update alias if changed
            if (newAlias != null && !newAlias.equals(currentAlias)) {
                if (parentView.existingAliases.contains(newAlias)) {
                    UpdateAliasRequest aliasRequest = UpdateAliasRequest.builder()
                            .aliasName(newAlias)
                            .targetKeyId(keyId)
                            .build();
                    kmsApiService.updateAlias(newAlias, aliasRequest);
                } else {
                    CreateAliasRequest createAliasRequest = CreateAliasRequest.builder()
                            .aliasName(newAlias)
                            .targetKeyId(keyId)
                            .build();
                    kmsApiService.createAlias(createAliasRequest);
                }
            }

            // Update rotation settings if changed
            boolean rotationChanged = newRotationEnabled != currentRotationEnabled;
            boolean periodChanged = (newRotationPeriod != null && !newRotationPeriod.equals(currentRotationPeriodInDays)) ||
                    (newRotationPeriod == null && currentRotationPeriodInDays != null);
            if (rotationChanged || periodChanged) {
                UpdateKeyRotationRequest rotationRequest = UpdateKeyRotationRequest.builder()
                        .enableRotation(newRotationEnabled)
                        .rotationPeriodInDays(newRotationPeriod)
                        .applyImmediately(true)
                        .build();
                kmsApiService.updateKeyRotation(keyId, rotationRequest);
            }

            // Update tags: remove all existing, add new ones
            if (!currentTags.isEmpty()) {
                List<String> keysToRemove = currentTags.stream()
                        .map(ListResourceTagsResponse.Tag::getTagKey)
                        .collect(Collectors.toList());
                UntagResourceRequest untagRequest = UntagResourceRequest.builder()
                        .keyId(keyId)
                        .tagKeys(keysToRemove)
                        .build();
                kmsApiService.untagResource(keyId, untagRequest);
            }
            if (!newTags.isEmpty()) {
                List<ListResourceTagsResponse.Tag> tagList = newTags.stream()
                        .map(t -> ListResourceTagsResponse.Tag.builder()
                                .tagKey(t.getTagKey())
                                .tagValue(t.getTagValue())
                                .build())
                        .collect(Collectors.toList());
                TagResourceRequest tagRequest = TagResourceRequest.builder()
                        .keyId(keyId)
                        .tags(tagList)
                        .build();
                kmsApiService.tagResource(keyId, tagRequest);
            }

            close();
            Notification.show("Key updated successfully", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            parentView.loadAliases();
            parentView.loadKeys();

        } catch (Exception ex) {
            Notification.show("Update failed: " + ex.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}