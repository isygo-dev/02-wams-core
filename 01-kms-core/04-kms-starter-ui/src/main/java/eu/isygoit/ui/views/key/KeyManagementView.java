package eu.isygoit.ui.views.key;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.ListAliasesResponse;
import eu.isygoit.dto.KmsDtos.ListKeysResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainLayout;
import eu.isygoit.ui.views.key.dialog.CreateKeyDialog;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Route(value = "keys", layout = MainLayout.class)
@PageTitle("Key Management")
@PermitAll
public class KeyManagementView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final ObjectMapper objectMapper;

    private final VerticalLayout cardsContainer = new VerticalLayout();
    private final Button createButton = new Button("Create key", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ProgressBar loadingBar = new ProgressBar();

    // Pagination controls for keys (server-side cursor-based)
    private int pageSize = 10;
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageInfoLabel = new Span();      // shows "Page X/Y : N keys"
    private final Span totalCountLabel = new Span();    // shows "TotalElements keys found"

    // Key pagination state
    private final Stack<String> previousTokens = new Stack<>();
    private String currentNextToken = null;
    private String currentToken = null;
    private int currentPage = 1;
    private int totalPages = 0;
    private long totalElements = 0;
    private int numberOfElements = 0;
    private boolean truncated = false;
    private List<KeyCard> currentPageCards = new ArrayList<>();

    // Alias browser components
    private final Button toggleAliasBrowser = new Button("Browse Aliases", new Icon(VaadinIcon.LIST));
    private final VerticalLayout aliasBrowserPanel = new VerticalLayout();
    private final Grid<ListAliasesResponse.AliasEntry> aliasGrid = new Grid<>();
    private final HorizontalLayout aliasPaginationLayout = new HorizontalLayout();
    private final Button aliasPrevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button aliasNextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span aliasPageInfo = new Span();
    private final ComboBox<Integer> aliasPageSizeSelect = new ComboBox<>("Per page", 10, 20, 50);
    private final ProgressBar aliasLoading = new ProgressBar();

    // Alias pagination state (cursor-based)
    private final Stack<String> aliasPreviousTokens = new Stack<>();
    private String aliasCurrentNextToken = null;
    private int aliasCurrentLimit = 10;
    private boolean aliasesLoaded = false;

    // Filters
    private String currentSearch = "";
    private String currentStatus = "All";

    @Autowired
    public KeyManagementView(KmsApiService kmsApiService, ObjectMapper objectMapper) {
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-keys-view");

        H2 header = new H2("Key Management");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        // Toggle button for alias browser
        toggleAliasBrowser.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        toggleAliasBrowser.setTooltipText("Show/Hide alias browser");
        toggleAliasBrowser.addClickListener(e -> {
            if (aliasBrowserPanel.isVisible()) {
                aliasBrowserPanel.setVisible(false);
                toggleAliasBrowser.setIcon(new Icon(VaadinIcon.LIST));
                toggleAliasBrowser.setText("Browse Aliases");
            } else {
                aliasBrowserPanel.setVisible(true);
                toggleAliasBrowser.setIcon(new Icon(VaadinIcon.CLOSE));
                toggleAliasBrowser.setText("Hide Aliases");
                if (!aliasesLoaded) {
                    loadAliasesPage(null);
                }
            }
        });
        add(toggleAliasBrowser);

        buildAliasBrowser();
        aliasBrowserPanel.setVisible(false);
        add(aliasBrowserPanel);

        cardsContainer.setWidthFull();
        cardsContainer.setPadding(false);
        cardsContainer.setSpacing(true);
        add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        createButton.addClickListener(e -> openCreateKeyDialog());
        createButton.setTooltipText("Create a new KMS key");

        refreshButton.addClickListener(e -> resetKeyPaginationAndLoad());
        refreshButton.setTooltipText("Refresh keys from server");

        searchField.setPlaceholder("Search by alias or key ID");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setTooltipText("Filter keys by alias or key ID");
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            resetKeyPaginationAndLoad();
        });

        statusFilter.setItems("All", "Enabled", "Disabled", "PendingDeletion");
        statusFilter.setValue("All");
        statusFilter.setPlaceholder("Status");
        statusFilter.setTooltipText("Filter by key status");
        statusFilter.addValueChangeListener(e -> {
            currentStatus = e.getValue();
            resetKeyPaginationAndLoad();
        });

        pageSizeSelect.setItems(10, 20, 30, 40, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.setPlaceholder("Per page");
        pageSizeSelect.setTooltipText("Number of keys per page");
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                resetKeyPaginationAndLoad();
            }
        });

        prevButton.addClickListener(e -> {
            if (!previousTokens.isEmpty()) {
                String prevToken = previousTokens.pop();
                loadKeysPage(prevToken);
            }
        });
        prevButton.setTooltipText("Previous page");

        nextButton.addClickListener(e -> {
            if (truncated && currentNextToken != null) {
                previousTokens.push(currentToken);
                loadKeysPage(currentNextToken);
            }
        });
        nextButton.setTooltipText("Next page");

        injectResponsiveStyles();

        // Load first page of keys
        resetKeyPaginationAndLoad();
    }

    private void resetKeyPaginationAndLoad() {
        previousTokens.clear();
        currentNextToken = null;
        currentToken = null;
        currentPage = 1;
        totalPages = 0;
        totalElements = 0;
        numberOfElements = 0;
        truncated = false;
        loadKeysPage(null);
    }

    private void loadKeysPage(String nextToken) {
        showLoading(true);
        try {
            ResponseEntity<ListKeysResponse> response = kmsApiService.listKeys(pageSize, nextToken);
            ListKeysResponse body = response.getBody();
            List<ListKeysResponse.KeyEntry> keyEntries = (body != null && body.getKeys() != null) ? body.getKeys() : new ArrayList<>();
            currentNextToken = (body != null) ? body.getNextToken() : null;
            numberOfElements = (body != null && body.getNumberOfElements() != null) ? body.getNumberOfElements() : keyEntries.size();
            totalPages = (body != null && body.getTotalPages() != null) ? body.getTotalPages() : 0;
            totalElements = (body != null && body.getTotalElements() != null) ? body.getTotalElements() : 0L;
            truncated = (body != null && Boolean.TRUE.equals(body.getTruncated()));
            currentToken = nextToken;

            // Compute current page number from navigation stack
            if (nextToken == null) {
                currentPage = 1;
            } else {
                currentPage = previousTokens.size() + 1;
            }

            // Build KeyCard list from key entries
            List<KeyCard> cards = new ArrayList<>();
            for (ListKeysResponse.KeyEntry entry : keyEntries) {
                try {
                    ResponseEntity<DescribeKeyResponse> descResponse = kmsApiService.describeKey(entry.getKeyId());
                    DescribeKeyResponse describe = descResponse.getBody();
                    if (describe != null && describe.getKeyMetadata() != null) {
                        cards.add(new KeyCard(this, kmsApiService, objectMapper, entry.getKeyId(), describe.getKeyMetadata()));
                    } else {
                        cards.add(new KeyCard(this, kmsApiService, objectMapper, entry.getKeyId(), null));
                    }
                } catch (Exception ex) {
                    cards.add(new KeyCard(this, kmsApiService, objectMapper, entry.getKeyId(), null));
                }
            }
            currentPageCards = cards;
            updatePaginationDisplay();
            filterCards();
        } catch (Exception e) {
            Notification.show("Failed to load keys: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void updatePaginationDisplay() {
        // Display: "Page X/Y : N keys"
        if (totalPages > 0) {
            pageInfoLabel.setText(String.format("Page %d/%d : %d keys", currentPage, totalPages, numberOfElements));
        } else {
            // Fallback when totalPages is not available (e.g., initial load or zero)
            pageInfoLabel.setText(String.format("Page %d : %d keys", currentPage, numberOfElements));
        }
        // Display: "TotalElements keys found"
        totalCountLabel.setText(String.format("%d keys found", totalElements));

        prevButton.setEnabled(!previousTokens.isEmpty());
        nextButton.setEnabled(truncated && currentNextToken != null);
    }

    private void filterCards() {
        cardsContainer.removeAll();
        List<KeyCard> filtered = currentPageCards.stream()
                .filter(card -> {
                    if (!currentStatus.equals("All")) {
                        String status = card.getStatusText();
                        if (!status.equalsIgnoreCase(currentStatus)) return false;
                    }
                    if (currentSearch != null && !currentSearch.isEmpty()) {
                        String searchLower = currentSearch.toLowerCase();
                        return (card.getAliasOrId().toLowerCase().contains(searchLower) ||
                                card.getKeyId().toLowerCase().contains(searchLower));
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.KEY.create();
            emptyIcon.setSize("48px");
            emptyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
            H4 emptyTitle = new H4("No keys found");
            Paragraph emptyDesc = new Paragraph("Try adjusting your search or filter criteria.");
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
        } else {
            filtered.forEach(cardsContainer::add);
        }
    }

    // Alias browser with full tooltips and grid cell tooltips
    private void buildAliasBrowser() {
        aliasBrowserPanel.setSpacing(true);
        aliasBrowserPanel.setPadding(true);
        aliasBrowserPanel.setWidthFull();

        aliasLoading.setIndeterminate(true);
        aliasLoading.setVisible(false);
        aliasLoading.setWidth("100%");

        aliasGrid.setWidthFull();
        aliasGrid.setHeight("300px");

        // Add columns with tooltips for full content
        aliasGrid.addColumn(ListAliasesResponse.AliasEntry::getAliasName)
                .setHeader("Alias Name")
                .setTooltipGenerator(item -> item.getAliasName() != null ? item.getAliasName() : "");
        aliasGrid.addColumn(ListAliasesResponse.AliasEntry::getTargetKeyId)
                .setHeader("Target Key ID")
                .setTooltipGenerator(item -> item.getTargetKeyId() != null ? item.getTargetKeyId() : "");
        aliasGrid.setEmptyStateText("No aliases found.");

        aliasPageSizeSelect.setValue(10);
        aliasPageSizeSelect.setTooltipText("Number of aliases per page");
        aliasPageSizeSelect.addValueChangeListener(e -> {
            aliasCurrentLimit = e.getValue();
            resetAliasPagination();
            loadAliasesPage(null);
        });

        aliasPrevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        aliasNextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        aliasPrevButton.setTooltipText("Previous alias page");
        aliasNextButton.setTooltipText("Next alias page");
        aliasPrevButton.setEnabled(false);
        aliasNextButton.setEnabled(false);

        aliasPrevButton.addClickListener(e -> {
            if (!aliasPreviousTokens.isEmpty()) {
                String prevToken = aliasPreviousTokens.pop();
                loadAliasesPage(prevToken);
            }
        });
        aliasNextButton.addClickListener(e -> {
            if (aliasCurrentNextToken != null) {
                aliasPreviousTokens.push(aliasCurrentNextToken);
                loadAliasesPage(aliasCurrentNextToken);
            }
        });

        aliasPaginationLayout.add(aliasPrevButton, aliasPageInfo, aliasNextButton, aliasPageSizeSelect);
        aliasPaginationLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        aliasPaginationLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        aliasBrowserPanel.add(aliasLoading, aliasGrid, aliasPaginationLayout);
    }

    private void resetAliasPagination() {
        aliasPreviousTokens.clear();
        aliasCurrentNextToken = null;
        aliasPrevButton.setEnabled(false);
        aliasNextButton.setEnabled(false);
        aliasPageInfo.setText("");
        aliasesLoaded = false;
    }

    private void loadAliasesPage(String nextToken) {
        aliasLoading.setVisible(true);
        aliasGrid.setVisible(false);
        try {
            ResponseEntity<ListAliasesResponse> response = kmsApiService.listAliases(aliasCurrentLimit, nextToken);
            ListAliasesResponse body = response.getBody();
            List<ListAliasesResponse.AliasEntry> aliases = (body != null && body.getAliases() != null) ? body.getAliases() : new ArrayList<>();
            aliasGrid.setItems(aliases);
            aliasCurrentNextToken = (body != null) ? body.getNextToken() : null;
            aliasNextButton.setEnabled(aliasCurrentNextToken != null);
            aliasPrevButton.setEnabled(!aliasPreviousTokens.isEmpty());
            String info = aliases.isEmpty() ? "No results" : "Showing " + aliases.size() + " aliases";
            aliasPageInfo.setText(info);
            aliasPageInfo.getElement().setAttribute("title", info);
            aliasesLoaded = true;
        } catch (Exception e) {
            Notification.show("Failed to load aliases: " + e.getMessage(), 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            aliasGrid.setItems(new ArrayList<>());
            aliasNextButton.setEnabled(false);
        } finally {
            aliasLoading.setVisible(false);
            aliasGrid.setVisible(true);
        }
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("key-management-toolbar");

        // Left group
        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        searchField.setWidth("200px");
        Span statusLabel = new Span("Status:");
        statusLabel.getElement().setAttribute("title", "Filter by key status");
        statusFilter.setWidth("140px");
        HorizontalLayout statusLayout = new HorizontalLayout(statusLabel, statusFilter);
        statusLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        statusLayout.setSpacing(false);
        leftGroup.add(searchField, statusLayout);

        // Center group
        HorizontalLayout centerGroup = new HorizontalLayout();
        centerGroup.setSpacing(true);
        centerGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        centerGroup.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        pageSizeSelect.setWidth("120px");
        pageInfoLabel.getStyle().set("margin", "0 0.5rem");
        totalCountLabel.getStyle().set("margin", "0 0.5rem");
        centerGroup.add(prevButton, pageInfoLabel, nextButton, totalCountLabel, pageSizeSelect);

        // Right group
        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        rightGroup.setAlignItems(FlexComponent.Alignment.END);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText("Refresh keys");
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        rightGroup.add(refreshButton, createButton);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    private void injectResponsiveStyles() {
        String css = """
                .key-management-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                @media (max-width: 768px) {
                    .key-management-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .key-management-toolbar > * {
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

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        createButton.setEnabled(!show);
    }

    private void openCreateKeyDialog() {
        new CreateKeyDialog(this, kmsApiService, objectMapper, response -> {
            resetKeyPaginationAndLoad();
        }).open();
    }

    // Utility method for tag rows in other dialogs (kept for compatibility)
    public void addTagRow(VerticalLayout container, List<HorizontalLayout> rows, String existingKey, String existingValue) {
        String randomKey = (existingKey != null) ? existingKey : "tag-" + UUID.randomUUID().toString().substring(0, 8);
        TextField keyField = new TextField();
        keyField.setValue(randomKey);
        keyField.setReadOnly(true);
        keyField.setWidth("150px");
        keyField.setTooltipText(randomKey);
        TextField valueField = new TextField();
        valueField.setValue(existingValue != null ? existingValue : "");
        valueField.setPlaceholder("Tag value");
        valueField.setWidth("250px");
        valueField.setTooltipText(existingValue != null ? existingValue : "");
        Button removeBtn = new Button(new Icon(VaadinIcon.TRASH));
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        removeBtn.setTooltipText("Remove this tag");
        HorizontalLayout row = new HorizontalLayout(keyField, valueField, removeBtn);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        rows.add(row);
        container.add(row);
        removeBtn.addClickListener(e -> {
            container.remove(row);
            rows.remove(row);
        });
    }

    void copyToClipboard(String text) {
        UI.getCurrent().getPage().executeJs(
                "navigator.clipboard.writeText($0).then(() => { $0.dispatchEvent(new Event('copy-success')); }).catch(() => { $0.dispatchEvent(new Event('copy-error')); });",
                text
        );
        Notification.show("Key ID copied to clipboard", 1500, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}