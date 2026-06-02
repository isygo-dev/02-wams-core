package eu.isygoit.ui.views.keyAlias;

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
import eu.isygoit.dto.KmsDtos.ListAliasesResponse;
import eu.isygoit.dto.KmsDtos.ListKeysResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainLayout;
import eu.isygoit.ui.views.keyAlias.dialog.CreateAliasDialog;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

@Route(value = "aliases", layout = MainLayout.class)
@PageTitle("Key Aliases")
@PermitAll
public class AliasesView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final VerticalLayout cardsContainer = new VerticalLayout();
    private final Button createButton = new Button("Create alias", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ProgressBar loadingBar = new ProgressBar();
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageInfoLabel = new Span();   // "Page X/Y : N aliases"
    private final Span totalCountLabel = new Span(); // "Total: N aliases"
    // Pagination state (cursor‑based)
    private final Stack<String> previousTokens = new Stack<>();
    // Pagination controls
    private int pageSize = 10;
    private String currentNextToken = null;
    private String currentToken = null;
    private int currentPage = 1;
    private int totalPages = 0;
    private long totalElements = 0;
    private int numberOfElements = 0;
    private boolean truncated = false;
    private List<AliasCard> currentPageCards = new ArrayList<>();

    // Client‑side filter
    private String currentSearch = "";

    @Autowired
    public AliasesView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-aliases-view");

        H2 header = new H2("Key Aliases");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
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

        createButton.addClickListener(e -> createAlias());
        createButton.setTooltipText("Create a new alias for a KMS key");

        refreshButton.addClickListener(e -> resetPaginationAndLoad());
        refreshButton.setTooltipText("Refresh aliases from server");

        searchField.setPlaceholder("Search by alias name or target key ID");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setTooltipText("Filter aliases by name or target key ID");
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            filterCards();
        });

        pageSizeSelect.setItems(10, 20, 30, 40, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.setPlaceholder("Per page");
        pageSizeSelect.setTooltipText("Number of aliases per page");
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                resetPaginationAndLoad();
            }
        });

        prevButton.addClickListener(e -> {
            if (!previousTokens.isEmpty()) {
                String prevToken = previousTokens.pop();
                loadAliasesPage(prevToken);
            }
        });
        prevButton.setTooltipText("Previous page");

        nextButton.addClickListener(e -> {
            if (truncated && currentNextToken != null) {
                previousTokens.push(currentToken);
                loadAliasesPage(currentNextToken);
            }
        });
        nextButton.setTooltipText("Next page");

        pageInfoLabel.getElement().setAttribute("title", "Current page / total pages and number of aliases on this page");
        totalCountLabel.getElement().setAttribute("title", "Total number of aliases across all pages");

        injectResponsiveStyles();

        // Load first page
        resetPaginationAndLoad();
    }

    void resetPaginationAndLoad() {
        previousTokens.clear();
        currentNextToken = null;
        currentToken = null;
        currentPage = 1;
        totalPages = 0;
        totalElements = 0;
        numberOfElements = 0;
        truncated = false;
        loadAliasesPage(null);
    }

    private void loadAliasesPage(String nextToken) {
        showLoading(true);
        try {
            ResponseEntity<ListAliasesResponse> response = kmsApiService.listAliases(pageSize, nextToken);
            ListAliasesResponse body = response.getBody();
            List<ListAliasesResponse.AliasEntry> aliasEntries = (body != null && body.getAliases() != null)
                    ? body.getAliases() : new ArrayList<>();

            currentNextToken = (body != null) ? body.getNextToken() : null;
            numberOfElements = (body != null && body.getNumberOfElements() != null)
                    ? body.getNumberOfElements() : aliasEntries.size();
            totalPages = (body != null && body.getTotalPages() != null) ? body.getTotalPages() : 0;
            totalElements = (body != null && body.getTotalElements() != null) ? body.getTotalElements() : 0L;
            truncated = (body != null && Boolean.TRUE.equals(body.getTruncated()));
            currentToken = nextToken;

            if (nextToken == null) {
                currentPage = 1;
            } else {
                currentPage = previousTokens.size() + 1;
            }

            // Build AliasCard list
            List<AliasCard> cards = new ArrayList<>();
            for (ListAliasesResponse.AliasEntry entry : aliasEntries) {
                cards.add(new AliasCard(this, kmsApiService, entry));
            }
            currentPageCards = cards;
            updatePaginationDisplay();
            filterCards();
        } catch (Exception e) {
            Notification.show("Failed to load aliases: " + e.getMessage(), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void updatePaginationDisplay() {
        if (totalPages > 0) {
            pageInfoLabel.setText(String.format("Page %d/%d : %d aliases", currentPage, totalPages, numberOfElements));
        } else {
            pageInfoLabel.setText(String.format("Page %d : %d aliases", currentPage, numberOfElements));
        }
        totalCountLabel.setText(String.format("%d aliases found", totalElements));
        prevButton.setEnabled(!previousTokens.isEmpty());
        nextButton.setEnabled(truncated && currentNextToken != null);
    }

    private void filterCards() {
        cardsContainer.removeAll();
        List<AliasCard> filtered = currentPageCards.stream()
                .filter(card -> {
                    if (currentSearch == null || currentSearch.isEmpty()) return true;
                    String searchLower = currentSearch.toLowerCase();
                    return card.getAliasName().toLowerCase().contains(searchLower) ||
                            card.getTargetKeyId().toLowerCase().contains(searchLower);
                })
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.TAG.create();
            emptyIcon.setSize("48px");
            emptyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
            H4 emptyTitle = new H4("No aliases found");
            Paragraph emptyDesc = new Paragraph("Create an alias to give your KMS keys friendly names.");
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
        } else {
            filtered.forEach(cardsContainer::add);
        }
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("aliases-toolbar");

        // Left group: search field only (no status filter for aliases)
        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        searchField.setWidth("200px");
        leftGroup.add(searchField);

        // Center group: pagination controls (identical to KeyManagementView)
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

        // Right group: refresh + create button (refresh moved from left to right)
        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        rightGroup.setAlignItems(FlexComponent.Alignment.END);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText("Refresh aliases");
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        rightGroup.add(refreshButton, createButton);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    private void injectResponsiveStyles() {
        String css = """
                .aliases-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                @media (max-width: 768px) {
                    .aliases-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .aliases-toolbar > * {
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

    private void createAlias() {
        new CreateAliasDialog(this, kmsApiService, this::resetPaginationAndLoad).open();
    }

    public List<String> fetchKeyIds() {
        List<String> keyIds = new ArrayList<>();
        try {
            ResponseEntity<ListKeysResponse> response = kmsApiService.listKeys(100, null);
            ListKeysResponse keys = response.getBody();
            if (keys != null && keys.getKeys() != null) {
                keyIds = keys.getKeys().stream()
                        .map(ListKeysResponse.KeyEntry::getKeyId)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            Notification.show("Could not load keys: " + e.getMessage(), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        return keyIds;
    }
}