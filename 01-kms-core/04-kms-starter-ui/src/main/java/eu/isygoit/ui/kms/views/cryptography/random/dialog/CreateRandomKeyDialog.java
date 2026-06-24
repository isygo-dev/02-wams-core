package eu.isygoit.ui.kms.views.cryptography.random.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import org.springframework.http.ResponseEntity;

public class CreateRandomKeyDialog extends BaseActionDialog {

    private final RandomKeyService keyService;
    private final Runnable onSuccess;

    private TextField nameField;
    private IntegerField lengthField;
    private ComboBox<IEnumCharSet.Types> charSetCombo;

    public CreateRandomKeyDialog(RandomKeyService keyService, Runnable onSuccess) {
        super(I18n.t("random.key.dialog.create.title"), onSuccess);
        this.keyService = keyService;
        this.onSuccess = onSuccess;
        setOkButtonText(I18n.t("random.key.dialog.create.button"));
        setWidth("550px");
        buildForm();
        addContent(createFormLayout());
    }

    private void buildForm() {
        nameField = new TextField(I18n.t("random.key.dialog.field.name"));
        nameField.setRequired(true);
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder(I18n.t("random.key.dialog.field.name.placeholder"));
        nameField.setWidthFull();

        lengthField = new IntegerField(I18n.t("random.key.dialog.field.length"));
        lengthField.setValue(32);
        lengthField.setMin(1);
        lengthField.setMax(4096);
        lengthField.setStepButtonsVisible(true);
        lengthField.setHelperText(I18n.t("random.key.dialog.field.length.helper"));
        lengthField.setWidthFull();

        charSetCombo = new ComboBox<>(I18n.t("random.key.dialog.field.char.set"));
        charSetCombo.setItems(IEnumCharSet.Types.values());
        charSetCombo.setValue(IEnumCharSet.Types.ALL);
        charSetCombo.setRequired(true);
        charSetCombo.setHelperText(I18n.t("random.key.dialog.field.char.set.helper"));
        charSetCombo.setWidthFull();
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.add(nameField, lengthField, charSetCombo);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }

    @Override
    protected boolean onOk() {
        String name = nameField.getValue();
        if (name == null || name.isBlank()) {
            append(I18n.t("random.key.dialog.field.name.required"));
            return false;
        }
        Integer length = lengthField.getValue();
        if (length == null || length <= 0) {
            append(I18n.t("random.key.dialog.field.length.required"));
            return false;
        }
        IEnumCharSet.Types charSet = charSetCombo.getValue();
        if (charSet == null) {
            append(I18n.t("random.key.dialog.field.char.set.required"));
            return false;
        }

        try {
            ResponseEntity<String> response = keyService.renewRandomKey(name, length, charSet);
            if (response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("random.key.dialog.create.success"));
                return true;
            } else {
                append(I18n.t("random.key.dialog.create.failed", response.getStatusCode()));
                return false;
            }
        } catch (Exception e) {
            append(I18n.t("random.key.dialog.error", e.getMessage()));
            return false;
        }
    }
}