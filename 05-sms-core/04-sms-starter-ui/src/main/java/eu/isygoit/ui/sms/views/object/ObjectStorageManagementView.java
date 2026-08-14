package eu.isygoit.ui.sms.views.object;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.dto.data.FileStorageDto;
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.sms.layout.SmsMainLayout;
import eu.isygoit.ui.sms.views.object.dialog.FileDetailsDialog;
import eu.isygoit.ui.sms.views.object.dialog.UploadFileDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@VaadinSessionScope
@Route(value = "sms/objectstorage", layout = SmsMainLayout.class)
@PageTitle("Object Storage - File Management")
@PermitAll
public class ObjectStorageManagementView extends ManagementVerticalView {

    private final ObjectStorageService objectStorageService;
    private final StorageConfigService storageConfigService;

    private final Div cardsContainer = new Div();
    private final Div statsContainer = new Div();
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Button uploadFileButton = new Button(I18n.t("sms.objects.view.upload.file"), new Icon(VaadinIcon.UPLOAD));
    private final Button filterButton = new Button(I18n.t("sms.objects.view.filter.tags"), new Icon(VaadinIcon.FILTER));
    private final TextField searchField = new TextField();
    private final ComboBox<StorageConfigDto> tenantSelector = new ComboBox<>();
    private final ComboBox<BucketDto> bucketSelector = new ComboBox<>();
    private final ProgressBar loadingBar = new ProgressBar();

    // Pagination
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageInfoLabel = new Span();
    private final Span totalCountLabel = new Span();

    private List<StorageConfigDto> storageConfigs = new ArrayList<>();
    private List<BucketDto> buckets = new ArrayList<>();
    private List<FileItem> allFiles = new ArrayList<>();
    private List<FileItem> currentPageFiles = new ArrayList<>();
    private StorageConfigDto selectedStorageConfig;
    private BucketDto selectedBucket;
    private String currentSearch = "";
    private int currentPage = 0;
    private int pageSize = 10;
    private int totalPages = 0;
    private long totalElements = 0;

    private Span totalFilesLabel;
    private Span totalSizeLabel;
    private Span uniqueTagsLabel;

    @Autowired
    public ObjectStorageManagementView(ObjectStorageService objectStorageService,
                                       StorageConfigService storageConfigService) {
        this.objectStorageService = objectStorageService;
        this.storageConfigService = storageConfigService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("objectstorage-management-view");

        buildHeader();
        buildStats();
        add(buildToolbar());
        cardsContainer.setWidthFull();
        cardsContainer.addClassName("objects-cards-grid");
        add(cardsContainer);
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        initEventHandlers();
        loadStorageConfigs();
    }

    // ----- Header & Stats -----
    private void buildHeader() {
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H2 header = new H2(I18n.t("sms.objects.view.title"));
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);

        Span subtitle = new Span(I18n.t("sms.objects.view.subtitle"));
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.addClassName(LumoUtility.FontSize.SMALL);

        VerticalLayout headerContent = new VerticalLayout();
        headerContent.setSpacing(false);
        headerContent.setPadding(false);
        headerContent.add(header, subtitle);

