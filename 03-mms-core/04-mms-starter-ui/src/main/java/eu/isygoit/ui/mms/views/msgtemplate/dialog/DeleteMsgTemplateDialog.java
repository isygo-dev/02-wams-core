package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.mms.views.common.MmsEnumTag;
import eu.isygoit.ui.mms.views.msgtemplate.MsgTemplateManagementView;

public class DeleteMsgTemplateDialog extends DeleteActionDialog {

    public DeleteMsgTemplateDialog(MsgTemplateManagementView parentView,
                                   MsgTemplateService templateService,
                                   MsgTemplateDto template,
                                   Runnable onSuccess) {
        super(new Texts(
                        I18n.t("mms.msgtemplate.dialog.delete.title"),
                        buildMessage(template),
                        I18n.t("mms.msgtemplate.dialog.delete.button"),
                        I18n.t("mms.msgtemplate.dialog.delete.invalid.code"),
                        I18n.t("mms.msgtemplate.dialog.delete.success"),
                        detail -> I18n.t("mms.msgtemplate.dialog.delete.error", detail)),
                () -> templateService.delete(template.getId()),
                onSuccess,
                parentView != null ? parentView::showLoading : null,
                "mms-dialog");
    }

    private static String buildMessage(MsgTemplateDto template) {
        String name = template.getName() != null
                ? MmsEnumTag.label(template.getName(), "mms.msgtemplate.enum.name")
                : "ID: " + template.getId();
        return I18n.t("mms.msgtemplate.dialog.delete.message", name);
    }
}
