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

public class CreateRegisteredUserDialog extends BaseActionDialog {

    private final RegisteredManagementView parentView;
    private final RegisteredUserService registeredUserService;
    private final Runnable onSuccess;

    private TextField firstNameField;
    private TextField lastNameField;
    private EmailField emailField;
    private TextField phoneField;
    private ComboBox<IEnumAccountOrigin.Types> originCombo;
    private TextField functionRoleField;

    public CreateRegisteredUserDialog(RegisteredManagementView parentView,
                                      RegisteredUserService registeredUserService,
                                      Runnable onSuccess) {
        super(I18n.t("ims.registered.dialog.create.title"), onSuccess);
        this.parentView = parentView;
        this.registeredUserService = registeredUserService;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("ims.registered.dialog.create.button"));
        setWidth("600px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
    }

    private void buildForm() {
        firstNameField = new TextField(I18n.t("ims.registered.dialog.field.first.name"));
        firstNameField.setRequiredIndicatorVisible(true);
        firstNameField.setPlaceholder(I18n.t("ims.registered.dialog.field.first.name.placeholder"));
        firstNameField.setWidthFull();

        lastNameField = new TextField(I18n.t("ims.registered.dialog.field.last.name"));
        lastNameField.setRequiredIndicatorVisible(true);
        lastNameField.setPlaceholder(I18n.t("ims.registered.dialog.field.last.name.placeholder"));
        lastNameField.setWidthFull();

        emailField = new EmailField(I18n.t("ims.registered.dialog.field.email"));
        emailField.setRequiredIndicatorVisible(true);
        emailField.setPlaceholder(I18n.t("ims.registered.dialog.field.email.placeholder"));
        emailField.setWidthFull();

        phoneField = new TextField(I18n.t("ims.registered.dialog.field.phone"));
        phoneField.setRequiredIndicatorVisible(true);
        phoneField.setPlaceholder(I18n.t("ims.registered.dialog.field.phone.placeholder"));
        phoneField.setWidthFull();

        originCombo = new ComboBox<>(I18n.t("ims.registered.dialog.field.origin"));
        originCombo.setItems(IEnumAccountOrigin.Types.values());
        originCombo.setValue(IEnumAccountOrigin.Types.SYS_ADMIN);
        originCombo.setWidthFull();

        functionRoleField = new TextField(I18n.t("ims.registered.dialog.field.function.role"));
        functionRoleField.setPlaceholder(I18n.t("ims.registered.dialog.field.function.role.placeholder"));
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

    @Override
    protected boolean onOk() {
        if (firstNameField.getValue().isBlank()) {
            append(I18n.t("ims.registered.dialog.field.first.name.required"));
            return false;
        }
        if (lastNameField.getValue().isBlank()) {
            append(I18n.t("ims.registered.dialog.field.last.name.required"));
            return false;
        }
        if (emailField.getValue().isBlank()) {
            append(I18n.t("ims.registered.dialog.field.email.required"));
            return false;
        }
        if (phoneField.getValue().isBlank()) {
            append(I18n.t("ims.registered.dialog.field.phone.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            RegisteredUserDto newRegisteredUser = RegisteredUserDto.builder()
                    .firstName(firstNameField.getValue())
                    .lastName(lastNameField.getValue())
                    .email(emailField.getValue())
                    .phoneNumber(phoneField.getValue())
                    .origin(originCombo.getValue() != null
                            ? originCombo.getValue()
                            : IEnumAccountOrigin.Types.SYS_ADMIN)
                    .functionRole(functionRoleField.getValue())
                    .build();

            ResponseEntity<RegisteredUserDto> response = registeredUserService.create(newRegisteredUser);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("ims.registered.dialog.create.failed", response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("ims.registered.dialog.create.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.registered.dialog.create.error", e.getMessage()));
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
