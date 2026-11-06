package eu.isygoit.ui.mms.views.msgtemplate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.component.ManagementGridSupport;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.mms.layout.MmsMainLayout;
import eu.isygoit.ui.mms.views.common.MmsEnumTag;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.CreateMsgTemplateDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@SpringComponent
@RouteScope
@Route(value = "mms/templates", layout = MmsMainLayout.class)
@PermitAll
public class MsgTemplateManagementView extends AbstractPaginatedView<MsgTemplateDto> {
    private final MsgTemplateService templateService;
    private final MsgTemplateFileService templateFileService;
    private final SenderConfigService senderConfigService;
    private final ComboBox<TemplateLanguageOption> languageFilter = new ComboBox<>();
    private final ComboBox<TemplateNameOption> nameFilter = new ComboBox<>();
    private IEnumLanguage.Types currentLanguageFilter = null;
    private String currentNameFilter = null;

    @Autowired
    public MsgTemplateManagementView(MsgTemplateService templateService,
                                     MsgTemplateFileService templateFileService,
                                     SenderConfigService senderConfigService) {
        this.templateService = templateService;
        this.templateFileService = templateFileService;
        this.senderConfigService = senderConfigService;
        this.pageSize = 10;
        initPaginatedView();
        createButton.setTooltipText(I18n.t("mms.msgtemplate.view.create.tooltip"));
        searchField.setTooltipText(I18n.t("mms.msgtemplate.view.search.tooltip"));
    }

    @Override
    public String getPageTitle() {
        return I18n.t("mms.msgtemplate.view.title");
    }

    // ─── Data ────────────────────────────────────────────────────────────────

    /**
     * The backend returns the full list: filters (search, language, name) are applied over the
     * whole list and the result is paged manually, exactly as before.
     */
    @Override
    protected PaginatedResponseDto<MsgTemplateDto> fetchPage(int page, int size) {
        ResponseEntity<List<MsgTemplateDto>> response = templateService.findAllList();
        List<MsgTemplateDto> all = response.getBody() != null ? response.getBody() : new ArrayList<>();
        List<MsgTemplateDto> filtered = filterTemplates(all);

        int total = filtered.size();
        int totalPagesCount = (int) Math.ceil((double) total / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, total);
        List<MsgTemplateDto> pageContent = startIndex < total
                ? new ArrayList<>(filtered.subList(startIndex, endIndex))
                : new ArrayList<>();

        return PaginatedResponseDto.<MsgTemplateDto>builder()
                .content(pageContent)
                .totalElements(total)
                .totalPages(totalPagesCount)
                .pageNumber(page)
                .pageSize(size)
                .build();
    }

