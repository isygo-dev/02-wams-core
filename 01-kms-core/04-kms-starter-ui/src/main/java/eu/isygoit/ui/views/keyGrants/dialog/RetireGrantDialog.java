package eu.isygoit.ui.views.keyGrants.dialog;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.PinBaseActionDialog;
import feign.FeignException;

public class RetireGrantDialog extends PinBaseActionDialog {

    private final String keyId;
    private final KmsApiService kmsApiService;
    private final KmsDtos.ListGrantsResponse.Grant grant;

    public RetireGrantDialog(String keyId, KmsDtos.ListGrantsResponse.Grant grant,
                             KmsApiService kmsApiService, Runnable onSuccess) {
        super("Retire Grant",
                "Retiring a grant will immediately retire it. The grant cannot be used after retirement. This action is irreversible.",
                onSuccess);
        this.keyId = keyId;
        this.grant = grant;
        this.kmsApiService = kmsApiService;
        setOkButtonText("Retire");
        addThemeVariantsOkButton(com.vaadin.flow.component.button.ButtonVariant.LUMO_WARNING);
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append("Invalid confirmation code");
            return false;
        }
        try {
            KmsDtos.RetireGrantRequest request = KmsDtos.RetireGrantRequest.builder()
                    .keyId(keyId)
                    .grantId(grant.getGrantId())
                    .build();
            kmsApiService.retireGrant(request);
            close();
            Notification.show("Grant retired successfully", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (FeignException ex) {
            append("Failed to retire grant: " + (ex.status() == 500 ? ex.contentUTF8() : ex.getMessage()));
            return false;
        } catch (Exception e) {
            append("Failed to retire grant: " + e.getMessage());
            return false;
        }
    }
}