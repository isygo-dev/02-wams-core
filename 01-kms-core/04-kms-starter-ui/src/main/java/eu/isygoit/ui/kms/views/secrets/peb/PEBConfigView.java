package eu.isygoit.ui.kms.views.secrets.peb;

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
import eu.isygoit.dto.data.PEBConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PEBConfigService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.secrets.peb.dialog.CreatePEBConfigDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@VaadinSessionScope
@Route(value = "kms/peb-configs", layout = KmsMainLayout.class)
@PageTitle("PEB Configurations")
@PermitAll
public class PEBConfigView extends ManagementVerticalView {

    private final PEBConfigService configService;
    private final Div cardsContainer = new Div();
    private final TextField searchField = new TextField();
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Button createButton = new Button(I18n.t("kms.peb.config.create.button"), new Icon(VaadinIcon.PLUS_CIRCLE));
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
    private List<PEBConfigDto> currentPageContent = new ArrayList<>();
    private String currentSearch = "";

    @Autowired
    public PEBConfigView(PEBConfigService configService) {
        this.configService = configService;
        buildUI();
        loadConfigs();
    }

    private void buildUI() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("peb-config-view");

        H2 header = new H2(I18n.t("kms.peb.config.header"));
        header.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.Margin.Bottom.NONE);
        add(header);

        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        cardsContainer.setWidthFull();
        cardsContainer.addClassName("peb-configs-grid");
        add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        refreshButton.addClickListener(e -> {
            currentPage = 0;
            loadConfigs();
        });
        refreshButton.setTooltipText(I18n.t("kms.peb.config.refresh.tooltip"));

        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.addClickListener(e -> openCreateDialog());

        searchField.setPlaceholder(I18n.t("kms.peb.config.search.placeholder"));
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
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("peb-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout(searchField);
        searchField.setWidth("250px");

        HorizontalLayout centerGroup = new HorizontalLayout();
        centerGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        pageSizeSelect.setWidth("100px");
        pageInfoLabel.addClassName("page-info-label");
        totalCountLabel.addClassName("total-count-label");
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
            ResponseEntity<PaginatedResponseDto<PEBConfigDto>> response;
            if (currentSearch == null || currentSearch.isBlank()) {
                response = configService.findAll(currentPage, pageSize);
            } else {
                String criteria = "code~" + currentSearch;
                response = configService.findAllFilteredByCriteria(criteria, currentPage, pageSize);
            }

            PaginatedResponseDto<PEBConfigDto> body = response.getBody();
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
            showError(I18n.t("kms.peb.config.load.error", errorMsg));
        } catch (Exception e) {
            showError(I18n.t("kms.peb.config.load.error", e.getMessage()));
        } finally {
            showLoading(false);
        }
    }

    private void updatePaginationDisplay() {
        if (totalPages > 0) {
            pageInfoLabel.setText(I18n.t("kms.peb.config.page.info", currentPage + 1, totalPages));
        } else {
            pageInfoLabel.setText(I18n.t("kms.peb.config.page.info", 0, 0));
        }
        totalCountLabel.setText(I18n.t("kms.peb.config.total.count", totalElements));
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage + 1 < totalPages);
    }

    private void renderCards() {
        cardsContainer.removeAll();
        if (currentPageContent.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.COG.create();
            emptyIcon.setSize("48px");
            emptyIcon.addClassName("empty-state-icon");
            H4 emptyTitle = new H4(I18n.t("kms.peb.config.empty.title"));
            Paragraph emptyDesc = new Paragraph(I18n.t("kms.peb.config.empty.description"));
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
            return;
        }

        for (PEBConfigDto dto : currentPageContent) {
            PEBConfigCard card = new PEBConfigCard(this, configService, dto, () -> loadConfigs());
            cardsContainer.add(card);
        }
    }

    public void refreshCard(PEBConfigCard card) {
        try {
            ResponseEntity<PEBConfigDto> response = configService.findById(card.getDto().getId());
            PEBConfigDto updated = response.getBody();
            if (updated != null) {
                card.updateDto(updated);
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
            showError(I18n.t("kms.peb.config.refresh.card.error", e.getMessage()));
        }
    }

    private void openCreateDialog() {
        new CreatePEBConfigDialog(configService, this::loadConfigs).open();
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
}