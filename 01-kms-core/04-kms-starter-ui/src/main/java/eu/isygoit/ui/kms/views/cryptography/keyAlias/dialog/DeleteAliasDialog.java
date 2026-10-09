package eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.AliasesView;

/**
 * Confirms the deletion of a key alias. The PIN is only required for the primary
 * alias of a key; other aliases need the confirmation message only.
 */
public class DeleteAliasDialog extends DeleteActionDialog {

    public DeleteAliasDialog(AliasesView parentView,
                             KmsApiService kmsApiService,
                             Runnable onSuccess,
                             String aliasName,
                             Boolean primaryKey) {
        super(new Texts(
                        I18n.t("kms.alias.dialog.delete.title"),
                        primaryKey ? I18n.t("kms.alias.dialog.delete.primary.warning")
                                : I18n.t("kms.alias.dialog.delete.confirmation", aliasName),
                        I18n.t("kms.alias.dialog.delete.button"),
                        I18n.t("kms.alias.dialog.delete.invalid.code"),
                        I18n.t("kms.alias.dialog.delete.success"),
                        detail -> I18n.t("kms.alias.dialog.delete.failed", detail)),
                () -> kmsApiService.deleteAlias(aliasName),
                onSuccess,
                parentView::showLoading,
                "kms-dialog",
                primaryKey);
    }
}