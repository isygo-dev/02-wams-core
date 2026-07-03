package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class PermanentDeleteKeyDialog extends PinBaseActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;
    private final String keyId;

    public PermanentDeleteKeyDialog(KeyManagementView parentView,
                                    KmsApiService kmsApiService,
                                    String keyId,
                                    Runnable onSuccess) {
        super(I18n.t("kms.key.dialog.permanent.title"),
                I18n.t("kms.key.dialog.permanent.message"),
                onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;

        setOkButtonText(I18n.t("kms.key.dialog.permanent.button"));
        addThemeVariantsOkButton(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("kms.key.dialog.permanent.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.DeleteKeyResponse> response = kmsApiService.deleteKey(keyId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("kms.key.dialog.permanent.failed", (response.getBody() != null ? response.getBody().toString() : "unknown error")));
                return false;
            }
            append(I18n.t("kms.key.dialog.permanent.success"));
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("kms.key.dialog.permanent.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}