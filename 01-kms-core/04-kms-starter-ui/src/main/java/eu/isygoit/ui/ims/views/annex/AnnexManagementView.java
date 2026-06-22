package eu.isygoit.ui.ims.views.annex;

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
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import eu.isygoit.ui.ims.views.annex.dialog.CreateAnnexDialog;
import eu.isygoit.ui.ims.views.annex.dialog.UpdateAnnexDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@VaadinSessionScope
@Route(value = "ims/annexes", layout = ImsMainLayout.class)
@PageTitle("Annex Management")
@PermitAll
public class AnnexManagementView extends ManagementVerticalView {

    private final AnnexService annexService;

    private final Div cardsContainer = new Div();
    private final Button createButton = new Button("Create annex", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final TextField tableCodeFilter = new TextField();
    private final ComboBox<IEnumLanguage.Types> languageFilter = new ComboBox<>();

    private final ProgressBar loadingBar = new ProgressBar();
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageInfoLabel = new Span();
    private final Span totalCountLabel = new Span();

    private int currentPage = 0;
    private int pageSize = 10;
    private int totalPages = 0;
    private long totalElements = 0;
    private List<AnnexDto> currentPageAnnexes = new ArrayList<>();

    private String currentSearch = "";
    private String currentTableCode = "";
    private IEnumLanguage.Types currentLanguage = null;

    @Autowired
    public AnnexManagementView(AnnexService annexService) {
        this.annexService = annexService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("annex-management-view");

        H2 header = new H2("Annex Management");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        cardsContainer.setWidthFull();
        cardsContainer.addClassName("annex-cards-grid");
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
        createButton.addClickListener(e -> openCreateAnnexDialog());
        createButton.setTooltipText("Create a new annex entry");

        refreshButton.addClickListener(e -> loadPage(0));
        refreshButton.setTooltipText("Refresh annexes from server");

        searchField.setPlaceholder("Search by value");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            loadPage(0);
        });

        tableCodeFilter.setPlaceholder("Table code");
        tableCodeFilter.setClearButtonVisible(true);
        tableCodeFilter.addValueChangeListener(e -> {
            currentTableCode = e.getValue();
            loadPage(0);
        });

        languageFilter.setItems(IEnumLanguage.Types.values());
        languageFilter.setItemLabelGenerator(lang -> lang.name());
        languageFilter.setPlaceholder("Language");
        languageFilter.addValueChangeListener(e -> {
            currentLanguage = e.getValue();
            loadPage(0);
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
            ResponseEntity<PaginatedResponseDto<AnnexDto>> response =
                    annexService.findAll(page, pageSize);
            PaginatedResponseDto<AnnexDto> body = response.getBody();
            if (body != null && body.getContent() != null) {
                currentPageAnnexes = body.getContent();
                totalElements = body.getTotalElements();
                totalPages = body.getTotalPages();
                currentPage = body.getPageNumber();
            } else {
                currentPageAnnexes = new ArrayList<>();
                totalElements = 0;
                totalPages = 0;
            }
            updatePaginationDisplay();
            filterAndDisplayCards();
        } catch (FeignException ex) {
            String errorMsg = extractErrorMessage(ex);
            Notification.show("Failed to load annexes: " + errorMsg, 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load annexes", ex);
        } catch (Exception e) {
            Notification.show("Failed to load annexes: " + e.getMessage(), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load annexes", e);
        } finally {
            showLoading(false);
        }
    }

    private void filterAndDisplayCards() {
        List<AnnexDto> filtered = currentPageAnnexes.stream()
                .filter(annex -> {
                    if (currentTableCode != null && !currentTableCode.isBlank() &&
                            !currentTableCode.equals(annex.getTableCode())) {
                        return false;
                    }
                    if (currentLanguage != null && annex.getLanguage() != currentLanguage) {
                        return false;
                    }
                    if (currentSearch != null && !currentSearch.isBlank()) {
                        String searchLower = currentSearch.toLowerCase();
                        return annex.getValue() != null && annex.getValue().toLowerCase().contains(searchLower);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        cardsContainer.removeAll();

        if (filtered.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.FILE_CODE.create();
            emptyIcon.setSize("48px");
            emptyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
            H4 emptyTitle = new H4("No annex entries found");
            Paragraph emptyDesc = new Paragraph("Try adjusting your search or filter criteria.");
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
        } else {
            for (AnnexDto annex : filtered) {
                cardsContainer.add(new AnnexCard(this, annexService, annex, this::loadPageZero));
            }
        }
    }

    private void updatePaginationDisplay() {
        pageInfoLabel.setText(String.format("Page %d / %d", currentPage + 1, totalPages));
        totalCountLabel.setText(String.format("%d total annexes", totalElements));
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
        toolbar.addClassName("annex-management-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        searchField.setWidth("200px");
        tableCodeFilter.setWidth("150px");
        languageFilter.setWidth("150px");
        leftGroup.add(searchField, tableCodeFilter, languageFilter);

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

    private void openCreateAnnexDialog() {
        new CreateAnnexDialog(this, annexService, this::loadPageZero).open();
    }

    public void openUpdateAnnexDialog(AnnexDto annex, Runnable onSuccess) {
        new UpdateAnnexDialog(this, annexService, annex, onSuccess).open();
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
                .annex-management-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .annex-management-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                .annex-cards-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
                    gap: var(--lumo-space-m);
                    padding: var(--lumo-space-s);
                }
                @media (max-width: 768px) {
                    .annex-management-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .annex-management-toolbar > * {
                        width: 100% !important;
                        justify-content: center;
                    }
                    .annex-cards-grid {
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
}