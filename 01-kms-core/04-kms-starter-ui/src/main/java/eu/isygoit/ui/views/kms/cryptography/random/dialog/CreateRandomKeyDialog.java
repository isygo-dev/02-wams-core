package eu.isygoit.ui.views.kms.cryptography.random.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.views.common.dialog.BaseActionDialog;
import org.springframework.http.ResponseEntity;

public class CreateRandomKeyDialog extends BaseActionDialog {

    private final RandomKeyService keyService;
    private final Runnable onSuccess;

    private TextField nameField;
    private IntegerField lengthField;
    private ComboBox<IEnumCharSet.Types> charSetCombo;

    public CreateRandomKeyDialog(RandomKeyService keyService, Runnable onSuccess) {
        super("Create Random Key", onSuccess);
        this.keyService = keyService;
        this.onSuccess = onSuccess;
        setOkButtonText("Create");
        setWidth("550px");
        buildForm();
        add(createFormLayout());
    }

    private void buildForm() {
        nameField = new TextField("Key name");
        nameField.setRequired(true);
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder("e.g., my-api-secret");

        lengthField = new IntegerField("Length (bytes)");
        lengthField.setValue(32);
        lengthField.setMin(1);
        lengthField.setMax(4096);
        lengthField.setStepButtonsVisible(true);
        lengthField.setHelperText("Number of random bytes to generate");

        charSetCombo = new ComboBox<>("Character set");
        charSetCombo.setItems(IEnumCharSet.Types.values());
        charSetCombo.setValue(IEnumCharSet.Types.ALL);
        charSetCombo.setRequired(true);
        charSetCombo.setHelperText("Allowed characters for the random string");
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
            append("Key name is required");
            return false;
        }
        Integer length = lengthField.getValue();
        if (length == null || length <= 0) {
            append("Length must be positive");
            return false;
        }
        IEnumCharSet.Types charSet = charSetCombo.getValue();
        if (charSet == null) {
            append("Character set is required");
            return false;
        }

        try {
            ResponseEntity<String> response = keyService.renewRandomKey(name, length, charSet);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Random key created", 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                return true;
            } else {
                append("Creation failed: " + response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            append("Error: " + e.getMessage());
            return false;
        }
    }
}