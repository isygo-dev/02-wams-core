package eu.isygoit.ui.mms.views.sender;

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
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.mms.layout.MmsMainLayout;
import eu.isygoit.ui.mms.views.sender.dialog.CreateSenderConfigDialog;
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
@Route(value = "mms/sender-config", layout = MmsMainLayout.class)
@PageTitle("Sender Configuration Management")
@PermitAll
public class SenderConfigManagementView extends ManagementVerticalView {

    private final SenderConfigService senderConfigService;

    private final Div cardsContainer = new Div();
    private final Button createButton = new Button(I18n.t("mms.sender.view.create.button"), new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ComboBox<SenderStatusOption> statusFilter = new ComboBox<>();
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
    private List<SenderConfigCard> currentPageCards = new ArrayList<>();
    private String currentSearch = "";
    private Boolean currentActiveFilter = null;

    private List<SenderConfigDto> allConfigs = new ArrayList<>();
    private List<SenderConfigDto> filteredConfigs = new ArrayList<>();

    @Autowired
    public SenderConfigManagementView(SenderConfigService senderConfigService) {
        this.senderConfigService = senderConfigService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("sender-config-view");

        H2 header = new H2(I18n.t("mms.sender.view.title"));
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        cardsContainer.setWidthFull();
        cardsContainer.addClassName("sender-grid");
        add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        createButton.addClickListener(e -> openCreateSenderDialog());
        createButton.setTooltipText(I18n.t("mms.sender.view.create.tooltip"));

        refreshButton.addClickListener(e -> resetPaginationAndLoad());
        refreshButton.setTooltipText(I18n.t("mms.sender.view.refresh.tooltip"));

        searchField.setPlaceholder(I18n.t("mms.sender.view.search.placeholder"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setTooltipText(I18n.t("mms.sender.view.search.tooltip"));
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            resetPaginationAndLoad();
        });

        statusFilter.setItems(
                new SenderStatusOption(I18n.t("mms.sender.view.status.all"), null),
                new SenderStatusOption(I18n.t("mms.sender.view.status.active"), true),
                new SenderStatusOption(I18n.t("mms.sender.view.status.inactive"), false)
        );
        statusFilter.setItemLabelGenerator(option -> option.label());
        statusFilter.setValue(new SenderStatusOption(I18n.t("mms.sender.view.status.all"), null));
        statusFilter.setPlaceholder(I18n.t("mms.sender.view.status.placeholder"));
        statusFilter.setTooltipText(I18n.t("mms.sender.view.status.tooltip"));
        statusFilter.addValueChangeListener(e -> {
            SenderStatusOption option = e.getValue();
            currentActiveFilter = option != null ? option.value() : null;
            resetPaginationAndLoad();
        });

        pageSizeSelect.setItems(10, 20, 30, 40, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.setPlaceholder(I18n.t("mms.sender.view.page.per.page"));
        pageSizeSelect.setTooltipText(I18n.t("mms.sender.view.page.per.page.tooltip"));
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                resetPaginationAndLoad();
            }
        });

        prevButton.addClickListener(e -> {
            if (!previousTokens.isEmpty()) {
                String prevToken = previousTokens.pop();
                loadSenderConfigsPage(prevToken);
            }
        });
        prevButton.setTooltipText(I18n.t("mms.sender.view.prev.page.tooltip"));

        nextButton.addClickListener(e -> {
            if (truncated && currentNextToken != null) {
                previousTokens.push(currentToken);
                loadSenderConfigsPage(currentNextToken);
            }
        });
        nextButton.setTooltipText(I18n.t("mms.sender.view.next.page.tooltip"));

