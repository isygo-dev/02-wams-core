package eu.isygoit.ui.ims.views.tenant;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.TenantImageService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import eu.isygoit.ui.ims.views.tenant.dialog.CreateTenantDialog;
import eu.isygoit.ui.ims.views.tenant.dialog.UpdateTenantDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@VaadinSessionScope
@Route(value = "ims/tenants", layout = ImsMainLayout.class)
//@PageTitle("Tenant Management")
@PermitAll
public class TenantManagementView extends AbstractPaginatedView<TenantDto> {

    private final TenantService tenantService;
    private final TenantImageService tenantImageService;

    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final ComboBox<IEnumEnabledBinaryStatus.Types> statusFilter = new ComboBox<>();

    private IEnumEnabledBinaryStatus.Types currentAdminStatus;

    @Autowired
    public TenantManagementView(TenantService tenantService, TenantImageService tenantImageService) {
        this.tenantService = tenantService;
        this.tenantImageService = tenantImageService;
        initPaginatedView();
    }

    @Override
    protected PaginatedResponseDto<TenantDto> fetchPage(int page, int size) {
        var r = tenantService.findAll(page, size);
        return r != null ? r.getBody() : null;
    }

    @Override
    protected Component renderItem(TenantDto item) {
        return new TenantCard(this, tenantService, tenantImageService, item, this::loadPageZero);
    }

    @Override
    protected boolean matchesSearch(TenantDto t, String search) {
        if (search == null || search.isBlank()) return true;
        String s = search.toLowerCase();
        return (t.getName() != null && t.getName().toLowerCase().contains(s))
                || (t.getEmail() != null && t.getEmail().toLowerCase().contains(s));
    }

    @Override
    protected boolean matchesAdditionalFilters(TenantDto t) {
        return currentAdminStatus == null || t.getAdminStatus() == currentAdminStatus;
    }

    @Override
    protected void openCreateDialog() {
        new CreateTenantDialog(this, tenantService, tenantImageService, this::loadPageZero).open();
    }

    @Override
    protected void onSearchChanged() {
        loadPageZero();
    }

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"tenant-management-view"};
    }

    @Override
    protected List<Component> buildToolbarFilters() {
        statusFilter.setItems(IEnumEnabledBinaryStatus.Types.values());
        statusFilter.setItemLabelGenerator(s -> ImsEnumTag.label(s, null));
        statusFilter.setRenderer(ImsEnumTag.renderer(null));
        statusFilter.setPlaceholder(I18n.t("ims.tenant.view.status.placeholder"));
        statusFilter.setWidth("160px");
        statusFilter.addValueChangeListener(e -> {
            currentAdminStatus = e.getValue();
            loadPageZero();
        });
        return List.of(statusFilter);
    }

    @Override
    protected List<Component> buildToolbarActions() {
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("ims.tenant.view.refresh.tooltip"));
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
        return "ims.tenant.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "ims.tenant.view.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "ims.tenant.view.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "ims.tenant.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "ims.tenant.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "ims.tenant.view.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.BUILDING;
    }

    @Override
    protected String getLoadErrorKey() {
        return "ims.tenant.view.load.error";
    }

    @Override
    public String getPageTitle() {
        return I18n.t("ims.tenant.view.title");
    }

    public void loadPageZero() {
        loadPage(0);
    }

    public void openUpdateTenantDialog(TenantDto tenant, Runnable onSuccess) {
        new UpdateTenantDialog(this, tenantService, tenantImageService, tenant, onSuccess).open();
    }
}