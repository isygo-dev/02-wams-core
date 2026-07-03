package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos.RotateKeyResponse;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class RotateKeyConfirmDialog extends PinBaseActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;
    private final String keyId;


    public RotateKeyConfirmDialog(KeyManagementView parentView,
                                  KmsApiService kmsApiService,
                                  String keyId,
                                  Runnable onSuccess) {
        super(I18n.t("kms.key.dialog.rotate.title"),
                I18n.t("kms.key.dialog.rotate.message"),
                onSuccess);
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        setOkButtonText(I18n.t("kms.key.dialog.rotate.button"));
        addThemeVariantsOkButton(com.vaadin.flow.component.button.ButtonVariant.LUMO_WARNING);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            ResponseEntity<RotateKeyResponse> response = kmsApiService.rotateKey(keyId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = I18n.t("kms.key.dialog.rotate.failed", response.getStatusCode());
                this.append(errorMsg);
                return false;
            }

            Notification.show(I18n.t("kms.key.dialog.rotate.success"), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            return true;
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
        } catch (Exception e) {
            String errorMsg = I18n.t("kms.key.dialog.rotate.error", e.getMessage());
            this.append(errorMsg);
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }
}