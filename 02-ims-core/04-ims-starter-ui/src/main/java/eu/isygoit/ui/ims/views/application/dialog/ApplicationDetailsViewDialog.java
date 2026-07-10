package eu.isygoit.ui.ims.views.application.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import eu.isygoit.ui.ims.views.application.ApplicationManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class ApplicationDetailsViewDialog extends DetailsViewDialog {

    private final ApplicationManagementView parentView;
    private final ApplicationService applicationService;
    private final Long applicationId;

    public ApplicationDetailsViewDialog(ApplicationManagementView parentView,
                                        ApplicationService applicationService,
                                        Long applicationId) {
        super(I18n.t("ims.app.details.title"));
        this.parentView = parentView;
        this.applicationService = applicationService;
        this.applicationId = applicationId;

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("application-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<ApplicationDto> response = applicationService.findById(applicationId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("ims.app.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.app.details.load.error", extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.app.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(ApplicationDto app) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity — name/title/code (text identifiers)
        Div identityInfo = new Div();
        identityInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(identityInfo, VaadinIcon.PLAY, I18n.t("ims.app.details.field.name"), app.getName());
        addFieldToGrid(identityInfo, VaadinIcon.FUNCTION, I18n.t("ims.app.details.field.title"), app.getTitle());
        addFieldToGrid(identityInfo, VaadinIcon.CODE, I18n.t("ims.app.details.field.code"), app.getCode(), true);

        mainLayout.add(createSection(I18n.t("ims.app.details.section.identity"), identityInfo));

        // Classification & status — category/order/admin status
        Div classificationInfo = new Div();
        classificationInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(classificationInfo, VaadinIcon.DESKTOP, I18n.t("ims.app.details.field.category"), app.getCategory());
        addFieldToGrid(classificationInfo, VaadinIcon.SORT, I18n.t("ims.app.details.field.order"), app.getOrder() != null ? String.valueOf(app.getOrder()) : null);
        addFieldToGrid(classificationInfo, VaadinIcon.SHIELD, I18n.t("ims.app.details.field.admin.status"), app.getAdminStatus() != null ? app.getAdminStatus().name() : null);

        mainLayout.add(createSection(I18n.t("ims.app.details.section.classification"), classificationInfo));

        // Contact / relations — tenant/url/description
        Div contactInfo = new Div();
        contactInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(contactInfo, VaadinIcon.BUILDING, I18n.t("ims.app.details.field.tenant"), app.getTenant(), true);
        addFieldToGrid(contactInfo, VaadinIcon.GLOBE, I18n.t("ims.app.details.field.url"), app.getUrl(), true);

        mainLayout.add(createSection(I18n.t("ims.app.details.section.contact"), contactInfo));

        if (app.getDescription() != null && !app.getDescription().isBlank()) {
            Div descGrid = new Div();
            descGrid.addClassName("wams-card__detail-grid");
            addFieldToGrid(descGrid, VaadinIcon.FILE_TEXT, I18n.t("ims.app.details.field.description"), app.getDescription(), false);
            mainLayout.add(descGrid);
        }

        // Audit — created/updated by & date
        Div auditInfo = new Div();
        auditInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR, I18n.t("ims.app.details.field.created"), app.getCreateDate() != null ? DateHelper.formatToHumanReadable(app.getCreateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.USER_CHECK, I18n.t("ims.app.details.field.created.by"), app.getCreatedBy());
        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR_O, I18n.t("ims.app.details.field.updated"), app.getUpdateDate() != null ? DateHelper.formatToHumanReadable(app.getUpdateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.EDIT, I18n.t("ims.app.details.field.updated.by"), app.getUpdatedBy());

        mainLayout.add(createSection(I18n.t("ims.app.details.section.audit"), auditInfo));

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