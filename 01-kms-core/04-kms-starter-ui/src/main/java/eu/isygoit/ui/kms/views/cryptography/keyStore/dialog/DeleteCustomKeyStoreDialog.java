package eu.isygoit.ui.kms.views.cryptography.keyStore.dialog;

import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.kms.views.cryptography.keyStore.CustomKeyStoresView;

public class DeleteCustomKeyStoreDialog extends DeleteActionDialog {

    public DeleteCustomKeyStoreDialog(CustomKeyStoresView parentView,
                                      KmsApiService kmsApiService,
                                      Runnable onSuccess,
                                      KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store) {
        super(new Texts(
                        I18n.t("kms.keystore.dialog.delete.title"),
                        I18n.t("kms.keystore.dialog.delete.message"),
                        I18n.t("kms.keystore.dialog.delete.button"),
                        I18n.t("kms.keystore.dialog.delete.invalid.code"),
                        I18n.t("kms.keystore.dialog.delete.success"),
                        detail -> I18n.t("kms.keystore.dialog.delete.operation.failed", detail)),
                () -> kmsApiService.deleteCustomKeyStore(store.getCustomKeyStoreId()),
                onSuccess,
                parentView::showLoading,
                "kms-dialog");
    }
}
