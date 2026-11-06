package eu.isygoit.ui.sms.views.object;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.dto.data.FileStorageDto;
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.sms.layout.SmsMainLayout;
import eu.isygoit.ui.sms.views.common.SmsEnumTag;
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
 * tenant's storage buckets, on the shared paginated management view.
 *
 * <p>Accepts an optional {@code /sms/objectstorage/{bucketName}} URL segment
 * (plus an optional {@code ?tenant=} query parameter) so other views - e.g.
 * the "Browse Files" action on {@code BucketManagementView} - can deep-link
 * straight into a specific bucket instead of always landing on the default
 * tenant/bucket selection.
 */
@Slf4j
@SpringComponent
@RouteScope
@Route(value = "sms/objectstorage", layout = SmsMainLayout.class)
@PermitAll
public class ObjectStorageManagementView extends AbstractPaginatedView<ObjectStorageManagementView.FileItem>
        implements HasUrlParameter<String> {
    private final ObjectStorageService objectStorageService;
    private final StorageConfigService storageConfigService;
    private final Button createBucketButton = new Button(new Icon(VaadinIcon.PLUS));
    private final ComboBox<StorageConfigDto> tenantSelector = new ComboBox<>();
    private final ComboBox<BucketDto> bucketSelector = new ComboBox<>();
    // Stats
    private StatCard totalFilesCard;
    private StatCard totalSizeCard;
    private StatCard uniqueTagsCard;
    private List<StorageConfigDto> storageConfigs = new ArrayList<>();
    private List<BucketDto> buckets = new ArrayList<>();
    private List<FileItem> allFiles = new ArrayList<>();
    // True when the next fetchPage() must query the backend for the objects of the selected bucket.
    // Search and page changes only re-filter the cached list.
    private boolean remoteReloadNeeded = true;
    private StorageConfigDto selectedStorageConfig;
    private BucketDto selectedBucket;

    @Autowired
    public ObjectStorageManagementView(ObjectStorageService objectStorageService,
                                       StorageConfigService storageConfigService) {
        this.objectStorageService = objectStorageService;
        this.storageConfigService = storageConfigService;
        this.pageSize = 10;
        initPaginatedView();
        createButton.setIcon(new Icon(VaadinIcon.UPLOAD));
        createButton.setTooltipText(I18n.t("sms.objects.view.upload.file.tooltip"));
        createButton.setEnabled(false);
    }

    @Override
    public String getPageTitle() {
        return I18n.t("sms.objects.view.title");
    }

    /**
     * Called by the router on navigation to this view. Owns the initial data
     * load so a plain {@code /sms/objectstorage} visit and a deep link with a
     * bucket segment both go through the same path.
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

    // ----- Stats -----
    @Override
    protected Component buildPreToolbarContent() {
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

    // ----- Data loading -----
    @Override
    protected PaginatedResponseDto<FileItem> fetchPage(int page, int size) {
        if (selectedBucket == null || selectedStorageConfig == null) {
            allFiles = new ArrayList<>();
            remoteReloadNeeded = true;
            updateStats();
            return null;
        }
        if (remoteReloadNeeded) {
            ResponseEntity<List<FileStorageDto>> response =
                    objectStorageService.getObjects(selectedStorageConfig.getTenant(), selectedBucket.getName());
            List<FileItem> files = parseFileResponse(response.getBody());
            allFiles = files != null ? files : new ArrayList<>();
            updateStats();
            if (allFiles.isEmpty()) showWarning(I18n.t("sms.objects.view.no.files"));
            remoteReloadNeeded = false;
        }

        String search = searchField.getValue() == null ? "" : searchField.getValue();
        List<FileItem> filtered = allFiles.stream()
                .filter(f -> search.isBlank() ||
                        f.getFileName().toLowerCase().contains(search.toLowerCase()) ||
                        f.getTags().stream().anyMatch(t -> t.toLowerCase().contains(search.toLowerCase())))
                .collect(Collectors.toList());

        int total = filtered.size();
        int totalPagesCount = (int) Math.ceil((double) total / size);
        int safePage = totalPagesCount == 0 ? 0 : Math.min(page, totalPagesCount - 1);
        int start = safePage * size;
        int end = Math.min(start + size, total);
        List<FileItem> pageContent = new ArrayList<>(filtered.subList(start, end));

        return PaginatedResponseDto.<FileItem>builder()
                .content(pageContent)
                .totalElements(total)
                .totalPages(totalPagesCount)
                .pageNumber(safePage)
                .pageSize(size)
                .build();
    }

    /**
     * Search (file name / tags) is applied over the whole cached list in {@link #fetchPage(int, int)}.
     */
    @Override
    protected boolean matchesSearch(FileItem item, String search) {
        return true;
    }

    @Override
    protected void onSearchChanged() {
        loadPage(0);
    }

    private void loadStorageConfigs() {
        showLoading(true);
        try {
            ResponseEntity<List<StorageConfigDto>> response = storageConfigService.findAllList();
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                storageConfigs = response.getBody();
                tenantSelector.setItems(storageConfigs);
                tenantSelector.setItemLabelGenerator(c ->
                        c.getTenant() + " (" + SmsEnumTag.label(c.getType(), "sms.enum.storage") + ")");
                tenantSelector.setRenderer(SmsEnumTag.compositeRenderer(
                        StorageConfigDto::getTenant,
                        StorageConfigDto::getType,
                        "sms.enum.storage"));
                tenantSelector.setValue(storageConfigs.get(0));
            } else {
                showWarning(I18n.t("sms.objects.view.no.configs"));
            }
        } catch (FeignException ex) {
            showError(I18n.t("sms.objects.view.load.configs.error", extractErrorMessage(ex)));
            log.error("Failed to load storage configs", ex);
        } catch (Exception e) {
            showError(I18n.t("sms.objects.view.load.configs.error", e.getMessage()));
            log.error("Failed to load storage configs", e);
        } finally {
            showLoading(false);
        }
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
                else showWarning(I18n.t("sms.objects.view.no.buckets"));
            }
        } catch (FeignException ex) {
            showError(I18n.t("sms.objects.view.load.buckets.error", extractErrorMessage(ex)));
            log.error("Failed to load buckets for tenant: {}", tenant, ex);
        } catch (Exception e) {
            showError(I18n.t("sms.objects.view.load.buckets.error", e.getMessage()));
            log.error("Failed to load buckets for tenant: {}", tenant, e);
        } finally {
            showLoading(false);
        }
    }

    private void loadFiles() {
        remoteReloadNeeded = true;
        loadPage(0);
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

    // ----- Rendering -----
    @Override
    protected Component renderItem(FileItem item) {
        return new FileCard(this, objectStorageService, item, getSelectedTenant(),
                selectedBucket.getName(), this::refreshAll);
    }

    @Override
    protected boolean configureGrid(Grid<FileItem> grid) {
        grid.addColumn(FileItem::getFileName)
                .setHeader(I18n.t("sms.objects.card.name")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(file -> file.getPath().isBlank()
                        ? I18n.t("sms.objects.card.path.root") : file.getPath())
                .setHeader(I18n.t("sms.objects.card.path")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(FileItem::getType)
                .setHeader(I18n.t("sms.objects.card.type")).setAutoWidth(true);
        grid.addColumn(FileItem::getSizeDisplay)
                .setHeader(I18n.t("sms.objects.card.size")).setAutoWidth(true);
        grid.addColumn(file -> file.getTags() == null ? "" : String.join(", ", file.getTags()))
                .setHeader(I18n.t("sms.objects.card.tags")).setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    // ----- Actions -----
    private void refreshAll() {
        loadStorageConfigs();
        if (selectedStorageConfig != null) {
            loadBuckets(selectedStorageConfig.getTenant());
            if (selectedBucket != null) loadFiles();
        }
    }

    private void clearBuckets() {
        buckets.clear();
        bucketSelector.clear();
        clearFiles();
    }

    private void clearFiles() {
        allFiles = new ArrayList<>();
        remoteReloadNeeded = true;
        loadPage(0);
    }

    public String getSelectedTenant() {
        return selectedStorageConfig != null ? selectedStorageConfig.getTenant() : null;
    }

    // ----- Toolbar & Dialogs -----
    @Override
    protected List<Component> buildToolbarFilters() {
        tenantSelector.setPlaceholder(I18n.t("sms.objects.view.select.tenant"));
        tenantSelector.setWidth("220px");
        bucketSelector.setPlaceholder(I18n.t("sms.objects.view.select.bucket"));
        bucketSelector.setWidth("180px");

        tenantSelector.addValueChangeListener(e -> {
            selectedStorageConfig = e.getValue();
            createBucketButton.setEnabled(selectedStorageConfig != null);
            if (selectedStorageConfig != null) loadBuckets(selectedStorageConfig.getTenant());
            else clearBuckets();
        });

        bucketSelector.addValueChangeListener(e -> {
            selectedBucket = e.getValue();
            if (selectedBucket != null && selectedStorageConfig != null) loadFiles();
            else clearFiles();
            createButton.setEnabled(selectedBucket != null && selectedStorageConfig != null);
        });
        return List.of(tenantSelector, bucketSelector);
    }

    @Override
    protected List<Component> buildToolbarActions() {
        createBucketButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        createBucketButton.setTooltipText(I18n.t("sms.objects.view.create.bucket.tooltip"));
        createBucketButton.setEnabled(false);
        createBucketButton.addClickListener(e -> openCreateBucketDialog());
        return List.of(createBucketButton);
    }

    /**
     * Floating refresh: re-query the objects of the selected bucket from the backend while keeping
     * the selected tenant/bucket, search text, current page and page size.
     */
    @Override
    protected void onRefreshRequested() {
        remoteReloadNeeded = true;
        super.onRefreshRequested();
    }

    /**
     * The primary toolbar button is the "upload file" action.
     */
    @Override
    protected void openCreateDialog() {
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

    @Override
    public void showLoading(boolean show) {
        super.showLoading(show);
        createBucketButton.setEnabled(!show && selectedStorageConfig != null);
        createButton.setEnabled(!show && selectedBucket != null);
        tenantSelector.setEnabled(!show);
        bucketSelector.setEnabled(!show);
        searchField.setEnabled(!show);
    }

    // ----- Helpers / shell hooks -----
    private void showError(String msg) {
        Notification.show(msg, 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showWarning(String msg) {
        Notification.show(msg, 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage() != null ? ex.getMessage() : "Unknown error";
    }

    @Override
    protected String extractErrorMessage(Exception e) {
        if (e instanceof FeignException ex) {
            return extractErrorMessage(ex);
        }
        return e.getMessage();
    }

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"objectstorage-management-view"};
    }

    @Override
    protected String getLoadErrorKey() {
        return "sms.objects.view.load.files.error";
    }

    @Override
    protected String getTitleKey() {
        return "sms.objects.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "sms.objects.view.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "sms.objects.view.upload.file";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "sms.objects.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "sms.objects.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "sms.objects.view.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.FILE;
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

        public String getType() {
            return type;
        }

        public long getSize() {
            return size;
        }

        public String getSizeDisplay() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }

        public LocalDateTime getModifiedDate() {
            return modifiedDate;
        }

        public List<String> getTags() {
            return tags;
        }

        public String getEtag() {
            return etag;
        }

        public String getVersionID() {
            return versionID;
        }

        public boolean isCurrentVersion() {
            return currentVersion;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }
    }
}