        resetPaginationAndLoad();
    }

    private void resetPaginationAndLoad() {
        previousTokens.clear();
        currentNextToken = null;
        currentToken = null;
        currentPage = 1;
        totalPages = 0;
        totalElements = 0;
        numberOfElements = 0;
        truncated = false;
        loadSenderConfigsPage(null);
    }

    private void loadSenderConfigsPage(String nextToken) {
        showLoading(true);
        try {
            // Use paginated API if available, otherwise use findAllList with manual pagination
            ResponseEntity<List<SenderConfigDto>> response = senderConfigService.findAllList();
            allConfigs = response.getBody() != null ? response.getBody() : new ArrayList<>();

            // Apply filters
            filteredConfigs = filterConfigs(allConfigs);

            // Manual pagination
            totalElements = filteredConfigs.size();
            totalPages = (int) Math.ceil((double) totalElements / pageSize);

            int startIndex = (currentPage - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, (int) totalElements);

            List<SenderConfigDto> pageConfigs;
            if (startIndex < totalElements) {
                pageConfigs = filteredConfigs.subList(startIndex, endIndex);
            } else {
                pageConfigs = new ArrayList<>();
            }

            numberOfElements = pageConfigs.size();
            truncated = endIndex < totalElements;
            currentNextToken = truncated ? String.valueOf(currentPage + 1) : null;

            currentPageCards = pageConfigs.stream()
                    .map(config -> new SenderConfigCard(this, senderConfigService, config, this::resetPaginationAndLoad))
                    .collect(Collectors.toList());

            updatePaginationDisplay();
            displayCards();

        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            Notification.show(I18n.t("mms.sender.view.load.error", errorMsg), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load sender configs", ex.getMessage());
        } catch (Exception e) {
            Notification.show(I18n.t("mms.sender.view.load.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load sender configs", e);
        } finally {
            showLoading(false);
        }
    }

    private List<SenderConfigDto> filterConfigs(List<SenderConfigDto> configs) {
        return configs.stream()
                .filter(config -> {
                    if (currentActiveFilter != null) {
                        boolean isActive = config.getSmtpStarttlsEnable() != null && config.getSmtpStarttlsEnable();
                        if (isActive != currentActiveFilter) return false;
                    }
                    if (currentSearch != null && !currentSearch.isEmpty()) {
                        String searchLower = currentSearch.toLowerCase();
                        String code = config.getCode() != null ? config.getCode().toLowerCase() : "";
                        String name = config.getName() != null ? config.getName().toLowerCase() : "";
                        String host = config.getHost() != null ? config.getHost().toLowerCase() : "";
                        String username = config.getUsername() != null ? config.getUsername().toLowerCase() : "";
                        String tenant = config.getTenant() != null ? config.getTenant().toLowerCase() : "";
                        return code.contains(searchLower) ||
                                name.contains(searchLower) ||
                                host.contains(searchLower) ||
                                username.contains(searchLower) ||
                                tenant.contains(searchLower);
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private void updatePaginationDisplay() {
        if (totalPages > 0) {
            pageInfoLabel.setText(I18n.t("mms.sender.view.page.info", currentPage, totalPages, numberOfElements));
        } else {
            pageInfoLabel.setText(I18n.t("mms.sender.view.page.info.simple", currentPage, numberOfElements));
        }
        totalCountLabel.setText(I18n.t("mms.sender.view.total.count", totalElements));

        prevButton.setEnabled(!previousTokens.isEmpty());
        nextButton.setEnabled(truncated && currentNextToken != null);
    }

    private void displayCards() {
        cardsContainer.removeAll();

        if (currentPageCards.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.INBOX.create();
            emptyIcon.setSize("48px");
            emptyIcon.addClassName("wams-empty-state-icon");
            H4 emptyTitle = new H4(I18n.t("mms.sender.view.empty.title"));
            Paragraph emptyDesc = new Paragraph(I18n.t("mms.sender.view.empty.description"));
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
        } else {
            currentPageCards.forEach(cardsContainer::add);
        }
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("sender-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        leftGroup.addClassName("wams-toolbar-group");
        searchField.setWidth("200px");
        Span statusLabel = new Span(I18n.t("mms.sender.view.status.label"));
        statusLabel.getElement().setAttribute("title", I18n.t("mms.sender.view.status.tooltip"));
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
        pageInfoLabel.addClassName("wams-page-info-label");
        totalCountLabel.addClassName("wams-page-info-label");
        centerGroup.add(prevButton, pageInfoLabel, nextButton, totalCountLabel, pageSizeSelect);

        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        rightGroup.setAlignItems(FlexComponent.Alignment.END);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("mms.sender.view.refresh.tooltip"));
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        rightGroup.add(refreshButton, createButton);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        createButton.setEnabled(!show);
    }

    private void openCreateSenderDialog() {
        new CreateSenderConfigDialog(this, senderConfigService, this::resetPaginationAndLoad).open();
    }

    public record SenderStatusOption(String label, Boolean value) {
    }
}