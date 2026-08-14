package eu.isygoit.ui.dms.views.linkedFile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.LinkedFileRequestDto;
import eu.isygoit.dto.common.LinkedFileResponseDto;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.remote.dms.LinkedFileService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.dms.layout.DmsMainLayout;
import eu.isygoit.ui.dms.views.linkedFile.dialog.UploadLinkedFileDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@VaadinSessionScope
@Route(value = "dms/linked-files", layout = DmsMainLayout.class)
@PageTitle("Linked File Management")
@PermitAll
public class LinkedFileManagementView extends ManagementVerticalView {

    private final LinkedFileService linkedFileService;
    private final CategoryService categoryService;

    private final Div cardsContainer = new Div();
    private final Button uploadButton = new Button(I18n.t("dms.linkedfile.view.upload.button"), new Icon(VaadinIcon.UPLOAD));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Button clearFiltersButton = new Button(I18n.t("dms.linkedfile.view.clear.filters"), new Icon(VaadinIcon.ERASER));

    private final TextField searchField = new TextField();
    private final TextField tagsField = new TextField();
    private final ComboBox<String> categoryFilterComboBox = new ComboBox<>();

    private final ProgressBar loadingBar = new ProgressBar();
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageInfoLabel = new Span();
    private final Span totalCountLabel = new Span();

    private List<LinkedFileResponseDto> allFiles = new ArrayList<>();
    private List<LinkedFileResponseDto> filteredFiles = new ArrayList<>();

    // The linked-file search endpoints return a plain List (no pagination
    // support server-side), so paging is done in-memory over filteredFiles —
    // same page/pageSize/prev/next shape as every other management view.
    private int currentPage = 0;
    private int pageSize = 10;
    private int totalPages = 0;

    private String currentTenant;

    @Autowired
    public LinkedFileManagementView(LinkedFileService linkedFileService, CategoryService categoryService) {
        this.linkedFileService = linkedFileService;
        this.categoryService = categoryService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("linked-file-management-view");

        // Get tenant from security context or use default
        this.currentTenant = getTenant();

        H2 header = new H2(I18n.t("dms.linkedfile.view.title"));
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        cardsContainer.setWidthFull();
        cardsContainer.addClassName("linked-file-cards-grid");
        add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        initEventHandlers();
        loadCategories();
        loadAllFiles();
    }

    private void initEventHandlers() {
        uploadButton.addClickListener(e -> openUploadLinkedFileDialog());
        uploadButton.setTooltipText(I18n.t("dms.linkedfile.view.upload.tooltip"));

        refreshButton.addClickListener(e -> loadAllFiles());
        refreshButton.setTooltipText(I18n.t("dms.linkedfile.view.refresh.tooltip"));

        clearFiltersButton.addClickListener(e -> clearFilters());
        clearFiltersButton.setTooltipText(I18n.t("dms.linkedfile.view.clear.filters.tooltip"));

        // Free-text search has no matching backend endpoint (no "contains"
        // search by file name/code), so it only ever re-filters whatever
        // loadAllFiles() already fetched — no new network call needed.
        searchField.setPlaceholder(I18n.t("dms.linkedfile.view.search.placeholder"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> applyFilters());

        // Tags and category, unlike free-text search, ARE backed by real
        // searchByTags/searchByCategories endpoints — route their value
        // changes into a fresh server-side query instead of just filtering
        // whatever the last (possibly unrelated) query happened to return.
        tagsField.setPlaceholder(I18n.t("dms.linkedfile.view.tags.placeholder"));
        tagsField.setClearButtonVisible(true);
        tagsField.setValueChangeMode(ValueChangeMode.LAZY);
        tagsField.addValueChangeListener(e -> loadAllFiles());

        categoryFilterComboBox.setPlaceholder(I18n.t("dms.linkedfile.view.category.placeholder"));
        categoryFilterComboBox.setClearButtonVisible(true);
        categoryFilterComboBox.addValueChangeListener(e -> loadAllFiles());

        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                currentPage = 0;
                displayFiles();
            }
        });

        prevButton.addClickListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                displayFiles();
            }
        });
        nextButton.addClickListener(e -> {
            if (currentPage + 1 < totalPages) {
                currentPage++;
                displayFiles();
            }
        });
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

    private void loadAllFiles() {
        showLoading(true);
        allFiles.clear();
        filteredFiles.clear();

        try {
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
            allFiles = new ArrayList<>(byCode.values());

            applyFilters();

            if (allFiles.isEmpty()) {
                Notification.show(I18n.t("dms.linkedfile.view.no.files"), 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
            }

        } catch (Exception e) {
            Notification.show(I18n.t("dms.linkedfile.view.load.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load linked files", e);
        } finally {
            showLoading(false);
        }
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

    private void applyFilters() {
        String searchText = searchField.getValue() != null ? searchField.getValue().toLowerCase().trim() : "";
        String tagsText = tagsField.getValue() != null ? tagsField.getValue().toLowerCase().trim() : "";
        String selectedCategory = categoryFilterComboBox.getValue();

        filteredFiles = allFiles.stream()
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

        currentPage = 0;
        displayFiles();
    }

    private void clearFilters() {
        searchField.clear();
        tagsField.clear();
        categoryFilterComboBox.clear();
        applyFilters();
    }

    private void displayFiles() {
        cardsContainer.removeAll();

        int totalElements = filteredFiles.size();
        totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }
        updatePaginationDisplay(totalElements);

        if (filteredFiles.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.FILE_O.create();
            emptyIcon.setSize("48px");
            emptyIcon.addClassName("wams-empty-state-icon");
            H4 emptyTitle = new H4(I18n.t("dms.linkedfile.view.empty.title"));
            Paragraph emptyDesc = new Paragraph(I18n.t("dms.linkedfile.view.empty.description"));
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
        } else {
            int start = currentPage * pageSize;
            int end = Math.min(start + pageSize, totalElements);
            for (LinkedFileResponseDto file : filteredFiles.subList(start, end)) {
                cardsContainer.add(new LinkedFileCard(this, linkedFileService, file, this::loadAllFiles));
            }
        }
    }

    private void updatePaginationDisplay(int totalElements) {
        pageInfoLabel.setText(I18n.t("dms.linkedfile.view.page.info", totalPages == 0 ? 0 : currentPage + 1, totalPages));
        totalCountLabel.setText(I18n.t("dms.linkedfile.view.total.count", totalElements));
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage + 1 < totalPages);
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("linked-file-management-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);

        searchField.setWidth("200px");
        tagsField.setWidth("200px");
        categoryFilterComboBox.setWidth("180px");
        clearFiltersButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearFiltersButton.setSizeUndefined();

        leftGroup.add(searchField, tagsField, categoryFilterComboBox, clearFiltersButton);

        HorizontalLayout centerGroup = new HorizontalLayout();
        centerGroup.setSpacing(true);
        centerGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        pageSizeSelect.setWidth("100px");
        pageInfoLabel.addClassName("wams-page-info-label");
        centerGroup.add(prevButton, pageInfoLabel, nextButton, totalCountLabel, pageSizeSelect);

        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        rightGroup.setAlignItems(FlexComponent.Alignment.END);

        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        uploadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        rightGroup.add(refreshButton, uploadButton);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    private void openUploadLinkedFileDialog() {
        new UploadLinkedFileDialog(this, linkedFileService, categoryService, this::loadAllFiles).open();
    }

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        uploadButton.setEnabled(!show);
    }

    public String getTenant() {
        return currentTenant != null ? currentTenant : "default";
    }
}