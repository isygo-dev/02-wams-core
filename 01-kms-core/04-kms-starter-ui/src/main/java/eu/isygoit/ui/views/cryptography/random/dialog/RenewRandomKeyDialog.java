package eu.isygoit.ui.views.cryptography.random.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.views.common.dialog.PinBaseActionDialog;
import org.springframework.http.ResponseEntity;

public class RenewRandomKeyDialog extends PinBaseActionDialog {

    private final RandomKeyService keyService;
    private final String keyName;
    private Integer length;
    private IEnumCharSet.Types charSet;
    private IntegerField lengthField;
    private ComboBox<IEnumCharSet.Types> charSetCombo;

    public RenewRandomKeyDialog(RandomKeyService keyService, String keyName, Runnable onSuccess) {
        super("Renew Random Key",
                "Renewing will generate a completely new random value. The old value will be lost forever.",
                onSuccess, true);
        this.keyService = keyService;
        this.keyName = keyName;
        this.length = 32;      // default
        this.charSet = IEnumCharSet.Types.ALL;
        setOkButtonText("Renew");
        setWidth("500px");
        buildForm();
        add(createFormLayout());
    }

    private void buildForm() {
        lengthField = new IntegerField("Length (bytes)");
        lengthField.setValue(length);
        lengthField.setMin(1);
        lengthField.setMax(4096);
        lengthField.setStepButtonsVisible(true);
        lengthField.addValueChangeListener(e -> length = e.getValue());

        charSetCombo = new ComboBox<>("Character set");
        charSetCombo.setItems(IEnumCharSet.Types.values());
        charSetCombo.setValue(charSet);
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
        if (length == null || length <= 0) {
            append("Length must be positive");
            return false;
        }
        if (charSet == null) {
            append("Character set is required");
            return false;
        }
        try {
            ResponseEntity<String> response = keyService.renewRandomKey(keyName, length, charSet);
            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            } else {
                append("Renew failed: " + response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            append("Renew failed: " + e.getMessage());
            return false;
        }
    }
}