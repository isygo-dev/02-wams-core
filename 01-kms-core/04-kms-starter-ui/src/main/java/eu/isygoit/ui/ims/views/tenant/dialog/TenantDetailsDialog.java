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
import eu.isygoit.i18n.I18n;
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
        super(I18n.t("ims.tenant.details.title"));
        this.parentView = parentView;
        this.tenantService = tenantService;
        this.tenantId = tenantId;

        setWidth("700px");
        setMaxWidth("95%");
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
                add(new Span(I18n.t("ims.tenant.details.not.found")));
                addCloseButton();
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.tenant.details.load.error", extractErrorMessage(ex))));
            addCloseButton();
        } catch (Exception e) {
            add(new Span(I18n.t("ims.tenant.details.load.error", e.getMessage())));
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
        basicInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(basicInfo, VaadinIcon.BUILDING, I18n.t("ims.tenant.details.field.name"), tenant.getName());
        addFieldToGrid(basicInfo, VaadinIcon.CODE, I18n.t("ims.tenant.details.field.code"), tenant.getCode());
        addFieldToGrid(basicInfo, VaadinIcon.ENVELOPE, I18n.t("ims.tenant.details.field.email"), tenant.getEmail());
        addFieldToGrid(basicInfo, VaadinIcon.PHONE, I18n.t("ims.tenant.details.field.phone"), tenant.getPhone());
        addFieldToGrid(basicInfo, VaadinIcon.INSTITUTION, I18n.t("ims.tenant.details.field.industry"), tenant.getIndustry());
        addFieldToGrid(basicInfo, VaadinIcon.GLOBE, I18n.t("ims.tenant.details.field.website"), tenant.getUrl());

        mainLayout.add(createSection(I18n.t("ims.tenant.details.section.general"), basicInfo));

        // Description (full width)
        if (tenant.getDescription() != null && !tenant.getDescription().isBlank()) {
            HorizontalLayout descRow = new HorizontalLayout();
            descRow.setAlignItems(FlexComponent.Alignment.START);
            descRow.setSpacing(true);
            descRow.setWidthFull();
            Icon descIcon = VaadinIcon.FILE_TEXT.create();
            descIcon.setSize("16px");
            descIcon.addClassName("detail-field-icon");
            Span descLabel = new Span(I18n.t("ims.tenant.details.field.description"));
            descLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            Span descValue = new Span(tenant.getDescription());
            descValue.addClassName("detail-field-value");
            descRow.add(descIcon, descLabel, descValue);
            descRow.expand(descValue);
            mainLayout.add(descRow);
        }

        // Status & audit section
        Div statusInfo = new Div();
        statusInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(statusInfo, VaadinIcon.SHIELD, I18n.t("ims.tenant.details.field.admin.status"), tenant.getAdminStatus() != null ? tenant.getAdminStatus().name() : null);
        addFieldToGrid(statusInfo, VaadinIcon.CALENDAR, I18n.t("ims.tenant.details.field.created"), tenant.getCreateDate() != null ? DateHelper.formatToHumanReadable(tenant.getCreateDate()) : null);
        addFieldToGrid(statusInfo, VaadinIcon.USER_CHECK, I18n.t("ims.tenant.details.field.created.by"), tenant.getCreatedBy());
        addFieldToGrid(statusInfo, VaadinIcon.CALENDAR_O, I18n.t("ims.tenant.details.field.updated"), tenant.getUpdateDate() != null ? DateHelper.formatToHumanReadable(tenant.getUpdateDate()) : null);
        addFieldToGrid(statusInfo, VaadinIcon.EDIT, I18n.t("ims.tenant.details.field.updated.by"), tenant.getUpdatedBy());

        mainLayout.add(createSection(I18n.t("ims.tenant.details.section.status"), statusInfo));

        // Social links (if present)
        if (hasAnySocialLink(tenant)) {
            Div socialInfo = new Div();
            socialInfo.addClassName("wams-card__detail-grid");

            addFieldToGrid(socialInfo, VaadinIcon.LINK, I18n.t("ims.tenant.details.field.facebook"), tenant.getLnk_facebook());
            addFieldToGrid(socialInfo, VaadinIcon.LINK, I18n.t("ims.tenant.details.field.linkedin"), tenant.getLnk_linkedin());
            addFieldToGrid(socialInfo, VaadinIcon.LINK, I18n.t("ims.tenant.details.field.xing"), tenant.getLnk_xing());

            mainLayout.add(createSection(I18n.t("ims.tenant.details.section.social"), socialInfo));
        }

        // Address (if present)
        if (tenant.getAddress() != null) {
            Div addressInfo = new Div();
            addressInfo.addClassName("wams-card__detail-grid");

            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.country"), tenant.getAddress().getCountry());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.state"), tenant.getAddress().getState());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.city"), tenant.getAddress().getCity());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.street"), tenant.getAddress().getStreet());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.zip.code"), tenant.getAddress().getZipCode());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.additional.info"), tenant.getAddress().getAdditionalInfo());

            mainLayout.add(createSection(I18n.t("ims.tenant.details.section.address"), addressInfo));
        }

        add(mainLayout);
        addCloseButton();
    }

    private boolean hasAnySocialLink(TenantDto tenant) {
        return (tenant.getLnk_facebook() != null && !tenant.getLnk_facebook().isBlank())
                || (tenant.getLnk_linkedin() != null && !tenant.getLnk_linkedin().isBlank())
                || (tenant.getLnk_xing() != null && !tenant.getLnk_xing().isBlank());
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
        Button closeButton = new Button(I18n.t("ims.tenant.details.close"), e -> close());
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