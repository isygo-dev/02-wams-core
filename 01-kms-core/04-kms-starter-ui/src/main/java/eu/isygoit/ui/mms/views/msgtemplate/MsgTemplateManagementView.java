package eu.isygoit.ui.mms.views.msgtemplate;

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
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.mms.layout.MmsMainLayout;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.CreateMsgTemplateDialog;
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
@Route(value = "mms/templates", layout = MmsMainLayout.class)
@PageTitle("Template Management")
@PermitAll
public class MsgTemplateManagementView extends ManagementVerticalView {

    private final MsgTemplateService templateService;
    private final MsgTemplateFileService templateFileService;

    private final Div cardsContainer = new Div();
    private final Button createButton = new Button(I18n.t("template.view.create.button"), new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ComboBox<TemplateLanguageOption> languageFilter = new ComboBox<>();
    private final ComboBox<TemplateNameOption> nameFilter = new ComboBox<>();
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
    private List<MsgTemplateCard> currentPageCards = new ArrayList<>();
    private String currentSearch = "";
    private IEnumLanguage.Types currentLanguageFilter = null;
    private String currentNameFilter = null;

    private List<MsgTemplateDto> allTemplates = new ArrayList<>();
    private List<MsgTemplateDto> filteredTemplates = new ArrayList<>();

    @Autowired
    public MsgTemplateManagementView(MsgTemplateService templateService,
                                     MsgTemplateFileService templateFileService) {
        this.templateService = templateService;
        this.templateFileService = templateFileService;
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

        cardsContainer.setWidthFull();
        cardsContainer.addClassName("template-grid");
        add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        createButton.addClickListener(e -> openCreateTemplateDialog());
        createButton.setTooltipText(I18n.t("template.view.create.tooltip"));

        refreshButton.addClickListener(e -> resetPaginationAndLoad());
        refreshButton.setTooltipText(I18n.t("template.view.refresh.tooltip"));

        searchField.setPlaceholder(I18n.t("template.view.search.placeholder"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setTooltipText(I18n.t("template.view.search.tooltip"));
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            resetPaginationAndLoad();
        });

        languageFilter.setItems(
                new TemplateLanguageOption(I18n.t("template.view.language.all"), null),
                new TemplateLanguageOption(I18n.t("template.view.language.en"), IEnumLanguage.Types.EN),
                new TemplateLanguageOption(I18n.t("template.view.language.fr"), IEnumLanguage.Types.FR),
                new TemplateLanguageOption(I18n.t("template.view.language.ar"), IEnumLanguage.Types.AR)
        );
        languageFilter.setItemLabelGenerator(option -> option.label());
        languageFilter.setValue(new TemplateLanguageOption(I18n.t("template.view.language.all"), null));
        languageFilter.setPlaceholder(I18n.t("template.view.language.placeholder"));
        languageFilter.setTooltipText(I18n.t("template.view.language.tooltip"));
        languageFilter.addValueChangeListener(e -> {
            TemplateLanguageOption option = e.getValue();
            currentLanguageFilter = option != null ? option.value() : null;
            resetPaginationAndLoad();
        });

        nameFilter.setItems(
                new TemplateNameOption(I18n.t("template.view.name.all"), null)
        );
        nameFilter.setItemLabelGenerator(option -> option.label());
        nameFilter.setValue(new TemplateNameOption(I18n.t("template.view.name.all"), null));
        nameFilter.setPlaceholder(I18n.t("template.view.name.placeholder"));
        nameFilter.setTooltipText(I18n.t("template.view.name.tooltip"));
        nameFilter.addValueChangeListener(e -> {
            TemplateNameOption option = e.getValue();
            currentNameFilter = option != null ? option.value() : null;
            resetPaginationAndLoad();
        });

        // Load template names for filter
        loadTemplateNames();

        pageSizeSelect.setItems(10, 20, 30, 40, 50);
        pageSizeSelect.setValue(10);
        pageSizeSelect.setPlaceholder(I18n.t("template.view.page.per.page"));
        pageSizeSelect.setTooltipText(I18n.t("template.view.page.per.page.tooltip"));
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                resetPaginationAndLoad();
            }
        });

        prevButton.addClickListener(e -> {
            if (!previousTokens.isEmpty()) {
                String prevToken = previousTokens.pop();
                loadTemplatesPage(prevToken);
            }
        });
        prevButton.setTooltipText(I18n.t("template.view.prev.page.tooltip"));

        nextButton.addClickListener(e -> {
            if (truncated && currentNextToken != null) {
                previousTokens.push(currentToken);
                loadTemplatesPage(currentNextToken);
            }
        });
        nextButton.setTooltipText(I18n.t("template.view.next.page.tooltip"));

        injectResponsiveStyles();
        resetPaginationAndLoad();
    }

    private void loadTemplateNames() {
        try {
            ResponseEntity<List<String>> response = templateService.getTemplateNames();
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                List<TemplateNameOption> options = new ArrayList<>();
                options.add(new TemplateNameOption(I18n.t("template.view.name.all"), null));
                response.getBody().forEach(name ->
                        options.add(new TemplateNameOption(name, name))
                );
                nameFilter.setItems(options);
                nameFilter.setValue(options.get(0));
            }
        } catch (Exception e) {
            log.error("Failed to load template names", e);
        }
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
        loadTemplatesPage(null);
    }

    private void loadTemplatesPage(String nextToken) {
        showLoading(true);
        try {
            ResponseEntity<List<MsgTemplateDto>> response = templateService.findAllList();
            allTemplates = response.getBody() != null ? response.getBody() : new ArrayList<>();

            // Apply filters
            filteredTemplates = filterTemplates(allTemplates);

            // Manual pagination
            totalElements = filteredTemplates.size();
            totalPages = (int) Math.ceil((double) totalElements / pageSize);

            int startIndex = (currentPage - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, (int) totalElements);

            List<MsgTemplateDto> pageTemplates;
            if (startIndex < totalElements) {
                pageTemplates = filteredTemplates.subList(startIndex, endIndex);
            } else {
                pageTemplates = new ArrayList<>();
            }

            numberOfElements = pageTemplates.size();
            truncated = endIndex < totalElements;
            currentNextToken = truncated ? String.valueOf(currentPage + 1) : null;

            currentPageCards = pageTemplates.stream()
                    .map(template -> new MsgTemplateCard(this, templateService, templateFileService, template, this::resetPaginationAndLoad))
                    .collect(Collectors.toList());

            updatePaginationDisplay();
            displayCards();

        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            Notification.show(I18n.t("template.view.load.error", errorMsg), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load templates", ex.getMessage());
        } catch (Exception e) {
            Notification.show(I18n.t("template.view.load.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load templates", e);
        } finally {
            showLoading(false);
        }
    }

    private List<MsgTemplateDto> filterTemplates(List<MsgTemplateDto> templates) {
        return templates.stream()
                .filter(template -> {
                    // Language filter
                    if (currentLanguageFilter != null) {
                        if (template.getLanguage() != currentLanguageFilter) return false;
                    }

                    // Name filter
                    if (currentNameFilter != null) {
                        if (template.getName() == null || !template.getName().equals(currentNameFilter)) return false;
                    }

                    // Search filter
                    if (currentSearch != null && !currentSearch.isEmpty()) {
                        String searchLower = currentSearch.toLowerCase();
                        String name = template.getName() != null ? template.getName().toLowerCase() : "";
                        String code = template.getCode() != null ? template.getCode().toLowerCase() : "";
                        String description = template.getDescription() != null ? template.getDescription().toLowerCase() : "";
                        String tenant = template.getTenant() != null ? template.getTenant().toLowerCase() : "";
                        return name.contains(searchLower) ||
                                code.contains(searchLower) ||
                                description.contains(searchLower) ||
                                tenant.contains(searchLower);
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private void updatePaginationDisplay() {
        if (totalPages > 0) {
            pageInfoLabel.setText(I18n.t("template.view.page.info", currentPage, totalPages, numberOfElements));
        } else {
            pageInfoLabel.setText(I18n.t("template.view.page.info.simple", currentPage, numberOfElements));
        }
        totalCountLabel.setText(I18n.t("template.view.total.count", totalElements));

        prevButton.setEnabled(!previousTokens.isEmpty());
        nextButton.setEnabled(truncated && currentNextToken != null);
    }

    private void displayCards() {
        cardsContainer.removeAll();

        if (currentPageCards.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.FILE_CODE.create();
            emptyIcon.setSize("48px");
            emptyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
            H4 emptyTitle = new H4(I18n.t("template.view.empty.title"));
            Paragraph emptyDesc = new Paragraph(I18n.t("template.view.empty.description"));
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
        toolbar.addClassName("template-toolbar");

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setSpacing(true);
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        leftGroup.getStyle().set("flex-wrap", "wrap");
        leftGroup.getStyle().set("gap", "var(--lumo-space-s)");

        searchField.setWidth("200px");

        Span languageLabel = new Span(I18n.t("template.view.language.label"));
        languageLabel.getElement().setAttribute("title", I18n.t("template.view.language.tooltip"));
        languageFilter.setWidth("120px");

        Span nameLabel = new Span(I18n.t("template.view.name.label"));
        nameLabel.getElement().setAttribute("title", I18n.t("template.view.name.tooltip"));
        nameFilter.setWidth("150px");

        HorizontalLayout languageLayout = new HorizontalLayout(languageLabel, languageFilter);
        languageLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        languageLayout.setSpacing(true);

        HorizontalLayout nameLayout = new HorizontalLayout(nameLabel, nameFilter);
        nameLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        nameLayout.setSpacing(true);

        leftGroup.add(searchField, languageLayout, nameLayout);

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
        refreshButton.setTooltipText(I18n.t("template.view.refresh.tooltip"));
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        rightGroup.add(refreshButton, createButton);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        return toolbar;
    }

    private void injectResponsiveStyles() {
        String css = """
                .template-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .template-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                .template-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
                    gap: var(--lumo-space-m);
                    padding: var(--lumo-space-s);
                }
                @media (max-width: 768px) {
                    .template-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .template-toolbar > * {
                        width: 100% !important;
                        justify-content: center;
                    }
                    .template-grid {
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

    private void openCreateTemplateDialog() {
        new CreateMsgTemplateDialog(this, templateService, templateFileService, this::resetPaginationAndLoad).open();
    }

    public record TemplateLanguageOption(String label, IEnumLanguage.Types value) {
    }

    public record TemplateNameOption(String label, String value) {
    }
}