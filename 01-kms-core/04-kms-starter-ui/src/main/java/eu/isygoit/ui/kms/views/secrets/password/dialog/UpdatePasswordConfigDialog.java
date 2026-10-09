package eu.isygoit.ui.kms.views.secrets.password.dialog;

import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PasswordConfigService;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link PasswordConfigDto}. The form lives in
 * {@link AbstractPasswordConfigFormDialog}; this class only updates the configuration.
 * The loaded DTO is edited in place, so fields not edited in the form
 * (id, code, tenant, audit) are sent back unchanged.
 */
public class UpdatePasswordConfigDialog extends AbstractPasswordConfigFormDialog {

    private final PasswordConfigDto original;

    public UpdatePasswordConfigDialog(PasswordConfigService configService, PasswordConfigDto dto, Runnable onSuccess) {
        super(I18n.t("kms.password.dialog.update.title"), onSuccess, configService,
                "kms.password.dialog.update.success",
                "kms.password.dialog.update.failed.status",
                "kms.password.dialog.update.failed");
        this.original = dto;
        setOkButtonText(I18n.t("kms.password.dialog.update.button"));

        buildForm();
        fillFrom(dto);
    }

    @Override
    PasswordConfigDto target() {
        return original;
    }

    @Override
    ResponseEntity<PasswordConfigDto> send(PasswordConfigDto dto) {
        return configService.update(original.getId(), dto);
    }
}
