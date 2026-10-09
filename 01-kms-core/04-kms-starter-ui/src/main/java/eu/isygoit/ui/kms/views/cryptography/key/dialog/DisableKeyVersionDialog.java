package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinConfirmActionDialog;

public class DisableKeyVersionDialog extends PinConfirmActionDialog {

    public DisableKeyVersionDialog(KmsApiService kmsApiService,
                                   String keyId,
                                   String versionId,
                                   Runnable onSuccess) {
        super(new Texts(
                        I18n.t("kms.key.dialog.disable.version.title"),
                        I18n.t("kms.key.dialog.disable.version.message"),
                        I18n.t("kms.key.dialog.disable.version.button"),
                        I18n.t("common.dialog.pin.invalid"),
                        I18n.t("kms.key.dialog.disable.version.success"),
                        detail -> detail != null && detail.startsWith("HTTP ")
                                ? I18n.t("kms.key.dialog.disable.version.failed", detail.substring(5))
                                : I18n.t("kms.key.dialog.disable.version.error", detail)),
                // two path variables: keyId and keyVersionId
                () -> kmsApiService.disableKeyVersion(keyId, versionId),
                onSuccess,
                null,
                "kms-dialog",
                true,
                ButtonVariant.LUMO_PRIMARY);
    }
}
