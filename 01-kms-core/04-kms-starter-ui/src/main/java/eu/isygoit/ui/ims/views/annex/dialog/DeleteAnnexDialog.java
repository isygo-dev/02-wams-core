package eu.isygoit.ui.ims.views.annex.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.annex.AnnexManagementView;
import feign.FeignException;

public class DeleteAnnexDialog extends PinBaseActionDialog {

    private final AnnexManagementView parentView;
    private final AnnexService annexService;
    private final Long annexId;

    public DeleteAnnexDialog(AnnexManagementView parentView,
                             AnnexService annexService,
                             Long annexId,
                             Runnable onSuccess) {
        super(I18n.t("ims.annex.dialog.delete.title"),
                I18n.t("ims.annex.dialog.delete.message"),
                onSuccess);
        this.parentView = parentView;
        this.annexService = annexService;
        this.annexId = annexId;

        setOkButtonText(I18n.t("ims.annex.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("ims.annex.dialog.delete.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            annexService.delete(annexId);
            append(I18n.t("ims.annex.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.annex.dialog.delete.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}