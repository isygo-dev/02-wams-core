package eu.isygoit.ui.kms.views.secrets.digest.dialog;

import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.DigestConfigService;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link DigestConfigDto}. The form lives in
 * {@link AbstractDigestConfigFormDialog}; this class only updates the configuration.
 * The loaded DTO is edited in place, so fields not edited in the form
 * (id, code, tenant, audit) are sent back unchanged.
 */
public class UpdateDigestConfigDialog extends AbstractDigestConfigFormDialog {

    private final DigestConfigDto original;

    public UpdateDigestConfigDialog(DigestConfigService configService, DigestConfigDto dto, Runnable onSuccess) {
        super(I18n.t("kms.digest.dialog.update.title"), onSuccess, configService,
                "kms.digest.dialog.update.success",
                "kms.digest.dialog.update.failed.status",
                "kms.digest.dialog.update.failed");
        this.original = dto;
        setOkButtonText(I18n.t("kms.digest.dialog.update.button"));

        buildForm();
        fillFrom(dto);
    }

    @Override
    DigestConfigDto target() {
        return original;
    }

    @Override
    ResponseEntity<DigestConfigDto> send(DigestConfigDto dto) {
        return configService.update(original.getId(), dto);
    }
}
