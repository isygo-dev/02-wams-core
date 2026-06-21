package eu.isygoit.ui.kms.views.cryptography.keyStore;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.DescribeCustomKeyStoreResponse;
import eu.isygoit.dto.KmsDtos.ListCustomKeyStoresResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.cryptography.keyStore.dialog.CreateCustomKeyStoreDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

@Slf4j
@VaadinSessionScope
@Route(value = "kms/custom-key-stores", layout = KmsMainLayout.class)
@PageTitle("Custom Key Stores")
@PermitAll
public class CustomKeyStoresView extends VerticalLayout implements BeforeEnterObserver {

    private final KmsApiService kmsApiService;
    private final VerticalLayout cardsContainer = new VerticalLayout();
    private final Button createButton = new Button("Create Store", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final ProgressBar loadingBar = new ProgressBar();
    private final TextField filterField = new TextField();
    private final Button clearFilterButton = new Button(new Icon(VaadinIcon.CLOSE));

    // Pagination controls
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageInfoLabel = new Span();
    private final Span totalCountLabel = new Span();

    // Pagination state (cursor-based)
    private final Stack<String> previousTokens = new Stack<>();
    private int pageSize = 10;
    private String currentNextToken = null;
    private String currentToken = null;
    private int currentPage = 1;
    private int totalPages = 0;
    private long totalElements = 0;
    private int numberOfElements = 0;
    private boolean truncated = false;

    // Filter state
    private String currentFilter = "";

    // Store data for current page (unfiltered)
    private List<DescribeCustomKeyStoreResponse.CustomKeyStore> currentPageStores = new ArrayList<>();

    @Autowired
    public CustomKeyStoresView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-custom-stores-view");

        H2 header = new H2("Custom Key Stores");
        header.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.Margin.Bottom.NONE);
        add(header);

        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        cardsContainer.setWidthFull();
        cardsContainer.setPadding(false);
        cardsContainer.setSpacing(true);
        add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        createButton.addClickListener(e -> createCustomKeyStore());
        refreshButton.addClickListener(e -> resetPaginationAndLoad());

        filterField.setPlaceholder("Filter by name or type...");
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setValueChangeTimeout(300);
        filterField.setWidth("250px");
        filterField.addValueChangeListener(e -> {
            currentFilter = e.getValue();
            applyFilter();
        });

        clearFilterButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearFilterButton.setTooltipText("Clear filter");
        clearFilterButton.setEnabled(false);
        clearFilterButton.addClickListener(e -> {
            filterField.clear();
            currentFilter = "";
            applyFilter();
        });

        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.setPlaceholder("Per page");
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                resetPaginationAndLoad();
            }
        });

        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        prevButton.addClickListener(e -> {
            if (!previousTokens.isEmpty()) {
                String prevToken = previousTokens.pop();
                loadStoresPage(prevToken);
            }
        });

        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addClickListener(e -> {
            if (truncated && currentNextToken != null) {
                previousTokens.push(currentToken);
                loadStoresPage(currentNextToken);
            }
        });

        injectResponsiveStyles();

        // Initial load
        resetPaginationAndLoad();
    }

    private void resetPaginationAndLoad() {
        previousTokens.clear();
        currentNextToken = null;
        currentToken = null;
        currentPage = 1;
        totalPages = 0;
        totalElements = 0;
        numberOfElements = 0;
        truncated = false;
        loadStoresPage(null);
    }

    private void loadStoresPage(String nextToken) {
        showLoading(true);
        try {
            ResponseEntity<ListCustomKeyStoresResponse> response = kmsApiService.listCustomKeyStores(pageSize, nextToken);
            ListCustomKeyStoresResponse body = response.getBody();
            List<DescribeCustomKeyStoreResponse.CustomKeyStore> stores = (body != null && body.getCustomKeyStores() != null)
                    ? body.getCustomKeyStores() : new ArrayList<>();

            currentNextToken = (body != null) ? body.getNextToken() : null;
            numberOfElements = (body != null && body.getNumberOfElements() != null) ? body.getNumberOfElements() : stores.size();
            totalPages = (body != null && body.getTotalPages() != null) ? body.getTotalPages() : 0;
            totalElements = (body != null && body.getTotalElements() != null) ? body.getTotalElements() : 0L;
            truncated = (body != null && Boolean.TRUE.equals(body.getTruncated()));
            currentToken = nextToken;

            if (nextToken == null) {
                currentPage = 1;
            } else {
                currentPage = previousTokens.size() + 1;
            }

            currentPageStores = stores;
            updatePaginationDisplay();
            applyFilter();
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            Notification.show("Failed to load stores: " + errorMsg, 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load custom key stores: {}", errorMsg);
            currentPageStores = new ArrayList<>();
            updatePaginationDisplay();
            showEmptyState();
        } catch (Exception e) {
            Notification.show("Failed to load stores: " + e.getMessage(), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load custom key stores: {}", e.getMessage());
            currentPageStores = new ArrayList<>();
            updatePaginationDisplay();
            showEmptyState();
        } finally {
            showLoading(false);
        }
    }

    private void updatePaginationDisplay() {
        if (totalPages > 0) {
            pageInfoLabel.setText(String.format("Page %d/%d : %d stores", currentPage, totalPages, numberOfElements));
        } else {
            pageInfoLabel.setText(String.format("Page %d : %d stores", currentPage, numberOfElements));
        }
        totalCountLabel.setText(String.format("%d stores total", totalElements));

        prevButton.setEnabled(!previousTokens.isEmpty());
        nextButton.setEnabled(truncated && currentNextToken != null);
    }

    private void applyFilter() {
        cardsContainer.removeAll();

        List<DescribeCustomKeyStoreResponse.CustomKeyStore> filtered = currentPageStores.stream()
                .filter(s -> {
                    if (StringUtils.hasText(currentFilter)) {
                        String lower = currentFilter.toLowerCase();
                        boolean nameMatch = s.getName() != null && s.getName().toLowerCase().contains(lower);
                        boolean typeMatch = s.getCustomKeyStoreType() != null && s.getCustomKeyStoreType().toLowerCase().contains(lower);
                        return nameMatch || typeMatch;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        clearFilterButton.setEnabled(StringUtils.hasText(currentFilter));

        if (filtered.isEmpty()) {
            showEmptyState();
        } else {
            filtered.forEach(s -> cardsContainer.add(new StoreCard(this, kmsApiService, s)));
        }
    }

    private void showEmptyState() {
        cardsContainer.removeAll();
        Div emptyState = new Div();
        emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
        emptyState.addClassName(LumoUtility.Padding.XLARGE);
        Icon emptyIcon = VaadinIcon.STORAGE.create();
        emptyIcon.setSize("48px");
        emptyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
        H4 emptyTitle = new H4("No custom key stores found");
        Span emptyDesc = new Span("Click 'Create Store' to add one.");
        emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
        emptyState.add(emptyIcon, emptyTitle, emptyDesc);
        cardsContainer.add(emptyState);
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("custom-stores-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.BASELINE);
        leftGroup.add(filterField, clearFilterButton);

        HorizontalLayout centerGroup = new HorizontalLayout();
        centerGroup.setSpacing(true);
        centerGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        pageSizeSelect.setWidth("120px");
        centerGroup.add(prevButton, pageInfoLabel, nextButton, totalCountLabel, pageSizeSelect);

        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        rightGroup.setAlignItems(FlexComponent.Alignment.END);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText("Refresh stores");
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.setTooltipText("Create a new custom key store");
        rightGroup.add(refreshButton, createButton);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    private void injectResponsiveStyles() {
        String css = """
                .kms-custom-stores-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .custom-stores-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                @media (max-width: 768px) {
                    .custom-stores-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .custom-stores-toolbar > * {
                        width: 100% !important;
                        justify-content: center;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    private void createCustomKeyStore() {
        new CreateCustomKeyStoreDialog(this, kmsApiService, this::resetPaginationAndLoad).open();
    }

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        createButton.setEnabled(!show);
        refreshButton.setEnabled(!show);
        filterField.setEnabled(!show);
        prevButton.setEnabled(!show && !previousTokens.isEmpty());
        nextButton.setEnabled(!show && (truncated && currentNextToken != null));
        pageSizeSelect.setEnabled(!show);
    }

    public void loadStores() {
        resetPaginationAndLoad();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("user") == null) {
            String currentPath = event.getLocation().getPath();
            event.forwardTo("login?redirect=" + currentPath);
        }
    }
}