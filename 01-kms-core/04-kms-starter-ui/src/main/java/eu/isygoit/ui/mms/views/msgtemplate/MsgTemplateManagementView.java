package eu.isygoit.ui.mms.views.msgtemplate;

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
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.mms.layout.MmsMainLayout;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.CreateTemplateDialog;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.EditTemplateDialog;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.ViewTemplateDialog;
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
@Route(value = "mms/templates", layout = MmsMainLayout.class)
@PageTitle("Message Template Management")
@PermitAll
public class MsgTemplateManagementView extends ManagementVerticalView {

    private final MsgTemplateService templateService;

    private final Grid<MsgTemplateDto> grid = new Grid<>(MsgTemplateDto.class, false);
    private final Button createButton = new Button(I18n.t("template.view.create.button"), new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ProgressBar loadingBar = new ProgressBar();

    private List<MsgTemplateDto> allTemplates = new ArrayList<>();
    private List<MsgTemplateDto> filteredTemplates = new ArrayList<>();

    @Autowired
    public MsgTemplateManagementView(MsgTemplateService templateService) {
        this.templateService = templateService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("template-view");

        H2 header = new H2(I18n.t("template.view.title"));
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
        loadTemplates();
    }

    private void configureGrid() {
        grid.setWidthFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);

        grid.addColumn(MsgTemplateDto::getId)
                .setHeader(I18n.t("template.grid.id"))
                .setWidth("80px")
                .setFlexGrow(0)
                .setSortable(true);

        grid.addColumn(MsgTemplateDto::getTenant)
                .setHeader(I18n.t("template.grid.tenant"))
                .setWidth("150px")
                .setSortable(true);

        grid.addColumn(MsgTemplateDto::getName)
                .setHeader(I18n.t("template.grid.name"))
                .setSortable(true)
                .setResizable(true);

        grid.addColumn(MsgTemplateDto::getCode)
                .setHeader(I18n.t("template.grid.code"))
                .setWidth("150px")
                .setSortable(true);

        grid.addColumn(MsgTemplateDto::getDescription)
                .setHeader(I18n.t("template.grid.description"))
                .setSortable(true)
                .setResizable(true);

        grid.addComponentColumn(template -> {
                    String lang = template.getLanguage() != null ? template.getLanguage().name() : "EN";
                    Paragraph langLabel = new Paragraph(lang);
                    langLabel.addClassName(LumoUtility.Background.CONTRAST_5);
                    langLabel.addClassName(LumoUtility.Padding.XSMALL);
                    langLabel.addClassName(LumoUtility.BorderRadius.SMALL);
                    langLabel.addClassName(LumoUtility.FontSize.XSMALL);
                    langLabel.getStyle().set("display", "inline-block");
                    return langLabel;
                }).setHeader(I18n.t("template.grid.language"))
                .setWidth("100px")
                .setFlexGrow(0)
                .setSortable(true);

        grid.addComponentColumn(this::createActionButtons)
                .setHeader(I18n.t("template.grid.actions"))
                .setWidth("180px")
                .setFlexGrow(0);

        grid.setItems(filteredTemplates);
    }

    private HorizontalLayout createActionButtons(MsgTemplateDto template) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        Button viewBtn = new Button(new Icon(VaadinIcon.EYE));
        viewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        viewBtn.setTooltipText(I18n.t("template.action.view"));
        viewBtn.addClickListener(e -> openViewDialog(template));

        Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
        editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editBtn.setTooltipText(I18n.t("template.action.edit"));
        editBtn.addClickListener(e -> openEditDialog(template));

        Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteBtn.setTooltipText(I18n.t("template.action.delete"));
        deleteBtn.addClickListener(e -> confirmDelete(template));

        actions.add(viewBtn, editBtn, deleteBtn);
        return actions;
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.addClassName("template-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(Alignment.CENTER);

        searchField.setPlaceholder(I18n.t("template.search.placeholder"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setTooltipText(I18n.t("template.search.tooltip"));
        searchField.addValueChangeListener(e -> filterTemplates(e.getValue()));
        searchField.setWidth("300px");

        leftGroup.add(searchField);

        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        rightGroup.setAlignItems(Alignment.CENTER);

        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("template.refresh.tooltip"));
        refreshButton.addClickListener(e -> loadTemplates());

        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.setTooltipText(I18n.t("template.create.tooltip"));
        createButton.addClickListener(e -> openCreateDialog());

        rightGroup.add(refreshButton, createButton);

        toolbar.add(leftGroup, rightGroup);
        return toolbar;
    }

    private void loadTemplates() {
        showLoading(true);
        try {
            ResponseEntity<List<MsgTemplateDto>> response = templateService.findAllList();
            allTemplates = response.getBody() != null ? response.getBody() : new ArrayList<>();
            filteredTemplates = new ArrayList<>(allTemplates);
            grid.setItems(filteredTemplates);
            grid.getDataProvider().refreshAll();
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 404 ? I18n.t("template.load.not.found") : ex.getMessage();
            Notification.show(I18n.t("template.load.error", errorMsg), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load templates", ex);
        } catch (Exception e) {
            Notification.show(I18n.t("template.load.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load templates", e);
        } finally {
            showLoading(false);
        }
    }

    private void filterTemplates(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredTemplates = new ArrayList<>(allTemplates);
        } else {
            String searchLower = searchText.toLowerCase();
            filteredTemplates = allTemplates.stream()
                    .filter(template ->
                            template.getName() != null && template.getName().toLowerCase().contains(searchLower) ||
                                    template.getCode() != null && template.getCode().toLowerCase().contains(searchLower) ||
                                    template.getDescription() != null && template.getDescription().toLowerCase().contains(searchLower) ||
                                    template.getTenant() != null && template.getTenant().toLowerCase().contains(searchLower)
                    )
                    .collect(Collectors.toList());
        }
        grid.setItems(filteredTemplates);
        grid.getDataProvider().refreshAll();
    }

    private void openCreateDialog() {
        CreateTemplateDialog dialog = new CreateTemplateDialog(
                templateService,
                this::loadTemplates
        );
        dialog.open();
    }

    private void openEditDialog(MsgTemplateDto template) {
        EditTemplateDialog dialog = new EditTemplateDialog(
                templateService,
                template,
                this::loadTemplates
        );
        dialog.open();
    }

    private void openViewDialog(MsgTemplateDto template) {
        ViewTemplateDialog dialog = new ViewTemplateDialog(template);
        dialog.open();
    }

    private void confirmDelete(MsgTemplateDto template) {
        try {
            templateService.delete(template.getId());
            Notification.show(I18n.t("template.delete.success"), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            loadTemplates();
        } catch (Exception e) {
            Notification.show(I18n.t("template.delete.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        grid.setVisible(!show);
        refreshButton.setEnabled(!show);
        createButton.setEnabled(!show);
    }

    private void injectResponsiveStyles() {
        String css = """
                .template-view {
                    background: var(--lumo-base-color);
                    min-height: 100vh;
                }
                .template-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                @media (max-width: 768px) {
                    .template-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .template-toolbar > * {
                        width: 100% !important;
                    }
                    .template-toolbar vaadin-text-field {
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