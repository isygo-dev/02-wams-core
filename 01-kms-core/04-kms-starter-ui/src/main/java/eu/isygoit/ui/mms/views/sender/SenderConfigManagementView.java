package eu.isygoit.ui.mms.views.sender;

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
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.mms.layout.MmsMainLayout;
import eu.isygoit.ui.mms.views.sender.dialog.CreateSenderConfigDialog;
import eu.isygoit.ui.mms.views.sender.dialog.DeleteSenderConfigDialog;
import eu.isygoit.ui.mms.views.sender.dialog.EditSenderConfigDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@UIScope
@Route(value = "mms/sender-config", layout = MmsMainLayout.class)
@PageTitle("Sender Configuration Management")
@PermitAll
public class SenderConfigManagementView extends ManagementVerticalView {

    private final SenderConfigService senderConfigService;

    private final Div cardsContainer = new Div();
    private final Button createButton = new Button(I18n.t("sender.view.create.button"), new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ComboBox<SenderStatusOption> statusFilter = new ComboBox<>();
    private final ProgressBar loadingBar = new ProgressBar();

    private List<SenderConfigDto> allConfigs = new ArrayList<>();
    private List<SenderConfigCard> currentPageCards = new ArrayList<>();
    private String currentSearch = "";
    private Boolean currentActiveFilter = null;

    @Autowired
    public SenderConfigManagementView(SenderConfigService senderConfigService) {
        this.senderConfigService = senderConfigService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("sender-config-view");

        H2 header = new H2(I18n.t("sender.view.title"));
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
        createButton.setTooltipText(I18n.t("sender.view.create.tooltip"));

        refreshButton.addClickListener(e -> loadSenderConfigs());
        refreshButton.setTooltipText(I18n.t("sender.view.refresh.tooltip"));

        searchField.setPlaceholder(I18n.t("sender.view.search.placeholder"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setTooltipText(I18n.t("sender.view.search.tooltip"));
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            filterAndDisplayCards();
        });

        statusFilter.setItems(
                new SenderStatusOption(I18n.t("sender.view.status.all"), null),
                new SenderStatusOption(I18n.t("sender.view.status.active"), true),
                new SenderStatusOption(I18n.t("sender.view.status.inactive"), false)
        );
        statusFilter.setItemLabelGenerator(option -> option.label());
        statusFilter.setValue(new SenderStatusOption(I18n.t("sender.view.status.all"), null));
        statusFilter.setPlaceholder(I18n.t("sender.view.status.placeholder"));
        statusFilter.setTooltipText(I18n.t("sender.view.status.tooltip"));
        statusFilter.addValueChangeListener(e -> {
            currentActiveFilter = e.getValue().value();
            filterAndDisplayCards();
        });

        injectResponsiveStyles();
        loadSenderConfigs();
    }

    private void loadSenderConfigs() {
        showLoading(true);
        try {
            ResponseEntity<List<SenderConfigDto>> response = senderConfigService.findAllList();
            allConfigs = response.getBody() != null ? response.getBody() : new ArrayList<>();
            currentPageCards = allConfigs.stream()
                    .map(config -> new SenderConfigCard(this, senderConfigService, config, this::loadSenderConfigs))
                    .collect(Collectors.toList());
            filterAndDisplayCards();
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            Notification.show(I18n.t("sender.view.load.error", errorMsg), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load sender configs", ex.getMessage());
        } catch (Exception e) {
            Notification.show(I18n.t("sender.view.load.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load sender configs", e);
        } finally {
            showLoading(false);
        }
    }

    private void filterAndDisplayCards() {
        cardsContainer.removeAll();

        List<SenderConfigCard> filtered = currentPageCards.stream()
                .filter(card -> {
                    SenderConfigDto config = card.getConfig();
                    if (currentActiveFilter != null) {
                        boolean isActive = config.getSmtpStarttlsEnable() != null && config.getSmtpStarttlsEnable();
                        if (isActive != currentActiveFilter) return false;
                    }
                    if (currentSearch != null && !currentSearch.isEmpty()) {
                        String searchLower = currentSearch.toLowerCase();
                        String host = config.getHost() != null ? config.getHost().toLowerCase() : "";
                        String username = config.getUsername() != null ? config.getUsername().toLowerCase() : "";
                        String tenant = config.getTenant() != null ? config.getTenant().toLowerCase() : "";
                        return host.contains(searchLower) ||
                                username.contains(searchLower) ||
                                tenant.contains(searchLower);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.INBOX.create();
            emptyIcon.setSize("48px");
            emptyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
            H4 emptyTitle = new H4(I18n.t("sender.view.empty.title"));
            Paragraph emptyDesc = new Paragraph(I18n.t("sender.view.empty.description"));
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
        toolbar.addClassName("sender-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        searchField.setWidth("200px");
        Span statusLabel = new Span(I18n.t("sender.view.status.label"));
        statusLabel.getElement().setAttribute("title", I18n.t("sender.view.status.tooltip"));
        statusFilter.setWidth("140px");
        HorizontalLayout statusLayout = new HorizontalLayout(statusLabel, statusFilter);
        statusLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        statusLayout.setSpacing(true);
        leftGroup.add(searchField, statusLayout);

        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        rightGroup.setAlignItems(FlexComponent.Alignment.END);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("sender.view.refresh.tooltip"));
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        rightGroup.add(refreshButton, createButton);

        toolbar.add(leftGroup, rightGroup);
        return toolbar;
    }

    private void injectResponsiveStyles() {
        String css = """
                .sender-config-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .sender-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                .sender-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
                    gap: var(--lumo-space-m);
                    padding: var(--lumo-space-s);
                }
                @media (max-width: 768px) {
                    .sender-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .sender-toolbar > * {
                        width: 100% !important;
                        justify-content: center;
                    }
                    .sender-grid {
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

    private void openCreateSenderDialog() {
        // FIXED: Pass parentView, senderConfigService, and refresh callback
        new CreateSenderConfigDialog(this, senderConfigService, this::loadSenderConfigs).open();
    }

    private void openEditSenderDialog(SenderConfigDto config) {
        // FIXED: Pass senderConfigService, config, and refresh callback
        new EditSenderConfigDialog(senderConfigService, config, this::loadSenderConfigs).open();
    }

    private void openDeleteSenderDialog(SenderConfigDto config) {
        // FIXED: Pass parentView, senderConfigService, config, and refresh callback
        new DeleteSenderConfigDialog(this, senderConfigService, config, this::loadSenderConfigs).open();
    }

    public record SenderStatusOption(String label, Boolean value) {
    }
}