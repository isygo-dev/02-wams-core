package eu.isygoit.ui.views.key;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
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
import eu.isygoit.ui.views.key.dialogs.CreateKeyDialog;
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

    private final VerticalLayout cardsContainer = new VerticalLayout();
    private final Button createButton = new Button("Create key", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>("Status");
    private final ProgressBar loadingBar = new ProgressBar();
    private final ObjectMapper objectMapper;
    public List<String> existingAliases = new ArrayList<>();
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
            filterCards();
        });

        statusFilter.setItems("All", "Enabled", "Disabled", "PendingDeletion");
        statusFilter.setValue("All");
        statusFilter.addValueChangeListener(e -> {
            currentStatus = e.getValue();
            filterCards();
        });

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

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.getStyle().set("flex-wrap", "wrap");
        toolbar.addClassName(LumoUtility.Padding.Bottom.MEDIUM);

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        leftGroup.setSpacing(true);
        searchField.setWidth("280px");
        statusFilter.setWidth("160px");
        statusFilter.setPlaceholder("Filter by status");
        leftGroup.add(searchField, statusFilter);

        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText("Refresh keys");
        rightGroup.add(createButton, refreshButton);

        toolbar.add(leftGroup, rightGroup);
        toolbar.expand(leftGroup);
        return toolbar;
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
                            allCards.add(new KeyCard(this, this.kmsApiService, this.objectMapper, entry.getKeyId(), describe.getKeyMetadata()));
                        } else {
                            allCards.add(new KeyCard(this, this.kmsApiService, this.objectMapper, entry.getKeyId(), null));
                        }
                    } catch (Exception ex) {
                        allCards.add(new KeyCard(this, this.kmsApiService, this.objectMapper, entry.getKeyId(), null));
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

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        createButton.setEnabled(!show);
    }

    // =========================================================================
    // Create Key Dialog
    // =========================================================================
    private void openCreateKeyDialog() {
        new CreateKeyDialog(this, kmsApiService, objectMapper).open();
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
        // Optional: show a small notification
        Notification.show("Key ID copied to clipboard", 1500, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}