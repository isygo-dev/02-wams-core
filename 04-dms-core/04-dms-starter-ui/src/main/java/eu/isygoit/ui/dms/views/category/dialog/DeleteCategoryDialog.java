package eu.isygoit.ui.dms.views.category.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.dms.views.category.CategoryManagementView;

public class DeleteCategoryDialog extends DeleteActionDialog {

    public DeleteCategoryDialog(CategoryManagementView parentView,
                                CategoryService categoryService,
                                Long categoryId,
                                Runnable onSuccess) {
        super(new Texts(
                        I18n.t("dms.category.dialog.delete.title"),
                        I18n.t("dms.category.dialog.delete.message"),
                        I18n.t("dms.category.dialog.delete.button"),
                        I18n.t("dms.category.dialog.delete.invalid.code"),
                        I18n.t("dms.category.dialog.delete.success"),
                        detail -> I18n.t("dms.category.dialog.delete.error", detail)),
                () -> categoryService.delete(categoryId),
                onSuccess,
                parentView::showLoading,
                "dms-dialog");
    }
}
