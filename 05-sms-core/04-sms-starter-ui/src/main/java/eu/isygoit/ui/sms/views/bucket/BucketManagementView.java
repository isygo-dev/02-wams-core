package eu.isygoit.ui.sms.views.bucket;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.sms.layout.SmsMainLayout;
import eu.isygoit.ui.sms.views.bucket.dialog.CreateBucketDialog;
import eu.isygoit.ui.sms.views.common.SmsEnumTag;
import eu.isygoit.ui.sms.views.object.ObjectStorageManagementView;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@SpringComponent
@RouteScope
@Route(value = "sms/buckets", layout = SmsMainLayout.class)
@PermitAll
public class BucketManagementView extends AbstractPaginatedView<BucketDto> {
    private final ObjectStorageService objectStorageService;
    private final StorageConfigService storageConfigService;
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final ComboBox<StorageConfigDto> tenantSelector = new ComboBox<>();
    private List<BucketDto> allBuckets = new ArrayList<>();
    // True when the next fetchPage() must query the backend (tenant change, refresh, create/delete...).
    // Search and page changes only re-filter the cached list.
    private boolean remoteReloadNeeded = true;
    private StorageConfigDto selectedStorageConfig;
    private StatCard totalBucketsCard;

    @Autowired
    public BucketManagementView(ObjectStorageService objectStorageService,
                                StorageConfigService storageConfigService) {
        this.objectStorageService = objectStorageService;
        this.storageConfigService = storageConfigService;
        this.pageSize = 10;
        initPaginatedView();
        createButton.setTooltipText(I18n.t("sms.buckets.view.create.bucket.tooltip"));
        loadStorageConfigs();
    }

    @Override
    public String getPageTitle() {
        return I18n.t("sms.buckets.view.title");
    }

    // ─── Stats ───────────────────────────────────────────────────────────────

    @Override
    protected Component buildPreToolbarContent() {
        totalBucketsCard = new StatCard(VaadinIcon.DATABASE, StatCard.Variant.PRIMARY,
                I18n.t("sms.buckets.stats.total.buckets"), "0");
        return new StatCardGrid(totalBucketsCard);
    }

    private void updateStats() {
        totalBucketsCard.setValue(String.valueOf(allBuckets.size()));
    }

    // ─── Data ────────────────────────────────────────────────────────────────

    @Override
    protected PaginatedResponseDto<BucketDto> fetchPage(int page, int size) {
        if (selectedStorageConfig == null) {
            allBuckets = new ArrayList<>();
            remoteReloadNeeded = true;
            updateStats();
            return null;
        }
        if (remoteReloadNeeded) {
            ResponseEntity<List<BucketDto>> response = objectStorageService.getBuckets(selectedStorageConfig.getTenant());
            if (response.getBody() != null) {
                allBuckets = response.getBody();
                updateStats();
                if (allBuckets.isEmpty()) showWarning(I18n.t("sms.buckets.view.no.buckets"));
            }
            remoteReloadNeeded = false;
        }

        String search = searchField.getValue() == null ? "" : searchField.getValue();
        List<BucketDto> filtered = allBuckets.stream()
                .filter(b -> search.isBlank() || (b.getName() != null && b.getName().toLowerCase().contains(search.toLowerCase())))
                .collect(Collectors.toList());

        int total = filtered.size();
        int totalPagesCount = (int) Math.ceil((double) total / size);
        int safePage = totalPagesCount == 0 ? 0 : Math.min(page, totalPagesCount - 1);
        int start = safePage * size;
        int end = Math.min(start + size, total);
        List<BucketDto> pageContent = new ArrayList<>(filtered.subList(start, end));

        return PaginatedResponseDto.<BucketDto>builder()
                .content(pageContent)
                .totalElements(total)
                .totalPages(totalPagesCount)
                .pageNumber(safePage)
                .pageSize(size)
                .build();
    }

