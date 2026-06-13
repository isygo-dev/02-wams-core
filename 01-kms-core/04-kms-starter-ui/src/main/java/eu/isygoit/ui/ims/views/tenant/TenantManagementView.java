package eu.isygoit.ui.ims.views.tenant;

import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.remote.ims.TenantImageService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import eu.isygoit.ui.ims.views.tenant.dialog.CreateTenantDialog;
import eu.isygoit.ui.ims.views.tenant.dialog.UpdateTenantDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Route(value = "tenants", layout = ImsMainLayout.class)
@PageTitle("Tenant Management")
@PermitAll
public class TenantManagementView extends VerticalLayout {

    private final TenantService tenantService;
    private final TenantImageService tenantImageService;

    private final Div cardsContainer = new Div();
    private final Button createButton = new Button("Create tenant", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ComboBox<IEnumEnabledBinaryStatus.Types> statusFilter = new ComboBox<>();

    private final ProgressBar loadingBar = new ProgressBar();
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageInfoLabel = new Span();
    private final Span totalCountLabel = new Span();

    // Pagination state
    private int currentPage = 0;
    private int pageSize = 10;
    private int totalPages = 0;
    private long totalElements = 0;
    private List<TenantDto> currentPageTenants = new ArrayList<>();

    // Filters
    private String currentSearch = "";
    private IEnumEnabledBinaryStatus.Types currentAdminStatus = null;

    @Autowired
    public TenantManagementView(TenantService tenantService, TenantImageService tenantImageService) {
        this.tenantService = tenantService;
        this.tenantImageService = tenantImageService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("tenant-management-view");

        H2 header = new H2("Tenant Management");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        cardsContainer.setWidthFull();
        cardsContainer.addClassName("tenant-cards-grid");
        add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        initEventHandlers();
        injectResponsiveStyles();

        loadPage(0);
    }

    private void initEventHandlers() {
        createButton.addClickListener(e -> openCreateTenantDialog());
        createButton.setTooltipText("Create a new tenant");

        refreshButton.addClickListener(e -> loadPage(0));
        refreshButton.setTooltipText("Refresh tenants from server");

        searchField.setPlaceholder("Search by name or email");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            loadPage(0);
        });

        statusFilter.setItems(IEnumEnabledBinaryStatus.Types.values());
        statusFilter.setItemLabelGenerator(status -> status.name());
        statusFilter.setPlaceholder("Admin status");
        statusFilter.addValueChangeListener(e -> {
            currentAdminStatus = e.getValue();
            loadPage(0);
        });

        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                loadPage(0);
            }
        });

        prevButton.addClickListener(e -> {
            if (currentPage > 0) loadPage(currentPage - 1);
        });
        nextButton.addClickListener(e -> {
            if (currentPage + 1 < totalPages) loadPage(currentPage + 1);
        });
    }

    private void loadPage(int page) {
        showLoading(true);
        try {
            ResponseEntity<PaginatedResponseDto<TenantDto>> response =
                    tenantService.findAll(page, pageSize);
            PaginatedResponseDto<TenantDto> body = response.getBody();
            if (body != null && body.getContent() != null) {
                currentPageTenants = body.getContent();
                totalElements = body.getTotalElements();
                totalPages = body.getTotalPages();
                currentPage = body.getPageNumber();
            } else {
                currentPageTenants = new ArrayList<>();
                totalElements = 0;
                totalPages = 0;
            }
            updatePaginationDisplay();
            filterAndDisplayCards();
        } catch (FeignException ex) {
            String errorMsg = extractErrorMessage(ex);
            Notification.show("Failed to load tenants: " + errorMsg, 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load tenants", ex);
        } catch (Exception e) {
            Notification.show("Failed to load tenants: " + e.getMessage(), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load tenants", e);
        } finally {
            showLoading(false);
        }
    }

    private void filterAndDisplayCards() {
        List<TenantDto> filtered = currentPageTenants.stream()
                .filter(tenant -> {
                    if (currentAdminStatus != null && tenant.getAdminStatus() != currentAdminStatus)
                        return false;
                    if (currentSearch != null && !currentSearch.isEmpty()) {
                        String searchLower = currentSearch.toLowerCase();
                        return (tenant.getName() != null && tenant.getName().toLowerCase().contains(searchLower)) ||
                                (tenant.getEmail() != null && tenant.getEmail().toLowerCase().contains(searchLower));
                    }
                    return true;
                })
                .collect(Collectors.toList());

        cardsContainer.removeAll();

        if (filtered.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.BUILDING.create();
            emptyIcon.setSize("48px");
            emptyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
            H4 emptyTitle = new H4("No tenants found");
            Paragraph emptyDesc = new Paragraph("Try adjusting your search or filter criteria.");
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
        } else {
            for (TenantDto tenant : filtered) {
                cardsContainer.add(new TenantCard(this, tenantService, tenantImageService, tenant, this::loadPageZero));
            }
        }
    }

    private void updatePaginationDisplay() {
        pageInfoLabel.setText(String.format("Page %d / %d", currentPage + 1, totalPages));
        totalCountLabel.setText(String.format("%d total tenants", totalElements));
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
        toolbar.addClassName("tenant-management-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        searchField.setWidth("200px");
        statusFilter.setWidth("160px");
        leftGroup.add(searchField, statusFilter);

        HorizontalLayout centerGroup = new HorizontalLayout();
        centerGroup.setSpacing(true);
        centerGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        pageSizeSelect.setWidth("100px");
        pageInfoLabel.getStyle().set("margin", "0 0.5rem");
        centerGroup.add(prevButton, pageInfoLabel, nextButton, totalCountLabel, pageSizeSelect);

        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        rightGroup.setAlignItems(FlexComponent.Alignment.END);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        rightGroup.add(refreshButton, createButton);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    private void openCreateTenantDialog() {
        new CreateTenantDialog(this, tenantService, tenantImageService, this::loadPageZero).open();
    }

    public void openUpdateTenantDialog(TenantDto tenant, Runnable onSuccess) {
        new UpdateTenantDialog(this, tenantService, tenantImageService, tenant, onSuccess).open();
    }

    public void loadPageZero() {
        loadPage(0);
    }

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        createButton.setEnabled(!show);
    }

    private void injectResponsiveStyles() {
        String css = """
                .tenant-management-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                .tenant-cards-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
                    gap: var(--lumo-space-m);
                    padding: var(--lumo-space-s);
                }
                @media (max-width: 768px) {
                    .tenant-management-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .tenant-management-toolbar > * {
                        width: 100% !important;
                        justify-content: center;
                    }
                    .tenant-cards-grid {
                        grid-template-columns: 1fr;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) {
                return ex.contentUTF8();
            }
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}