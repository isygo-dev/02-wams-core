package eu.isygoit.ui.kms.views.cryptography.random;

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
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.common.RandomKeyDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.common.view.ManagementCompositeVerticalView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.cryptography.random.dialog.CreateRandomKeyDialog;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@VaadinSessionScope
@Route(value = "kms/random-keys", layout = KmsMainLayout.class)
@PageTitle("Random Keys")
@PermitAll
public class RandomKeyView extends ManagementCompositeVerticalView {

    private final RandomKeyService keyService;

    private final Div cardsContainer = new Div();
    private final TextField searchField = new TextField();
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Button createButton = new Button(I18n.t("kms.random.key.create.button"), new Icon(VaadinIcon.PLUS_CIRCLE));
    private final ProgressBar loadingBar = new ProgressBar();

    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageInfoLabel = new Span();
    private final Span totalCountLabel = new Span();

    private List<RandomKeyDto> allKeysForSearch = new ArrayList<>();
    private int currentPage = 0;
    private int pageSize = 10;
    private int totalPages = 0;
    private long totalElements = 0;
    private String currentSearch = "";

    @Autowired
    public RandomKeyView(RandomKeyService keyService) {
        this.keyService = keyService;
        buildUI();
        loadKeys();
    }

    private void buildUI() {
        VerticalLayout layout = getContent();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.addClassName("random-keys-view");

        H2 header = new H2(I18n.t("kms.random.key.title"));
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        layout.add(header);

        HorizontalLayout toolbar = buildToolbar();
        layout.add(toolbar);

        cardsContainer.setWidthFull();
        cardsContainer.addClassName("random-keys-grid");
        layout.add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        layout.add(loadingBar);

        refreshButton.addClickListener(e -> {
            currentPage = 0;
            loadKeys();
        });
        refreshButton.setTooltipText(I18n.t("kms.random.key.refresh.tooltip"));

        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.addClickListener(e -> new CreateRandomKeyDialog(keyService, this::loadKeys).open());

        searchField.setPlaceholder(I18n.t("kms.random.key.search.placeholder"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            currentPage = 0;
            loadKeys();
        });

        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                currentPage = 0;
                loadKeys();
            }
        });

        prevButton.addClickListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadKeys();
            }
        });
        nextButton.addClickListener(e -> {
            if (currentPage + 1 < totalPages) {
                currentPage++;
                loadKeys();
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
        toolbar.addClassName("randomkey-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout(searchField);
        searchField.setWidth("250px");

        HorizontalLayout centerGroup = new HorizontalLayout();
        centerGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        pageSizeSelect.setWidth("100px");
        pageInfoLabel.addClassName("randomkey-pagination-label");
        totalCountLabel.addClassName("randomkey-pagination-label");
        centerGroup.add(prevButton, pageInfoLabel, nextButton, totalCountLabel, pageSizeSelect);

        HorizontalLayout rightGroup = new HorizontalLayout(refreshButton, createButton);
        rightGroup.setSpacing(true);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    private void loadKeys() {
        showLoading(true);
        try {
            if (currentSearch == null || currentSearch.isBlank()) {
                ResponseEntity<PaginatedResponseDto<RandomKeyDto>> response =
                        keyService.listRandomKeys(currentPage, pageSize);
                PaginatedResponseDto<RandomKeyDto> body = response.getBody();
                if (body != null) {
                    totalElements = body.getTotalElements();
                    totalPages = body.getTotalPages();
                    List<RandomKeyDto> content = body.getContent();
                    allKeysForSearch = (content != null) ? content : new ArrayList<>();
                } else {
                    totalElements = 0;
                    totalPages = 0;
                    allKeysForSearch = new ArrayList<>();
                }
                updatePaginationDisplay();
                renderCards(allKeysForSearch);
            } else {
                ResponseEntity<PaginatedResponseDto<RandomKeyDto>> response =
                        keyService.listRandomKeys(0, 5000);
                PaginatedResponseDto<RandomKeyDto> body = response.getBody();
                List<RandomKeyDto> fullList = (body != null && body.getContent() != null)
                        ? body.getContent() : new ArrayList<>();

                List<RandomKeyDto> filtered = fullList.stream()
                        .filter(k -> k.getName().toLowerCase().contains(currentSearch.toLowerCase()))
                        .collect(Collectors.toList());

                totalElements = filtered.size();
                totalPages = (int) Math.ceil((double) totalElements / pageSize);
                int start = currentPage * pageSize;
                int end = Math.min(start + pageSize, (int) totalElements);
                List<RandomKeyDto> pageItems = (start < totalElements) ? filtered.subList(start, end) : new ArrayList<>();

                updatePaginationDisplay();
                renderCards(pageItems);
            }
        } catch (Exception e) {
            Notification.show(I18n.t("kms.random.key.load.error", e.getMessage()), 5000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void updatePaginationDisplay() {
        if (totalPages > 0) {
            pageInfoLabel.setText(I18n.t("kms.random.key.page.info", currentPage + 1, totalPages));
        } else {
            pageInfoLabel.setText(I18n.t("kms.random.key.page.info", 0, 0));
        }
        totalCountLabel.setText(I18n.t("kms.random.key.total.count", totalElements));
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage + 1 < totalPages);
    }

    private void renderCards(List<RandomKeyDto> keys) {
        cardsContainer.removeAll();
        if (keys.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.KEY.create();
            emptyIcon.setSize("48px");
            emptyIcon.addClassName("kms-parta-empty-icon");
            H4 emptyTitle = new H4(I18n.t("kms.random.key.empty.title"));
            Paragraph emptyDesc = new Paragraph(I18n.t("kms.random.key.empty.description"));
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
            return;
        }
        for (RandomKeyDto dto : keys) {
            RandomKeyCard card = new RandomKeyCard(this, keyService, dto);
            cardsContainer.add(card);
        }
    }

    public void refreshCard(RandomKeyCard card) {
        loadKeys();
    }

    public void removeCard(RandomKeyCard card) {
        cardsContainer.remove(card);
        loadKeys();
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        createButton.setEnabled(!show);
    }

}