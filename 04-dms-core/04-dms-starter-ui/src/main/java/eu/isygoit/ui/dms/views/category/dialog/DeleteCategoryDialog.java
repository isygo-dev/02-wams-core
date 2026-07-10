package eu.isygoit.ui.dms.views.category.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.dms.views.category.CategoryManagementView;
import feign.FeignException;

public class DeleteCategoryDialog extends PinBaseActionDialog {

    private final CategoryManagementView parentView;
    private final CategoryService categoryService;
    private final Long categoryId;

    public DeleteCategoryDialog(CategoryManagementView parentView,
                                CategoryService categoryService,
                                Long categoryId,
                                Runnable onSuccess) {
        super(I18n.t("dms.category.dialog.delete.title"),
                I18n.t("dms.category.dialog.delete.message"),
                onSuccess);
        this.parentView = parentView;
        this.categoryService = categoryService;
        this.categoryId = categoryId;

        setOkButtonText(I18n.t("dms.category.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("dms.category.dialog.delete.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            categoryService.delete(categoryId);
            append(I18n.t("dms.category.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("dms.category.dialog.delete.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}