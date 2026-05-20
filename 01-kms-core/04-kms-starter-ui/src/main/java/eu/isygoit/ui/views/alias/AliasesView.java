package eu.isygoit.ui.views.alias;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import eu.isygoit.dto.KmsDtos.ListAliasesResponse;
import eu.isygoit.dto.KmsDtos.ListKeysResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainLayout;
import eu.isygoit.ui.views.alias.dialog.CreateAliasDialog;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
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
    private List<AliasCard> allCards = new ArrayList<>();
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
        refreshButton.addClickListener(e -> loadAliases());
        searchField.setPlaceholder("Search by alias name or target key ID");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            filterCards();
        });

        loadAliases();
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
        searchField.setWidth("300px");
        leftGroup.add(searchField);

        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText("Refresh aliases");
        rightGroup.add(createButton, refreshButton);

        toolbar.add(leftGroup, rightGroup);
        toolbar.expand(leftGroup);
        return toolbar;
    }

    public void loadAliases() {
        showLoading(true);
        allCards.clear();
        cardsContainer.removeAll();
        try {
            ResponseEntity<ListAliasesResponse> response = kmsApiService.listAliases(100, null);
            ListAliasesResponse aliasesResponse = response.getBody();
            if (aliasesResponse != null && aliasesResponse.getAliases() != null) {
                for (ListAliasesResponse.AliasEntry entry : aliasesResponse.getAliases()) {
                    allCards.add(new AliasCard(this, kmsApiService, entry));
                }
            }
            filterCards();
        } catch (Exception e) {
            Notification.show("Failed to load aliases: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void filterCards() {
        cardsContainer.removeAll();
        List<AliasCard> filtered = allCards.stream()
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

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        createButton.setEnabled(!show);
    }

    private void createAlias() {
        new CreateAliasDialog(this, kmsApiService, this::loadAliases).open();
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
            Notification.show("Could not load keys: " + e.getMessage(), 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        return keyIds;
    }

}