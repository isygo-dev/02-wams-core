package eu.isygoit.ui.sms.views.object;

import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
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
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.sms.layout.SmsMainLayout;
import eu.isygoit.ui.sms.views.object.dialog.CreateBucketDialog;
import eu.isygoit.ui.sms.views.object.dialog.UploadFileDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Browse, upload, tag, share, download and delete objects across the
 * tenant's storage buckets. Aligned to the same header/stats/3-group-toolbar/
 * pagination/card-grid shape used by every other management view in the app
 * (see e.g. CategoryManagementView) rather than a bespoke layout.
 *
 * <p>Accepts an optional {@code /sms/objectstorage/{bucketName}} URL segment
 * (plus an optional {@code ?tenant=} query parameter) so other views — e.g.
 * the "Browse Files" action on {@code BucketManagementView} — can deep-link
 * straight into a specific bucket instead of always landing on the default
 * tenant/bucket selection.
 */
@Slf4j
@VaadinSessionScope
@Route(value = "sms/objectstorage", layout = SmsMainLayout.class)
@PageTitle("Object Storage - File Management")
@PermitAll
public class ObjectStorageManagementView extends ManagementVerticalView implements HasUrlParameter<String> {

    private final ObjectStorageService objectStorageService;
    private final StorageConfigService storageConfigService;

    private final Div cardsContainer = new Div();
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Button createBucketButton = new Button(new Icon(VaadinIcon.PLUS));
    private final Button uploadFileButton = new Button(I18n.t("sms.objects.view.upload.file"), new Icon(VaadinIcon.UPLOAD));
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

    // Stats
    private StatCard totalFilesCard;
    private StatCard totalSizeCard;
    private StatCard uniqueTagsCard;

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

    @Autowired
    public ObjectStorageManagementView(ObjectStorageService objectStorageService,
                                       StorageConfigService storageConfigService) {
        this.objectStorageService = objectStorageService;
        this.storageConfigService = storageConfigService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("objectstorage-management-view");

        add(buildHeader());
        add(buildStats());
        add(buildToolbar());

        cardsContainer.setWidthFull();
        cardsContainer.addClassName("objects-cards-grid");
        add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        initEventHandlers();
    }

