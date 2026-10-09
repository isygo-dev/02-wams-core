package eu.isygoit.ui.ims.views.registered.dialog;

import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.ui.ims.views.registered.RegisteredManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link RegisteredUserDto}. The form itself lives in
 * {@link AbstractRegisteredUserFormDialog}; this class only updates the record.
 * The e-mail stays read-only.
 */
public class UpdateRegisteredUserDialog extends AbstractRegisteredUserFormDialog {

    private final RegisteredUserDto registeredUser;

    public UpdateRegisteredUserDialog(RegisteredManagementView parentView,
                                      RegisteredUserService registeredUserService,
                                      RegisteredUserDto registeredUser,
                                      Runnable onSuccess) {
        super(I18n.t("ims.registered.dialog.update.title"), onSuccess, "ims.registered.dialog.update",
                parentView, registeredUserService);
        this.registeredUser = registeredUser;
        setOkButtonText(I18n.t("ims.registered.dialog.update.button"));

        buildForm();
        fillFrom(registeredUser);
    }

    @Override
    boolean emailEditable() {
        return false;
    }

    @Override
    RegisteredUserDto target() {
        return registeredUser;
    }

    @Override
    boolean persist(RegisteredUserDto dto) {
        ResponseEntity<RegisteredUserDto> response = registeredUserService.update(dto.getId(), dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.registered.dialog.update.failed", response.getStatusCodeValue()));
            return false;
        }
        return true;
    }
}
