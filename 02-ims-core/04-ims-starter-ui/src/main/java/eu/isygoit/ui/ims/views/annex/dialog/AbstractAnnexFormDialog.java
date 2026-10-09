package eu.isygoit.ui.ims.views.annex.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.annex.AnnexManagementView;
import eu.isygoit.ui.ims.views.common.ImsActionDialog;
import eu.isygoit.ui.ims.views.common.ImsDialogSupport;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import feign.FeignException;

/**
 * Shared form of the annex create/update dialogs: every editable {@link AnnexDto}
 * field is declared here once. {@code id} is never shown. Subclasses only decide
 * how the DTO is persisted and whether the tenant can be edited.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
abstract class AbstractAnnexFormDialog extends ImsActionDialog {

    final AnnexManagementView parentView;
    final AnnexService annexService;
    private final String messagePrefix;

    TextField tenantField;
    TextField tableCodeField;
    ComboBox<IEnumLanguage.Types> languageCombo;
    TextField valueField;
    TextField referenceField;
    IntegerField orderField;
    TextArea descriptionArea;

    AbstractAnnexFormDialog(String title,
                            Runnable onSuccess,
                            String messagePrefix,
                            AnnexManagementView parentView,
                            AnnexService annexService) {
        super(title, onSuccess);
        this.messagePrefix = messagePrefix;
        this.parentView = parentView;
        this.annexService = annexService;
    }

    /** The DTO that receives the form values on save. */
    abstract AnnexDto target();

    /** Persists the DTO; returns false after appending an error message. */
    abstract boolean persist(AnnexDto dto);

    /** True when the tenant cannot be changed (update). */
    abstract boolean isTenantReadOnly();

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_M);

        VerticalLayout root = DialogLayout.stack();
        root.add(buildIdentificationSection(), buildDescriptionSection());
        add(root);
    }

    private VerticalLayout buildIdentificationSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.annex.dialog.section.identification"), VaadinIcon.TAGS);
        FormLayout form = DialogLayout.responsiveForm();

        tenantField = new TextField(I18n.t("ims.annex.dialog.field.tenant"));
        tenantField.setPlaceholder(I18n.t("ims.annex.dialog.field.tenant.placeholder"));
        tenantField.setReadOnly(isTenantReadOnly());
        tenantField.setWidthFull();

        tableCodeField = new TextField(I18n.t("ims.annex.dialog.field.table.code"));
        tableCodeField.setRequiredIndicatorVisible(true);
        tableCodeField.setPlaceholder(I18n.t("ims.annex.dialog.field.table.code.placeholder"));
        tableCodeField.setWidthFull();

        languageCombo = new ComboBox<>(I18n.t("ims.annex.dialog.field.language"));
        languageCombo.setItems(IEnumLanguage.Types.values());
        languageCombo.setItemLabelGenerator(language -> ImsEnumTag.label(language, "ims.enum.language"));
        languageCombo.setRenderer(ImsEnumTag.renderer("ims.enum.language"));
        languageCombo.setRequiredIndicatorVisible(true);
        languageCombo.setPlaceholder(I18n.t("ims.annex.dialog.field.language.placeholder"));
        languageCombo.setWidthFull();

        valueField = new TextField(I18n.t("ims.annex.dialog.field.value"));
        valueField.setRequiredIndicatorVisible(true);
        valueField.setPlaceholder(I18n.t("ims.annex.dialog.field.value.placeholder"));
        valueField.setWidthFull();

        referenceField = new TextField(I18n.t("ims.annex.dialog.field.reference"));
        referenceField.setPlaceholder(I18n.t("ims.annex.dialog.field.reference.placeholder"));
        referenceField.setWidthFull();

        orderField = new IntegerField(I18n.t("ims.annex.dialog.field.order"));
        orderField.setPlaceholder(I18n.t("ims.annex.dialog.field.order.placeholder"));
        orderField.setWidthFull();

        form.add(tenantField, tableCodeField, languageCombo, valueField, referenceField, orderField);
        section.add(form);
        return section;
    }

    private VerticalLayout buildDescriptionSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.annex.dialog.section.description"), VaadinIcon.FILE_TEXT);

        descriptionArea = DialogLayout.tall(new TextArea(I18n.t("ims.annex.dialog.field.description")));
        descriptionArea.setPlaceholder(I18n.t("ims.annex.dialog.field.description.placeholder"));

        section.add(descriptionArea);
        return section;
    }

    /** Fills the form from an existing annex. */
    final void fillFrom(AnnexDto dto) {
        tenantField.setValue(dto.getTenant() != null ? dto.getTenant() : "");
        tableCodeField.setValue(dto.getTableCode() != null ? dto.getTableCode() : "");
        languageCombo.setValue(dto.getLanguage());
        valueField.setValue(dto.getValue() != null ? dto.getValue() : "");
        referenceField.setValue(dto.getReference() != null ? dto.getReference() : "");
        orderField.setValue(dto.getAnnexOrder());
        descriptionArea.setValue(dto.getDescription() != null ? dto.getDescription() : "");
    }

    /** Copies the form values into the DTO ({@code id} is left untouched). */
    private void applyTo(AnnexDto dto) {
        if (!tenantField.isReadOnly()) {
            dto.setTenant(tenantField.getValue().isBlank() ? null : tenantField.getValue());
        }
        dto.setTableCode(tableCodeField.getValue());
        dto.setLanguage(languageCombo.getValue());
        dto.setValue(valueField.getValue());
        dto.setReference(referenceField.getValue());
        dto.setAnnexOrder(orderField.getValue());
        dto.setDescription(descriptionArea.getValue());
    }

    private boolean isValid() {
        if (tableCodeField.getValue().isBlank()) {
            append(I18n.t("ims.annex.dialog.field.table.code.required"));
            return false;
        }
        if (languageCombo.getValue() == null) {
            append(I18n.t("ims.annex.dialog.field.language.required"));
            return false;
        }
        if (valueField.getValue().isBlank()) {
            append(I18n.t("ims.annex.dialog.field.value.required"));
            return false;
        }
        return true;
    }

    @Override
    protected final boolean onOk() {
        if (!isValid()) {
            return false;
        }

        parentView.showLoading(true);
        try {
            AnnexDto dto = target();
            applyTo(dto);
            if (!persist(dto)) {
                return false;
            }
            append(I18n.t(messagePrefix + ".success"));
            return true;
        } catch (FeignException ex) {
            append(ImsDialogSupport.extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t(messagePrefix + ".error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}
