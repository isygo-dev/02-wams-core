package eu.isygoit.ui.ims.views.customer.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class CustomerDetailsDialog extends NoActionDialog {

    private final CustomerManagementView parentView;
    private final CustomerService customerService;
    private final Long customerId;

    public CustomerDetailsDialog(CustomerManagementView parentView,
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
                addCloseButton();
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.customer.details.load.error", extractErrorMessage(ex))));
            addCloseButton();
        } catch (Exception e) {
            add(new Span(I18n.t("ims.customer.details.load.error", e.getMessage())));
            addCloseButton();
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(CustomerDto customer) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Basic info
        Div basicInfo = new Div();
        basicInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(basicInfo, VaadinIcon.USER, I18n.t("ims.customer.details.field.name"), customer.getName());
        addFieldToGrid(basicInfo, VaadinIcon.ENVELOPE, I18n.t("ims.customer.details.field.email"), customer.getEmail());
        addFieldToGrid(basicInfo, VaadinIcon.PHONE, I18n.t("ims.customer.details.field.phone"), customer.getPhoneNumber());
        addFieldToGrid(basicInfo, VaadinIcon.KEY, I18n.t("ims.customer.details.field.account.code"), customer.getAccountCode());
        addFieldToGrid(basicInfo, VaadinIcon.GLOBE, I18n.t("ims.customer.details.field.website"), customer.getUrl());
        addFieldToGrid(basicInfo, VaadinIcon.BUILDING, I18n.t("ims.customer.details.field.tenant"), customer.getTenant());
        addFieldToGrid(basicInfo, VaadinIcon.SHIELD, I18n.t("ims.customer.details.field.status"), customer.getAdminStatus() != null ? customer.getAdminStatus().name() : null);

        mainLayout.add(createSection(I18n.t("ims.customer.details.section.general"), basicInfo));

        // Description
        if (customer.getDescription() != null && !customer.getDescription().isBlank()) {
            HorizontalLayout descRow = new HorizontalLayout();
            descRow.setAlignItems(FlexComponent.Alignment.START);
            descRow.setSpacing(true);
            descRow.setWidthFull();
            Icon descIcon = VaadinIcon.FILE_TEXT.create();
            descIcon.setSize("16px");
            descIcon.addClassName("detail-field-icon");
            Span descLabel = new Span(I18n.t("ims.customer.details.field.description"));
            descLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            Span descValue = new Span(customer.getDescription());
            descValue.addClassName("detail-field-value");
            descRow.add(descIcon, descLabel, descValue);
            descRow.expand(descValue);
            mainLayout.add(descRow);
        }

        // Address
        if (customer.getAddress() != null) {
            AddressDto addr = customer.getAddress();
            Div addressInfo = new Div();
            addressInfo.addClassName("wams-card__detail-grid");

            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.country"), addr.getCountry());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.state"), addr.getState());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.city"), addr.getCity());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.street"), addr.getStreet());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.zip.code"), addr.getZipCode());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.customer.details.field.additional.info"), addr.getAdditionalInfo());

            mainLayout.add(createSection(I18n.t("ims.customer.details.section.address"), addressInfo));
        }

        // Audit
        Div auditInfo = new Div();
        auditInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR, I18n.t("ims.customer.details.field.created"), customer.getCreateDate() != null ? DateHelper.formatToHumanReadable(customer.getCreateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.USER_CHECK, I18n.t("ims.customer.details.field.created.by"), customer.getCreatedBy());
        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR_O, I18n.t("ims.customer.details.field.updated"), customer.getUpdateDate() != null ? DateHelper.formatToHumanReadable(customer.getUpdateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.EDIT, I18n.t("ims.customer.details.field.updated.by"), customer.getUpdatedBy());

        mainLayout.add(createSection(I18n.t("ims.customer.details.section.audit"), auditInfo));

        add(mainLayout);
        addCloseButton();
    }

    private void addFieldToGrid(Div container, VaadinIcon icon, String label, String value) {
        if (value == null || value.isBlank()) return;
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("detail-field");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.addClassName("detail-field-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.addClassName("detail-field-value");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        container.add(row);
    }

    private Component createSection(String title, Component content) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.addClassName("wams-section-title");
        section.add(titleSpan, content);
        return section;
    }

    private void addCloseButton() {
        Button closeButton = new Button(I18n.t("ims.customer.details.close"), e -> close());
        closeButton.addClassName(LumoUtility.Margin.Top.MEDIUM);
        HorizontalLayout buttonBar = new HorizontalLayout(closeButton);
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.setWidthFull();
        add(buttonBar);
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