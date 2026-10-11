package eu.isygoit.ui.ims.views.roleinfo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.remote.ims.RoleInfoService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import eu.isygoit.ui.ims.views.roleinfo.dialog.CreateRoleDialog;
import eu.isygoit.ui.ims.views.roleinfo.dialog.UpdateRoleDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@VaadinSessionScope
@Route(value = "ims/roles", layout = ImsMainLayout.class)
//@PageTitle("Role Management")
@PermitAll
public class RoleManagementView extends AbstractPaginatedView<RoleInfoDto> {

    private final RoleInfoService roleService;
    private final ApplicationService applicationService;

    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());

    @Autowired
    public RoleManagementView(RoleInfoService roleService, ApplicationService applicationService) {
        this.roleService = roleService;
        this.applicationService = applicationService;
        initPaginatedView();
    }

    @Override
    protected PaginatedResponseDto<RoleInfoDto> fetchPage(int page, int size) {
        var r = roleService.findAll(page, size);
        return r != null ? r.getBody() : null;
    }

    @Override
    protected Component renderItem(RoleInfoDto item) {
        return new RoleCard(this, roleService, item, this::loadPageZero);
    }

    @Override
    protected boolean matchesSearch(RoleInfoDto r, String search) {
        if (search == null || search.isBlank()) return true;
        String s = search.toLowerCase();
        return (r.getName() != null && r.getName().toLowerCase().contains(s))
                || (r.getCode() != null && r.getCode().toLowerCase().contains(s));
    }

    @Override
    protected void openCreateDialog() {
        new CreateRoleDialog(this, roleService, applicationService, this::loadPageZero).open();
    }

    @Override
    protected void onSearchChanged() { loadPageZero(); }

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"role-management-view"};
    }

    @Override
    protected List<Component> buildToolbarActions() {
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("ims.role.view.refresh.tooltip"));
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

    @Override protected String getTitleKey()             { return "ims.role.view.title"; }
    @Override protected String getSubtitleKey()          { return "ims.role.view.subtitle"; }
    @Override protected String getCreateButtonKey()      { return "ims.role.view.create.button"; }
    @Override protected String getSearchPlaceholderKey() { return "ims.role.view.search.placeholder"; }
    @Override protected String getEmptyTitleKey()        { return "ims.role.view.empty.title"; }
    @Override protected String getEmptyDescriptionKey()  { return "ims.role.view.empty.description"; }
    @Override protected VaadinIcon getEmptyIcon()        { return VaadinIcon.SHIELD; }
    @Override protected String getLoadErrorKey()         { return "ims.role.view.load.error"; }

    @Override
    public String getPageTitle() { return I18n.t("ims.role.view.title"); }

    public void loadPageZero() { loadPage(0); }

    public void openUpdateRoleDialog(RoleInfoDto role, Runnable onSuccess) {
        new UpdateRoleDialog(this, roleService, applicationService, role, onSuccess).open();
    }
}