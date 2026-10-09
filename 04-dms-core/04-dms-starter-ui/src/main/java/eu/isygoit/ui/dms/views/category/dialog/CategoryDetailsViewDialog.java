package eu.isygoit.ui.dms.views.category.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.dms.views.category.CategoryManagementView;
import eu.isygoit.ui.dms.views.common.DmsDetailsDialog;
import eu.isygoit.ui.dms.views.common.DmsDialogSupport;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Read-only view of a {@link CategoryDto}: name and description, plus the audit
 * tab. {@code id} is never displayed.
 */
public class CategoryDetailsViewDialog extends DmsDetailsDialog {

    private final CategoryManagementView parentView;
    private final CategoryService categoryService;
    private final Long categoryId;

    public CategoryDetailsViewDialog(CategoryManagementView parentView,
                                     CategoryService categoryService,
                                     Long categoryId) {
        super(I18n.t("dms.category.details.title"));
        this.parentView = parentView;
        this.categoryService = categoryService;
        this.categoryId = categoryId;

        applyWidth(DialogLayout.WIDTH_S);
        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<CategoryDto> response = categoryService.findById(categoryId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("dms.category.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("dms.category.details.load.error", DmsDialogSupport.extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("dms.category.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(CategoryDto category) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.TAG, I18n.t("dms.category.details.field.name"),
                dash(category.getName()), false);
        addFieldToGrid(grid, VaadinIcon.FILE_TEXT, I18n.t("dms.category.details.field.description"),
                dash(category.getDescription()), false);
        addTab(I18n.t("dms.category.details.section.identity"),
                createSection(I18n.t("dms.category.details.section.identity"), grid));

        addAuditTab(category.getCreatedBy(), DmsDialogSupport.formatDateTime(category.getCreateDate()),
                category.getUpdatedBy(), DmsDialogSupport.formatDateTime(category.getUpdateDate()));
    }
}
