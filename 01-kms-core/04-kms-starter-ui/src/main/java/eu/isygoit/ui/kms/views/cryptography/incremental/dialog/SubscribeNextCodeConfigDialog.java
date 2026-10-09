package eu.isygoit.ui.kms.views.cryptography.incremental.dialog;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsActionDialog;
import org.springframework.http.ResponseEntity;

public class SubscribeNextCodeConfigDialog extends KmsActionDialog {

    private final KmsAppNextCodeService nextCodeService;

    private TextField entityField;
    private TextField attributeField;
    private TextField prefixField;
    private TextField suffixField;
    private IntegerField valueLengthField;
    private IntegerField incrementField;
    private IntegerField startValueField;
    private TextField codePreviewField;

    public SubscribeNextCodeConfigDialog(KmsAppNextCodeService nextCodeService, Runnable onSuccess) {
        super(I18n.t("kms.subscribe.dialog.title"), onSuccess);
        this.nextCodeService = nextCodeService;
        setOkButtonText(I18n.t("kms.subscribe.dialog.button"));
        DialogLayout.size(this, DialogLayout.WIDTH_M);
        buildForm();
        addContent(createFormLayout());
    }

    private void buildForm() {
        entityField = new TextField(I18n.t("kms.subscribe.dialog.field.entity"));
        entityField.setRequired(true);
        entityField.setWidthFull();
        entityField.setRequiredIndicatorVisible(true);
        entityField.setPlaceholder(I18n.t("kms.subscribe.dialog.field.entity.placeholder"));

        attributeField = new TextField(I18n.t("kms.subscribe.dialog.field.attribute"));
        attributeField.setRequired(true);
        attributeField.setRequiredIndicatorVisible(true);
        attributeField.setPlaceholder(I18n.t("kms.subscribe.dialog.field.attribute.placeholder"));

        prefixField = new TextField(I18n.t("kms.subscribe.dialog.field.prefix"));
        prefixField.setPlaceholder(I18n.t("kms.subscribe.dialog.field.prefix.placeholder"));

        suffixField = new TextField(I18n.t("kms.subscribe.dialog.field.suffix"));
        suffixField.setPlaceholder(I18n.t("kms.subscribe.dialog.field.suffix.placeholder"));

        valueLengthField = new IntegerField(I18n.t("kms.subscribe.dialog.field.value.length"));
        valueLengthField.setValue(6);
        valueLengthField.setStepButtonsVisible(true);
        valueLengthField.setMin(1);
        valueLengthField.setMax(20);
        valueLengthField.setHelperText(I18n.t("kms.subscribe.dialog.field.value.length.helper"));

        incrementField = new IntegerField(I18n.t("kms.subscribe.dialog.field.increment"));
        incrementField.setValue(1);
        incrementField.setStepButtonsVisible(true);
        incrementField.setMin(1);
        incrementField.setMax(1000);
        incrementField.setHelperText(I18n.t("kms.subscribe.dialog.field.increment.helper"));

        startValueField = new IntegerField(I18n.t("kms.subscribe.dialog.field.start.value"));
        startValueField.setValue(0);
        startValueField.setStepButtonsVisible(true);
        startValueField.setMin(0);
        startValueField.setHelperText(I18n.t("kms.subscribe.dialog.field.start.value.helper"));

        // NextCodeDto.code is computed: shown read-only as a live preview
        codePreviewField = new TextField(I18n.t("kms.nextcode.card.next.code"));
        codePreviewField.setReadOnly(true);
        prefixField.addValueChangeListener(e -> updateCodePreview());
        suffixField.addValueChangeListener(e -> updateCodePreview());
        valueLengthField.addValueChangeListener(e -> updateCodePreview());
        startValueField.addValueChangeListener(e -> updateCodePreview());
        updateCodePreview();
    }

    private FormLayout createFormLayout() {
        FormLayout form = DialogLayout.responsiveForm();
        form.add(entityField, attributeField,
                prefixField, suffixField,
                valueLengthField, incrementField,
                startValueField, codePreviewField);
        return form;
    }

    /** Read-only code computed from the format fields; never sent (the server assigns it). */
    private void updateCodePreview() {
        NextCodeDto preview = NextCodeDto.builder()
                .prefix(prefixField.getValue())
                .suffix(suffixField.getValue())
                .valueLength(valueLengthField.getValue() != null ? valueLengthField.getValue().longValue() : 6L)
                .codeValue(startValueField.getValue() != null ? startValueField.getValue().longValue() : 0L)
                .build();
        codePreviewField.setValue(preview.getCode());
    }

    @Override
    protected boolean onOk() {
        String entity = entityField.getValue();
        String attribute = attributeField.getValue();

        if (entity == null || entity.isBlank() || attribute == null || attribute.isBlank()) {
            append(I18n.t("kms.subscribe.dialog.entity.attribute.required"));
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
                append(I18n.t("kms.subscribe.dialog.success"));
                return true;
            } else {
                append(I18n.t("kms.subscribe.dialog.failed", response.getStatusCode()));
                return false;
            }
        } catch (Exception e) {
            append(I18n.t("kms.subscribe.dialog.error", e.getMessage()));
            return false;
        }
    }
}