    /**
     * Search is applied over the whole cached bucket list in {@link #fetchPage(int, int)}.
     */
    @Override
    protected boolean matchesSearch(BucketDto item, String search) {
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
                List<StorageConfigDto> configs = response.getBody();
                tenantSelector.setItems(configs);
                tenantSelector.setItemLabelGenerator(c ->
                        c.getTenant() + " (" + SmsEnumTag.label(c.getType(), "sms.enum.storage") + ")");
                tenantSelector.setRenderer(SmsEnumTag.compositeRenderer(
                        StorageConfigDto::getTenant,
                        StorageConfigDto::getType,
                        "sms.enum.storage"));
                tenantSelector.setValue(configs.get(0));
            } else {
                showWarning(I18n.t("sms.buckets.view.no.configs"));
            }
        } catch (FeignException ex) {
            showError(I18n.t("sms.buckets.view.load.configs.error", extractErrorMessage(ex)));
            log.error("Failed to load storage configs", ex);
        } catch (Exception e) {
            showError(I18n.t("sms.buckets.view.load.configs.error", e.getMessage()));
            log.error("Failed to load storage configs", e);
        } finally {
            showLoading(false);
        }
    }

    private void refreshAll() {
        loadStorageConfigs();
        if (selectedStorageConfig != null) {
            remoteReloadNeeded = true;
            loadPage(0);
        }
    }

    public String getSelectedTenant() {
        return selectedStorageConfig != null ? selectedStorageConfig.getTenant() : null;
    }

    public void navigateToObjectStorage(String tenant, BucketDto bucket) {
        QueryParameters queryParameters = (tenant != null && !tenant.isBlank())
                ? QueryParameters.of("tenant", tenant)
                : QueryParameters.empty();
        getUI().ifPresent(ui -> ui.navigate(ObjectStorageManagementView.class, bucket.getName(), queryParameters));
    }

    // ─── Rendering ───────────────────────────────────────────────────────────

    @Override
    protected Component renderItem(BucketDto item) {
        return new BucketCard(this, objectStorageService, item, this::refreshAll);
    }

    @Override
    protected boolean configureGrid(Grid<BucketDto> grid) {
        grid.addColumn(BucketDto::getName)
                .setHeader(I18n.t("sms.buckets.card.name")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(bucket -> bucket.getCreationDate() != null
                        ? bucket.getCreationDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "")
                .setHeader(I18n.t("sms.buckets.card.created")).setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected void openCreateDialog() {
        if (selectedStorageConfig == null) {
            showError(I18n.t("sms.buckets.error.select.tenant"));
            return;
        }
        new CreateBucketDialog(this, objectStorageService, selectedStorageConfig.getTenant(), this::refreshAll).open();
    }

    // ─── Toolbar ─────────────────────────────────────────────────────────────

    @Override
    protected List<Component> buildToolbarFilters() {
        tenantSelector.setPlaceholder(I18n.t("sms.buckets.view.select.tenant"));
        tenantSelector.setWidth("250px");
        tenantSelector.addValueChangeListener(e -> {
            selectedStorageConfig = e.getValue();
            remoteReloadNeeded = true;
            loadPage(0);
        });
        return List.of(tenantSelector);
    }

    @Override
    protected List<Component> buildToolbarActions() {
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("sms.buckets.view.refresh.tooltip"));
        refreshButton.addClickListener(e -> refreshAll());
        return List.of(refreshButton);
    }

    @Override
    public void showLoading(boolean show) {
        super.showLoading(show);
        refreshButton.setEnabled(!show);
        tenantSelector.setEnabled(!show);
        searchField.setEnabled(!show);
    }

    // ─── Helpers / shell hooks ───────────────────────────────────────────────

    private void showError(String msg) {
        Notification.show(msg, 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showWarning(String msg) {
        Notification.show(msg, 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
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
        return new String[]{"bucket-management-view"};
    }

    @Override
    protected String getLoadErrorKey() {
        return "sms.buckets.view.load.buckets.error";
    }

    @Override
    protected String getTitleKey() {
        return "sms.buckets.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "sms.buckets.view.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "sms.buckets.view.create.bucket";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "sms.buckets.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "sms.buckets.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "sms.buckets.view.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.FOLDER;
    }
}
