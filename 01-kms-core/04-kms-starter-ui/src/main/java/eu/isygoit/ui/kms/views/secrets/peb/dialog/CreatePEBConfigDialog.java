package eu.isygoit.ui.kms.views.secrets.peb.dialog;

import eu.isygoit.dto.data.PEBConfigDto;
import eu.isygoit.enums.IEnumStringOutputType;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PEBConfigService;
import org.springframework.http.ResponseEntity;

/**
 * Create dialog for {@link PEBConfigDto}. The form lives in
 * {@link AbstractPEBConfigFormDialog}; this class only creates the configuration.
 */
public class CreatePEBConfigDialog extends AbstractPEBConfigFormDialog {

    public CreatePEBConfigDialog(PEBConfigService configService, Runnable onSuccess) {
        super(I18n.t("kms.peb.dialog.create.title"), onSuccess, configService,
                "kms.peb.dialog.create.success",
                "kms.peb.dialog.create.failed",
                "kms.peb.dialog.creation.failed");
        setOkButtonText(I18n.t("kms.peb.dialog.create.button"));

        buildForm();
        iterationsField.setValue(10000);
        poolSizeField.setValue(10);
        outputTypeCombo.setValue(IEnumStringOutputType.Types.Base64);
    }

    @Override
    PEBConfigDto target() {
        // code and tenant are assigned by the server: they are not sent.
        return new PEBConfigDto();
    }

    @Override
    ResponseEntity<PEBConfigDto> send(PEBConfigDto dto) {
        return configService.create(dto);
    }
}
