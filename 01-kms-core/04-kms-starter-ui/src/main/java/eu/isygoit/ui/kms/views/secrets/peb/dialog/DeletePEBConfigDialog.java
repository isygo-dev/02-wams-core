package eu.isygoit.ui.kms.views.secrets.peb.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PEBConfigService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import feign.FeignException;

public class DeletePEBConfigDialog extends PinBaseActionDialog {

    private final PEBConfigService configService;
    private final Long configId;

    public DeletePEBConfigDialog(PEBConfigService configService, Long configId, String code, Runnable onSuccess) {
        super(I18n.t("peb.dialog.delete.title"),
                I18n.t("peb.dialog.delete.confirmation", code),
                onSuccess,
                true);
        this.configService = configService;
        this.configId = configId;
        setOkButtonText(I18n.t("peb.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("peb.dialog.delete.invalid.code"));
            return false;
        }

        try {
            configService.delete(configId);
            append(I18n.t("peb.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
            return false;
        } catch (Exception e) {
            append(I18n.t("peb.dialog.delete.error", e.getMessage()));
            return false;
        }
    }
}