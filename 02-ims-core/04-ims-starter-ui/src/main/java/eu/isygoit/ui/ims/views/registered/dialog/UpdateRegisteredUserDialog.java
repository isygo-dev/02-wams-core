package eu.isygoit.ui.ims.views.registered.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.enums.IEnumAccountOrigin;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.ims.views.registered.RegisteredManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class UpdateRegisteredUserDialog extends BaseActionDialog {

    private final RegisteredManagementView parentView;
    private final RegisteredUserService registeredUserService;
    private final RegisteredUserDto registeredUser;
    private final Runnable onSuccess;

    private TextField firstNameField;
    private TextField lastNameField;
    private EmailField emailField;
    private TextField phoneField;
    private ComboBox<IEnumAccountOrigin.Types> originCombo;
    private TextField functionRoleField;

    public UpdateRegisteredUserDialog(RegisteredManagementView parentView,
                                      RegisteredUserService registeredUserService,
                                      RegisteredUserDto registeredUser,
                                      Runnable onSuccess) {
        super(I18n.t("ims.registered.dialog.update.title"), onSuccess);
        this.parentView = parentView;
        this.registeredUserService = registeredUserService;
        this.registeredUser = registeredUser;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("ims.registered.dialog.update.button"));
        setWidth("600px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
        populateFields();
    }

    private void buildForm() {
        firstNameField = new TextField(I18n.t("ims.registered.dialog.update.field.first.name"));
        firstNameField.setRequiredIndicatorVisible(true);
        firstNameField.setWidthFull();

        lastNameField = new TextField(I18n.t("ims.registered.dialog.update.field.last.name"));
        lastNameField.setRequiredIndicatorVisible(true);
        lastNameField.setWidthFull();

        emailField = new EmailField(I18n.t("ims.registered.dialog.update.field.email"));
        emailField.setReadOnly(true);
        emailField.setWidthFull();

        phoneField = new TextField(I18n.t("ims.registered.dialog.update.field.phone"));
        phoneField.setRequiredIndicatorVisible(true);
        phoneField.setWidthFull();

        originCombo = new ComboBox<>(I18n.t("ims.registered.dialog.update.field.origin"));
        originCombo.setItems(IEnumAccountOrigin.Types.values());
        originCombo.setWidthFull();

        functionRoleField = new TextField(I18n.t("ims.registered.dialog.update.field.function.role"));
        functionRoleField.setWidthFull();
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        form.add(firstNameField, lastNameField, emailField, phoneField, originCombo, functionRoleField);
        return form;
    }

    private void populateFields() {
        firstNameField.setValue(registeredUser.getFirstName() != null ? registeredUser.getFirstName() : "");
        lastNameField.setValue(registeredUser.getLastName() != null ? registeredUser.getLastName() : "");
        emailField.setValue(registeredUser.getEmail() != null ? registeredUser.getEmail() : "");
        phoneField.setValue(registeredUser.getPhoneNumber() != null ? registeredUser.getPhoneNumber() : "");
        if (registeredUser.getOrigin() != null) {
            try {
                originCombo.setValue(registeredUser.getOrigin());
            } catch (IllegalArgumentException ignored) {
            }
        }
        functionRoleField.setValue(registeredUser.getFunctionRole() != null ? registeredUser.getFunctionRole() : "");
    }

    @Override
    protected boolean onOk() {
        if (firstNameField.getValue().isBlank()) {
            append(I18n.t("ims.registered.dialog.update.field.first.name.required"));
            return false;
        }
        if (lastNameField.getValue().isBlank()) {
            append(I18n.t("ims.registered.dialog.update.field.last.name.required"));
            return false;
        }
        if (phoneField.getValue().isBlank()) {
            append(I18n.t("ims.registered.dialog.update.field.phone.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            registeredUser.setFirstName(firstNameField.getValue());
            registeredUser.setLastName(lastNameField.getValue());
            registeredUser.setPhoneNumber(phoneField.getValue());
            registeredUser.setOrigin(originCombo.getValue() != null
                    ? originCombo.getValue()
                    : registeredUser.getOrigin());
            registeredUser.setFunctionRole(functionRoleField.getValue());

            ResponseEntity<RegisteredUserDto> response = registeredUserService.update(registeredUser.getId(), registeredUser);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("ims.registered.dialog.update.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("ims.registered.dialog.update.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.registered.dialog.update.error", e.getMessage()));
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
