package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.mms.views.msgtemplate.MsgTemplateManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

@Slf4j
public class DeleteMsgTemplateDialog extends PinBaseActionDialog {

    private final MsgTemplateManagementView parentView;
    private final MsgTemplateService templateService;
    private final MsgTemplateDto template;

    public DeleteMsgTemplateDialog(MsgTemplateManagementView parentView,
                                   MsgTemplateService templateService,
                                   MsgTemplateDto template,
                                   Runnable onSuccess) {
        super(I18n.t("template.dialog.delete.title"),
                buildMessage(template),
                onSuccess);
        this.parentView = parentView;
        this.templateService = templateService;
        this.template = template;

        setOkButtonText(I18n.t("template.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    private static String buildMessage(MsgTemplateDto template) {
        String name = template.getName() != null ? template.getName() : "ID: " + template.getId();
        return I18n.t("template.dialog.delete.message", name);
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("template.dialog.delete.invalid.code"));
            return false;
        }

        if (parentView != null) {
            parentView.showLoading(true);
        }
        try {
            // Use MsgTemplateService for delete
            ResponseEntity<?> response = templateService.delete(template.getId());
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("template.dialog.delete.failed", "unknown error"));
                return false;
            }
            showSuccess(I18n.t("template.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ?
                    ex.contentUTF8() : ex.getMessage();
            append(I18n.t("template.dialog.delete.error", errorMsg));
            log.error("Failed to delete template {}", template.getId(), ex);
        } catch (Exception e) {
            append(I18n.t("template.dialog.delete.error", e.getMessage()));
            log.error("Failed to delete template {}", template.getId(), e);
        } finally {
            if (parentView != null) {
                parentView.showLoading(false);
            }
        }
        return false;
    }
}