package eu.isygoit.ui.kms.views.cryptography.keyGrants.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import feign.FeignException;

public class RevokeGrantDialog extends PinBaseActionDialog {

    private final String keyId;
    private final KmsApiService kmsApiService;
    private final KmsDtos.ListGrantsResponse.Grant grant;

    public RevokeGrantDialog(String keyId, KmsDtos.ListGrantsResponse.Grant grant,
                             KmsApiService kmsApiService, Runnable onSuccess) {
        super(I18n.t("grant.revoke.title"),
                I18n.t("grant.revoke.message"),
                onSuccess);
        this.keyId = keyId;
        this.grant = grant;
        this.kmsApiService = kmsApiService;
        setOkButtonText(I18n.t("grant.revoke.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("500px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("grant.revoke.invalid.code"));
            return false;
        }
        try {
            kmsApiService.revokeGrant(keyId, grant.getGrantId());

            append(I18n.t("grant.revoke.success"));
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("grant.revoke.failed", e.getMessage()));
        }

        return false;
    }
}