package eu.isygoit.ui.kms.views.cryptography.random.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import org.springframework.http.ResponseEntity;

public class RenewRandomKeyDialog extends PinBaseActionDialog {

    private final RandomKeyService keyService;
    private final String keyName;
    private Integer length;
    private IEnumCharSet.Types charSet;
    private IntegerField lengthField;
    private ComboBox<IEnumCharSet.Types> charSetCombo;

    public RenewRandomKeyDialog(RandomKeyService keyService, String keyName, Runnable onSuccess) {
        super(I18n.t("kms.random.key.dialog.renew.title"),
                I18n.t("kms.random.key.dialog.renew.message"),
                onSuccess, true);
        this.keyService = keyService;
        this.keyName = keyName;
        this.length = 32;      // default
        this.charSet = IEnumCharSet.Types.ALL;
        setOkButtonText(I18n.t("kms.random.key.dialog.renew.button"));
        setWidth("500px");
        buildForm();
        addContent(createFormLayout());
    }

    private void buildForm() {
        lengthField = new IntegerField(I18n.t("kms.random.key.dialog.field.length"));
        lengthField.setValue(length);
        lengthField.setMin(1);
        lengthField.setMax(4096);
        lengthField.setStepButtonsVisible(true);
        lengthField.setWidthFull();
        lengthField.addValueChangeListener(e -> length = e.getValue());

        charSetCombo = new ComboBox<>(I18n.t("kms.random.key.dialog.field.char.set"));
        charSetCombo.setItems(IEnumCharSet.Types.values());
        charSetCombo.setValue(charSet);
        charSetCombo.setWidthFull();
        charSetCombo.addValueChangeListener(e -> charSet = e.getValue());
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.add(lengthField, charSetCombo);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("kms.random.key.dialog.renew.invalid.code"));
            return false;
        }

        if (length == null || length <= 0) {
            append(I18n.t("kms.random.key.dialog.renew.length.required"));
            return false;
        }
        if (charSet == null) {
            append(I18n.t("kms.random.key.dialog.renew.char.set.required"));
            return false;
        }

        try {
            ResponseEntity<String> response = keyService.renewRandomKey(keyName, length, charSet);
            if (response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("kms.random.key.dialog.renew.success"));
                return true;
            } else {
                append(I18n.t("kms.random.key.dialog.renew.failed", response.getStatusCode()));
                return false;
            }
        } catch (Exception e) {
            append(I18n.t("kms.random.key.dialog.renew.failed", e.getMessage()));
            return false;
        }
    }
}