package eu.isygoit.ui.views.kms.tokenizer.config;

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
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.ui.layout.KmsMainLayout;
import eu.isygoit.ui.views.kms.tokenizer.config.dialog.CreateTokenConfigDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Route(value = "token-configs", layout = KmsMainLayout.class)
@PageTitle("Token Configurations")
@PermitAll
public class TokenConfigView extends VerticalLayout {

    private final KmsTokenConfigService tokenConfigService;
    private final KmsApiService kmsApiService;  // for dialogs

    private final VerticalLayout cardsContainer = new VerticalLayout();
    private final TextField searchField = new TextField();
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Button createButton = new Button("Create Config", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final ProgressBar loadingBar = new ProgressBar();

    // Pagination
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageInfoLabel = new Span();
    private final Span totalCountLabel = new Span();

    private int currentPage = 0;       // 0‑based
    private int pageSize = 10;
    private int totalPages = 0;
    private long totalElements = 0;
    private List<TokenConfigDto> currentPageContent = new ArrayList<>();
    private String currentSearch = "";

    @Autowired
    public TokenConfigView(KmsTokenConfigService tokenConfigService, KmsApiService kmsApiService) {
        this.tokenConfigService = tokenConfigService;
        this.kmsApiService = kmsApiService;
        buildUI();
        loadConfigs();
    }

    private void buildUI() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("token-config-view");

        H2 header = new H2("Token Configurations");
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

        refreshButton.addClickListener(e -> {
            currentPage = 0;
            loadConfigs();
        });
        refreshButton.setTooltipText("Refresh configurations");

        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.addClickListener(e -> openCreateDialog());

        searchField.setPlaceholder("Search by code or token type...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            currentPage = 0;
            loadConfigs();
        });

        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                currentPage = 0;
                loadConfigs();
            }
        });

        prevButton.addClickListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadConfigs();
            }
        });
        nextButton.addClickListener(e -> {
            if (currentPage + 1 < totalPages) {
                currentPage++;
                loadConfigs();
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
        toolbar.addClassName("token-config-toolbar");

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

        HorizontalLayout rightGroup = new HorizontalLayout(refreshButton, createButton);
        rightGroup.setSpacing(true);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    private void loadConfigs() {
        showLoading(true);
        try {
            ResponseEntity<PaginatedResponseDto<TokenConfigDto>> response;
            if (currentSearch == null || currentSearch.isBlank()) {
                response = tokenConfigService.findAll(currentPage, pageSize);
            } else {
                String criteria = "code~" + currentSearch + ",OR tokenType~" + currentSearch;
                response = tokenConfigService.findAllFilteredByCriteria(criteria, currentPage, pageSize);
            }

            PaginatedResponseDto<TokenConfigDto> body = response.getBody();
            if (body != null) {
                currentPageContent = body.getContent() != null ? body.getContent() : new ArrayList<>();
                totalPages = body.getTotalPages();
                totalElements = body.getTotalElements();
                if (currentPage >= totalPages && totalPages > 0) {
                    currentPage = totalPages - 1;
                    loadConfigs();
                    return;
                }
            } else {
                currentPageContent = new ArrayList<>();
                totalPages = 0;
                totalElements = 0;
            }
            updatePaginationDisplay();
            renderCards();
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showError("Failed to load configurations: " + errorMsg);
        } catch (Exception e) {
            showError("Failed to load configurations: " + e.getMessage());
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
            Icon emptyIcon = VaadinIcon.CODE.create();
            emptyIcon.setSize("48px");
            emptyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
            H4 emptyTitle = new H4("No configurations found");
            Paragraph emptyDesc = new Paragraph("Click 'Create Config' to add a token configuration.");
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
            return;
        }

        for (TokenConfigDto dto : currentPageContent) {
            TokenConfigCard card = new TokenConfigCard(
                    this,
                    tokenConfigService,
                    kmsApiService,
                    dto,
                    () -> loadConfigs()  // refresh after delete
            );
            cardsContainer.add(card);
        }
    }

    public void refreshCard(TokenConfigCard card) {
        try {
            ResponseEntity<TokenConfigDto> response = tokenConfigService.findById(card.getDto().getId());
            TokenConfigDto updated = response.getBody();
            if (updated != null) {
                card.updateDto(updated);
                // also update in currentPageContent
                for (int i = 0; i < currentPageContent.size(); i++) {
                    if (currentPageContent.get(i).getId().equals(updated.getId())) {
                        currentPageContent.set(i, updated);
                        break;
                    }
                }
            } else {
                cardsContainer.remove(card);
                loadConfigs();
            }
        } catch (Exception e) {
            showError("Failed to refresh card: " + e.getMessage());
        }
    }

    private void openCreateDialog() {
        new CreateTokenConfigDialog(tokenConfigService, kmsApiService, this::loadConfigs).open();
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        createButton.setEnabled(!show);
    }

    private void showError(String msg) {
        Notification.show(msg, 5000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void injectResponsiveStyles() {
        String css = """
                .token-config-view .token-config-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                @media (max-width: 768px) {
                    .token-config-view .token-config-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .token-config-view .token-config-toolbar > * {
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