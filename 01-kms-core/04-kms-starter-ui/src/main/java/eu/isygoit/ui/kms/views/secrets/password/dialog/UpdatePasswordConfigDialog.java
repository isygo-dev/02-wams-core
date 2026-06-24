package eu.isygoit.ui.kms.views.secrets.password.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PasswordConfigService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class UpdatePasswordConfigDialog extends BaseActionDialog {

    private final PasswordConfigService configService;
    private final PasswordConfigDto original;

    private TextField codeField;
    private ComboBox<IEnumAuth.Types> typeCombo;
    private TextField patternField;
    private ComboBox<IEnumCharSet.Types> charSetCombo;
    private TextField initialField;
    private IntegerField minLengthField;
    private IntegerField maxLengthField;
    private IntegerField lifeTimeField;

    public UpdatePasswordConfigDialog(PasswordConfigService configService, PasswordConfigDto dto, Runnable onSuccess) {
        super(I18n.t("password.dialog.update.title"), onSuccess);
        this.configService = configService;
        this.original = dto;
        setOkButtonText(I18n.t("password.dialog.update.button"));
        setWidth("600px");
        buildForm();
        addContent(createFormLayout());
        bindData();
    }

    private void buildForm() {
        codeField = new TextField(I18n.t("password.dialog.field.code"));
        codeField.setReadOnly(true);
        codeField.setWidthFull();

        typeCombo = new ComboBox<>(I18n.t("password.dialog.field.type"));
        typeCombo.setItems(IEnumAuth.Types.values());
        typeCombo.setRequired(true);
        typeCombo.setWidthFull();

        patternField = new TextField(I18n.t("password.dialog.field.pattern"));
        patternField.setWidthFull();

        charSetCombo = new ComboBox<>(I18n.t("password.dialog.field.char.set"));
        charSetCombo.setItems(IEnumCharSet.Types.values());
        charSetCombo.setRequired(true);
        charSetCombo.setWidthFull();

        initialField = new TextField(I18n.t("password.dialog.field.initial.value"));
        initialField.setWidthFull();

        minLengthField = new IntegerField(I18n.t("password.dialog.field.min.length"));
        minLengthField.setMin(1);
        minLengthField.setRequired(true);
        minLengthField.setWidthFull();

        maxLengthField = new IntegerField(I18n.t("password.dialog.field.max.length"));
        maxLengthField.setMin(1);
        maxLengthField.setRequired(true);
        maxLengthField.setWidthFull();

        lifeTimeField = new IntegerField(I18n.t("password.dialog.field.lifetime"));
        lifeTimeField.setMin(1);
        lifeTimeField.setRequired(true);
        lifeTimeField.setWidthFull();
    }

    private void bindData() {
        codeField.setValue(original.getCode());
        typeCombo.setValue(original.getType());
        patternField.setValue(original.getPattern());
        charSetCombo.setValue(original.getCharSetType());
        initialField.setValue(original.getInitial());
        minLengthField.setValue(original.getMinLength());
        maxLengthField.setValue(original.getMaxLength());
        lifeTimeField.setValue(original.getLifeTime());
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(codeField, typeCombo, patternField, charSetCombo, initialField,
                minLengthField, maxLengthField, lifeTimeField);
        return form;
    }

    @Override
    protected boolean onOk() {
        IEnumAuth.Types type = typeCombo.getValue();
        if (type == null) {
            append(I18n.t("password.dialog.field.type.required"));
            return false;
        }
        IEnumCharSet.Types charSet = charSetCombo.getValue();
        if (charSet == null) {
            append(I18n.t("password.dialog.field.char.set.required"));
            return false;
        }
        Integer minLen = minLengthField.getValue();
        if (minLen == null || minLen < 1) {
            append(I18n.t("password.dialog.field.min.length.required"));
            return false;
        }
        Integer maxLen = maxLengthField.getValue();
        if (maxLen == null || maxLen < minLen) {
            append(I18n.t("password.dialog.field.max.length.required"));
            return false;
        }
        Integer lifeTime = lifeTimeField.getValue();
        if (lifeTime == null || lifeTime < 1) {
            append(I18n.t("password.dialog.field.lifetime.required"));
            return false;
        }

        PasswordConfigDto updated = PasswordConfigDto.builder()
                .id(original.getId())
                .code(original.getCode())
                .type(type)
                .pattern(patternField.getValue())
                .charSetType(charSet)
                .initial(initialField.getValue())
                .minLength(minLen)
                .maxLength(maxLen)
                .lifeTime(lifeTime)
                .build();

        try {
            ResponseEntity<PasswordConfigDto> response = configService.update(original.getId(), updated);
            if (response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("password.dialog.update.success"));
                return true;
            } else {
                append(I18n.t("password.dialog.update.failed.status", response.getStatusCode()));
                return false;
            }
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
            return false;
        } catch (Exception ex) {
            append(I18n.t("password.dialog.update.failed", ex.getMessage()));
            return false;
        }
    }
}