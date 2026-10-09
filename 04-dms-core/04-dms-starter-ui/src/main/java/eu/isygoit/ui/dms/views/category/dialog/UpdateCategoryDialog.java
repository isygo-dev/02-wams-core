package eu.isygoit.ui.dms.views.category.dialog;

import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.ui.dms.views.category.CategoryManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link CategoryDto}. The form itself lives in
 * {@link AbstractCategoryFormDialog}; this class only updates the category.
 */
public class UpdateCategoryDialog extends AbstractCategoryFormDialog {

    private final CategoryDto category;

    public UpdateCategoryDialog(CategoryManagementView parentView,
                                CategoryService categoryService,
                                CategoryDto category,
                                Runnable onSuccess) {
        super(I18n.t("dms.category.dialog.update.title"), onSuccess,
                "dms.category.dialog.update", parentView, categoryService);
        this.category = category;
        setOkButtonText(I18n.t("dms.category.dialog.update.button"));

        buildForm();
        fillFrom(category);
    }

    @Override
    CategoryDto target() {
        return category;
    }

    @Override
    ResponseEntity<CategoryDto> persist(CategoryDto dto) {
        return service.update(dto.getId(), dto);
    }
}
