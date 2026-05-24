package eu.isygoit.ui.views.key;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageLabel = new Span();
    public List<String> existingAliases = new ArrayList<>();
    // Pagination controls
    private int currentPage = 1;
    private int pageSize = 10;
    private List<KeyCard> allCards = new ArrayList<>();
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

        cardsContainer.setWidthFull();
        cardsContainer.setPadding(false);
        cardsContainer.setSpacing(true);
        add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        createButton.addClickListener(e -> openCreateKeyDialog());
        refreshButton.addClickListener(e -> {
            loadAliases();
            loadKeys();
        });
        searchField.setPlaceholder("Search by alias or key ID");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            currentPage = 1;
            filterCards();
        });

        statusFilter.setItems("All", "Enabled", "Disabled", "PendingDeletion");
        statusFilter.setValue("All");
        statusFilter.setPlaceholder("Status");
        statusFilter.addValueChangeListener(e -> {
            currentStatus = e.getValue();
            currentPage = 1;
            filterCards();
        });

        pageSizeSelect.setItems(10, 20, 30, 40, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.setPlaceholder("Per page");
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                currentPage = 1;
                filterCards();
            }
        });

        prevButton.addClickListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                filterCards();
            }
        });
        nextButton.addClickListener(e -> {
            int totalPages = getTotalPages();
            if (currentPage < totalPages) {
                currentPage++;
                filterCards();
            }
        });

        injectResponsiveStyles();
        loadAliases();
        loadKeys();
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("key-management-toolbar"); // add class for CSS

        // Left group
        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        searchField.setWidth("200px");
        Span statusLabel = new Span("Status:");
        statusLabel.getStyle().set("margin-right", "4px");
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
        pageLabel.getStyle().set("margin", "0 0.5rem");
        centerGroup.add(prevButton, pageLabel, nextButton, pageSizeSelect);

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

    private void updatePaginationDisplay(int totalItems) {
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) totalPages = 1;
        pageLabel.setText(String.format("Page %d of %d", currentPage, totalPages));
        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
    }

    private int getTotalPages() {
        int totalFiltered = (int) allCards.stream()
                .filter(this::matchesFilter)
                .count();
        return (int) Math.ceil((double) totalFiltered / pageSize);
    }

    private boolean matchesFilter(KeyCard card) {
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
    }

    private void injectResponsiveStyles() {
        String css = """
                /* Base toolbar: flex row, wrap, justify between */
                .key-management-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                /* On narrow screens, each group takes full width and centers its content */
                @media (max-width: 768px) {
                    .key-management-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .key-management-toolbar > * {
                        width: 100% !important;
                        justify-content: center;
                    }
                    /* Keep inner elements inline (horizontal) */
                    .key-management-toolbar > * > * {
                        justify-content: center;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
        // Add class to toolbar for CSS targeting
        getElement().executeJs(
                "const toolbar = document.querySelector('.key-management-toolbar'); if(toolbar) toolbar.classList.add('key-management-toolbar');"
        );
    }

    // Helper to add class to the toolbar
    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // The toolbar is already created; we can add class via element
        getElement().getChildren()
                .filter(child -> child.getAttribute("class") != null && child.getAttribute("class").contains("key-management-toolbar"))
                .findFirst()
                .ifPresent(toolbar -> toolbar.getClassList().add("key-management-toolbar"));
    }

    public void loadAliasesAndKeys() {
        loadAliases();
        loadKeys();
    }

    public void loadAliases() {
        try {
            ResponseEntity<ListAliasesResponse> response = kmsApiService.listAliases(100, null);
            ListAliasesResponse aliasesResponse = response.getBody();
            if (aliasesResponse != null && aliasesResponse.getAliases() != null) {
                existingAliases = aliasesResponse.getAliases().stream()
                        .map(ListAliasesResponse.AliasEntry::getAliasName)
                        .collect(Collectors.toList());
            } else {
                existingAliases = new ArrayList<>();
            }
        } catch (Exception e) {
            Notification.show("Failed to load aliases: " + e.getMessage(), 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            existingAliases = new ArrayList<>();
        }
    }

    public void loadKeys() {
        showLoading(true);
        allCards.clear();
        cardsContainer.removeAll();
        try {
            ResponseEntity<ListKeysResponse> response = kmsApiService.listKeys(100, null);
            ListKeysResponse keysResponse = response.getBody();
            if (keysResponse != null && keysResponse.getKeys() != null) {
                for (ListKeysResponse.KeyEntry entry : keysResponse.getKeys()) {
                    try {
                        ResponseEntity<DescribeKeyResponse> descResponse =
                                kmsApiService.describeKey(entry.getKeyId());
                        DescribeKeyResponse describe = descResponse.getBody();
                        if (describe != null && describe.getKeyMetadata() != null) {
                            allCards.add(new KeyCard(this,
                                    this.kmsApiService, this.objectMapper, entry.getKeyId(), describe.getKeyMetadata()));
                        } else {
                            allCards.add(new KeyCard(this,
                                    this.kmsApiService, this.objectMapper, entry.getKeyId(), null));
                        }
                    } catch (Exception ex) {
                        allCards.add(new KeyCard(this,
                                this.kmsApiService, this.objectMapper, entry.getKeyId(), null));
                    }
                }
            }
            filterCards();
        } catch (Exception e) {
            Notification.show("Failed to load keys: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void filterCards() {
        cardsContainer.removeAll();

        List<KeyCard> filtered = allCards.stream()
                .filter(this::matchesFilter)
                .collect(Collectors.toList());

        int totalFiltered = filtered.size();
        updatePaginationDisplay(totalFiltered);

        if (totalFiltered == 0) {
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
            return;
        }

        int startIndex = (currentPage - 1) * pageSize;
        if (startIndex >= totalFiltered) {
            currentPage = (int) Math.ceil((double) totalFiltered / pageSize);
            startIndex = (currentPage - 1) * pageSize;
        }
        int endIndex = Math.min(startIndex + pageSize, totalFiltered);
        List<KeyCard> pageCards = filtered.subList(startIndex, endIndex);
        pageCards.forEach(cardsContainer::add);
    }

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        createButton.setEnabled(!show);
    }

    private void openCreateKeyDialog() {
        new CreateKeyDialog(this, kmsApiService, this::loadAliasesAndKeys, objectMapper).open();
    }

    public void addTagRow(VerticalLayout container, List<HorizontalLayout> rows, String existingKey, String existingValue) {
        String randomKey = (existingKey != null) ? existingKey : "tag-" + UUID.randomUUID().toString().substring(0, 8);
        TextField keyField = new TextField();
        keyField.setValue(randomKey);
        keyField.setReadOnly(true);
        keyField.setWidth("150px");
        TextField valueField = new TextField();
        valueField.setValue(existingValue != null ? existingValue : "");
        valueField.setPlaceholder("Tag value");
        valueField.setWidth("250px");
        Button removeBtn = new Button(new Icon(VaadinIcon.TRASH));
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
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
                "navigator.clipboard.writeText($0).then(() => {" +
                        "  $0.dispatchEvent(new Event('copy-success'));" +
                        "}).catch(() => {" +
                        "  $0.dispatchEvent(new Event('copy-error'));" +
                        "});",
                text
        );
        Notification.show("Key ID copied to clipboard", 1500, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}