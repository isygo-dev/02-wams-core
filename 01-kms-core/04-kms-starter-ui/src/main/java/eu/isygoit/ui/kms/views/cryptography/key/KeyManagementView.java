package eu.isygoit.ui.kms.views.cryptography.key;

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
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.ListKeysResponse;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.cryptography.key.dialog.CreateKeyDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

@Slf4j
@UIScope
@Route(value = "kms/keys", layout = KmsMainLayout.class)
@PageTitle("Key Management")
@PermitAll
public class KeyManagementView extends ManagementVerticalView {

    private final KmsApiService kmsApiService;
    private final ObjectMapper objectMapper;

    private final Div cardsContainer = new Div();
    private final Button createButton = new Button(I18n.t("kms.key.view.create.button"), new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ComboBox<KeyStatusOption> statusFilter = new ComboBox<>();
    private final ProgressBar loadingBar = new ProgressBar();
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final Span pageInfoLabel = new Span();
    private final Span totalCountLabel = new Span();
    private final Stack<String> previousTokens = new Stack<>();
    private int pageSize = 10;
    private String currentNextToken = null;
    private String currentToken = null;
    private int currentPage = 1;
    private int totalPages = 0;
    private long totalElements = 0;
    private int numberOfElements = 0;
    private boolean truncated = false;
    private List<KeyCard> currentPageCards = new ArrayList<>();
    private String currentSearch = "";
    private IEnumKeyStatus.Types currentStatus = null;

    @Autowired
    public KeyManagementView(KmsApiService kmsApiService, ObjectMapper objectMapper) {
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-keys-view");

        H2 header = new H2(I18n.t("kms.key.view.title"));
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        cardsContainer.setWidthFull();
        cardsContainer.addClassName("keys-grid");
        add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        createButton.addClickListener(e -> openCreateKeyDialog());
        createButton.setTooltipText(I18n.t("kms.key.view.create.tooltip"));

        refreshButton.addClickListener(e -> resetKeyPaginationAndLoad());
        refreshButton.setTooltipText(I18n.t("kms.key.view.refresh.tooltip"));

        searchField.setPlaceholder(I18n.t("kms.key.view.search.placeholder"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setTooltipText(I18n.t("kms.key.view.search.tooltip"));
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            resetKeyPaginationAndLoad();
        });

        statusFilter.setItems(
                new KeyStatusOption(I18n.t("kms.key.view.status.all"), null),
                new KeyStatusOption(IEnumKeyStatus.Types.ENABLED.meaning(), IEnumKeyStatus.Types.ENABLED),
                new KeyStatusOption(IEnumKeyStatus.Types.DISABLED.meaning(), IEnumKeyStatus.Types.DISABLED),
                new KeyStatusOption(IEnumKeyStatus.Types.PENDING_DELETION.meaning(), IEnumKeyStatus.Types.PENDING_DELETION)
        );
        statusFilter.setItemLabelGenerator(option -> option.label());
        statusFilter.setValue(new KeyStatusOption(I18n.t("kms.key.view.status.all"), null));
        statusFilter.setPlaceholder(I18n.t("kms.key.view.status.placeholder"));
        statusFilter.setTooltipText(I18n.t("kms.key.view.status.tooltip"));
        statusFilter.addValueChangeListener(e -> {
            currentStatus = e.getValue().value();
            resetKeyPaginationAndLoad();
        });

        pageSizeSelect.setItems(10, 20, 30, 40, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.setPlaceholder(I18n.t("kms.key.view.page.per.page"));
        pageSizeSelect.setTooltipText(I18n.t("kms.key.view.page.per.page.tooltip"));
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                resetKeyPaginationAndLoad();
            }
        });

        prevButton.addClickListener(e -> {
            if (!previousTokens.isEmpty()) {
                String prevToken = previousTokens.pop();
                loadKeysPage(prevToken);
            }
        });
        prevButton.setTooltipText(I18n.t("kms.key.view.prev.page.tooltip"));

        nextButton.addClickListener(e -> {
            if (truncated && currentNextToken != null) {
                previousTokens.push(currentToken);
                loadKeysPage(currentNextToken);
            }
        });
        nextButton.setTooltipText(I18n.t("kms.key.view.next.page.tooltip"));

