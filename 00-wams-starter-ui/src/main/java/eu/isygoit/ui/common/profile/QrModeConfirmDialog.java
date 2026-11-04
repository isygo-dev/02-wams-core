package eu.isygoit.ui.common.profile;

import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.common.dialog.DialogLayout;

import java.util.function.Supplier;

final class QrModeConfirmDialog extends BaseActionDialog {

    private final transient Supplier<String> action;

    QrModeConfirmDialog(Supplier<String> action, Runnable onSuccess) {
        super(I18n.t("profile.authmethod.qr.confirm.title"), onSuccess);
        this.action = action;
        DialogLayout.size(this, DialogLayout.WIDTH_S);
        setOkButtonText(I18n.t("profile.authmethod.qr.confirm.ok"));
        addContent(new com.vaadin.flow.component.html.Paragraph(
                I18n.t("profile.authmethod.qr.confirm.message")));
    }

    @Override
    protected boolean onOk() {
        String errorKey = action.get();
        if (errorKey == null) {
            append(I18n.t("profile.authmethod.success"));
            return true;
        }
        append(I18n.t(errorKey));
        return false;
    }
}
