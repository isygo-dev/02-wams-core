package eu.isygoit.ui.ims.views.annex.dialog;

import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.ims.views.annex.AnnexManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link AnnexDto}. The form itself lives in
 * {@link AbstractAnnexFormDialog}; this class only updates the annex.
 * The tenant is read-only.
 */
public class UpdateAnnexDialog extends AbstractAnnexFormDialog {

    private final AnnexDto original;

    public UpdateAnnexDialog(AnnexManagementView parentView,
                             AnnexService annexService,
                             AnnexDto annex,
                             Runnable onSuccess) {
        super(I18n.t("ims.annex.dialog.update.title"), onSuccess, "ims.annex.dialog.update",
                parentView, annexService);
        this.original = annex;
        setOkButtonText(I18n.t("ims.annex.dialog.update.button"));

        buildForm();
        fillFrom(annex);
    }

    @Override
    AnnexDto target() {
        return original;
    }

    @Override
    boolean isTenantReadOnly() {
        return true;
    }

    @Override
    boolean persist(AnnexDto dto) {
        ResponseEntity<AnnexDto> response = annexService.update(dto.getId(), dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.annex.dialog.update.failed", response.getStatusCodeValue()));
            return false;
        }
        return true;
    }
}
