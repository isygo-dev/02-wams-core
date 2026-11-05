package eu.isygoit.ui.dms.views.category;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.dms.layout.DmsMainLayout;
import eu.isygoit.ui.dms.views.category.dialog.CreateCategoryDialog;
import eu.isygoit.ui.dms.views.category.dialog.UpdateCategoryDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@VaadinSessionScope
@Route(value = "dms/categories", layout = DmsMainLayout.class)
@PageTitle("Category Management")
@PermitAll
public class CategoryManagementView extends AbstractPaginatedView<CategoryDto> {

    private final CategoryService categoryService;
    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final com.vaadin.flow.component.html.Span totalCountLabel =
            new com.vaadin.flow.component.html.Span();

    @Autowired
    public CategoryManagementView(CategoryService categoryService) {
        this.categoryService = categoryService;
        pageSize = 10;
        initPaginatedView();
        refreshButton.setTooltipText(I18n.t("dms.category.view.refresh.tooltip"));
    }

    @Override
    protected PaginatedResponseDto<CategoryDto> fetchPage(int page, int size) {
        var response = categoryService.findAll(page, size);
        return response != null ? response.getBody() : null;
    }

    @Override
    protected PaginatedResponseDto<? extends CategoryDto> fetchFilteredPage(String criteria, int page, int size) {
        var response = categoryService.findAllFilteredByCriteria(criteria, page, size);
        return response != null ? response.getBody() : null;
    }

    @Override
    protected Map<String, String> fetchCriteriaMetadata() {
        var response = categoryService.getAnnotatedCriteria();
        return response != null && response.getBody() != null
                ? response.getBody().stream().collect(Collectors.toMap(
                field -> field.name(), field -> field.type(), (first, second) -> second,
                java.util.LinkedHashMap::new))
                : Map.of();
    }

    @Override
    protected boolean configureGrid(Grid<CategoryDto> grid) {
        grid.addColumn(CategoryDto::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(CategoryDto::getName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(CategoryDto::getDescription).setHeader("Description").setFlexGrow(1);
        grid.addColumn(CategoryDto::getCreatedBy).setHeader("Created by").setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected Component renderItem(CategoryDto item) {
        return new CategoryCard(this, categoryService, item, this::loadPageZero);
    }

    @Override
    protected boolean matchesSearch(CategoryDto category, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String query = search.toLowerCase();
        return (category.getName() != null && category.getName().toLowerCase().contains(query))
                || (category.getDescription() != null && category.getDescription().toLowerCase().contains(query));
    }

    @Override
    protected void openCreateDialog() {
        new CreateCategoryDialog(this, categoryService, this::loadPageZero).open();
    }

    @Override
    protected void onSearchChanged() {
        loadPageZero();
    }

    @Override
    protected List<Component> buildToolbarActions() {
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(event -> loadPageZero());
        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(pageSize);
        pageSizeSelect.setWidth("100px");
        pageSizeSelect.addValueChangeListener(event -> {
            if (event.getValue() != null && event.getValue() != pageSize) {
                pageSize = event.getValue();
                loadPageZero();
            }
        });
        totalCountLabel.addClassName("management-total-count");
        return List.of(refreshButton, pageSizeSelect, totalCountLabel);
    }

    @Override
    protected void onPageLoaded() {
        totalCountLabel.setText(I18n.t("dms.category.view.total.count", totalElements));
    }

    @Override
    protected String extractErrorMessage(Exception exception) {
        if (exception instanceof FeignException feignException
                && feignException.contentUTF8() != null && !feignException.contentUTF8().isBlank()) {
            return feignException.contentUTF8();
        }
        return exception.getMessage();
    }

    @Override
    protected String getTitleKey() {
        return "dms.category.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return null;
    }

    @Override
    protected String getCreateButtonKey() {
        return "dms.category.view.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "dms.category.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "dms.category.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "dms.category.view.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.FOLDER_O;
    }

    @Override
    protected String getLoadErrorKey() {
        return "dms.category.view.load.error";
    }

    @Override
    public String getPageTitle() {
        return I18n.t(getTitleKey());
    }

    public void loadPageZero() {
        loadPage(0);
    }

    public void openUpdateCategoryDialog(CategoryDto category, Runnable onSuccess) {
        new UpdateCategoryDialog(this, categoryService, category, onSuccess).open();
    }
}
