package eu.isygoit.ui.ims.views.customer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.remote.ims.CustomerImageService;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import eu.isygoit.ui.ims.views.customer.dialog.CreateCustomerDialog;
import eu.isygoit.ui.ims.views.customer.dialog.UpdateCustomerDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@VaadinSessionScope
@Route(value = "ims/customers", layout = ImsMainLayout.class)
//@PageTitle("Customer Management")
@PermitAll
public class CustomerManagementView extends AbstractPaginatedView<CustomerDto> {

    private final CustomerService customerService;
    private final CustomerImageService customerImageService;
    private final AccountService accountService;

    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final ComboBox<IEnumEnabledBinaryStatus.Types> statusFilter = new ComboBox<>();

    private IEnumEnabledBinaryStatus.Types currentAdminStatus;

    @Autowired
    public CustomerManagementView(CustomerService customerService,
                                  CustomerImageService customerImageService,
                                  AccountService accountService) {
        this.customerService = customerService;
        this.customerImageService = customerImageService;
        this.accountService = accountService;
        initPaginatedView();
    }

    @Override
    protected PaginatedResponseDto<CustomerDto> fetchPage(int page, int size) {
        var r = customerService.findAll(page, size);
        return r != null ? r.getBody() : null;
    }

    @Override
    protected Component renderItem(CustomerDto item) {
        return new CustomerCard(this, customerService, customerImageService,
                accountService, item, this::loadPageZero);
    }

    @Override
    protected boolean matchesSearch(CustomerDto c, String search) {
        if (search == null || search.isBlank()) return true;
        String s = search.toLowerCase();
        return (c.getName() != null && c.getName().toLowerCase().contains(s))
                || (c.getEmail() != null && c.getEmail().toLowerCase().contains(s));
    }

    @Override
    protected boolean matchesAdditionalFilters(CustomerDto c) {
        return currentAdminStatus == null || c.getAdminStatus() == currentAdminStatus;
    }

    @Override
    protected void openCreateDialog() {
        new CreateCustomerDialog(this, customerService, customerImageService,
                this::loadPageZero).open();
    }

    @Override
    protected void onSearchChanged() {
        loadPageZero();
    }

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"customer-management-view"};
    }

    @Override
    protected List<Component> buildToolbarFilters() {
        statusFilter.setItems(IEnumEnabledBinaryStatus.Types.values());
        statusFilter.setItemLabelGenerator(s -> ImsEnumTag.label(s, null));
        statusFilter.setRenderer(ImsEnumTag.renderer(null));
        statusFilter.setPlaceholder(I18n.t("ims.customer.view.status.placeholder"));
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
        refreshButton.setTooltipText(I18n.t("ims.customer.view.refresh.tooltip"));
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
        return "ims.customer.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "ims.customer.view.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "ims.customer.view.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "ims.customer.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "ims.customer.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "ims.customer.view.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.USER_HEART;
    }

    @Override
    protected String getLoadErrorKey() {
        return "ims.customer.view.load.error";
    }

    @Override
    public String getPageTitle() {
        return I18n.t("ims.customer.view.title");
    }

    public void loadPageZero() {
        loadPage(0);
    }

    public void openUpdateCustomerDialog(CustomerDto c, Runnable onSuccess) {
        new UpdateCustomerDialog(this, customerService, customerImageService, c, onSuccess).open();
    }
}