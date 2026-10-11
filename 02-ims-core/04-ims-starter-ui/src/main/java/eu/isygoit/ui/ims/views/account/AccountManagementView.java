package eu.isygoit.ui.ims.views.account;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.MinAccountDto;
import eu.isygoit.enums.IEnumAccountSystemStatus;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import eu.isygoit.ui.ims.views.account.dialog.CreateAccountDialog;
import eu.isygoit.ui.ims.views.account.dialog.ResetPasswordDialog;
import eu.isygoit.ui.ims.views.account.dialog.UpdateAccountDialog;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@VaadinSessionScope
@Route(value = "ims/accounts", layout = ImsMainLayout.class)
@PermitAll
public class AccountManagementView extends AbstractPaginatedView<MinAccountDto> {

    private final AccountService accountService;
    private final AccountImageService accountImageService;
    private final TenantService tenantService;

    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final ComboBox<StatusFilterOption> statusFilter = new ComboBox<>();

    private IEnumEnabledBinaryStatus.Types  currentAdminStatus;
    private IEnumAccountSystemStatus.Types  currentSystemStatus;

    @Autowired
    public AccountManagementView(AccountService accountService,
                                 AccountImageService accountImageService,
                                 TenantService tenantService) {
        this.accountService = accountService;
        this.accountImageService = accountImageService;
        this.tenantService = tenantService;
        initPaginatedView();
    }

    @Override
    protected PaginatedResponseDto<MinAccountDto> fetchPage(int page, int size) {
        var r = accountService.findAll(page, size);
        return r != null ? r.getBody() : null;
    }

    @Override
    protected Component renderItem(MinAccountDto item) {
        return new AccountCard(this, accountService, accountImageService, item, this::loadPageZero);
    }

    @Override
    protected boolean matchesSearch(MinAccountDto a, String search) {
        if (search == null || search.isBlank()) return true;
        String s = search.toLowerCase();
        return (a.getEmail()    != null && a.getEmail().toLowerCase().contains(s))
                || (a.getFullName() != null && a.getFullName().toLowerCase().contains(s));
    }

    @Override
    protected boolean matchesAdditionalFilters(MinAccountDto a) {
        if (currentAdminStatus  != null && a.getAdminStatus()  != currentAdminStatus)  return false;
        if (currentSystemStatus != null && a.getSystemStatus() != currentSystemStatus) return false;
        return true;
    }

    @Override
    protected void openCreateDialog() {
        new CreateAccountDialog(this, accountService, accountImageService,
                tenantService, this::loadPageZero).open();
    }

    @Override
    protected void onSearchChanged() { loadPageZero(); }

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"account-management-view"};
    }

    @Override
    protected List<Component> buildToolbarFilters() {
        statusFilter.setPlaceholder(I18n.t("ims.account.view.status.filter.placeholder"));
        statusFilter.setWidth("180px");
        statusFilter.setItems(
                new StatusFilterOption(I18n.t("ims.account.view.status.all"), null, null),
                new StatusFilterOption(ImsEnumTag.label(IEnumEnabledBinaryStatus.Types.ENABLED, null),
                        IEnumEnabledBinaryStatus.Types.ENABLED, null),
                new StatusFilterOption(ImsEnumTag.label(IEnumEnabledBinaryStatus.Types.DISABLED, null),
                        IEnumEnabledBinaryStatus.Types.DISABLED, null),
                new StatusFilterOption(ImsEnumTag.label(IEnumAccountSystemStatus.Types.EXPIRED, null),
                        null, IEnumAccountSystemStatus.Types.EXPIRED),
                new StatusFilterOption(ImsEnumTag.label(IEnumAccountSystemStatus.Types.REGISTRED, null),
                        null, IEnumAccountSystemStatus.Types.REGISTRED),
                new StatusFilterOption(ImsEnumTag.label(IEnumAccountSystemStatus.Types.TEM_LOCKED, null),
                        null, IEnumAccountSystemStatus.Types.TEM_LOCKED),
                new StatusFilterOption(ImsEnumTag.label(IEnumAccountSystemStatus.Types.IDLE, null),
                        null, IEnumAccountSystemStatus.Types.IDLE),
                new StatusFilterOption(ImsEnumTag.label(IEnumAccountSystemStatus.Types.LOCKED, null),
                        null, IEnumAccountSystemStatus.Types.LOCKED));
        statusFilter.setItemLabelGenerator(StatusFilterOption::label);
        statusFilter.setRenderer(new ComponentRenderer<>(o -> {
            if (o.adminStatus()  != null) return ImsEnumTag.of(o.adminStatus());
            if (o.systemStatus() != null) return ImsEnumTag.of(o.systemStatus());
            return new Span(o.label());
        }));
        statusFilter.setValue(new StatusFilterOption(I18n.t("ims.account.view.status.all"), null, null));
        statusFilter.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                currentAdminStatus  = e.getValue().adminStatus();
                currentSystemStatus = e.getValue().systemStatus();
                loadPageZero();
            }
        });
        return List.of(statusFilter);
    }

    @Override
    protected List<Component> buildToolbarActions() {
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("ims.account.view.refresh.tooltip"));
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

    @Override protected String getTitleKey()             { return "ims.account.view.title"; }
    @Override protected String getSubtitleKey()          { return "ims.account.view.subtitle"; }
    @Override protected String getCreateButtonKey()      { return "ims.account.view.create.button"; }
    @Override protected String getSearchPlaceholderKey() { return "ims.account.view.search.placeholder"; }
    @Override protected String getEmptyTitleKey()        { return "ims.account.view.empty.title"; }
    @Override protected String getEmptyDescriptionKey()  { return "ims.account.view.empty.description"; }
    @Override protected VaadinIcon getEmptyIcon()        { return VaadinIcon.USER; }
    @Override protected String getLoadErrorKey()         { return "ims.account.view.load.error"; }

    @Override
    public String getPageTitle() { return I18n.t("ims.account.view.title"); }

    public void loadPageZero() { loadPage(0); }

    public void openUpdateAccountDialog(Long accountId, Runnable onSuccess) {
        new UpdateAccountDialog(this, accountService, accountImageService,
                tenantService, accountId, onSuccess).open();
    }

    public void openResetPasswordDialog(Long accountId, String email) {
        new ResetPasswordDialog(this, accountService, accountId, email, () ->
                Notification.show(I18n.t("ims.account.dialog.reset.success", email),
                                3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS)).open();
    }

    private record StatusFilterOption(String label,
                                      IEnumEnabledBinaryStatus.Types adminStatus,
                                      IEnumAccountSystemStatus.Types systemStatus) {}
}