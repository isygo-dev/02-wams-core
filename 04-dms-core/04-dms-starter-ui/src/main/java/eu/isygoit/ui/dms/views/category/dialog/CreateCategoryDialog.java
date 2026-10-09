package eu.isygoit.ui.dms.views.category.dialog;

import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.ui.dms.views.category.CategoryManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Create dialog for {@link CategoryDto}. The form itself lives in
 * {@link AbstractCategoryFormDialog}; this class only creates the category.
 */
public class CreateCategoryDialog extends AbstractCategoryFormDialog {

    public CreateCategoryDialog(CategoryManagementView parentView,
                                CategoryService categoryService,
                                Runnable onSuccess) {
        super(I18n.t("dms.category.dialog.create.title"), onSuccess,
                "dms.category.dialog.create", parentView, categoryService);
        setOkButtonText(I18n.t("dms.category.dialog.create.button"));

        buildForm();
    }

    @Override
    CategoryDto target() {
        return new CategoryDto();
    }

    @Override
    ResponseEntity<CategoryDto> persist(CategoryDto dto) {
        return service.create(dto);
    }
}