    /**
     * Called by the router on every navigation to this view — including
     * subsequent ones, since the view is {@code @VaadinSessionScope} and thus
     * reused across navigations within the same session. Owns the initial
     * data load so a plain {@code /sms/objectstorage} visit and a deep link
     * with a bucket segment both go through the same path.
     */
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String bucketName) {
        String tenantParam = event.getLocation().getQueryParameters().getSingleParameter("tenant").orElse(null);
        loadStorageConfigs();
        applyDeepLinkSelection(tenantParam, bucketName);
    }

    private void applyDeepLinkSelection(String tenantParam, String bucketName) {
        if (tenantParam != null && !tenantParam.isBlank()) {
            StorageConfigDto match = storageConfigs.stream()
                    .filter(c -> tenantParam.equalsIgnoreCase(c.getTenant()))
                    .findFirst().orElse(null);
            if (match != null && !match.equals(tenantSelector.getValue())) {
                tenantSelector.setValue(match);
            }
        }

        if (bucketName == null || bucketName.isBlank()) {
            return;
        }
        BucketDto match = buckets.stream()
                .filter(b -> bucketName.equalsIgnoreCase(b.getName()))
                .findFirst().orElse(null);
        if (match != null) {
            bucketSelector.setValue(match);
        } else {
            showWarning(I18n.t("sms.objects.view.bucket.not.found", bucketName));
        }
    }

    // ----- Header & Stats -----
    private Component buildHeader() {
        H2 header = new H2(I18n.t("sms.objects.view.title"));
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);

        Span subtitle = new Span(I18n.t("sms.objects.view.subtitle"));
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.addClassName(LumoUtility.FontSize.SMALL);

        VerticalLayout headerContent = new VerticalLayout(header, subtitle);
        headerContent.setSpacing(false);
        headerContent.setPadding(false);
        return headerContent;
    }

    private Component buildStats() {
        totalFilesCard = new StatCard(VaadinIcon.FILE, StatCard.Variant.PRIMARY,
                I18n.t("sms.objects.stats.total.files"), "0");
        totalSizeCard = new StatCard(VaadinIcon.HARDDRIVE, StatCard.Variant.PRIMARY,
                I18n.t("sms.objects.stats.total.size"), "0 B");
        uniqueTagsCard = new StatCard(VaadinIcon.TAGS, StatCard.Variant.PRIMARY,
                I18n.t("sms.objects.stats.unique.tags"), "0");
        return new StatCardGrid(totalFilesCard, totalSizeCard, uniqueTagsCard);
    }

    private void updateStats() {
        long totalFiles = allFiles.size();
        long totalSize = allFiles.stream().mapToLong(FileItem::getSize).sum();
        long uniqueTags = allFiles.stream().flatMap(f -> f.getTags().stream()).distinct().count();
        totalFilesCard.setValue(String.valueOf(totalFiles));
        totalSizeCard.setValue(formatSize(totalSize));
        uniqueTagsCard.setValue(String.valueOf(uniqueTags));
    }

    // ----- Event handlers -----
    private void initEventHandlers() {
        refreshButton.addClickListener(e -> refreshAll());
        refreshButton.setTooltipText(I18n.t("sms.objects.view.refresh.tooltip"));

        createBucketButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        createBucketButton.setTooltipText(I18n.t("sms.objects.view.create.bucket.tooltip"));
        createBucketButton.setEnabled(false);
        createBucketButton.addClickListener(e -> openCreateBucketDialog());

        uploadFileButton.addClickListener(e -> openUploadFileDialog());
        uploadFileButton.setTooltipText(I18n.t("sms.objects.view.upload.file.tooltip"));

        searchField.setPlaceholder(I18n.t("sms.objects.view.search.placeholder"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> { currentSearch = e.getValue(); applyFiltersAndPagination(); });

        tenantSelector.addValueChangeListener(e -> {
            selectedStorageConfig = e.getValue();
            createBucketButton.setEnabled(selectedStorageConfig != null);
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

    // ----- File parsing with path extraction -----
    private List<FileItem> parseFileResponse(List<FileStorageDto> fileStorageList) {
        if (fileStorageList == null || fileStorageList.isEmpty()) {
            return new ArrayList<>();
        }

        List<FileItem> items = new ArrayList<>();
        for (FileStorageDto dto : fileStorageList) {
            try {
                String objectName = dto.objectName != null ? dto.objectName : "unknown";
                // Extract path and file name
                String path = "";
                String fileName = objectName;
                int lastSlash = objectName.lastIndexOf('/');
                if (lastSlash >= 0) {
                    path = objectName.substring(0, lastSlash);
                    fileName = objectName.substring(lastSlash + 1);
                }

                long size = dto.size;
                String etag = dto.etag;
                LocalDateTime modified = dto.lastModified != null ? dto.lastModified.toLocalDateTime() : LocalDateTime.now();
                List<String> tags = dto.tags != null ? dto.tags : new ArrayList<>();
                String versionID = dto.versionID;
                boolean currentVersion = dto.currentVersion;
                Map<String, String> metadata = dto.metadata;

                String type = "unknown";
                if (fileName.contains(".")) {
                    type = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                }

                items.add(new FileItem(objectName, path, fileName, type, size, modified,
                        tags, etag, versionID, currentVersion, metadata));
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
                        f.getFileName().toLowerCase().contains(currentSearch.toLowerCase()) ||
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
            String tenant = getSelectedTenant();
            String bucketName = selectedBucket.getName();
            for (FileItem file : currentPageFiles) {
                cardsContainer.add(new FileCard(this, objectStorageService, file, tenant, bucketName, this::refreshAll));
            }
        }
    }

    private void updatePaginationDisplay() {
        pageInfoLabel.setText(I18n.t("sms.objects.view.page.info", totalPages == 0 ? 0 : currentPage + 1, totalPages));
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

    public String getSelectedTenant() {
        return selectedStorageConfig != null ? selectedStorageConfig.getTenant() : null;
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
        tenantSelector.setWidth("220px");
        bucketSelector.setPlaceholder(I18n.t("sms.objects.view.select.bucket"));
        bucketSelector.setWidth("180px");
        searchField.setWidth("200px");
        searchField.setPlaceholder(I18n.t("sms.objects.view.search.placeholder"));
        leftGroup.add(tenantSelector, bucketSelector, createBucketButton, searchField);

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
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        rightGroup.add(refreshButton, uploadFileButton);

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

    private void openCreateBucketDialog() {
        if (selectedStorageConfig == null) {
            showError(I18n.t("sms.objects.error.select.tenant"));
            return;
        }
        new CreateBucketDialog(this, objectStorageService, selectedStorageConfig.getTenant(),
                () -> loadBuckets(selectedStorageConfig.getTenant())).open();
    }

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        createBucketButton.setEnabled(!show && selectedStorageConfig != null);
        uploadFileButton.setEnabled(!show && selectedBucket != null);
        tenantSelector.setEnabled(!show);
        bucketSelector.setEnabled(!show);
        searchField.setEnabled(!show);
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

    // ----- Inner FileItem with all fields -----
    public static class FileItem {
        private final String objectName;       // full path+name
        private final String path;             // directory (without trailing '/')
        private final String fileName;         // base name
        private final String type;
        private final long size;
        private final LocalDateTime modifiedDate;
        private final List<String> tags;
        private final String etag;
        private final String versionID;
        private final boolean currentVersion;
        private final Map<String, String> metadata;

        public FileItem(String objectName, String path, String fileName, String type, long size,
                        LocalDateTime modifiedDate, List<String> tags, String etag,
                        String versionID, boolean currentVersion, Map<String, String> metadata) {
            this.objectName = objectName;
            this.path = path != null ? path : "";
            this.fileName = fileName;
            this.type = type;
            this.size = size;
            this.modifiedDate = modifiedDate;
            this.tags = tags != null ? tags : new ArrayList<>();
            this.etag = etag;
            this.versionID = versionID;
            this.currentVersion = currentVersion;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        public String getName() {
            return objectName;
        }

        public String getPath() {
            return path;
        }

        public String getFileName() {
            return fileName;
        }
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

        public Map<String, String> getMetadata() {
            return metadata;
        }
    }
}
