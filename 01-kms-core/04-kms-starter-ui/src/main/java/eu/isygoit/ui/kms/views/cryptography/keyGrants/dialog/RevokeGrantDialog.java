package eu.isygoit.ui.kms.views.cryptography.keyGrants.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinConfirmActionDialog;

public class RevokeGrantDialog extends PinConfirmActionDialog {

    public RevokeGrantDialog(String keyId, KmsDtos.ListGrantsResponse.Grant grant,
                             KmsApiService kmsApiService, Runnable onSuccess) {
        super(new Texts(
                        I18n.t("kms.grant.revoke.title"),
                        I18n.t("kms.grant.revoke.message"),
                        I18n.t("kms.grant.revoke.button"),
                        I18n.t("kms.grant.revoke.invalid.code"),
                        I18n.t("kms.grant.revoke.success"),
                        detail -> I18n.t("kms.grant.revoke.failed", detail)),
                () -> kmsApiService.revokeGrant(keyId, grant.getGrantId()),
                onSuccess,
                null,
                "kms-dialog",
                true,
                ButtonVariant.LUMO_ERROR);
    }
}
