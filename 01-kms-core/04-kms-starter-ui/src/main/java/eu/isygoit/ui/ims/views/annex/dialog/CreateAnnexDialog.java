package eu.isygoit.ui.ims.views.annex.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.ims.views.annex.AnnexManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class CreateAnnexDialog extends BaseActionDialog {

    private final AnnexManagementView parentView;
    private final AnnexService annexService;
    private final Runnable onSuccess;

    private TextField tableCodeField;
    private ComboBox<IEnumLanguage.Types> languageCombo;
    private TextField valueField;
    private TextArea descriptionArea;
    private TextField referenceField;
    private IntegerField orderField;

    public CreateAnnexDialog(AnnexManagementView parentView,
                             AnnexService annexService,
                             Runnable onSuccess) {
        super(I18n.t("ims.annex.dialog.create.title"), onSuccess);
        this.parentView = parentView;
        this.annexService = annexService;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("ims.annex.dialog.create.button"));
        setWidth("600px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
    }

    private void buildForm() {
        tableCodeField = new TextField(I18n.t("ims.annex.dialog.field.table.code"));
        tableCodeField.setRequiredIndicatorVisible(true);
        tableCodeField.setPlaceholder(I18n.t("ims.annex.dialog.field.table.code.placeholder"));
        tableCodeField.setWidthFull();

        languageCombo = new ComboBox<>(I18n.t("ims.annex.dialog.field.language"));
        languageCombo.setItems(IEnumLanguage.Types.values());
        languageCombo.setRequiredIndicatorVisible(true);
        languageCombo.setPlaceholder(I18n.t("ims.annex.dialog.field.language.placeholder"));
        languageCombo.setWidthFull();

        valueField = new TextField(I18n.t("ims.annex.dialog.field.value"));
        valueField.setRequiredIndicatorVisible(true);
        valueField.setPlaceholder(I18n.t("ims.annex.dialog.field.value.placeholder"));
        valueField.setWidthFull();

        descriptionArea = new TextArea(I18n.t("ims.annex.dialog.field.description"));
        descriptionArea.setPlaceholder(I18n.t("ims.annex.dialog.field.description.placeholder"));
        descriptionArea.setWidthFull();

        referenceField = new TextField(I18n.t("ims.annex.dialog.field.reference"));
        referenceField.setPlaceholder(I18n.t("ims.annex.dialog.field.reference.placeholder"));
        referenceField.setWidthFull();

        orderField = new IntegerField(I18n.t("ims.annex.dialog.field.order"));
        orderField.setPlaceholder(I18n.t("ims.annex.dialog.field.order.placeholder"));
        orderField.setWidthFull();
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        form.add(tableCodeField, languageCombo, valueField,
                descriptionArea, referenceField, orderField);
        form.setColspan(descriptionArea, 2);
        return form;
    }

    @Override
    protected boolean onOk() {
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

        parentView.showLoading(true);
        try {
            AnnexDto newAnnex = AnnexDto.builder()
                    .tableCode(tableCodeField.getValue())
                    .language(languageCombo.getValue())
                    .value(valueField.getValue())
                    .description(descriptionArea.getValue())
                    .reference(referenceField.getValue())
                    .annexOrder(orderField.getValue())
                    .build();

            ResponseEntity<AnnexDto> response = annexService.create(newAnnex);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("ims.annex.dialog.create.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("ims.annex.dialog.create.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.annex.dialog.create.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}