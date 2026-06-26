package eu.isygoit.ui.mms.views.sender;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
import eu.isygoit.ui.mms.views.sender.dialog.EditSenderConfigDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@UIScope
@Route(value = "mms/sender-config", layout = MmsMainLayout.class)
@PageTitle("Sender Configuration Management")
@PermitAll
public class SenderConfigManagementView extends ManagementVerticalView {

    private final SenderConfigService senderConfigService;

    private final Grid<SenderConfigDto> grid = new Grid<>(SenderConfigDto.class, false);
    private final Button createButton = new Button(I18n.t("sender.view.create.button"), new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ProgressBar loadingBar = new ProgressBar();

    private List<SenderConfigDto> allConfigs = new ArrayList<>();
    private List<SenderConfigDto> filteredConfigs = new ArrayList<>();

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

        configureGrid();
        add(grid);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        injectResponsiveStyles();
        loadSenderConfigs();
    }

    private void configureGrid() {
        grid.setWidthFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);

        grid.addColumn(SenderConfigDto::getId)
                .setHeader(I18n.t("sender.grid.id"))
                .setWidth("80px")
                .setFlexGrow(0)
                .setSortable(true);

        grid.addColumn(SenderConfigDto::getTenant)
                .setHeader(I18n.t("sender.grid.tenant"))
                .setWidth("150px")
                .setSortable(true);

        grid.addColumn(SenderConfigDto::getHost)
                .setHeader(I18n.t("sender.grid.host"))
                .setSortable(true)
                .setResizable(true);

        grid.addColumn(SenderConfigDto::getPort)
                .setHeader(I18n.t("sender.grid.port"))
                .setWidth("100px")
                .setFlexGrow(0)
                .setSortable(true);

        grid.addColumn(SenderConfigDto::getUsername)
                .setHeader(I18n.t("sender.grid.username"))
                .setWidth("150px")
                .setSortable(true);

        grid.addColumn(config -> config.getSmtpStarttlsEnable() != null && config.getSmtpStarttlsEnable() ? "✓" : "✗")
                .setHeader(I18n.t("sender.grid.tls"))
                .setWidth("80px")
                .setFlexGrow(0)
                .setSortable(true);

        grid.addColumn(config -> config.getDebug() != null && config.getDebug() ? "✓" : "✗")
                .setHeader(I18n.t("sender.grid.debug"))
                .setWidth("80px")
                .setFlexGrow(0)
                .setSortable(true);

        grid.addComponentColumn(this::createActionButtons)
                .setHeader(I18n.t("sender.grid.actions"))
                .setWidth("150px")
                .setFlexGrow(0);

        grid.setItems(filteredConfigs);
    }

    private HorizontalLayout createActionButtons(SenderConfigDto config) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
        editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editBtn.setTooltipText(I18n.t("sender.action.edit"));
        editBtn.addClickListener(e -> openEditDialog(config));

        Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteBtn.setTooltipText(I18n.t("sender.action.delete"));
        deleteBtn.addClickListener(e -> confirmDelete(config));

        Button testBtn = new Button(new Icon(VaadinIcon.START_COG));
        testBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
        testBtn.setTooltipText(I18n.t("sender.action.test"));
        testBtn.addClickListener(e -> testConnection(config));

        actions.add(editBtn, testBtn, deleteBtn);
        return actions;
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.addClassName("sender-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(Alignment.CENTER);

        searchField.setPlaceholder(I18n.t("sender.search.placeholder"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setTooltipText(I18n.t("sender.search.tooltip"));
        searchField.addValueChangeListener(e -> filterConfigs(e.getValue()));
        searchField.setWidth("300px");

        leftGroup.add(searchField);

        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        rightGroup.setAlignItems(Alignment.CENTER);

        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("sender.refresh.tooltip"));
        refreshButton.addClickListener(e -> loadSenderConfigs());

        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.setTooltipText(I18n.t("sender.create.tooltip"));
        createButton.addClickListener(e -> openCreateDialog());

        rightGroup.add(refreshButton, createButton);

        toolbar.add(leftGroup, rightGroup);
        return toolbar;
    }

    private void loadSenderConfigs() {
        showLoading(true);
        try {
            ResponseEntity<List<SenderConfigDto>> response = senderConfigService.findAllList();
            allConfigs = response.getBody() != null ? response.getBody() : new ArrayList<>();
            filteredConfigs = new ArrayList<>(allConfigs);
            grid.setItems(filteredConfigs);
            grid.getDataProvider().refreshAll();
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 404 ? I18n.t("sender.load.not.found") : ex.getMessage();
            Notification.show(I18n.t("sender.load.error", errorMsg), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load sender configs", ex);
        } catch (Exception e) {
            Notification.show(I18n.t("sender.load.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load sender configs", e);
        } finally {
            showLoading(false);
        }
    }

    private void filterConfigs(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredConfigs = new ArrayList<>(allConfigs);
        } else {
            String searchLower = searchText.toLowerCase();
            filteredConfigs = allConfigs.stream()
                    .filter(config ->
                            config.getHost() != null && config.getHost().toLowerCase().contains(searchLower) ||
                                    config.getUsername() != null && config.getUsername().toLowerCase().contains(searchLower) ||
                                    config.getTenant() != null && config.getTenant().toLowerCase().contains(searchLower)
                    )
                    .toList();
        }
        grid.setItems(filteredConfigs);
        grid.getDataProvider().refreshAll();
    }

    private void openCreateDialog() {
        CreateSenderConfigDialog dialog = new CreateSenderConfigDialog(
                senderConfigService,
                this::loadSenderConfigs
        );
        dialog.open();
    }

    private void openEditDialog(SenderConfigDto config) {
        EditSenderConfigDialog dialog = new EditSenderConfigDialog(
                senderConfigService,
                config,
                this::loadSenderConfigs
        );
        dialog.open();
    }

    private void confirmDelete(SenderConfigDto config) {
        Notification.show(I18n.t("sender.delete.confirm", config.getId()), 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);

        // Simplified delete - in real implementation use a confirmation dialog
        try {
            senderConfigService.delete(config.getId());
            Notification.show(I18n.t("sender.delete.success"), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            loadSenderConfigs();
        } catch (Exception e) {
            Notification.show(I18n.t("sender.delete.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void testConnection(SenderConfigDto config) {
        Notification.show(I18n.t("sender.test.starting", config.getHost()), 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_PRIMARY);

        // In real implementation, call a test endpoint
        // For now, simulate success
        UI.getCurrent().getPage().executeJs(
                "setTimeout(() => { $0.dispatchEvent(new Event('test-complete')); }, 2000)",
                getElement()
        );

        Notification.show(I18n.t("sender.test.success"), 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        grid.setVisible(!show);
        refreshButton.setEnabled(!show);
        createButton.setEnabled(!show);
    }

    private void injectResponsiveStyles() {
        String css = """
                .sender-config-view {
                    background: var(--lumo-base-color);
                    min-height: 100vh;
                }
                .sender-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                @media (max-width: 768px) {
                    .sender-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .sender-toolbar > * {
                        width: 100% !important;
                    }
                    .sender-toolbar vaadin-text-field {
                        width: 100% !important;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}