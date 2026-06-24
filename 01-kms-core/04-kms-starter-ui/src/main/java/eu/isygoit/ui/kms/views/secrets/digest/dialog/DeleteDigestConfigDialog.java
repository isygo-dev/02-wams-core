package eu.isygoit.ui.kms.views.secrets.digest.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.DigestConfigService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import feign.FeignException;

public class DeleteDigestConfigDialog extends PinBaseActionDialog {

    private final DigestConfigService configService;
    private final Long configId;

    public DeleteDigestConfigDialog(DigestConfigService configService, Long configId, String code, Runnable onSuccess) {
        super(I18n.t("digest.dialog.delete.title"),
                I18n.t("digest.dialog.delete.confirmation", code),
                onSuccess,
                true);
        this.configService = configService;
        this.configId = configId;
        setOkButtonText(I18n.t("digest.dialog.delete.button"));
        addThemeVariantsOkButton(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("digest.dialog.delete.invalid.code"));
            return false;
        }

        try {
            configService.delete(configId);
            append(I18n.t("digest.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
            return false;
        } catch (Exception e) {
            append(I18n.t("digest.dialog.delete.failed", e.getMessage()));
            return false;
        }
    }
}