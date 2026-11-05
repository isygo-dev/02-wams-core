package eu.isygoit.ui.ims.views.roleinfo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.RouteScope;
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
@SpringComponent
@RouteScope
@Route(value = "ims/roles", layout = ImsMainLayout.class)
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
    protected boolean configureGrid(com.vaadin.flow.component.grid.Grid<RoleInfoDto> grid) {
        grid.addColumn(RoleInfoDto::getCode).setHeader("Code").setAutoWidth(true);
        grid.addColumn(RoleInfoDto::getName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(RoleInfoDto::getLevel).setHeader("Level").setAutoWidth(true);
        grid.addColumn(RoleInfoDto::getNumberOfUsers).setHeader("Users").setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected PaginatedResponseDto<? extends RoleInfoDto> fetchFilteredPage(String criteria, int page, int size) {
        var r = roleService.findAllFilteredByCriteria(criteria, page, size);
        return r != null ? r.getBody() : null;
    }

    @Override
    protected java.util.Map<String, String> fetchCriteriaMetadata() {
        var r = roleService.getAnnotatedCriteria();
        return r != null && r.getBody() != null
                ? r.getBody().stream().collect(java.util.stream.Collectors.toMap(
                field -> field.name(), field -> field.type(), (first, second) -> second,
                java.util.LinkedHashMap::new))
                : java.util.Map.of();
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
    protected void onSearchChanged() {
        loadPageZero();
    }

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

    @Override
    protected String getTitleKey() {
        return "ims.role.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "ims.role.view.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "ims.role.view.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "ims.role.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "ims.role.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "ims.role.view.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.SHIELD;
    }

    @Override
    protected String getLoadErrorKey() {
        return "ims.role.view.load.error";
    }

    @Override
    public String getPageTitle() {
        return I18n.t("ims.role.view.title");
    }

    public void loadPageZero() {
        loadPage(0);
    }

    public void openUpdateRoleDialog(RoleInfoDto role, Runnable onSuccess) {
        new UpdateRoleDialog(this, roleService, applicationService, role, onSuccess).open();
    }
}