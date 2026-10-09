package eu.isygoit.ui.ims.views.annex.dialog;

import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.ims.views.annex.AnnexManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Create dialog for {@link AnnexDto}. The form itself lives in
 * {@link AbstractAnnexFormDialog}; this class only creates the annex.
 */
public class CreateAnnexDialog extends AbstractAnnexFormDialog {

    public CreateAnnexDialog(AnnexManagementView parentView,
                             AnnexService annexService,
                             Runnable onSuccess) {
        super(I18n.t("ims.annex.dialog.create.title"), onSuccess, "ims.annex.dialog.create",
                parentView, annexService);
        setOkButtonText(I18n.t("ims.annex.dialog.create.button"));

        buildForm();
    }

    @Override
    AnnexDto target() {
        return new AnnexDto();
    }

    @Override
    boolean isTenantReadOnly() {
        return false;
    }

    @Override
    boolean persist(AnnexDto dto) {
        ResponseEntity<AnnexDto> response = annexService.create(dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.annex.dialog.create.failed", response.getStatusCodeValue()));
            return false;
        }
        return true;
    }
}
