package eu.isygoit.ui.ims.views.customer.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import org.springframework.http.ResponseEntity;

public class CustomerDetailsDialog extends NoActionDialog {

    private final CustomerManagementView parentView;
    private final CustomerService customerService;
    private final Long customerId;

    public CustomerDetailsDialog(CustomerManagementView parentView,
                                 CustomerService customerService,
                                 Long customerId) {
        super("Customer Details");
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
                add(new Span("Customer not found"));
                addCloseButton();
            }
        } catch (FeignException ex) {
            add(new Span("Failed to load details: " + extractErrorMessage(ex)));
            addCloseButton();
        } catch (Exception e) {
            add(new Span("Error: " + e.getMessage()));
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
        basicInfo.addClassName("details-grid");
        basicInfo.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-s)");

        addFieldToGrid(basicInfo, VaadinIcon.USER, "Name", customer.getName());
        addFieldToGrid(basicInfo, VaadinIcon.ENVELOPE, "Email", customer.getEmail());
        addFieldToGrid(basicInfo, VaadinIcon.PHONE, "Phone", customer.getPhoneNumber());
        addFieldToGrid(basicInfo, VaadinIcon.KEY, "Account code", customer.getAccountCode());
        addFieldToGrid(basicInfo, VaadinIcon.GLOBE, "Website", customer.getUrl());
        addFieldToGrid(basicInfo, VaadinIcon.SHIELD, "Status", customer.getAdminStatus() != null ? customer.getAdminStatus().name() : null);

        mainLayout.add(createSection("General Information", basicInfo));

        // Description
        if (customer.getDescription() != null && !customer.getDescription().isBlank()) {
            HorizontalLayout descRow = new HorizontalLayout();
            descRow.setAlignItems(FlexComponent.Alignment.START);
            descRow.setSpacing(true);
            descRow.setWidthFull();
            Icon descIcon = VaadinIcon.FILE_TEXT.create();
            descIcon.setSize("16px");
            descIcon.getStyle().set("color", "var(--lumo-primary-color)");
            Span descLabel = new Span("Description:");
            descLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            Span descValue = new Span(customer.getDescription());
            descValue.getStyle().set("flex", "1");
            descRow.add(descIcon, descLabel, descValue);
            descRow.expand(descValue);
            mainLayout.add(descRow);
        }

        // Address
        if (customer.getAddress() != null) {
            AddressDto addr = customer.getAddress();
            Div addressInfo = new Div();
            addressInfo.getStyle().set("display", "grid")
                    .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                    .set("gap", "var(--lumo-space-s)");

            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, "Country", addr.getCountry());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, "State", addr.getState());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, "City", addr.getCity());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, "Street", addr.getStreet());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, "Zip code", addr.getZipCode());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, "Additional info", addr.getAdditionalInfo());

            mainLayout.add(createSection("Address", addressInfo));
        }

        // Audit
        Div auditInfo = new Div();
        auditInfo.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-s)");

        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR, "Created", customer.getCreateDate() != null ? DateHelper.formatToHumanReadable(customer.getCreateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.USER_CHECK, "Created by", customer.getCreatedBy());
        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR_O, "Updated", customer.getUpdateDate() != null ? DateHelper.formatToHumanReadable(customer.getUpdateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.EDIT, "Updated by", customer.getUpdatedBy());

        mainLayout.add(createSection("Audit Information", auditInfo));

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
        iconComponent.getStyle().set("color", "var(--lumo-primary-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.getStyle().set("flex", "1");

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
        titleSpan.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-20pct)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("padding-bottom", "var(--lumo-space-xs)");
        section.add(titleSpan, content);
        return section;
    }

    private void addCloseButton() {
        Button closeButton = new Button("Close", e -> close());
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