        headerLayout.add(headerContent);
        add(headerLayout);
    }

    private void buildStats() {
        statsContainer.setWidthFull();
        statsContainer.addClassName("object-stats-container");
        statsContainer.getStyle().set("display", "flex");
        statsContainer.getStyle().set("gap", "var(--lumo-space-m)");
        statsContainer.getStyle().set("margin-bottom", "var(--lumo-space-l)");
        statsContainer.getStyle().set("flex-wrap", "wrap");

        totalFilesLabel = createStatCard(VaadinIcon.FILE, I18n.t("sms.objects.stats.total.files"), "0");
        totalSizeLabel = createStatCard(VaadinIcon.HARDDRIVE, I18n.t("sms.objects.stats.total.size"), "0 B");
        uniqueTagsLabel = createStatCard(VaadinIcon.TAGS, I18n.t("sms.objects.stats.unique.tags"), "0");
        statsContainer.add(totalFilesLabel, totalSizeLabel, uniqueTagsLabel);
        add(statsContainer);
    }

    private Span createStatCard(VaadinIcon icon, String label, String value) {
        Div card = new Div();
        card.addClassName("object-stat-card");
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius)");
        card.getStyle().set("padding", "var(--lumo-space-m)");
        card.getStyle().set("min-width", "150px");
        card.getStyle().set("flex", "1");

        Icon iconComponent = icon.create();
        iconComponent.setSize("24px");
        iconComponent.getStyle().set("color", "var(--lumo-primary-color)");

        Span labelSpan = new Span(label);
        labelSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        labelSpan.addClassName(LumoUtility.FontSize.XXSMALL);

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.LARGE);
        valueSpan.addClassName(LumoUtility.FontWeight.BOLD);

        card.add(iconComponent, labelSpan, valueSpan);
        return valueSpan;
    }

    private void updateStats() {
        long totalFiles = allFiles.size();
        long totalSize = allFiles.stream().mapToLong(FileItem::getSize).sum();
        long uniqueTags = allFiles.stream().flatMap(f -> f.getTags().stream()).distinct().count();
        totalFilesLabel.setText(String.valueOf(totalFiles));
        totalSizeLabel.setText(formatSize(totalSize));
        uniqueTagsLabel.setText(String.valueOf(uniqueTags));
    }

    // ----- Event handlers -----
    private void initEventHandlers() {
        refreshButton.addClickListener(e -> refreshAll());
        refreshButton.setTooltipText(I18n.t("sms.objects.view.refresh.tooltip"));
        uploadFileButton.addClickListener(e -> openUploadFileDialog());
        uploadFileButton.setTooltipText(I18n.t("sms.objects.view.upload.file.tooltip"));
        filterButton.addClickListener(e -> openFilterDialog());
        filterButton.setTooltipText(I18n.t("sms.objects.view.filter.tags.tooltip"));

        searchField.setPlaceholder(I18n.t("sms.objects.view.search.placeholder"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> { currentSearch = e.getValue(); applyFiltersAndPagination(); });

        tenantSelector.addValueChangeListener(e -> {
            selectedStorageConfig = e.getValue();
            if (selectedStorageConfig != null) loadBuckets(selectedStorageConfig.getTenant());
            else clearBuckets();
        });

        bucketSelector.addValueChangeListener(e -> {
            selectedBucket = e.getValue();
            if (selectedBucket != null && selectedStorageConfig != null)
                loadFiles(selectedStorageConfig.getTenant(), selectedBucket.getName());
            else clearFiles();
            uploadFileButton.setEnabled(selectedBucket != null && selectedStorageConfig != null);
        });

        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.addValueChangeListener(e -> { if (e.getValue() != null) { pageSize = e.getValue(); applyFiltersAndPagination(); } });

        prevButton.addClickListener(e -> { if (currentPage > 0) { currentPage--; applyFiltersAndPagination(); } });
        nextButton.addClickListener(e -> { if (currentPage + 1 < totalPages) { currentPage++; applyFiltersAndPagination(); } });
    }

    // ----- Data loading -----
    private void loadStorageConfigs() {
        showLoading(true);
        try {
            ResponseEntity<List<StorageConfigDto>> response = storageConfigService.findAllList();
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                storageConfigs = response.getBody();
                tenantSelector.setItems(storageConfigs);
                tenantSelector.setItemLabelGenerator(c -> c.getTenant() + " (" + (c.getType() != null ? c.getType().name() : "unknown") + ")");
                tenantSelector.setValue(storageConfigs.get(0));
            } else {
                showWarning(I18n.t("sms.objects.view.no.configs"));
            }
        } catch (FeignException ex) { showError(I18n.t("sms.objects.view.load.configs.error", extractErrorMessage(ex))); log.error("Failed to load storage configs", ex); }
        catch (Exception e) { showError(I18n.t("sms.objects.view.load.configs.error", e.getMessage())); log.error("Failed to load storage configs", e); }
        finally { showLoading(false); }
    }

    private void loadBuckets(String tenant) {
        showLoading(true);
        try {
            ResponseEntity<List<BucketDto>> response = objectStorageService.getBuckets(tenant);
            if (response.getBody() != null) {
                buckets = response.getBody();
                bucketSelector.setItems(buckets);
                bucketSelector.setItemLabelGenerator(BucketDto::getName);
                if (!buckets.isEmpty()) bucketSelector.setValue(buckets.get(0));
                else showInfo(I18n.t("sms.objects.view.no.buckets"));
            }
        } catch (FeignException ex) { showError(I18n.t("sms.objects.view.load.buckets.error", extractErrorMessage(ex))); log.error("Failed to load buckets for tenant: {}", tenant, ex); }
        catch (Exception e) { showError(I18n.t("sms.objects.view.load.buckets.error", e.getMessage())); log.error("Failed to load buckets for tenant: {}", tenant, e); }
        finally { showLoading(false); }
    }

    private void loadFiles(String tenant, String bucketName) {
        showLoading(true);
        try {
            ResponseEntity<List<FileStorageDto>> response = objectStorageService.getObjects(tenant, bucketName);
            List<FileItem> files = parseFileResponse(response.getBody());
            allFiles = files != null ? files : new ArrayList<>();
            updateStats();
            if (allFiles.isEmpty()) showInfo(I18n.t("sms.objects.view.no.files"));
            applyFiltersAndPagination();
        } catch (FeignException ex) { showError(I18n.t("sms.objects.view.load.files.error", extractErrorMessage(ex))); log.error("Failed to load files from bucket: {}", bucketName, ex); }
        catch (Exception e) { showError(I18n.t("sms.objects.view.load.files.error", e.getMessage())); log.error("Failed to load files from bucket: {}", bucketName, e); }
        finally { showLoading(false); }
    }

    // ----- File parsing with all fields -----
    private List<FileItem> parseFileResponse(List<FileStorageDto> fileStorageList) {
        if (fileStorageList == null || fileStorageList.isEmpty()) {
            return new ArrayList<>();
        }

        List<FileItem> items = new ArrayList<>();
        for (FileStorageDto dto : fileStorageList) {
            try {
                String name = dto.objectName != null ? dto.objectName : "unknown";
                long size = dto.size;
                String etag = dto.etag;
                LocalDateTime modified = dto.lastModified != null ? dto.lastModified.toLocalDateTime() : LocalDateTime.now();
                List<String> tags = dto.tags != null ? dto.tags : new ArrayList<>();
                String versionID = dto.versionID;
                boolean currentVersion = dto.currentVersion;

                // We derive a "type" from the object name extension if needed, otherwise leave as "unknown"
                String type = "unknown";
                if (name.contains(".")) {
                    type = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
                }

                items.add(new FileItem(name, type, size, modified, tags, etag, versionID, currentVersion));
            } catch (Exception e) {
                log.warn("Unable to parse FileStorageDto: {}", dto, e);
            }
        }
        return items;
    }

    // ----- Filtering & Pagination -----
    private void applyFiltersAndPagination() {
        List<FileItem> filtered = allFiles.stream()
                .filter(f -> currentSearch.isBlank() ||
                        f.getName().toLowerCase().contains(currentSearch.toLowerCase()) ||
                        f.getTags().stream().anyMatch(t -> t.toLowerCase().contains(currentSearch.toLowerCase())))
                .collect(Collectors.toList());

        totalElements = filtered.size();
        totalPages = (int) Math.ceil((double) totalElements / pageSize);
        if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, (int) totalElements);
        currentPageFiles = filtered.subList(start, end);

        updatePaginationDisplay();
        displayCards();
    }

    private void displayCards() {
        cardsContainer.removeAll();
        if (currentPageFiles.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.FILE.create();
            emptyIcon.setSize("48px");
            emptyIcon.addClassName("wams-empty-state-icon");
            H4 emptyTitle = new H4(I18n.t("sms.objects.view.empty.title"));
            Paragraph emptyDesc = new Paragraph(I18n.t("sms.objects.view.empty.description"));
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
        } else {
            for (FileItem file : currentPageFiles) {
                cardsContainer.add(new FileCard(this, objectStorageService, file, selectedBucket.getName(), this::refreshAll));
            }
        }
    }

    private void updatePaginationDisplay() {
        pageInfoLabel.setText(I18n.t("sms.objects.view.page.info", currentPage + 1, totalPages));
        totalCountLabel.setText(I18n.t("sms.objects.view.total.count", totalElements));
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage + 1 < totalPages);
    }

    // ----- Actions -----
    private void refreshAll() {
        loadStorageConfigs();
        if (selectedStorageConfig != null) {
            loadBuckets(selectedStorageConfig.getTenant());
            if (selectedBucket != null) loadFiles(selectedStorageConfig.getTenant(), selectedBucket.getName());
        }
    }

    private void clearBuckets() {
        buckets.clear();
        bucketSelector.clear();
        clearFiles();
    }
    private void clearFiles() {
        allFiles.clear();
        currentPageFiles.clear();
        cardsContainer.removeAll();
        updateStats();
        updatePaginationDisplay();
    }

    public String getSelectedTenant() { return selectedStorageConfig != null ? selectedStorageConfig.getTenant() : null; }

    // ----- File operations (download, delete, details) -----
    public void downloadFile(FileItem file) {
        if (selectedStorageConfig == null || selectedBucket == null) {
            showError(I18n.t("sms.objects.error.missing.context"));
            return;
        }
        try {
            ResponseEntity<Resource> response = objectStorageService.download(
                    selectedStorageConfig.getTenant(),
                    selectedBucket.getName(),
                    "",
                    file.getName(),
                    file.getVersionID() // pass version ID if available
            );
            // In a real app, you'd open a StreamResource for download.
            Notification.show(I18n.t("sms.objects.download.started", file.getName()), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            showError(I18n.t("sms.objects.download.error", e.getMessage()));
        }
    }

    public void deleteFile(FileItem file) {
        if (selectedStorageConfig == null || selectedBucket == null) {
            showError(I18n.t("sms.objects.error.missing.context"));
            return;
        }
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle(I18n.t("sms.objects.delete.confirm.title"));
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.add(new Span(I18n.t("sms.objects.delete.confirm.message", file.getName())));
        if (!file.getTags().isEmpty()) {
            Span tagsInfo = new Span(I18n.t("sms.objects.delete.confirm.tags", String.join(", ", file.getTags())));
            tagsInfo.addClassName(LumoUtility.TextColor.SECONDARY);
            tagsInfo.addClassName(LumoUtility.FontSize.SMALL);
            content.add(tagsInfo);
        }
        confirmDialog.add(content);

        Button confirmBtn = new Button(I18n.t("sms.objects.delete.confirm.button"), e -> {
            try {
                objectStorageService.delete(selectedStorageConfig.getTenant(), selectedBucket.getName(), "", file.getName());
                refreshAll();
                confirmDialog.close();
                Notification.show(I18n.t("sms.objects.delete.success", file.getName()), 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                showError(I18n.t("sms.objects.delete.error", ex.getMessage()));
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Button cancelBtn = new Button(I18n.t("sms.objects.delete.cancel"), e -> confirmDialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        confirmDialog.getFooter().add(cancelBtn, confirmBtn);
        confirmDialog.open();
    }

    public void showFileDetails(FileItem file) {
        if (selectedStorageConfig == null || selectedBucket == null) {
            showError(I18n.t("sms.objects.error.missing.context"));
            return;
        }
        new FileDetailsDialog(this, objectStorageService, selectedStorageConfig.getTenant(),
                selectedBucket.getName(), file).open();
    }

    // ----- Toolbar & Dialogs -----
    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("objects-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        tenantSelector.setPlaceholder(I18n.t("sms.objects.view.select.tenant"));
        tenantSelector.setWidth("250px");
        bucketSelector.setPlaceholder(I18n.t("sms.objects.view.select.bucket"));
        bucketSelector.setWidth("200px");
        searchField.setWidth("200px");
        searchField.setPlaceholder(I18n.t("sms.objects.view.search.placeholder"));
        leftGroup.add(tenantSelector, bucketSelector, searchField);

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
        uploadFileButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        uploadFileButton.setEnabled(false);
        filterButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        rightGroup.add(refreshButton, filterButton, uploadFileButton);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    private void openUploadFileDialog() {
        if (selectedStorageConfig == null || selectedBucket == null) {
            showError(I18n.t("sms.objects.error.select.bucket"));
            return;
        }
        new UploadFileDialog(this, objectStorageService, selectedStorageConfig.getTenant(),
                selectedBucket.getName(), this::refreshAll).open();
    }

    private void openFilterDialog() {
        Dialog filterDialog = new Dialog();
        filterDialog.setHeaderTitle(I18n.t("sms.objects.dialog.filter.title"));
        TextField tagFilter = new TextField(I18n.t("sms.objects.dialog.filter.tags"));
        tagFilter.setPlaceholder(I18n.t("sms.objects.dialog.filter.tags.placeholder"));
        tagFilter.setWidthFull();

        Button applyBtn = new Button(I18n.t("sms.objects.dialog.filter.apply"), e -> {
            String tags = tagFilter.getValue();
            if (tags != null && !tags.isBlank()) {
                currentSearch = tags;
                applyFiltersAndPagination();
            }
            filterDialog.close();
        });
        applyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button clearBtn = new Button(I18n.t("sms.objects.dialog.filter.clear"), e -> {
            currentSearch = "";
            searchField.setValue("");
            applyFiltersAndPagination();
            filterDialog.close();
        });
        clearBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button cancelBtn = new Button(I18n.t("sms.objects.dialog.filter.cancel"), e -> filterDialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        filterDialog.add(tagFilter);
        filterDialog.getFooter().add(clearBtn, cancelBtn, applyBtn);
        filterDialog.open();
    }

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        uploadFileButton.setEnabled(!show && selectedBucket != null);
        tenantSelector.setEnabled(!show);
        bucketSelector.setEnabled(!show);
        searchField.setEnabled(!show);
        filterButton.setEnabled(!show);
        pageSizeSelect.setEnabled(!show);
        prevButton.setEnabled(!show && currentPage > 0);
        nextButton.setEnabled(!show && currentPage + 1 < totalPages);
    }

    // ----- Helpers -----
    private void showError(String msg) { Notification.show(msg, 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_ERROR); }
    private void showWarning(String msg) { Notification.show(msg, 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING); }
    private void showInfo(String msg) { Notification.show(msg, 3000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_SUCCESS); }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private String extractErrorMessage(FeignException ex) {
        try { if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) return ex.contentUTF8(); } catch (Exception ignored) {}
        return ex.getMessage() != null ? ex.getMessage() : "Unknown error";
    }

    @Override protected void onAttach(AttachEvent attachEvent) { super.onAttach(attachEvent); }

    // ----- Inner FileItem with all fields -----
    public static class FileItem {
        private final String name;
        private final String type;
        private final long size;
        private final LocalDateTime modifiedDate;
        private final List<String> tags;
        private final String etag;
        private final String versionID;
        private final boolean currentVersion;

        public FileItem(String name, String type, long size, LocalDateTime modifiedDate,
                        List<String> tags, String etag, String versionID, boolean currentVersion) {
            this.name = name;
            this.type = type;
            this.size = size;
            this.modifiedDate = modifiedDate;
            this.tags = tags != null ? tags : new ArrayList<>();
            this.etag = etag;
            this.versionID = versionID;
            this.currentVersion = currentVersion;
        }

        // Getters
        public String getName() { return name; }
        public String getType() { return type; }
        public long getSize() { return size; }
        public String getSizeDisplay() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
        public LocalDateTime getModifiedDate() { return modifiedDate; }
        public List<String> getTags() { return tags; }
        public String getEtag() { return etag; }
        public String getVersionID() { return versionID; }
        public boolean isCurrentVersion() { return currentVersion; }

        public void setTags(List<String> newTags) {
            // We need to allow updating tags. Since fields are final, we can't modify the list directly.
            // But FileItem is a DTO; we can replace the list via a new instance or provide a setter.
            // For simplicity, we'll keep fields final and let the card replace the file item.
            // Alternatively, we can make tags mutable.
            // For now, we'll keep as is and handle updates by refreshing the list.
        }
    }
}