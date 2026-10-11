package eu.isygoit.ui.ims.views.registered;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.enums.IEnumAccountOrigin;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import eu.isygoit.ui.ims.views.registered.dialog.CreateAccountFromRegisteredDialog;
import eu.isygoit.ui.ims.views.registered.dialog.CreateRegisteredUserDialog;
import eu.isygoit.ui.ims.views.registered.dialog.UpdateRegisteredUserDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@VaadinSessionScope
@Route(value = "ims/registered-users", layout = ImsMainLayout.class)
//@PageTitle("Registered User Management")
@PermitAll
public class RegisteredManagementView extends AbstractPaginatedView<RegisteredUserDto> {

    private final RegisteredUserService registeredUserService;
    private final AccountService accountService;
    private final TenantService tenantService;

    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final ComboBox<IEnumAccountOrigin.Types> originFilter = new ComboBox<>();

    private IEnumAccountOrigin.Types currentOrigin;

    @Autowired
    public RegisteredManagementView(RegisteredUserService registeredUserService,
                                    AccountService accountService,
                                    TenantService tenantService) {
        this.registeredUserService = registeredUserService;
        this.accountService = accountService;
        this.tenantService = tenantService;
        initPaginatedView();
    }

    @Override
    protected PaginatedResponseDto<RegisteredUserDto> fetchPage(int page, int size) {
        var r = registeredUserService.findAll(page, size);
        return r != null ? r.getBody() : null;
    }

    @Override
    protected Component renderItem(RegisteredUserDto item) {
        return new RegisteredCard(this, registeredUserService, accountService,
                tenantService, item, this::loadPageZero);
    }

    @Override
    protected boolean matchesSearch(RegisteredUserDto u, String search) {
        if (search == null || search.isBlank()) return true;
        String s = search.toLowerCase();
        return (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(s))
                || (u.getLastName()  != null && u.getLastName().toLowerCase().contains(s))
                || (u.getEmail()     != null && u.getEmail().toLowerCase().contains(s));
    }

    @Override
    protected boolean matchesAdditionalFilters(RegisteredUserDto u) {
        if (currentOrigin == null) return true;
        return u.getOrigin() != null && u.getOrigin().equals(currentOrigin.name());
    }

    @Override
    protected void openCreateDialog() {
        new CreateRegisteredUserDialog(this, registeredUserService, this::loadPageZero).open();
    }

    @Override
    protected void onSearchChanged() { loadPageZero(); }

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"registered-management-view"};
    }

    @Override
    protected List<Component> buildToolbarFilters() {
        originFilter.setItems(IEnumAccountOrigin.Types.values());
        originFilter.setItemLabelGenerator(o -> ImsEnumTag.label(o, "ims.enum.origin"));
        originFilter.setRenderer(ImsEnumTag.renderer("ims.enum.origin"));
        originFilter.setPlaceholder(I18n.t("ims.registered.view.origin.placeholder"));
        originFilter.setWidth("160px");
        originFilter.addValueChangeListener(e -> { currentOrigin = e.getValue(); loadPageZero(); });
        return List.of(originFilter);
    }

    @Override
    protected List<Component> buildToolbarActions() {
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("ims.registered.view.refresh.tooltip"));
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

    @Override protected String getTitleKey()             { return "ims.registered.view.title"; }
    @Override protected String getSubtitleKey()          { return "ims.registered.view.subtitle"; }
    @Override protected String getCreateButtonKey()      { return "ims.registered.view.create.button"; }
    @Override protected String getSearchPlaceholderKey() { return "ims.registered.view.search.placeholder"; }
    @Override protected String getEmptyTitleKey()        { return "ims.registered.view.empty.title"; }
    @Override protected String getEmptyDescriptionKey()  { return "ims.registered.view.empty.description"; }
    @Override protected VaadinIcon getEmptyIcon()        { return VaadinIcon.CLIPBOARD_USER; }
    @Override protected String getLoadErrorKey()         { return "ims.registered.view.load.error"; }

    @Override
    public String getPageTitle() { return I18n.t("ims.registered.view.title"); }

    public void loadPageZero() { loadPage(0); }

    public void openUpdateRegisteredUserDialog(RegisteredUserDto u, Runnable onSuccess) {
        new UpdateRegisteredUserDialog(this, registeredUserService, u, onSuccess).open();
    }

    public void openCreateAccountFromRegisteredDialog(RegisteredUserDto u, Runnable onSuccess) {
        new CreateAccountFromRegisteredDialog(this, registeredUserService, u, onSuccess).open();
    }

    public AccountService getAccountService() { return accountService; }
    public TenantService   getTenantService() { return tenantService; }
}