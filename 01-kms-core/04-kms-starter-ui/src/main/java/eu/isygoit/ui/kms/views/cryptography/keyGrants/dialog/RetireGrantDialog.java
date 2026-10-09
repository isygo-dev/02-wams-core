package eu.isygoit.ui.kms.views.cryptography.keyGrants.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinConfirmActionDialog;

public class RetireGrantDialog extends PinConfirmActionDialog {

    public RetireGrantDialog(String keyId, KmsDtos.ListGrantsResponse.Grant grant,
                             KmsApiService kmsApiService, Runnable onSuccess) {
        super(new Texts(
                        I18n.t("kms.grant.retire.title"),
                        I18n.t("kms.grant.retire.message"),
                        I18n.t("kms.grant.retire.button"),
                        I18n.t("kms.grant.retire.invalid.code"),
                        I18n.t("kms.grant.retire.success"),
                        detail -> I18n.t("kms.grant.retire.failed", detail)),
                () -> kmsApiService.retireGrant(KmsDtos.RetireGrantRequest.builder()
                        .keyId(keyId)
                        .grantId(grant.getGrantId())
                        .build()),
                onSuccess,
                null,
                "kms-dialog",
                true,
                ButtonVariant.LUMO_PRIMARY);
    }
}
