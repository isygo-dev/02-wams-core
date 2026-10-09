package eu.isygoit.ui.ims.views.registered.dialog;

import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.enums.IEnumAccountOrigin;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.ui.ims.views.registered.RegisteredManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Create dialog for {@link RegisteredUserDto}. The form itself lives in
 * {@link AbstractRegisteredUserFormDialog}; this class only creates the record.
 */
public class CreateRegisteredUserDialog extends AbstractRegisteredUserFormDialog {

    private final RegisteredUserDto draft = RegisteredUserDto.builder()
            .origin(IEnumAccountOrigin.Types.SYS_ADMIN)
            .build();

    public CreateRegisteredUserDialog(RegisteredManagementView parentView,
                                      RegisteredUserService registeredUserService,
                                      Runnable onSuccess) {
        super(I18n.t("ims.registered.dialog.create.title"), onSuccess, "ims.registered.dialog.create",
                parentView, registeredUserService);
        setOkButtonText(I18n.t("ims.registered.dialog.create.button"));

        buildForm();
        fillFrom(draft);
    }

    @Override
    boolean emailEditable() {
        return true;
    }

    @Override
    RegisteredUserDto target() {
        return draft;
    }

    @Override
    boolean persist(RegisteredUserDto dto) {
        ResponseEntity<RegisteredUserDto> response = registeredUserService.create(dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.registered.dialog.create.failed", response.getStatusCodeValue()));
            return false;
        }
        return true;
    }
}
