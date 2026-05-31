package eu.isygoit.ui.views.incremental;

import com.vaadin.flow.component.Composite;
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
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.ui.MainLayout;
import eu.isygoit.ui.views.incremental.dialog.SubscribeDialog;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Route(value = "incremental-key", layout = MainLayout.class)
@PageTitle("Incremental Key Configurations")
@PermitAll
public class IncrementalKeyView extends Composite<VerticalLayout> {

    @Getter
    private final KmsIncrementalKeyService incrementalKeyService;
    @Getter
    private final KmsAppNextCodeService nextCodeService;

    private final VerticalLayout cardsContainer = new VerticalLayout();
    private final TextField searchField = new TextField();
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Button subscribeButton = new Button("Subscribe", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final ProgressBar loadingBar = new ProgressBar();

    // Pagination
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageInfoLabel = new Span();
    private final Span totalCountLabel = new Span();

    private int currentPage = 0;       // 0‑based page index
    private int pageSize = 10;
    private int totalPages = 0;
    private long totalElements = 0;
    private List<NextCodeDto> currentPageContent = new ArrayList<>();
    private String currentSearch = "";

    @Autowired
    public IncrementalKeyView(KmsIncrementalKeyService incrementalKeyService,
                              KmsAppNextCodeService nextCodeService) {
        this.incrementalKeyService = incrementalKeyService;
        this.nextCodeService = nextCodeService;
        buildUI();
        loadNextCodes();
    }

    private void buildUI() {
        VerticalLayout layout = getContent();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.addClassName("incremental-keys-view");

        H2 header = new H2("Incremental Key Configurations");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        layout.add(header);

        HorizontalLayout toolbar = buildToolbar();
        layout.add(toolbar);

        cardsContainer.setWidthFull();
        cardsContainer.setPadding(false);
        cardsContainer.setSpacing(true);
        layout.add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        layout.add(loadingBar);

        refreshButton.addClickListener(e -> {
            currentPage = 0;
            loadNextCodes();
        });
        refreshButton.setTooltipText("Refresh configurations");

        subscribeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        subscribeButton.addClickListener(e -> openSubscribeDialog());

        searchField.setPlaceholder("Search by entity or attribute");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            currentPage = 0;
            loadNextCodes();
        });

        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                currentPage = 0;
                loadNextCodes();
            }
        });

        prevButton.addClickListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadNextCodes();
            }
        });
        nextButton.addClickListener(e -> {
            if (currentPage + 1 < totalPages) {
                currentPage++;
                loadNextCodes();
            }
        });

        injectResponsiveStyles();
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("incremental-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout(searchField);
        searchField.setWidth("250px");

        HorizontalLayout centerGroup = new HorizontalLayout();
        centerGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        pageSizeSelect.setWidth("100px");
        pageInfoLabel.getStyle().set("margin", "0 0.5rem");
        totalCountLabel.getStyle().set("margin", "0 0.5rem");
        centerGroup.add(prevButton, pageInfoLabel, nextButton, totalCountLabel, pageSizeSelect);

        HorizontalLayout rightGroup = new HorizontalLayout(refreshButton, subscribeButton);
        rightGroup.setSpacing(true);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        subscribeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    public void loadNextCodes() {
        showLoading(true);
        try {
            ResponseEntity<PaginatedResponseDto<NextCodeDto>> response;
            if (currentSearch == null || currentSearch.isBlank()) {
                response = nextCodeService.findAll(currentPage, pageSize);
            } else {
                String criteria = "entity~" + currentSearch + ",OR attribute~" + currentSearch;
                response = nextCodeService.findAllFilteredByCriteria(criteria, currentPage, pageSize);
            }

            PaginatedResponseDto<NextCodeDto> body = response.getBody();
            if (body != null) {
                currentPageContent = body.getContent() != null ? body.getContent() : new ArrayList<>();
                totalPages = body.getTotalPages();
                totalElements = body.getTotalElements();
                if (currentPage >= totalPages && totalPages > 0) {
                    currentPage = totalPages - 1;
                    loadNextCodes();
                    return;
                }
            } else {
                currentPageContent = new ArrayList<>();
                totalPages = 0;
                totalElements = 0;
            }
            updatePaginationDisplay();
            renderCards();
        } catch (Exception e) {
            Notification.show("Failed to load configurations: " + e.getMessage(), 5000,
                            Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void updatePaginationDisplay() {
        if (totalPages > 0) {
            pageInfoLabel.setText(String.format("Page %d/%d", currentPage + 1, totalPages));
        } else {
            pageInfoLabel.setText("Page 0/0");
        }
        totalCountLabel.setText(String.format("%d configs", totalElements));
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage + 1 < totalPages);
    }

    private void renderCards() {
        cardsContainer.removeAll();
        if (currentPageContent.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.KEY.create();
            emptyIcon.setSize("48px");
            emptyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
            H4 emptyTitle = new H4("No configurations found");
            Paragraph emptyDesc = new Paragraph("Click 'Subscribe' to create a new incremental key generator.");
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
            return;
        }

        for (NextCodeDto dto : currentPageContent) {
            NextCodeCard card = new NextCodeCard(
                    this,
                    nextCodeService,
                    dto,
                    () -> deleteConfig(dto.getId()),
                    this::generateCodeForConfig
            );
            cardsContainer.add(card);
        }
    }

    /**
     * Refreshes a single card after code generation or other updates.
     */
    public void refreshCard(NextCodeCard card) {
        try {
            ResponseEntity<NextCodeDto> response = nextCodeService.findById(card.getDto().getId());
            NextCodeDto updated = response.getBody();
            if (updated != null) {
                card.updateDto(updated);
                // Keep the in‑memory list consistent
                for (int i = 0; i < currentPageContent.size(); i++) {
                    if (currentPageContent.get(i).getId().equals(updated.getId())) {
                        currentPageContent.set(i, updated);
                        break;
                    }
                }
            } else {
                // If not found, remove the card and reload the page
                cardsContainer.remove(card);
                loadNextCodes();
            }
        } catch (Exception e) {
            Notification.show("Failed to refresh card: " + e.getMessage(), 3000,
                            Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void deleteConfig(Long id) {
        try {
            nextCodeService.delete(id);
            Notification.show("Configuration deleted successfully", 3000,
                            Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            loadNextCodes(); // reload current page
        } catch (Exception e) {
            Notification.show("Delete failed: " + e.getMessage(), 5000,
                            Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private String generateCodeForConfig(String entity, String attribute) {
        ResponseEntity<String> response = incrementalKeyService.generateNextCode(entity, attribute);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        throw new RuntimeException("Generation failed with status: " + response.getStatusCode());
    }

    private void openSubscribeDialog() {
        new SubscribeDialog(nextCodeService, () -> {
            currentPage = 0;
            loadNextCodes();
        }).open();
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        subscribeButton.setEnabled(!show);
    }

    private void injectResponsiveStyles() {
        String css = """
                .incremental-keys-view {
                    display: flex;
                    flex-direction: column;
                    gap: var(--lumo-space-m);
                }
                .incremental-toolbar {
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                }
                @media (max-width: 768px) {
                    .incremental-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .incremental-toolbar > * {
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
}