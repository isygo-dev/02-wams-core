package eu.isygoit.ui.sms.views.bucket;

import com.vaadin.flow.component.AttachEvent;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.sms.layout.SmsMainLayout;
import eu.isygoit.ui.sms.views.bucket.dialog.CreateBucketDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@VaadinSessionScope
@Route(value = "sms/buckets", layout = SmsMainLayout.class)
@PageTitle("Bucket Management")
@PermitAll
public class BucketManagementView extends ManagementVerticalView {

    private final ObjectStorageService objectStorageService;
    private final StorageConfigService storageConfigService;

    private final Div cardsContainer = new Div();
    private final Div statsContainer = new Div();
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Button createBucketButton = new Button(I18n.t("sms.buckets.view.create.bucket"), new Icon(VaadinIcon.PLUS_CIRCLE));
    private final TextField searchField = new TextField();
    private final ComboBox<StorageConfigDto> tenantSelector = new ComboBox<>();
    private final ProgressBar loadingBar = new ProgressBar();

    // Pagination
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageInfoLabel = new Span();
    private final Span totalCountLabel = new Span();

    private List<BucketDto> allBuckets = new ArrayList<>();
    private List<BucketDto> currentPageBuckets = new ArrayList<>();
    private StorageConfigDto selectedStorageConfig;
    private String currentSearch = "";
    private int currentPage = 0;
    private int pageSize = 10;
    private int totalPages = 0;
    private long totalElements = 0;
    private Span totalBucketsLabel;

    @Autowired
    public BucketManagementView(ObjectStorageService objectStorageService,
                                StorageConfigService storageConfigService) {
        this.objectStorageService = objectStorageService;
        this.storageConfigService = storageConfigService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("bucket-management-view");

        buildHeader();
        buildStats();
        add(buildToolbar());
        cardsContainer.setWidthFull();
        cardsContainer.addClassName("buckets-cards-grid");
        add(cardsContainer);
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        initEventHandlers();
        loadStorageConfigs();
    }

    private void buildHeader() {
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H2 header = new H2(I18n.t("sms.buckets.view.title"));
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);

        Span subtitle = new Span(I18n.t("sms.buckets.view.subtitle"));
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
        statsContainer.addClassName("bucket-stats-container");
        statsContainer.getStyle().set("display", "flex");
        statsContainer.getStyle().set("gap", "var(--lumo-space-m)");
        statsContainer.getStyle().set("margin-bottom", "var(--lumo-space-l)");
        statsContainer.getStyle().set("flex-wrap", "wrap");

