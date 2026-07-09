package eu.isygoit.ui.ims.views.application.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.ims.views.application.ApplicationManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class ApplicationDetailsDialog extends NoActionDialog {

    private final ApplicationManagementView parentView;
    private final ApplicationService applicationService;
    private final Long applicationId;

    public ApplicationDetailsDialog(ApplicationManagementView parentView,
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
        addFieldToGrid(identityInfo, VaadinIcon.CODE, I18n.t("ims.app.details.field.code"), app.getCode());

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

        addFieldToGrid(contactInfo, VaadinIcon.BUILDING, I18n.t("ims.app.details.field.tenant"), app.getTenant());
        addFieldToGrid(contactInfo, VaadinIcon.GLOBE, I18n.t("ims.app.details.field.url"), app.getUrl());

        mainLayout.add(createSection(I18n.t("ims.app.details.section.contact"), contactInfo));

        if (app.getDescription() != null && !app.getDescription().isBlank()) {
            Div descGrid = new Div();
            descGrid.addClassName("wams-card__detail-grid");
            addFieldToGrid(descGrid, VaadinIcon.FILE_TEXT, I18n.t("ims.app.details.field.description"), app.getDescription());
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

    private void addFieldToGrid(Div container, VaadinIcon icon, String label, String value) {
        if (value == null || value.isBlank()) return;

        VerticalLayout field = new VerticalLayout();
        field.setPadding(false);
        field.setSpacing(false);
        field.addClassName("wams-card__detail-field");

        HorizontalLayout labelRow = new HorizontalLayout();
        labelRow.setAlignItems(FlexComponent.Alignment.CENTER);
        labelRow.setSpacing(false);
        labelRow.addClassName("wams-card__detail-field-label-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("12px");
        iconComponent.addClassName("detail-field-icon");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("wams-card__detail-field-label");

        labelRow.add(iconComponent, labelSpan);

        Span valueSpan = new Span(value);
        valueSpan.addClassName("wams-card__detail-field-value");

        field.add(labelRow, valueSpan);
        container.add(field);
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

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}