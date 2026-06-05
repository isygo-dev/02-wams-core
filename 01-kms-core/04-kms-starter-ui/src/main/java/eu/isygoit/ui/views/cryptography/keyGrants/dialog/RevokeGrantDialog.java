package eu.isygoit.ui.views.cryptography.keyGrants.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.common.dialog.PinBaseActionDialog;
import feign.FeignException;

public class RevokeGrantDialog extends PinBaseActionDialog {

    private final String keyId;
    private final KmsApiService kmsApiService;
    private final KmsDtos.ListGrantsResponse.Grant grant;

    public RevokeGrantDialog(String keyId, KmsDtos.ListGrantsResponse.Grant grant,
                             KmsApiService kmsApiService, Runnable onSuccess) {
        super("Revoke Grant",
                "Revoking a grant immediately removes its permissions. This action cannot be undone.",
                onSuccess);
        this.keyId = keyId;
        this.grant = grant;
        this.kmsApiService = kmsApiService;
        setOkButtonText("Revoke");
        addThemeVariantsOkButton(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append("Invalid confirmation code");
            return false;
        }
        try {
            kmsApiService.revokeGrant(keyId, grant.getGrantId());

            Notification.show("Grant revoked successfully", 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed operation: " + e.getMessage();
            this.append(errorMsg);
        }

        return false;
    }
}