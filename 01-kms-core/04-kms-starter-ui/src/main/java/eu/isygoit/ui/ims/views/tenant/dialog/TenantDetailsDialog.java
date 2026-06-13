package eu.isygoit.ui.ims.views.tenant.dialog;

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
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.ims.views.tenant.TenantManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class TenantDetailsDialog extends NoActionDialog {

    private final TenantManagementView parentView;
    private final TenantService tenantService;
    private final Long tenantId;

    public TenantDetailsDialog(TenantManagementView parentView,
                               TenantService tenantService,
                               Long tenantId) {
        super("Tenant Details");
        this.parentView = parentView;
        this.tenantService = tenantService;
        this.tenantId = tenantId;

        setWidth("700px");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("tenant-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<TenantDto> response = tenantService.findById(tenantId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span("Tenant not found"));
                addCloseButton(); // also add close button on error
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

    private void buildContent(TenantDto tenant) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Basic information - two-column grid
        Div basicInfo = new Div();
        basicInfo.addClassName("details-grid");
        basicInfo.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-s)");

        addFieldToGrid(basicInfo, VaadinIcon.BUILDING, "Name", tenant.getName());
        addFieldToGrid(basicInfo, VaadinIcon.CODE, "Code", tenant.getCode());
        addFieldToGrid(basicInfo, VaadinIcon.ENVELOPE, "Email", tenant.getEmail());
        addFieldToGrid(basicInfo, VaadinIcon.PHONE, "Phone", tenant.getPhone());
        addFieldToGrid(basicInfo, VaadinIcon.INSTITUTION, "Industry", tenant.getIndustry());
        addFieldToGrid(basicInfo, VaadinIcon.GLOBE, "Website", tenant.getUrl());

        mainLayout.add(createSection("General Information", basicInfo));

        // Description (full width)
        if (tenant.getDescription() != null && !tenant.getDescription().isBlank()) {
            HorizontalLayout descRow = new HorizontalLayout();
            descRow.setAlignItems(FlexComponent.Alignment.START);
            descRow.setSpacing(true);
            descRow.setWidthFull();
            Icon descIcon = VaadinIcon.FILE_TEXT.create();
            descIcon.setSize("16px");
            descIcon.getStyle().set("color", "var(--lumo-primary-color)");
            Span descLabel = new Span("Description:");
            descLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            Span descValue = new Span(tenant.getDescription());
            descValue.getStyle().set("flex", "1");
            descRow.add(descIcon, descLabel, descValue);
            descRow.expand(descValue);
            mainLayout.add(descRow);
        }

        // Status & audit section
        Div statusInfo = new Div();
        statusInfo.addClassName("details-grid");
        statusInfo.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-s)");

        addFieldToGrid(statusInfo, VaadinIcon.SHIELD, "Admin status", tenant.getAdminStatus() != null ? tenant.getAdminStatus().name() : null);
        addFieldToGrid(statusInfo, VaadinIcon.CALENDAR, "Created", tenant.getCreateDate() != null ? DateHelper.formatToHumanReadable(tenant.getCreateDate()) : null);
        addFieldToGrid(statusInfo, VaadinIcon.USER_CHECK, "Created by", tenant.getCreatedBy());
        addFieldToGrid(statusInfo, VaadinIcon.CALENDAR_O, "Updated", tenant.getUpdateDate() != null ? DateHelper.formatToHumanReadable(tenant.getUpdateDate()) : null);
        addFieldToGrid(statusInfo, VaadinIcon.EDIT, "Updated by", tenant.getUpdatedBy());

        mainLayout.add(createSection("Status & Audit", statusInfo));

        // Address (if present)
        if (tenant.getAddress() != null &&
                (tenant.getAddress().getStreet() != null || tenant.getAddress().getCity() != null)) {
            String address = (tenant.getAddress().getStreet() != null ? tenant.getAddress().getStreet() : "") +
                    (tenant.getAddress().getCity() != null ? ", " + tenant.getAddress().getCity() : "") +
                    (tenant.getAddress().getCountry() != null ? ", " + tenant.getAddress().getCountry() : "");
            if (!address.isBlank()) {
                HorizontalLayout addrRow = new HorizontalLayout();
                addrRow.setAlignItems(FlexComponent.Alignment.CENTER);
                addrRow.setSpacing(true);
                Icon addrIcon = VaadinIcon.MAP_MARKER.create();
                addrIcon.setSize("16px");
                addrIcon.getStyle().set("color", "var(--lumo-primary-color)");
                Span addrLabel = new Span("Address:");
                addrLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
                Span addrValue = new Span(address);
                addrRow.add(addrIcon, addrLabel, addrValue);
                addrRow.expand(addrValue);
                mainLayout.add(addrRow);
            }
        }

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