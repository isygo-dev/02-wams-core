package eu.isygoit.ui.ims.views.customer.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.dialog.DetailHero;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.common.ImsDetailsDialog;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Read-only view of a {@link CustomerDto}: every DTO field except {@code id}
 * (never displayed) is shown, grouped in tabs (identity, contact, address, audit).
 */
public class CustomerDetailsViewDialog extends ImsDetailsDialog {

    private final CustomerManagementView parentView;
    private final CustomerService customerService;
    private final Long customerId;

    public CustomerDetailsViewDialog(CustomerManagementView parentView,
                                     CustomerService customerService,
                                     Long customerId) {
        super(I18n.t("ims.customer.details.title"));
        this.parentView = parentView;
        this.customerService = customerService;
        this.customerId = customerId;

        applyWidth(DialogLayout.WIDTH_L);
        setModal(true);
        setDraggable(true);

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<CustomerDto> response = customerService.findById(customerId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("ims.customer.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.customer.details.load.error",
                    CustomerDialogSupport.extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.customer.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(CustomerDto customer) {
        addIdentityTab(customer);
        addContactTab(customer);
        if (customer.getAddress() != null) {
            addAddressTab(customer.getAddress());
        }
        addAuditTab(customer.getCreatedBy(), formatDate(customer.getCreateDate()),
                customer.getUpdatedBy(), formatDate(customer.getUpdateDate()));
    }

    private void addIdentityTab(CustomerDto customer) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.USER, I18n.t("ims.customer.details.field.name"),
                dash(customer.getName()));
        addFieldToGrid(grid, VaadinIcon.KEY, I18n.t("ims.customer.details.field.account.code"),
                dash(customer.getAccountCode()), true);
        ImsEnumTag.addDetailField(grid, VaadinIcon.SHIELD, I18n.t("ims.customer.details.field.status"),
                customer.getAdminStatus(), null);
        addFieldToGrid(grid, VaadinIcon.FILE_TEXT, I18n.t("ims.customer.details.field.description"),
                dash(customer.getDescription()));

        VerticalLayout identity = DialogLayout.stack();
        identity.add(buildHero(customer),
                createSection(I18n.t("ims.customer.details.section.identity"), grid));
        addTab(I18n.t("ims.customer.details.section.identity"), identity);
    }

    private void addContactTab(CustomerDto customer) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.ENVELOPE, I18n.t("ims.customer.details.field.email"),
                dash(customer.getEmail()), true);
        addFieldToGrid(grid, VaadinIcon.PHONE, I18n.t("ims.customer.details.field.phone"),
                dash(customer.getPhoneNumber()), true);
        addFieldToGrid(grid, VaadinIcon.GLOBE, I18n.t("ims.customer.details.field.website"),
                dash(customer.getUrl()), true);
        addFieldToGrid(grid, VaadinIcon.BUILDING, I18n.t("ims.customer.details.field.tenant"),
                dash(customer.getTenant()), true);
        addTab(I18n.t("ims.customer.details.section.contact"),
                createSection(I18n.t("ims.customer.details.section.contact"), grid));
    }

    private void addAddressTab(AddressDto addr) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.country"), dash(addr.getCountry()));
        addFieldToGrid(grid, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.state"), dash(addr.getState()));
        addFieldToGrid(grid, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.city"), dash(addr.getCity()));
        addFieldToGrid(grid, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.street"), dash(addr.getStreet()));
        addFieldToGrid(grid, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.zip.code"),
                dash(addr.getZipCode()), true);
        addFieldToGrid(grid, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.additional.info"),
                dash(addr.getAdditionalInfo()));
        addTab(I18n.t("ims.customer.details.section.address"),
                createSection(I18n.t("ims.customer.details.section.address"), grid));
    }

    private Component buildHero(CustomerDto customer) {
        Component[] chips = customer.getAdminStatus() == null
                ? new Component[0]
                : new Component[]{ImsEnumTag.of(customer.getAdminStatus())};
        return new DetailHero(null, dash(customer.getName()), customer.getEmail(), chips);
    }

    private static String formatDate(java.time.LocalDateTime date) {
        return date == null ? null : DateHelper.formatToHumanReadable(date);
    }
}
