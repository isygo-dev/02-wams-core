package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Dialog for cancelling the scheduled deletion of a KMS key.
 */
public class CancelKeyDeletionDialog extends BaseActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;

    private final String keyId;
    private final String keyAliasOrId;

    public CancelKeyDeletionDialog(KeyManagementView parentView,
                                   KmsApiService kmsApiService,
                                   String keyId,
                                   String keyAliasOrId,
                                   Runnable onSuccess) {
        super(I18n.t("key.dialog.cancel.title"), onSuccess);
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.keyAliasOrId = keyAliasOrId;
        this.parentView = parentView;

        setOkButtonText(I18n.t("key.dialog.cancel.button"));
        setWidth("450px");

        buildContent();
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.CancelKeyDeletionResponse> response =
                    kmsApiService.cancelKeyDeletion(keyId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = I18n.t("key.dialog.cancel.failed", response.getStatusCode());
                this.append(errorMsg);
                return false;
            }

            Notification.show(I18n.t("key.dialog.cancel.success"), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            return true;
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            this.append(errorMsg);
        } catch (Exception e) {
            String errorMsg = I18n.t("key.dialog.cancel.error", e.getMessage());
            this.append(errorMsg);
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }

    private void buildContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.add(new Span(I18n.t("key.dialog.cancel.confirmation", keyAliasOrId)));
        add(layout);
    }
}