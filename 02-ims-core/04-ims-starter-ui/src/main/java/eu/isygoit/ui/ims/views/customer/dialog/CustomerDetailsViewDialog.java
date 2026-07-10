package eu.isygoit.ui.ims.views.customer.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class CustomerDetailsViewDialog extends DetailsViewDialog {

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

        setWidth("800px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("customer-details-dialog");

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
            add(new Span(I18n.t("ims.customer.details.load.error", extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.customer.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(CustomerDto customer) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity — name/account code (text identifiers)
        Div identityInfo = new Div();
        identityInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(identityInfo, VaadinIcon.USER, I18n.t("ims.customer.details.field.name"), customer.getName());
        addFieldToGrid(identityInfo, VaadinIcon.KEY, I18n.t("ims.customer.details.field.account.code"), customer.getAccountCode(), true);

        mainLayout.add(createSection(I18n.t("ims.customer.details.section.identity"), identityInfo));

        // Classification & status
        Div classificationInfo = new Div();
        classificationInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(classificationInfo, VaadinIcon.SHIELD, I18n.t("ims.customer.details.field.status"), customer.getAdminStatus() != null ? customer.getAdminStatus().name() : null);

        mainLayout.add(createSection(I18n.t("ims.customer.details.section.classification"), classificationInfo));

        // Contact / relations — email/phone/website/tenant
        Div contactInfo = new Div();
        contactInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(contactInfo, VaadinIcon.ENVELOPE, I18n.t("ims.customer.details.field.email"), customer.getEmail(), true);
        addFieldToGrid(contactInfo, VaadinIcon.PHONE, I18n.t("ims.customer.details.field.phone"), customer.getPhoneNumber(), true);
        addFieldToGrid(contactInfo, VaadinIcon.GLOBE, I18n.t("ims.customer.details.field.website"), customer.getUrl(), true);
        addFieldToGrid(contactInfo, VaadinIcon.BUILDING, I18n.t("ims.customer.details.field.tenant"), customer.getTenant(), true);

        mainLayout.add(createSection(I18n.t("ims.customer.details.section.contact"), contactInfo));

        // Description
        if (customer.getDescription() != null && !customer.getDescription().isBlank()) {
            Div descGrid = new Div();
            descGrid.addClassName("wams-card__detail-grid");
            addFieldToGrid(descGrid, VaadinIcon.FILE_TEXT, I18n.t("ims.customer.details.field.description"), customer.getDescription(), false);
            mainLayout.add(descGrid);
        }

        // Address (part of contact / relations)
        if (customer.getAddress() != null) {
            AddressDto addr = customer.getAddress();
            Div addressInfo = new Div();
            addressInfo.addClassName("wams-card__detail-grid");

            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.country"), addr.getCountry());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.state"), addr.getState());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.city"), addr.getCity());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.street"), addr.getStreet());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.zip.code"), addr.getZipCode(), true);
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.additional.info"), addr.getAdditionalInfo());

            mainLayout.add(createSection(I18n.t("ims.customer.details.section.address"), addressInfo));
        }

        // Audit — created/updated by & date
        Div auditInfo = new Div();
        auditInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR, I18n.t("ims.customer.details.field.created"), customer.getCreateDate() != null ? DateHelper.formatToHumanReadable(customer.getCreateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.USER_CHECK, I18n.t("ims.customer.details.field.created.by"), customer.getCreatedBy());
        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR_O, I18n.t("ims.customer.details.field.updated"), customer.getUpdateDate() != null ? DateHelper.formatToHumanReadable(customer.getUpdateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.EDIT, I18n.t("ims.customer.details.field.updated.by"), customer.getUpdatedBy());

        mainLayout.add(createSection(I18n.t("ims.customer.details.section.audit"), auditInfo));

        add(mainLayout);
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}