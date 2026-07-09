package eu.isygoit.ui.ims.views.tenant.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import eu.isygoit.ui.ims.views.tenant.TenantManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class TenantDetailsViewDialog extends DetailsViewDialog {

    private final TenantManagementView parentView;
    private final TenantService tenantService;
    private final Long tenantId;

    public TenantDetailsViewDialog(TenantManagementView parentView,
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
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.tenant.details.load.error", extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.tenant.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(TenantDto tenant) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity — name/code (text identifiers)
        Div identityInfo = new Div();
        identityInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(identityInfo, VaadinIcon.BUILDING, I18n.t("ims.tenant.details.field.name"), tenant.getName());
        addFieldToGrid(identityInfo, VaadinIcon.CODE, I18n.t("ims.tenant.details.field.code"), tenant.getCode(), true);

        mainLayout.add(createSection(I18n.t("ims.tenant.details.section.identity"), identityInfo));

        // Classification & status — admin status/industry
        Div classificationInfo = new Div();
        classificationInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(classificationInfo, VaadinIcon.SHIELD, I18n.t("ims.tenant.details.field.admin.status"), tenant.getAdminStatus() != null ? tenant.getAdminStatus().name() : null);
        addFieldToGrid(classificationInfo, VaadinIcon.INSTITUTION, I18n.t("ims.tenant.details.field.industry"), tenant.getIndustry());

        mainLayout.add(createSection(I18n.t("ims.tenant.details.section.classification"), classificationInfo));

        // Contact / relations — email/phone/website
        Div contactInfo = new Div();
        contactInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(contactInfo, VaadinIcon.ENVELOPE, I18n.t("ims.tenant.details.field.email"), tenant.getEmail(), true);
        addFieldToGrid(contactInfo, VaadinIcon.PHONE, I18n.t("ims.tenant.details.field.phone"), tenant.getPhone(), true);
        addFieldToGrid(contactInfo, VaadinIcon.GLOBE, I18n.t("ims.tenant.details.field.website"), tenant.getUrl(), true);

        mainLayout.add(createSection(I18n.t("ims.tenant.details.section.contact"), contactInfo));

        // Description (full width)
        if (tenant.getDescription() != null && !tenant.getDescription().isBlank()) {
            Div descGrid = new Div();
            descGrid.addClassName("wams-card__detail-grid");
            addFieldToGrid(descGrid, VaadinIcon.FILE_TEXT, I18n.t("ims.tenant.details.field.description"), tenant.getDescription(), false);
            mainLayout.add(descGrid);
        }

        // Social links (part of contact / relations, if present)
        if (hasAnySocialLink(tenant)) {
            Div socialInfo = new Div();
            socialInfo.addClassName("wams-card__detail-grid");

            addFieldToGrid(socialInfo, VaadinIcon.LINK, I18n.t("ims.tenant.details.field.facebook"), tenant.getLnk_facebook(), true);
            addFieldToGrid(socialInfo, VaadinIcon.LINK, I18n.t("ims.tenant.details.field.linkedin"), tenant.getLnk_linkedin(), true);
            addFieldToGrid(socialInfo, VaadinIcon.LINK, I18n.t("ims.tenant.details.field.xing"), tenant.getLnk_xing(), true);

            mainLayout.add(createSection(I18n.t("ims.tenant.details.section.social"), socialInfo));
        }

        // Address (part of contact / relations, if present)
        if (tenant.getAddress() != null) {
            Div addressInfo = new Div();
            addressInfo.addClassName("wams-card__detail-grid");

            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.country"), tenant.getAddress().getCountry());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.state"), tenant.getAddress().getState());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.city"), tenant.getAddress().getCity());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.street"), tenant.getAddress().getStreet());
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.zip.code"), tenant.getAddress().getZipCode(), true);
            addFieldToGrid(addressInfo, VaadinIcon.MAP_MARKER, I18n.t("ims.tenant.details.field.additional.info"), tenant.getAddress().getAdditionalInfo());

            mainLayout.add(createSection(I18n.t("ims.tenant.details.section.address"), addressInfo));
        }

        // Audit — created/updated by & date
        Div auditInfo = new Div();
        auditInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR, I18n.t("ims.tenant.details.field.created"), tenant.getCreateDate() != null ? DateHelper.formatToHumanReadable(tenant.getCreateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.USER_CHECK, I18n.t("ims.tenant.details.field.created.by"), tenant.getCreatedBy());
        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR_O, I18n.t("ims.tenant.details.field.updated"), tenant.getUpdateDate() != null ? DateHelper.formatToHumanReadable(tenant.getUpdateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.EDIT, I18n.t("ims.tenant.details.field.updated.by"), tenant.getUpdatedBy());

        mainLayout.add(createSection(I18n.t("ims.tenant.details.section.audit"), auditInfo));

        add(mainLayout);
    }

    private boolean hasAnySocialLink(TenantDto tenant) {
        return (tenant.getLnk_facebook() != null && !tenant.getLnk_facebook().isBlank())
                || (tenant.getLnk_linkedin() != null && !tenant.getLnk_linkedin().isBlank())
                || (tenant.getLnk_xing() != null && !tenant.getLnk_xing().isBlank());
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