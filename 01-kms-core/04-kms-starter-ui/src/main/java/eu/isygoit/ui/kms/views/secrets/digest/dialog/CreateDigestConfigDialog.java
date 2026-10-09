package eu.isygoit.ui.kms.views.secrets.digest.dialog;

import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.enums.IEnumSaltGenerator;
import eu.isygoit.enums.IEnumStringOutputType;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.DigestConfigService;
import org.springframework.http.ResponseEntity;

/**
 * Create dialog for {@link DigestConfigDto}. The form lives in
 * {@link AbstractDigestConfigFormDialog}; this class only creates the configuration.
 */
public class CreateDigestConfigDialog extends AbstractDigestConfigFormDialog {

    public CreateDigestConfigDialog(DigestConfigService configService, Runnable onSuccess) {
        super(I18n.t("kms.digest.dialog.create.title"), onSuccess, configService,
                "kms.digest.dialog.create.success",
                "kms.digest.dialog.create.failed",
                "kms.digest.dialog.creation.failed");
        setOkButtonText(I18n.t("kms.digest.dialog.create.button"));

        buildForm();
        iterationsField.setValue(1);
        saltSizeField.setValue(16);
        saltGeneratorCombo.setValue(IEnumSaltGenerator.Types.RandomSaltGenerator);
        poolSizeField.setValue(10);
        outputTypeCombo.setValue(IEnumStringOutputType.Types.Base64);
    }

    @Override
    DigestConfigDto target() {
        // code and tenant are assigned by the server: they are not sent.
        return new DigestConfigDto();
    }

    @Override
    ResponseEntity<DigestConfigDto> send(DigestConfigDto dto) {
        return configService.create(dto);
    }
}
