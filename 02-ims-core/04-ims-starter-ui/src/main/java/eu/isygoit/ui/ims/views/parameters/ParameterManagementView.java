package eu.isygoit.ui.ims.views.parameters;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.AppParameterDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AppParameterService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import eu.isygoit.ui.ims.views.parameters.dialog.CreateParameterDialog;
import eu.isygoit.ui.ims.views.parameters.dialog.UpdateParameterDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@VaadinSessionScope
@Route(value = "ims/parameters", layout = ImsMainLayout.class)
//@PageTitle("Parameter Management")
@PermitAll
public class ParameterManagementView extends AbstractPaginatedView<AppParameterDto> {

    private final AppParameterService parameterService;

    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final TextField tenantFilter = new TextField();

    private String currentTenant;

    @Autowired
    public ParameterManagementView(AppParameterService parameterService) {
        this.parameterService = parameterService;
        initPaginatedView();
    }

    @Override
    protected PaginatedResponseDto<AppParameterDto> fetchPage(int page, int size) {
        var r = parameterService.findAll(page, size);
        return r != null ? r.getBody() : null;
    }

    @Override
    protected Component renderItem(AppParameterDto item) {
        return new ParameterCard(this, parameterService, item, this::loadPageZero);
    }

    @Override
    protected boolean matchesSearch(AppParameterDto p, String search) {
        if (search == null || search.isBlank()) return true;
        String s = search.toLowerCase();
        return (p.getName()  != null && p.getName().toLowerCase().contains(s))
                || (p.getValue() != null && p.getValue().toLowerCase().contains(s));
    }

    @Override
    protected boolean matchesAdditionalFilters(AppParameterDto p) {
        return currentTenant == null || currentTenant.isBlank()
                || currentTenant.equals(p.getTenant());
    }

    @Override
    protected void openCreateDialog() {
        new CreateParameterDialog(this, parameterService, this::loadPageZero).open();
    }

    @Override
    protected void onSearchChanged() { loadPageZero(); }

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"parameter-management-view"};
    }

    @Override
    protected List<Component> buildToolbarFilters() {
        tenantFilter.setPlaceholder(I18n.t("ims.parameter.view.tenant.placeholder"));
        tenantFilter.setClearButtonVisible(true);
        tenantFilter.setWidth("150px");
        tenantFilter.addValueChangeListener(e -> { currentTenant = e.getValue(); loadPageZero(); });
        return List.of(tenantFilter);
    }

    @Override
    protected List<Component> buildToolbarActions() {
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("ims.parameter.view.refresh.tooltip"));
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

    @Override protected String getTitleKey()             { return "ims.parameter.view.title"; }
    @Override protected String getSubtitleKey()          { return "ims.parameter.view.subtitle"; }
    @Override protected String getCreateButtonKey()      { return "ims.parameter.view.create.button"; }
    @Override protected String getSearchPlaceholderKey() { return "ims.parameter.view.search.placeholder"; }
    @Override protected String getEmptyTitleKey()        { return "ims.parameter.view.empty.title"; }
    @Override protected String getEmptyDescriptionKey()  { return "ims.parameter.view.empty.description"; }
    @Override protected VaadinIcon getEmptyIcon()        { return VaadinIcon.COG; }
    @Override protected String getLoadErrorKey()         { return "ims.parameter.view.load.error"; }

    @Override
    public String getPageTitle() { return I18n.t("ims.parameter.view.title"); }

    public void loadPageZero() { loadPage(0); }

    public void openUpdateParameterDialog(AppParameterDto p, Runnable onSuccess) {
        new UpdateParameterDialog(this, parameterService, p, onSuccess).open();
    }
}