        totalBucketsLabel = createStatCard(VaadinIcon.DATABASE, I18n.t("sms.buckets.stats.total.buckets"), "0");
        statsContainer.add(totalBucketsLabel);
        add(statsContainer);
    }

    private Span createStatCard(VaadinIcon icon, String label, String value) {
        Div card = new Div();
        card.addClassName("bucket-stat-card");
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
        totalBucketsLabel.setText(String.valueOf(allBuckets.size()));
    }

    private void initEventHandlers() {
        refreshButton.addClickListener(e -> refreshAll());
        refreshButton.setTooltipText(I18n.t("sms.buckets.view.refresh.tooltip"));
        createBucketButton.addClickListener(e -> openCreateBucketDialog());
        createBucketButton.setTooltipText(I18n.t("sms.buckets.view.create.bucket.tooltip"));

        searchField.setPlaceholder(I18n.t("sms.buckets.view.search.placeholder"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> { currentSearch = e.getValue(); applyFiltersAndPagination(); });

        tenantSelector.addValueChangeListener(e -> {
            selectedStorageConfig = e.getValue();
            if (selectedStorageConfig != null) loadBuckets(selectedStorageConfig.getTenant());
            else clearBuckets();
        });

        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.addValueChangeListener(e -> { if (e.getValue() != null) { pageSize = e.getValue(); applyFiltersAndPagination(); } });

        prevButton.addClickListener(e -> { if (currentPage > 0) { currentPage--; applyFiltersAndPagination(); } });
        nextButton.addClickListener(e -> { if (currentPage + 1 < totalPages) { currentPage++; applyFiltersAndPagination(); } });
    }

    private void loadStorageConfigs() {
        showLoading(true);
        try {
            ResponseEntity<List<StorageConfigDto>> response = storageConfigService.findAllList();
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                List<StorageConfigDto> configs = response.getBody();
                tenantSelector.setItems(configs);
                tenantSelector.setItemLabelGenerator(c -> c.getTenant() + " (" + (c.getType() != null ? c.getType().name() : "unknown") + ")");
                tenantSelector.setValue(configs.get(0));
            } else {
                showWarning(I18n.t("sms.buckets.view.no.configs"));
            }
        } catch (FeignException ex) { showError(I18n.t("sms.buckets.view.load.configs.error", extractErrorMessage(ex))); log.error("Failed to load storage configs", ex); }
        catch (Exception e) { showError(I18n.t("sms.buckets.view.load.configs.error", e.getMessage())); log.error("Failed to load storage configs", e); }
        finally { showLoading(false); }
    }

    private void loadBuckets(String tenant) {
        showLoading(true);
        try {
            ResponseEntity<List<BucketDto>> response = objectStorageService.getBuckets(tenant);
            if (response.getBody() != null) {
                allBuckets = response.getBody();
                updateStats();
                if (allBuckets.isEmpty()) showInfo(I18n.t("sms.buckets.view.no.buckets"));
                applyFiltersAndPagination();
            }
        } catch (FeignException ex) { showError(I18n.t("sms.buckets.view.load.buckets.error", extractErrorMessage(ex))); log.error("Failed to load buckets for tenant: {}", tenant, ex); }
        catch (Exception e) { showError(I18n.t("sms.buckets.view.load.buckets.error", e.getMessage())); log.error("Failed to load buckets for tenant: {}", tenant, e); }
        finally { showLoading(false); }
    }

    private void applyFiltersAndPagination() {
        List<BucketDto> filtered = allBuckets.stream()
                .filter(b -> currentSearch.isBlank() || (b.getName() != null && b.getName().toLowerCase().contains(currentSearch.toLowerCase())))
                .collect(Collectors.toList());

        totalElements = filtered.size();
        totalPages = (int) Math.ceil((double) totalElements / pageSize);
        if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, (int) totalElements);
        currentPageBuckets = filtered.subList(start, end);

        updatePaginationDisplay();
        displayCards();
    }

    private void displayCards() {
        cardsContainer.removeAll();
        if (currentPageBuckets.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.FOLDER.create();
            emptyIcon.setSize("48px");
            emptyIcon.addClassName("wams-empty-state-icon");
            H4 emptyTitle = new H4(I18n.t("sms.buckets.view.empty.title"));
            Paragraph emptyDesc = new Paragraph(I18n.t("sms.buckets.view.empty.description"));
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
        } else {
            for (BucketDto bucket : currentPageBuckets) {
                cardsContainer.add(new BucketCard(this, objectStorageService, bucket, this::refreshAll));
            }
        }
    }

    private void updatePaginationDisplay() {
        pageInfoLabel.setText(I18n.t("sms.buckets.view.page.info", currentPage + 1, totalPages));
        totalCountLabel.setText(I18n.t("sms.buckets.view.total.count", totalElements));
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage + 1 < totalPages);
    }

    private void refreshAll() {
        loadStorageConfigs();
        if (selectedStorageConfig != null) loadBuckets(selectedStorageConfig.getTenant());
    }

    private void clearBuckets() {
        allBuckets.clear();
        currentPageBuckets.clear();
        cardsContainer.removeAll();
        updateStats();
        updatePaginationDisplay();
    }

    public String getSelectedTenant() { return selectedStorageConfig != null ? selectedStorageConfig.getTenant() : null; }

    public void navigateToObjectStorage(BucketDto bucket) {
        getUI().ifPresent(ui -> ui.navigate("sms/objectstorage"));
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("buckets-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        tenantSelector.setPlaceholder(I18n.t("sms.buckets.view.select.tenant"));
        tenantSelector.setWidth("250px");
        searchField.setWidth("200px");
        leftGroup.add(tenantSelector, searchField);

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
        createBucketButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        rightGroup.add(refreshButton, createBucketButton);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    private void openCreateBucketDialog() {
        if (selectedStorageConfig == null) {
            showError(I18n.t("sms.buckets.error.select.tenant"));
            return;
        }
        new CreateBucketDialog(this, objectStorageService, selectedStorageConfig.getTenant(), this::refreshAll).open();
    }

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        createBucketButton.setEnabled(!show);
        tenantSelector.setEnabled(!show);
        searchField.setEnabled(!show);
        pageSizeSelect.setEnabled(!show);
        prevButton.setEnabled(!show && currentPage > 0);
        nextButton.setEnabled(!show && currentPage + 1 < totalPages);
    }

    private void showError(String msg) { Notification.show(msg, 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_ERROR); }
    private void showWarning(String msg) { Notification.show(msg, 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING); }
    private void showInfo(String msg) { Notification.show(msg, 3000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_SUCCESS); }

    private String extractErrorMessage(FeignException ex) {
        try { if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) return ex.contentUTF8(); } catch (Exception ignored) {}
        return ex.getMessage() != null ? ex.getMessage() : "Unknown error";
    }

    @Override protected void onAttach(AttachEvent attachEvent) { super.onAttach(attachEvent); }
}