    private List<MsgTemplateDto> filterTemplates(List<MsgTemplateDto> templates) {
        String search = searchField.getValue();
        return templates.stream()
                .filter(template -> {
                    if (currentLanguageFilter != null && template.getLanguage() != currentLanguageFilter) {
                        return false;
                    }
                    if (currentNameFilter != null
                            && (template.getName() == null || !template.getName().equals(currentNameFilter))) {
                        return false;
                    }
                    if (search != null && !search.isEmpty()) {
                        String searchLower = search.toLowerCase();
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

    /**
     * Search is already applied over the whole list in {@link #fetchPage(int, int)}.
     */
    @Override
    protected boolean matchesSearch(MsgTemplateDto item, String search) {
        return true;
    }

    @Override
    protected void onSearchChanged() {
        loadPage(0);
    }

    // ─── Rendering ───────────────────────────────────────────────────────────

    @Override
    protected Component renderItem(MsgTemplateDto item) {
        return new MsgTemplateCard(this, templateService, templateFileService, senderConfigService,
                item, this::reloadFirstPage);
    }

    @Override
    protected boolean configureGrid(Grid<MsgTemplateDto> grid) {
        grid.addColumn(MsgTemplateDto::getName)
                .setHeader(I18n.t("mms.msgtemplate.view.name.label")).setAutoWidth(true);
        grid.addColumn(MsgTemplateDto::getCode)
                .setHeader(I18n.t("mms.msgtemplate.card.code")).setAutoWidth(true);
        grid.addColumn(t -> t.getLanguage() == null
                        ? "" : MmsEnumTag.label(t.getLanguage(), "mms.msgtemplate.view.language"))
                .setHeader(I18n.t("mms.msgtemplate.card.language")).setAutoWidth(true);
        grid.addColumn(MsgTemplateDto::getTenant)
                .setHeader(I18n.t("mms.msgtemplate.card.tenant")).setAutoWidth(true);
        ManagementGridSupport.addCardActionsColumn(grid, t -> new MsgTemplateCard(
                this, templateService, templateFileService, senderConfigService,
                t, this::reloadFirstPage, false));
        return true;
    }

    @Override
    protected void openCreateDialog() {
        new CreateMsgTemplateDialog(this, templateService, templateFileService, senderConfigService,
                this::reloadFirstPage).open();
    }

    private void reloadFirstPage() {
        loadPage(0);
    }

    // ─── Toolbar ─────────────────────────────────────────────────────────────

    @Override
    protected List<Component> buildToolbarFilters() {
        languageFilter.setLabel(I18n.t("mms.msgtemplate.view.language.label"));
        languageFilter.setItems(
                new TemplateLanguageOption(I18n.t("mms.msgtemplate.view.language.all"), null),
                new TemplateLanguageOption(I18n.t("mms.msgtemplate.view.language.en"), IEnumLanguage.Types.EN),
                new TemplateLanguageOption(I18n.t("mms.msgtemplate.view.language.fr"), IEnumLanguage.Types.FR),
                new TemplateLanguageOption(I18n.t("mms.msgtemplate.view.language.ar"), IEnumLanguage.Types.AR)
        );
        languageFilter.setItemLabelGenerator(option -> option.label());
        languageFilter.setRenderer(new ComponentRenderer<>(option -> option.value() == null
                ? new Span(option.label())
                : MmsEnumTag.of(option.value(), "mms.msgtemplate.view.language")));
        languageFilter.setValue(new TemplateLanguageOption(I18n.t("mms.msgtemplate.view.language.all"), null));
        languageFilter.setPlaceholder(I18n.t("mms.msgtemplate.view.language.placeholder"));
        languageFilter.setTooltipText(I18n.t("mms.msgtemplate.view.language.tooltip"));
        languageFilter.setWidth("160px");
        languageFilter.addValueChangeListener(e -> {
            TemplateLanguageOption option = e.getValue();
            currentLanguageFilter = option != null ? option.value() : null;
            loadPage(0);
        });

        nameFilter.setLabel(I18n.t("mms.msgtemplate.view.name.label"));
        nameFilter.setItems(
                new TemplateNameOption(I18n.t("mms.msgtemplate.view.name.all"), null)
        );
        nameFilter.setItemLabelGenerator(option -> option.label());
        nameFilter.setRenderer(new ComponentRenderer<>(option -> option.value() == null
                ? new Span(option.label())
                : MmsEnumTag.ofValue(option.value(), "mms.msgtemplate.enum.name")));
        nameFilter.setValue(new TemplateNameOption(I18n.t("mms.msgtemplate.view.name.all"), null));
        nameFilter.setPlaceholder(I18n.t("mms.msgtemplate.view.name.placeholder"));
        nameFilter.setTooltipText(I18n.t("mms.msgtemplate.view.name.tooltip"));
        nameFilter.setWidth("180px");

        // Load template names for filter
        loadTemplateNames();

        nameFilter.addValueChangeListener(e -> {
            TemplateNameOption option = e.getValue();
            currentNameFilter = option != null ? option.value() : null;
            loadPage(0);
        });

        return List.of(languageFilter, nameFilter);
    }

    private void loadTemplateNames() {
        try {
            ResponseEntity<List<String>> response = templateService.getTemplateNames();
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                List<TemplateNameOption> options = new ArrayList<>();
                options.add(new TemplateNameOption(I18n.t("mms.msgtemplate.view.name.all"), null));
                response.getBody().forEach(name ->
                        options.add(new TemplateNameOption(
                                MmsEnumTag.label(name, "mms.msgtemplate.enum.name"), name))
                );
                nameFilter.setItems(options);
                nameFilter.setValue(options.get(0));
            }
        } catch (Exception e) {
            log.error("Failed to load template names", e);
        }
    }

    // ─── Shell hooks ─────────────────────────────────────────────────────────

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"template-view"};
    }

    @Override
    protected String extractErrorMessage(Exception e) {
        if (e instanceof FeignException ex) {
            return (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
        }
        return e.getMessage();
    }

    @Override
    protected String getLoadErrorKey() {
        return "mms.msgtemplate.view.load.error";
    }

    @Override
    protected String getTitleKey() {
        return "mms.msgtemplate.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "mms.msgtemplate.view.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "mms.msgtemplate.view.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "mms.msgtemplate.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "mms.msgtemplate.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "mms.msgtemplate.view.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.FILE_CODE;
    }

    public record TemplateLanguageOption(String label, IEnumLanguage.Types value) {
    }

    public record TemplateNameOption(String label, String value) {
    }
}
