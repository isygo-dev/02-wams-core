package eu.isygoit.ui.kms.views.secrets.password.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PasswordConfigService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsActionDialog;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;
import eu.isygoit.ui.kms.views.secrets.SecretsDialogSupport;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Shared form of the password configuration create/update dialogs: every
 * {@link PasswordConfigDto} field is declared here once. {@code id} is never
 * shown; {@code code} and {@code tenant} are read-only (assigned by the
 * server). Subclasses only decide how the DTO is built and sent.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
abstract class AbstractPasswordConfigFormDialog extends KmsActionDialog {

    final PasswordConfigService configService;
    private final String successKey;
    private final String statusFailedKey;
    private final String errorKey;

    TextField codeField;
    TextField tenantField;
    ComboBox<IEnumAuth.Types> typeCombo;
    TextField patternField;
    ComboBox<IEnumCharSet.Types> charSetCombo;
    TextField initialField;
    IntegerField minLengthField;
    IntegerField maxLengthField;
    IntegerField lifeTimeField;

    AbstractPasswordConfigFormDialog(String title, Runnable onSuccess, PasswordConfigService configService,
                                     String successKey, String statusFailedKey, String errorKey) {
        super(title, onSuccess);
        this.configService = configService;
        this.successKey = successKey;
        this.statusFailedKey = statusFailedKey;
        this.errorKey = errorKey;
    }

    /** The DTO that receives the form values on save. */
    abstract PasswordConfigDto target();

    /** Sends the DTO to the service. */
    abstract ResponseEntity<PasswordConfigDto> send(PasswordConfigDto dto);

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_M);

        VerticalLayout root = DialogLayout.stack();
        root.add(buildIdentitySection(), buildPolicySection());
        add(root);
    }

    private VerticalLayout buildIdentitySection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.password.details.section.identity"), VaadinIcon.TAG);
        FormLayout form = DialogLayout.responsiveForm();

        codeField = SecretsDialogSupport.readOnlyField(I18n.t("kms.password.dialog.field.code"));
        tenantField = SecretsDialogSupport.readOnlyField(I18n.t("kms.password.details.field.tenant"));

        form.add(codeField, tenantField);
        section.add(form);
        return section;
    }

    private VerticalLayout buildPolicySection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("kms.password.details.section.policy"), VaadinIcon.KEY);
        FormLayout form = DialogLayout.responsiveForm();

        typeCombo = new ComboBox<>(I18n.t("kms.password.dialog.field.type"));
        typeCombo.setItems(IEnumAuth.Types.values());
        KmsEnumTag.useTagRenderer(typeCombo, "kms.enum");
        typeCombo.setRequired(true);
        typeCombo.setRequiredIndicatorVisible(true);
        typeCombo.setWidthFull();

        charSetCombo = new ComboBox<>(I18n.t("kms.password.dialog.field.char.set"));
        charSetCombo.setItems(IEnumCharSet.Types.values());
        KmsEnumTag.useTagRenderer(charSetCombo, "kms.enum");
        charSetCombo.setRequired(true);
        charSetCombo.setRequiredIndicatorVisible(true);
        charSetCombo.setWidthFull();

        patternField = new TextField(I18n.t("kms.password.dialog.field.pattern"));
        patternField.setPlaceholder(I18n.t("kms.password.dialog.field.pattern.placeholder"));
        patternField.setWidthFull();

        initialField = new TextField(I18n.t("kms.password.dialog.field.initial.value"));
        initialField.setPlaceholder(I18n.t("kms.password.dialog.field.initial.placeholder"));
        initialField.setWidthFull();

        minLengthField = new IntegerField(I18n.t("kms.password.dialog.field.min.length"));
        minLengthField.setMin(1);
        minLengthField.setRequired(true);
        minLengthField.setRequiredIndicatorVisible(true);
        minLengthField.setWidthFull();

        maxLengthField = new IntegerField(I18n.t("kms.password.dialog.field.max.length"));
        maxLengthField.setMin(1);
        maxLengthField.setRequired(true);
        maxLengthField.setRequiredIndicatorVisible(true);
        maxLengthField.setWidthFull();

        lifeTimeField = new IntegerField(I18n.t("kms.password.dialog.field.lifetime"));
        lifeTimeField.setMin(1);
        lifeTimeField.setRequired(true);
        lifeTimeField.setRequiredIndicatorVisible(true);
        lifeTimeField.setWidthFull();

        form.add(typeCombo, charSetCombo, patternField, initialField,
                minLengthField, maxLengthField, lifeTimeField);
        section.add(form);
        return section;
    }

    /** Fills the form from an existing configuration. */
    final void fillFrom(PasswordConfigDto dto) {
        SecretsDialogSupport.setText(codeField, dto.getCode());
        SecretsDialogSupport.setText(tenantField, dto.getTenant());
        typeCombo.setValue(dto.getType());
        SecretsDialogSupport.setText(patternField, dto.getPattern());
        charSetCombo.setValue(dto.getCharSetType());
        SecretsDialogSupport.setText(initialField, dto.getInitial());
        minLengthField.setValue(dto.getMinLength());
        maxLengthField.setValue(dto.getMaxLength());
        lifeTimeField.setValue(dto.getLifeTime());
    }

    /**
     * Copies the editable form values into the DTO; {@code id}, {@code code},
     * {@code tenant} and the audit fields are left untouched.
     */
    private void applyTo(PasswordConfigDto dto) {
        dto.setType(typeCombo.getValue());
        dto.setPattern(patternField.getValue());
        dto.setCharSetType(charSetCombo.getValue());
        dto.setInitial(initialField.getValue());
        dto.setMinLength(minLengthField.getValue());
        dto.setMaxLength(maxLengthField.getValue());
        dto.setLifeTime(lifeTimeField.getValue());
    }

    private boolean isValid() {
        if (typeCombo.getValue() == null) {
            append(I18n.t("kms.password.dialog.field.type.required"));
            return false;
        }
        if (charSetCombo.getValue() == null) {
            append(I18n.t("kms.password.dialog.field.char.set.required"));
            return false;
        }
        Integer minLen = minLengthField.getValue();
        if (minLen == null || minLen < 1) {
            append(I18n.t("kms.password.dialog.field.min.length.required"));
            return false;
        }
        Integer maxLen = maxLengthField.getValue();
        if (maxLen == null || maxLen < minLen) {
            append(I18n.t("kms.password.dialog.field.max.length.required"));
            return false;
        }
        Integer lifeTime = lifeTimeField.getValue();
        if (lifeTime == null || lifeTime < 1) {
            append(I18n.t("kms.password.dialog.field.lifetime.required"));
            return false;
        }
        return true;
    }

    @Override
    protected final boolean onOk() {
        if (!isValid()) {
            return false;
        }

        PasswordConfigDto dto = target();
        applyTo(dto);

        try {
            ResponseEntity<PasswordConfigDto> response = send(dto);
            if (response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t(successKey));
                return true;
            }
            append(I18n.t(statusFailedKey, response.getStatusCode()));
            return false;
        } catch (FeignException ex) {
            append(SecretsDialogSupport.feignMessage(ex));
            return false;
        } catch (Exception ex) {
            append(I18n.t(errorKey, ex.getMessage()));
            return false;
        }
    }
}
