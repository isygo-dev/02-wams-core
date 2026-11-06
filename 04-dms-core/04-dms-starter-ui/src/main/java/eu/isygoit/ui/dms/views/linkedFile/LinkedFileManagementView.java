package eu.isygoit.ui.dms.views.linkedFile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.common.LinkedFileResponseDto;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.remote.dms.LinkedFileService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.dms.layout.DmsMainLayout;
import eu.isygoit.ui.dms.views.linkedFile.dialog.UploadLinkedFileDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SpringComponent
@RouteScope
@Route(value = "dms/linked-files", layout = DmsMainLayout.class)
@PermitAll
public class LinkedFileManagementView extends AbstractPaginatedView<LinkedFileResponseDto> {
    private final LinkedFileService linkedFileService;
    private final CategoryService categoryService;
    private final Button clearFiltersButton = new Button(I18n.t("dms.linkedfile.view.clear.filters"), new Icon(VaadinIcon.ERASER));
    private final TextField tagsField = new TextField();
    private final ComboBox<String> categoryFilterComboBox = new ComboBox<>();

    // The linked-file search endpoints return a plain List (no pagination
    // support server-side), so paging is done in-memory over the filtered list.
    private List<LinkedFileResponseDto> allFiles = new ArrayList<>();
    // True when the next fetchPage() must query the backend again (initial load, refresh,
    // tags/category change, upload...). Free-text search and page changes only re-filter allFiles.
    private boolean remoteReloadNeeded = true;
    private boolean suppressFilterEvents = false;
    private String currentTenant;

    @Autowired
    public LinkedFileManagementView(LinkedFileService linkedFileService, CategoryService categoryService) {
        this.linkedFileService = linkedFileService;
        this.categoryService = categoryService;
        this.pageSize = 10;

        // Get tenant from security context or use default
        this.currentTenant = getTenant();

        initPaginatedView();
        createButton.setIcon(new Icon(VaadinIcon.UPLOAD));
        createButton.setTooltipText(I18n.t("dms.linkedfile.view.upload.tooltip"));
    }

    @Override
    public String getPageTitle() {
        return I18n.t("dms.linkedfile.view.title");
    }

    // ─── Data ────────────────────────────────────────────────────────────────

