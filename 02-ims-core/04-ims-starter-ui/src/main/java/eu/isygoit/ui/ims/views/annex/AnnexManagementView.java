package eu.isygoit.ui.ims.views.annex;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import eu.isygoit.ui.ims.views.annex.dialog.CreateAnnexDialog;
import eu.isygoit.ui.ims.views.annex.dialog.UpdateAnnexDialog;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@VaadinSessionScope
@Route(value = "ims/annexes", layout = ImsMainLayout.class)
//@PageTitle("Annex Management")
@PermitAll
public class AnnexManagementView extends AbstractPaginatedView<AnnexDto> {

    private final AnnexService annexService;

    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final TextField tableCodeFilter = new TextField();
    private final ComboBox<IEnumLanguage.Types> languageFilter = new ComboBox<>();

    private String currentTableCode;
    private IEnumLanguage.Types currentLanguage;

    @Autowired
    public AnnexManagementView(AnnexService annexService) {
        this.annexService = annexService;
        initPaginatedView();
    }

    @Override
    protected PaginatedResponseDto<AnnexDto> fetchPage(int page, int size) {
        var r = annexService.findAll(page, size);
        return r != null ? r.getBody() : null;
    }

    @Override
    protected Component renderItem(AnnexDto item) {
        return new AnnexCard(this, annexService, item, this::loadPageZero);
    }

    @Override
    protected boolean matchesSearch(AnnexDto a, String search) {
        if (search == null || search.isBlank()) return true;
        return a.getValue() != null && a.getValue().toLowerCase().contains(search.toLowerCase());
    }

    @Override
    protected boolean matchesAdditionalFilters(AnnexDto a) {
        if (currentTableCode != null && !currentTableCode.isBlank()
                && !currentTableCode.equals(a.getTableCode())) return false;
        return currentLanguage == null || a.getLanguage() == currentLanguage;
    }

    @Override
    protected void openCreateDialog() {
        new CreateAnnexDialog(this, annexService, this::loadPageZero).open();
    }

    @Override
    protected void onSearchChanged() { loadPageZero(); }

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"annex-management-view"};
    }

    @Override
    protected List<Component> buildToolbarFilters() {
        tableCodeFilter.setPlaceholder(I18n.t("ims.annex.view.table.code.placeholder"));
        tableCodeFilter.setClearButtonVisible(true);
        tableCodeFilter.setWidth("150px");
        tableCodeFilter.addValueChangeListener(e -> { currentTableCode = e.getValue(); loadPageZero(); });

        languageFilter.setItems(IEnumLanguage.Types.values());
        languageFilter.setItemLabelGenerator(l -> ImsEnumTag.label(l, "ims.enum.language"));
        languageFilter.setRenderer(ImsEnumTag.renderer("ims.enum.language"));
        languageFilter.setPlaceholder(I18n.t("ims.annex.view.language.placeholder"));
        languageFilter.setWidth("150px");
        languageFilter.addValueChangeListener(e -> { currentLanguage = e.getValue(); loadPageZero(); });

        return List.of(tableCodeFilter, languageFilter);
    }

    @Override
    protected List<Component> buildToolbarActions() {
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("ims.annex.view.refresh.tooltip"));
        refreshButton.addClickListener(e -> loadPageZero());
        return List.of(refreshButton);
    }

    @Override
    protected String extractErrorMessage(Exception e) {
        if (e instanceof FeignException fex
                && fex.contentUTF8() != null && !fex.contentUTF8().isBlank()) {
            return fex.contentUTF8();
        }
        return e.getMessage();
    }

    @Override protected String getTitleKey()             { return "ims.annex.view.title"; }
    @Override protected String getSubtitleKey()          { return "ims.annex.view.subtitle"; }
    @Override protected String getCreateButtonKey()      { return "ims.annex.view.create.button"; }
    @Override protected String getSearchPlaceholderKey() { return "ims.annex.view.search.placeholder"; }
    @Override protected String getEmptyTitleKey()        { return "ims.annex.view.empty.title"; }
    @Override protected String getEmptyDescriptionKey()  { return "ims.annex.view.empty.description"; }
    @Override protected VaadinIcon getEmptyIcon()        { return VaadinIcon.FILE_CODE; }
    @Override protected String getLoadErrorKey()         { return "ims.annex.view.load.error"; }

    @Override
    public String getPageTitle() { return I18n.t("ims.annex.view.title"); }

    public void loadPageZero() { loadPage(0); }

    public void openUpdateAnnexDialog(AnnexDto annex, Runnable onSuccess) {
        new UpdateAnnexDialog(this, annexService, annex, onSuccess).open();
    }
}