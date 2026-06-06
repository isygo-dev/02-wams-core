package eu.isygoit.ui.views.cryptography.key.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.CreateKeyRequest;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.common.dialog.BaseActionDialog;
import eu.isygoit.ui.views.cryptography.key.KeyManagementView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class KeyDialogBase extends BaseActionDialog {

    protected final KeyManagementView parentView;
    protected final KmsApiService kmsApiService;

    // Common UI fields
    protected TextField aliasField;
    protected TextArea descriptionField;
    protected Checkbox rotationEnabledCheckbox;
    protected IntegerField rotationPeriodField;
    protected VerticalLayout tagsContainer;
    protected List<HorizontalLayout> tagRows;

    protected KeyDialogBase(String title,
                            KeyManagementView parentView,
                            KmsApiService kmsApiService,
                            Runnable onSuccess) {
        super(title, onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        setWidth("700px");
    }

    /**
     * Subclasses must call this method in their constructor after setting up any specific fields.
     * It builds the common part of the form and adds it to the dialog.
     */
    protected void buildCommonForm() {
        // Alias field
        aliasField = new TextField("Alias (optional)");
        aliasField.setPlaceholder("alias:my-key");
        aliasField.setHelperText("Must start with 'alias:' if provided.");

        // Description
        descriptionField = new TextArea("Description");
        descriptionField.setMaxLength(500);
        descriptionField.setWidthFull();

        // Rotation settings
        rotationEnabledCheckbox = new Checkbox("Enable automatic rotation");
        rotationPeriodField = new IntegerField("Rotation period (days)");
        rotationPeriodField.setMin(90);
        rotationPeriodField.setMax(365);
        rotationPeriodField.setHelperText("Default 365 days, min 90, max 365");
        rotationPeriodField.setVisible(false);

        rotationEnabledCheckbox.addValueChangeListener(e -> {
            rotationPeriodField.setVisible(e.getValue());
            if (!e.getValue()) rotationPeriodField.clear();
        });

        // Tags container
        tagsContainer = new VerticalLayout();
        tagsContainer.setSpacing(true);
        tagsContainer.setPadding(false);
        tagRows = new ArrayList<>();

        // Add a default empty row (optional)
        addTagRow(null, null);
    }

    /**
     * Creates the full FormLayout with all common components.
     * Subclasses may override to add extra fields.
     */
    protected FormLayout createCommonFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

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
        return form;
    }

    /**
     * Adds a new tag row to the container, optionally prefilled with key/value.
     */
    protected void addTagRow(String existingKey, String existingValue) {
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

    /**
     * Collects tags from the current rows into a list of CreateKeyRequest.Tag.
     */
    protected List<CreateKeyRequest.Tag> getTagsFromRows() {
        List<CreateKeyRequest.Tag> tags = new ArrayList<>();
        for (HorizontalLayout row : tagRows) {
            TextField keyField = (TextField) row.getComponentAt(0);
            TextField valueField = (TextField) row.getComponentAt(1);
            if (valueField != null && !valueField.getValue().isBlank()) {
                tags.add(CreateKeyRequest.Tag.builder()
                        .tagKey(keyField.getValue())
                        .tagValue(valueField.getValue())
                        .build());
            }
        }
        return tags;
    }

    /**
     * Validates alias format and returns the alias (or null if empty).
     */
    protected String getAliasOrNull() {
        String alias = aliasField.getValue();
        if (alias != null && !alias.isBlank()) {
            if (!alias.startsWith("alias:")) {
                append("Alias must start with 'alias:' (e.g., alias:my-key)");
                return null;
            }
            return alias;
        }
        return null;
    }

    /**
     * Returns the description (or null if empty).
     */
    protected String getDescriptionOrNull() {
        String desc = descriptionField.getValue();
        return (desc != null && !desc.isBlank()) ? desc : null;
    }

    /**
     * Returns rotation period if rotation is enabled, else null.
     */
    protected Integer getRotationPeriodOrNull() {
        if (Boolean.TRUE.equals(rotationEnabledCheckbox.getValue())) {
            return rotationPeriodField.getValue();
        }
        return null;
    }

    /**
     * Abstract method to be implemented by subclasses for the save operation.
     */
    @Override
    protected abstract boolean onOk();

    /**
     * Subclasses may override this to set initial values for the common fields.
     */
    protected abstract void prefillData();
}