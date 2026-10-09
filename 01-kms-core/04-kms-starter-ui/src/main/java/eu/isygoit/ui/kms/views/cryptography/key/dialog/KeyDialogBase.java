package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.CreateKeyRequest;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsActionDialog;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared form parts of the create/update key dialogs. The fields common to
 * {@code CreateKeyRequest} and {@code UpdateKeyDescriptionRequest} (alias,
 * description, rotation, tags) are declared here once; subclasses decide how
 * the sections are assembled and how the request is sent.
 */
public abstract class KeyDialogBase extends KmsActionDialog {

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
        DialogLayout.size(this, DialogLayout.WIDTH_M);
    }

    /**
     * Subclasses must call this method in their constructor after setting up any specific fields.
     * It creates the common fields; the sections are assembled by {@link #addSections(Component...)}.
     */
    protected void buildCommonForm() {
        // Alias field
        aliasField = new TextField(I18n.t("kms.key.dialog.base.field.alias"));
        aliasField.setPlaceholder(I18n.t("kms.key.dialog.base.field.alias.placeholder"));
        aliasField.setHelperText(I18n.t("kms.key.dialog.base.field.alias.helper"));
        aliasField.setWidthFull();

        // Description
        descriptionField = DialogLayout.tall(new TextArea(I18n.t("kms.key.dialog.base.field.description")));
        descriptionField.setMaxLength(500);

        // Rotation settings
        rotationEnabledCheckbox = new Checkbox(I18n.t("kms.key.dialog.base.field.rotation.enabled"));
        rotationPeriodField = new IntegerField(I18n.t("kms.key.dialog.base.field.rotation.period"));
        rotationPeriodField.setMin(90);
        rotationPeriodField.setMax(365);
        rotationPeriodField.setHelperText(I18n.t("kms.key.dialog.base.field.rotation.period.helper"));
        rotationPeriodField.setWidthFull();
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

    /** Stacks the given sections in the dialog body. */
    protected final void addSections(Component... sections) {
        VerticalLayout root = DialogLayout.stack();
        root.add(sections);
        add(root);
    }

    /**
     * Identity section: optional leading read-only fields, then alias and description.
     */
    protected final VerticalLayout buildIdentitySection(Component... leadingFields) {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.key.dialog.describe.section.identity"), VaadinIcon.KEY);
        FormLayout form = DialogLayout.responsiveForm();
        form.add(leadingFields);
        form.add(aliasField, descriptionField);
        form.setColspan(descriptionField, 2);
        section.add(form);
        return section;
    }

    /** Rotation section: enable checkbox and (conditional) period. */
    protected final VerticalLayout buildRotationSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.key.dialog.base.section.rotation"), VaadinIcon.REFRESH);
        FormLayout form = DialogLayout.responsiveForm();
        form.add(rotationEnabledCheckbox, rotationPeriodField);
        section.add(form);
        return section;
    }

    /** Tags section: a single add button above the editable tag rows. */
    protected final VerticalLayout buildTagsSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.key.dialog.base.field.tags"), VaadinIcon.TAGS);
        Button addTagButton = new Button(I18n.t("kms.key.dialog.base.field.add.tag"), new Icon(VaadinIcon.PLUS));
        addTagButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        addTagButton.addClickListener(e -> addTagRow(null, null));
        section.add(addTagButton, tagsContainer);
        return section;
    }

    /**
     * Adds a new tag row to the container, optionally prefilled with key/value.
     */
    protected void addTagRow(String existingKey, String existingValue) {
        String randomKey = (existingKey != null) ? existingKey : "tag-" + UUID.randomUUID().toString().substring(0, 8);
        TextField keyField = new TextField();
        keyField.setAriaLabel(I18n.t("kms.tag.dialog.field.tag.key"));
        keyField.setValue(randomKey);
        keyField.setReadOnly(true);
        TextField valueField = new TextField();
        valueField.setAriaLabel(I18n.t("kms.tag.dialog.field.tag.value"));
        valueField.setValue(existingValue != null ? existingValue : "");
        valueField.setPlaceholder(I18n.t("kms.key.dialog.base.field.tag.value.placeholder"));
        Button removeBtn = new Button(new Icon(VaadinIcon.TRASH));
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        removeBtn.setAriaLabel(I18n.t("kms.tags.view.remove.tag"));
        HorizontalLayout row = new HorizontalLayout(keyField, valueField, removeBtn);
        row.setWidthFull();
        row.setFlexGrow(1, keyField);
        row.setFlexGrow(2, valueField);
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
                append(I18n.t("kms.key.dialog.base.alias.format"));
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
