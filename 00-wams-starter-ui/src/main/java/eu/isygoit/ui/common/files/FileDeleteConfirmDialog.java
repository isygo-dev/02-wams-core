package eu.isygoit.ui.common.files;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.common.dialog.DialogLayout;

final class FileDeleteConfirmDialog extends BaseActionDialog {

    FileDeleteConfirmDialog(String fileName, Runnable onConfirm) {
        super(I18n.t("files.delete.confirm.title"), onConfirm);
        DialogLayout.size(this, DialogLayout.WIDTH_S);
        setOkButtonText(I18n.t("files.delete.confirm.ok"));
        setCancelButtonText(I18n.t("files.delete.confirm.cancel"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        addContent(new Span(I18n.t("files.delete.confirm.message", fileName)));
    }

    @Override
    protected boolean onOk() {
        return true;
    }
}
