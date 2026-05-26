package eu.isygoit.ui.views.keyStore;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.DescribeCustomKeyStoreResponse;
import eu.isygoit.dto.KmsDtos.ListCustomKeyStoresResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainLayout;
import eu.isygoit.ui.views.keyStore.dialog.CreateCustomKeyStoreDialog;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "custom-key-stores", layout = MainLayout.class)
@PageTitle("Custom Key Stores")
@PermitAll
public class CustomKeyStoresView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final VerticalLayout cardsContainer = new VerticalLayout();
    private final Button createButton = new Button("Create Store", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final ProgressBar loadingBar = new ProgressBar();
    private final TextField filterField = new TextField();
    private final Button clearFilterButton = new Button(new Icon(VaadinIcon.CLOSE));

    private List<DescribeCustomKeyStoreResponse.CustomKeyStore> allStores = new ArrayList<>();

    @Autowired
    public CustomKeyStoresView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-custom-stores-view");

        H2 header = new H2("Custom Key Stores");
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

        createButton.addClickListener(e -> createCustomKeyStore());
        refreshButton.addClickListener(e -> loadStores());

        injectResponsiveStyles();

        loadStores();
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.addClassName("custom-stores-toolbar");

        // Left group: filter
        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.addClassName("toolbar-left-group");
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.BASELINE);

        filterField.setPlaceholder("Filter by name or type...");
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setValueChangeTimeout(300);
        filterField.setWidth("250px");
        filterField.addValueChangeListener(e -> applyFilter());

        clearFilterButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearFilterButton.setTooltipText("Clear filter");
        clearFilterButton.setEnabled(false);
        clearFilterButton.addClickListener(e -> {
            filterField.clear();
            applyFilter();
        });

        leftGroup.add(filterField, clearFilterButton);

        // Right group: create + refresh buttons
        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.addClassName("toolbar-right-group");
        rightGroup.setSpacing(true);
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText("Refresh stores");
        rightGroup.add(createButton, refreshButton);

        toolbar.add(leftGroup, rightGroup);
        return toolbar;
    }

    private void injectResponsiveStyles() {
        String css = """
                    .custom-stores-toolbar {
                        display: flex;
                        flex-wrap: wrap;
                        gap: var(--lumo-space-s);
                        align-items: center;
                        justify-content: space-between;
                        width: 100%;
                        margin-bottom: var(--lumo-space-m);
                    }
                    .toolbar-left-group,
                    .toolbar-right-group {
                        display: flex;
                        flex-wrap: wrap;
                        gap: var(--lumo-space-s);
                        align-items: center;
                    }
                    @media (max-width: 768px) {
                        .custom-stores-toolbar {
                            flex-direction: column;
                            align-items: stretch;
                        }
                        .toolbar-left-group,
                        .toolbar-right-group {
                            flex-direction: column;
                            align-items: stretch;
                            width: 100%;
                        }
                        .toolbar-left-group > *,
                        .toolbar-right-group > * {
                            width: 100% !important;
                        }
                    }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    private void createCustomKeyStore() {
        new CreateCustomKeyStoreDialog(this, kmsApiService, this::loadStores).open();
    }

    public void loadStores() {
        showLoading(true);
        try {
            ResponseEntity<ListCustomKeyStoresResponse> response = kmsApiService.listCustomKeyStores(100, null);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                allStores = response.getBody().getCustomKeyStores();
                if (allStores == null) allStores = new ArrayList<>();
                applyFilter();
            } else {
                allStores = new ArrayList<>();
                showEmptyState();
            }
        } catch (Exception e) {
            Notification.show("Failed to load stores: " + e.getMessage(), 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            showEmptyState();
        } finally {
            showLoading(false);
        }
    }

    private void applyFilter() {
        String filter = filterField.getValue();
        clearFilterButton.setEnabled(StringUtils.hasText(filter));
        cardsContainer.removeAll();

        List<DescribeCustomKeyStoreResponse.CustomKeyStore> filtered;
        if (!StringUtils.hasText(filter)) {
            filtered = allStores;
        } else {
            String lowerFilter = filter.toLowerCase();
            filtered = allStores.stream()
                    .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(lowerFilter)) ||
                            (s.getCustomKeyStoreType() != null && s.getCustomKeyStoreType().toLowerCase().contains(lowerFilter)))
                    .collect(Collectors.toList());
        }

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

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        createButton.setEnabled(!show);
        refreshButton.setEnabled(!show);
        filterField.setEnabled(!show);
    }
}