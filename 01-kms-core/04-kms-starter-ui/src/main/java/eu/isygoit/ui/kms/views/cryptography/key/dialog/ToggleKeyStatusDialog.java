package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class ToggleKeyStatusDialog extends BaseActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;
    private final String keyId;
    private final boolean currentlyEnabled;

    public ToggleKeyStatusDialog(KeyManagementView parentView,
                                 KmsApiService kmsApiService,
                                 String keyId,
                                 boolean currentlyEnabled,
                                 Runnable onSuccess) {
        super(currentlyEnabled ? I18n.t("kms.key.dialog.toggle.title.disable") : I18n.t("kms.key.dialog.toggle.title.enable"), onSuccess);
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.currentlyEnabled = currentlyEnabled;
        this.parentView = parentView;

        setOkButtonText(currentlyEnabled ? I18n.t("kms.key.dialog.toggle.button.disable") : I18n.t("kms.key.dialog.toggle.button.enable"));
        if (currentlyEnabled) {
            addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        } else {
            addThemeVariantsOkButton(ButtonVariant.LUMO_SUCCESS);
        }
        setWidth("450px");
        buildContent();
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            if (currentlyEnabled) {
                ResponseEntity<KmsDtos.DisableKeyResponse> response = kmsApiService.disableKey(keyId);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    append(I18n.t("kms.key.dialog.toggle.failed.disable", (response.getBody() != null ? response.getBody().toString() : "unknown error")));
                    return false;
                }
            } else {
                ResponseEntity<KmsDtos.EnableKeyResponse> response = kmsApiService.enableKey(keyId);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    append(I18n.t("kms.key.dialog.toggle.failed.enable", (response.getBody() != null ? response.getBody().toString() : "unknown error")));
                    return false;
                }
            }
            append(currentlyEnabled ? I18n.t("kms.key.dialog.toggle.success.disabled") : I18n.t("kms.key.dialog.toggle.success.enabled"));
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("kms.key.dialog.toggle.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private void buildContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.add(new Span(currentlyEnabled
                ? I18n.t("kms.key.dialog.toggle.message.disable")
                : I18n.t("kms.key.dialog.toggle.message.enable")));
        addContent(layout);
    }
}