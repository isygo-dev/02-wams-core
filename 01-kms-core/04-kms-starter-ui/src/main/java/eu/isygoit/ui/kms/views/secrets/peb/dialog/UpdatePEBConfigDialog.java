package eu.isygoit.ui.kms.views.secrets.peb.dialog;

import eu.isygoit.dto.data.PEBConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PEBConfigService;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link PEBConfigDto}. The form lives in
 * {@link AbstractPEBConfigFormDialog}; this class only updates the configuration.
 * The loaded DTO is edited in place, so fields not edited in the form
 * (id, code, tenant, audit) are sent back unchanged.
 */
public class UpdatePEBConfigDialog extends AbstractPEBConfigFormDialog {

    private final PEBConfigDto original;

    public UpdatePEBConfigDialog(PEBConfigService configService, PEBConfigDto dto, Runnable onSuccess) {
        super(I18n.t("kms.peb.dialog.update.title"), onSuccess, configService,
                "kms.peb.dialog.update.success",
                "kms.peb.dialog.update.failed.status",
                "kms.peb.dialog.update.failed");
        this.original = dto;
        setOkButtonText(I18n.t("kms.peb.dialog.update.button"));

        buildForm();
        fillFrom(dto);
    }

    @Override
    PEBConfigDto target() {
        return original;
    }

    @Override
    ResponseEntity<PEBConfigDto> send(PEBConfigDto dto) {
        return configService.update(original.getId(), dto);
    }
}
