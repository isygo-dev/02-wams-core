package eu.isygoit.ui.kms.views.secrets.password.dialog;

import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PasswordConfigService;
import org.springframework.http.ResponseEntity;

/**
 * Create dialog for {@link PasswordConfigDto}. The form lives in
 * {@link AbstractPasswordConfigFormDialog}; this class only creates the configuration.
 */
public class CreatePasswordConfigDialog extends AbstractPasswordConfigFormDialog {

    public CreatePasswordConfigDialog(PasswordConfigService configService, Runnable onSuccess) {
        super(I18n.t("kms.password.dialog.create.title"), onSuccess, configService,
                "kms.password.dialog.create.success",
                "kms.password.dialog.create.failed",
                "kms.password.dialog.creation.failed");
        setOkButtonText(I18n.t("kms.password.dialog.create.button"));

        buildForm();
        minLengthField.setValue(8);
        maxLengthField.setValue(20);
        lifeTimeField.setValue(90);
    }

    @Override
    PasswordConfigDto target() {
        // code and tenant are assigned by the server: they are not sent.
        return new PasswordConfigDto();
    }

    @Override
    ResponseEntity<PasswordConfigDto> send(PasswordConfigDto dto) {
        return configService.create(dto);
    }
}