    @Override
    protected PaginatedResponseDto<LinkedFileResponseDto> fetchPage(int page, int size) {
        if (remoteReloadNeeded) {
            allFiles = new ArrayList<>();
            allFiles = loadAllFilesFromBackend();
            remoteReloadNeeded = false;
            if (allFiles.isEmpty()) {
                Notification.show(I18n.t("dms.linkedfile.view.no.files"), 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
        }

        List<LinkedFileResponseDto> filtered = applyFilters(allFiles);
        int total = filtered.size();
        int totalPagesCount = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        int safePage = totalPagesCount == 0 ? 0 : Math.min(page, totalPagesCount - 1);
        int start = safePage * size;
        int end = Math.min(start + size, total);
        List<LinkedFileResponseDto> pageContent = start < total
                ? new ArrayList<>(filtered.subList(start, end))
                : new ArrayList<>();

        return PaginatedResponseDto.<LinkedFileResponseDto>builder()
                .content(pageContent)
                .totalElements(total)
                .totalPages(totalPagesCount)
                .pageNumber(safePage)
                .pageSize(size)
                .build();
    }

    private List<LinkedFileResponseDto> loadAllFilesFromBackend() {
        String selectedCategory = categoryFilterComboBox.getValue();
        String tagsQuery = tagsField.getValue();

        List<LinkedFileResponseDto> results;
        if (selectedCategory != null && !selectedCategory.isBlank()) {
            // A category is selected: query it directly instead of
            // fetching everything and filtering client-side.
            results = searchByCategoriesSafe(Arrays.asList(selectedCategory));
        } else if (tagsQuery != null && !tagsQuery.isBlank()) {
            results = searchByTagsSafe(Arrays.asList(tagsQuery));
        } else {
            // No category/tag filter selected and there is no dedicated
            // "list all" endpoint — combining both empty-query searches
            // is the closest available substitute for a full browse.
            List<LinkedFileResponseDto> combined = new ArrayList<>(searchByCategoriesSafe(Arrays.asList("")));
            combined.addAll(searchByTagsSafe(Arrays.asList("")));
            results = combined;
        }

        Map<String, LinkedFileResponseDto> byCode = new LinkedHashMap<>();
        for (LinkedFileResponseDto file : results) {
            byCode.putIfAbsent(file.getCode(), file);
        }
        return new ArrayList<>(byCode.values());
    }

    private List<LinkedFileResponseDto> searchByCategoriesSafe(List<String> categories) {
        try {
            ResponseEntity<List<LinkedFileResponseDto>> response = linkedFileService.searchByCategories(categories);
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (FeignException ex) {
            log.warn("Failed to search by categories: {}", categories, ex);
            return List.of();
        }
    }

    private List<LinkedFileResponseDto> searchByTagsSafe(List<String> tags) {
        try {
            ResponseEntity<List<LinkedFileResponseDto>> response = linkedFileService.searchByTags(tags);
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (FeignException ex) {
            log.warn("Failed to search by tags: {}", tags, ex);
            return List.of();
        }
    }

    private List<LinkedFileResponseDto> applyFilters(List<LinkedFileResponseDto> files) {
        String searchText = searchField.getValue() != null ? searchField.getValue().toLowerCase().trim() : "";
        String tagsText = tagsField.getValue() != null ? tagsField.getValue().toLowerCase().trim() : "";
        String selectedCategory = categoryFilterComboBox.getValue();

        return files.stream()
                .filter(file -> {
                    if (!searchText.isEmpty()) {
                        boolean matchesName = file.getOriginalFileName() != null &&
                                file.getOriginalFileName().toLowerCase().contains(searchText);
                        boolean matchesCode = file.getCode() != null &&
                                file.getCode().toLowerCase().contains(searchText);
                        if (!matchesName && !matchesCode) {
                            return false;
                        }
                    }

                    if (!tagsText.isEmpty()) {
                        List<String> searchTags = Arrays.asList(tagsText.split(","));
                        boolean hasMatchingTag = searchTags.stream()
                                .filter(tag -> !tag.trim().isEmpty())
                                .anyMatch(searchTag ->
                                        file.getTags() != null &&
                                                file.getTags().stream()
                                                        .anyMatch(fileTag -> fileTag.toLowerCase().contains(searchTag.trim()))
                                );
                        if (!hasMatchingTag) {
                            return false;
                        }
                    }

                    if (selectedCategory != null && !selectedCategory.isEmpty()) {
                        boolean hasCategory = file.getCategoryNames() != null &&
                                file.getCategoryNames().stream()
                                        .anyMatch(cat -> cat.equals(selectedCategory));
                        if (!hasCategory) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Free-text search is applied over the whole cached list in {@link #fetchPage(int, int)}.
     */
    @Override
    protected boolean matchesSearch(LinkedFileResponseDto item, String search) {
        return true;
    }

    /**
     * Free-text search has no matching backend endpoint, so it only re-filters whatever was
     * already fetched — no new network call.
     */
    @Override
    protected void onSearchChanged() {
        if (!suppressFilterEvents) {
            loadPage(0);
        }
    }

    /**
     * Forces a new backend query (tags/category/refresh/upload), then shows the first page.
     */
    private void reloadFromBackend() {
        if (suppressFilterEvents) {
            return;
        }
        remoteReloadNeeded = true;
        loadPage(0);
    }

    /**
     * Floating refresh button: re-query the backend, keeping current page and filters.
     */
    @Override
    protected void onRefreshRequested() {
        if (suppressFilterEvents) {
            return;
        }
        remoteReloadNeeded = true;
        refresh();
    }

    private void clearFilters() {
        suppressFilterEvents = true;
        try {
            searchField.clear();
            tagsField.clear();
            categoryFilterComboBox.clear();
        } finally {
            suppressFilterEvents = false;
        }
        reloadFromBackend();
    }

    // ─── Rendering ───────────────────────────────────────────────────────────

    @Override
    protected Component renderItem(LinkedFileResponseDto item) {
        return new LinkedFileCard(this, linkedFileService, item, this::reloadFromBackend);
    }

    @Override
    protected boolean configureGrid(Grid<LinkedFileResponseDto> grid) {
        grid.addColumn(LinkedFileResponseDto::getOriginalFileName)
                .setHeader(I18n.t("dms.linkedfile.table.name")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(LinkedFileResponseDto::getCode)
                .setHeader(I18n.t("dms.linkedfile.table.code")).setAutoWidth(true);
        grid.addColumn(LinkedFileResponseDto::getPath)
                .setHeader(I18n.t("dms.linkedfile.card.path")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(file -> file.getCategoryNames() == null ? "" : String.join(", ", file.getCategoryNames()))
                .setHeader(I18n.t("dms.linkedfile.card.categories")).setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected void openCreateDialog() {
        new UploadLinkedFileDialog(this, linkedFileService, categoryService, this::reloadFromBackend).open();
    }

    // ─── Toolbar ─────────────────────────────────────────────────────────────

    @Override
    protected List<Component> buildToolbarFilters() {
        loadCategories();

        // Tags and category, unlike free-text search, ARE backed by real
        // searchByTags/searchByCategories endpoints — route their value
        // changes into a fresh server-side query.
        tagsField.setPlaceholder(I18n.t("dms.linkedfile.view.tags.placeholder"));
        tagsField.setClearButtonVisible(true);
        tagsField.setValueChangeMode(ValueChangeMode.LAZY);
        tagsField.setWidth("220px");
        tagsField.addValueChangeListener(e -> reloadFromBackend());

        categoryFilterComboBox.setPlaceholder(I18n.t("dms.linkedfile.view.category.placeholder"));
        categoryFilterComboBox.setClearButtonVisible(true);
        categoryFilterComboBox.setWidth("200px");
        categoryFilterComboBox.addValueChangeListener(e -> reloadFromBackend());

        return List.of(tagsField, categoryFilterComboBox);
    }

    @Override
    protected List<Component> buildToolbarActions() {
        clearFiltersButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearFiltersButton.setTooltipText(I18n.t("dms.linkedfile.view.clear.filters.tooltip"));
        clearFiltersButton.addClickListener(e -> clearFilters());

        return List.of(clearFiltersButton);
    }

    private void loadCategories() {
        try {
            ResponseEntity<PaginatedResponseDto<CategoryDto>> response = categoryService.findAll(0, 100);
            if (response.getBody() != null && response.getBody().getContent() != null) {
                List<String> categoryNames = response.getBody().getContent().stream()
                        .map(CategoryDto::getName)
                        .collect(Collectors.toList());
                categoryFilterComboBox.setItems(categoryNames);
            }
        } catch (Exception e) {
            log.error("Failed to load categories", e);
        }
    }

    public String getTenant() {
        return currentTenant != null ? currentTenant : "default";
    }

    // ─── Shell hooks ─────────────────────────────────────────────────────────

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"linked-file-management-view"};
    }

    @Override
    protected String getLoadErrorKey() {
        return "dms.linkedfile.view.load.error";
    }

    @Override
    protected String getTitleKey() {
        return "dms.linkedfile.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "dms.linkedfile.view.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "dms.linkedfile.view.upload.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "dms.linkedfile.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "dms.linkedfile.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "dms.linkedfile.view.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.FILE_O;
    }
}