        injectResponsiveStyles();
        resetKeyPaginationAndLoad();
    }

    private void resetKeyPaginationAndLoad() {
        previousTokens.clear();
        currentNextToken = null;
        currentToken = null;
        currentPage = 1;
        totalPages = 0;
        totalElements = 0;
        numberOfElements = 0;
        truncated = false;
        loadKeysPage(null);
    }

    private void loadKeysPage(String nextToken) {
        showLoading(true);
        try {
            ResponseEntity<ListKeysResponse> response = kmsApiService.listKeys(pageSize, nextToken);
            ListKeysResponse body = response.getBody();
            List<ListKeysResponse.KeyEntry> keyEntries = (body != null && body.getKeys() != null) ? body.getKeys() : new ArrayList<>();
            currentNextToken = (body != null) ? body.getNextToken() : null;
            numberOfElements = (body != null && body.getNumberOfElements() != null) ? body.getNumberOfElements() : keyEntries.size();
            totalPages = (body != null && body.getTotalPages() != null) ? body.getTotalPages() : 0;
            totalElements = (body != null && body.getTotalElements() != null) ? body.getTotalElements() : 0L;
            truncated = (body != null && Boolean.TRUE.equals(body.getTruncated()));
            currentToken = nextToken;

            if (nextToken == null) {
                currentPage = 1;
            } else {
                currentPage = previousTokens.size() + 1;
            }

            List<KeyCard> cards = new ArrayList<>();
            for (ListKeysResponse.KeyEntry entry : keyEntries) {
                try {
                    ResponseEntity<DescribeKeyResponse> descResponse = kmsApiService.describeKey(entry.getKeyId());
                    DescribeKeyResponse describe = descResponse.getBody();
                    if (describe != null && describe.getKeyMetadata() != null) {
                        cards.add(new KeyCard(this, kmsApiService, objectMapper, entry.getKeyId(), describe.getKeyMetadata()));
                    } else {
                        cards.add(new KeyCard(this, kmsApiService, objectMapper, entry.getKeyId(), null));
                    }
                } catch (Exception ex) {
                    cards.add(new KeyCard(this, kmsApiService, objectMapper, entry.getKeyId(), null));
                }
            }
            currentPageCards = cards;
            updatePaginationDisplay();
            filterCards();
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            Notification.show(I18n.t("kms.key.view.load.error", errorMsg), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load keys", ex.getMessage());
        } catch (Exception e) {
            Notification.show(I18n.t("kms.key.view.load.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load keys", e);
        } finally {
            showLoading(false);
        }
    }

    private void updatePaginationDisplay() {
        if (totalPages > 0) {
            pageInfoLabel.setText(I18n.t("kms.key.view.page.info", currentPage, totalPages, numberOfElements));
        } else {
            pageInfoLabel.setText(I18n.t("kms.key.view.page.info.simple", currentPage, numberOfElements));
        }
        totalCountLabel.setText(I18n.t("kms.key.view.total.count", totalElements));

        prevButton.setEnabled(!previousTokens.isEmpty());
        nextButton.setEnabled(truncated && currentNextToken != null);
    }

    private void filterCards() {
        cardsContainer.removeAll();
        List<KeyCard> filtered = currentPageCards.stream()
                .filter(card -> {
                    if (currentStatus != null) {
                        IEnumKeyStatus.Types status = card.getStatus();
                        if (status != currentStatus) return false;
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
            H4 emptyTitle = new H4(I18n.t("kms.key.view.empty.title"));
            Paragraph emptyDesc = new Paragraph(I18n.t("kms.key.view.empty.description"));
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
        toolbar.addClassName("key-management-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        searchField.setWidth("200px");
        Span statusLabel = new Span(I18n.t("kms.key.view.status.label"));
        statusLabel.getElement().setAttribute("title", I18n.t("kms.key.view.status.tooltip"));
        statusFilter.setWidth("140px");
        HorizontalLayout statusLayout = new HorizontalLayout(statusLabel, statusFilter);
        statusLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        statusLayout.setSpacing(true);
        leftGroup.add(searchField, statusLayout);

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

        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        rightGroup.setAlignItems(FlexComponent.Alignment.END);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("kms.key.view.refresh.tooltip"));
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        rightGroup.add(refreshButton, createButton);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    private void injectResponsiveStyles() {
        String css = """
                .kms-keys-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .key-management-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                .keys-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
                    gap: var(--lumo-space-m);
                    padding: var(--lumo-space-s);
                }
                @media (max-width: 768px) {
                    .key-management-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .key-management-toolbar > * {
                        width: 100% !important;
                        justify-content: center;
                    }
                    .keys-grid {
                        grid-template-columns: 1fr;
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

    private void openCreateKeyDialog() {
        new CreateKeyDialog(this, kmsApiService, objectMapper, () -> resetKeyPaginationAndLoad()).open();
    }

    public record KeyStatusOption(String label, IEnumKeyStatus.Types value) {
    }
}