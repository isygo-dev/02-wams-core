package eu.isygoit.ui.mms.views.sender.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.mms.views.sender.SenderConfigManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

@Slf4j
public class DeleteSenderConfigDialog extends PinBaseActionDialog {

    private final SenderConfigManagementView parentView;
    private final SenderConfigService senderConfigService;
    private final SenderConfigDto config;

    public DeleteSenderConfigDialog(SenderConfigManagementView parentView,
                                    SenderConfigService senderConfigService,
                                    SenderConfigDto config,
                                    Runnable onSuccess) {
        super(I18n.t("sender.dialog.delete.title"),
                buildMessage(config),
                onSuccess);
        this.parentView = parentView;
        this.senderConfigService = senderConfigService;
        this.config = config;

        setOkButtonText(I18n.t("sender.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    private static String buildMessage(SenderConfigDto config) {
        String host = config.getHost() != null ? config.getHost() : "ID: " + config.getId();
        return I18n.t("sender.dialog.delete.message", host);
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("sender.dialog.delete.invalid.code"));
            return false;
        }

        if (parentView != null) {
            parentView.showLoading(true);
        }
        try {
            ResponseEntity<?> response = senderConfigService.delete(config.getId());
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("sender.dialog.delete.failed", "unknown error"));
                return false;
            }
            showSuccess(I18n.t("sender.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ?
                    ex.contentUTF8() : ex.getMessage();
            append(I18n.t("sender.dialog.delete.error", errorMsg));
            log.error("Failed to delete sender config {}", config.getId(), ex);
        } catch (Exception e) {
            append(I18n.t("sender.dialog.delete.error", e.getMessage()));
            log.error("Failed to delete sender config {}", config.getId(), e);
        } finally {
            if (parentView != null) {
                parentView.showLoading(false);
            }
        }
        return false;
    }
}