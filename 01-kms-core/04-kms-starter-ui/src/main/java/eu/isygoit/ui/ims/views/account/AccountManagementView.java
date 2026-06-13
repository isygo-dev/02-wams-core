package eu.isygoit.ui.ims.views.account;

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
import eu.isygoit.dto.data.MinAccountDto;
import eu.isygoit.enums.IEnumAccountSystemStatus;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import eu.isygoit.ui.ims.views.account.dialog.CreateAccountDialog;
import eu.isygoit.ui.ims.views.account.dialog.ResetPasswordDialog;
import eu.isygoit.ui.ims.views.account.dialog.UpdateAccountDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Route(value = "accounts", layout = ImsMainLayout.class)
@PageTitle("Account Management")
@PermitAll
public class AccountManagementView extends VerticalLayout {

    private final AccountService accountService;
    private final AccountImageService accountImageService;
    private final TenantService tenantService;

    private final Div cardsContainer = new Div();
    private final Button createButton = new Button("Create account", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ComboBox<StatusFilterOption> statusFilter = new ComboBox<>();

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
    private List<MinAccountDto> currentPageAccounts = new ArrayList<>();

    // Filters
    private String currentSearch = "";
    private IEnumEnabledBinaryStatus.Types currentAdminStatus = null;
    private IEnumAccountSystemStatus.Types currentSystemStatus = null;

    @Autowired
    public AccountManagementView(AccountService accountService,
                                 AccountImageService accountImageService, TenantService tenantService) {
        this.accountService = accountService;
        this.accountImageService = accountImageService;
        this.tenantService = tenantService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("account-management-view");

        H2 header = new H2("Account Management");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        cardsContainer.setWidthFull();
        cardsContainer.addClassName("account-cards-grid");
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
        createButton.addClickListener(e -> openCreateAccountDialog());
        createButton.setTooltipText("Create a new account");

        refreshButton.addClickListener(e -> loadPage(0));
        refreshButton.setTooltipText("Refresh accounts from server");

        searchField.setPlaceholder("Search by email or full name");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            loadPage(0);
        });

        statusFilter.setItems(
                new StatusFilterOption("All", null, null),
                new StatusFilterOption("Admin Enabled", IEnumEnabledBinaryStatus.Types.ENABLED, null),
                new StatusFilterOption("Admin Disabled", IEnumEnabledBinaryStatus.Types.DISABLED, null),
                new StatusFilterOption("System Expired", null, IEnumAccountSystemStatus.Types.EXPIRED),
                new StatusFilterOption("System Registered", null, IEnumAccountSystemStatus.Types.REGISTRED),
                new StatusFilterOption("System Temporarily Locked", null, IEnumAccountSystemStatus.Types.TEM_LOCKED),
                new StatusFilterOption("System Idle", null, IEnumAccountSystemStatus.Types.IDLE),
                new StatusFilterOption("System Locked", null, IEnumAccountSystemStatus.Types.LOCKED)
        );
        statusFilter.setItemLabelGenerator(StatusFilterOption::label);
        statusFilter.setValue(new StatusFilterOption("All", null, null));
        statusFilter.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                currentAdminStatus = e.getValue().adminStatus;
                currentSystemStatus = e.getValue().systemStatus;
                loadPage(0);
            }
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
            ResponseEntity<PaginatedResponseDto<MinAccountDto>> response =
                    accountService.findAll(page, pageSize);
            PaginatedResponseDto<MinAccountDto> body = response.getBody();
            if (body != null && body.getContent() != null) {
                currentPageAccounts = body.getContent();
                totalElements = body.getTotalElements();
                totalPages = body.getTotalPages();
                currentPage = body.getPageNumber();
            } else {
                currentPageAccounts = new ArrayList<>();
                totalElements = 0;
                totalPages = 0;
            }
            updatePaginationDisplay();
            filterAndDisplayCards();
        } catch (FeignException ex) {
            String errorMsg = extractErrorMessage(ex);
            Notification.show("Failed to load accounts: " + errorMsg, 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load accounts", ex);
        } catch (Exception e) {
            Notification.show("Failed to load accounts: " + e.getMessage(), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load accounts", e);
        } finally {
            showLoading(false);
        }
    }

    private void filterAndDisplayCards() {
        List<MinAccountDto> filtered = currentPageAccounts.stream()
                .filter(acc -> {
                    if (currentAdminStatus != null && acc.getAdminStatus() != currentAdminStatus)
                        return false;
                    if (currentSystemStatus != null && acc.getSystemStatus() != currentSystemStatus)
                        return false;
                    if (currentSearch != null && !currentSearch.isEmpty()) {
                        String searchLower = currentSearch.toLowerCase();
                        return (acc.getEmail() != null && acc.getEmail().toLowerCase().contains(searchLower)) ||
                                (acc.getFullName() != null && acc.getFullName().toLowerCase().contains(searchLower));
                    }
                    return true;
                })
                .collect(Collectors.toList());

        cardsContainer.removeAll();

        if (filtered.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.USER.create();
            emptyIcon.setSize("48px");
            emptyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
            H4 emptyTitle = new H4("No accounts found");
            Paragraph emptyDesc = new Paragraph("Try adjusting your search or filter criteria.");
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
        } else {
            for (MinAccountDto acc : filtered) {
                // Pass AccountImageService and a refresh callback that reloads the current page
                cardsContainer.add(new AccountCard(this, accountService, accountImageService, acc, this::loadPageZero));
            }
        }
    }

    private void updatePaginationDisplay() {
        pageInfoLabel.setText(String.format("Page %d / %d", currentPage + 1, totalPages));
        totalCountLabel.setText(String.format("%d total accounts", totalElements));
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
        toolbar.addClassName("account-management-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        searchField.setWidth("200px");
        statusFilter.setPlaceholder("Status filter");
        statusFilter.setWidth("180px");
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

    private void openCreateAccountDialog() {
        new CreateAccountDialog(this, accountService, accountImageService, tenantService, this::loadPageZero).open();
    }

    public void openUpdateAccountDialog(Long accountId, Runnable onSuccess) {
        new UpdateAccountDialog(this, accountService, accountImageService, tenantService, accountId, onSuccess).open();
    }

    public void openResetPasswordDialog(Long accountId, String email) {
        new ResetPasswordDialog(this, accountService, accountId, email, () -> {
            Notification.show("Password reset email sent (if supported).", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }).open();
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
                .account-management-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                .account-cards-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
                    gap: var(--lumo-space-m);
                    padding: var(--lumo-space-s);
                }
                @media (max-width: 768px) {
                    .account-management-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .account-management-toolbar > * {
                        width: 100% !important;
                        justify-content: center;
                    }
                    .account-cards-grid {
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

    private record StatusFilterOption(String label,
                                      IEnumEnabledBinaryStatus.Types adminStatus,
                                      IEnumAccountSystemStatus.Types systemStatus) {
    }
}