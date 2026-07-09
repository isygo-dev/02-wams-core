package eu.isygoit.ui.ims.views.parameters.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.AppParameterDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AppParameterService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.ims.views.parameters.ParameterManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class ParameterDetailsDialog extends NoActionDialog {

    private final ParameterManagementView parentView;
    private final AppParameterService parameterService;
    private final Long parameterId;

    public ParameterDetailsDialog(ParameterManagementView parentView,
                                  AppParameterService parameterService,
                                  Long parameterId) {
        super(I18n.t("ims.parameter.details.title"));
        this.parentView = parentView;
        this.parameterService = parameterService;
        this.parameterId = parameterId;

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("parameter-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<AppParameterDto> response = parameterService.findById(parameterId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("ims.parameter.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.parameter.details.load.error", extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.parameter.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(AppParameterDto param) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity — name/value (text identifiers)
        Div identityInfo = new Div();
        identityInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(identityInfo, VaadinIcon.KEY, I18n.t("ims.parameter.details.field.name"), param.getName());
        addFieldToGrid(identityInfo, VaadinIcon.INPUT, I18n.t("ims.parameter.details.field.value"), param.getValue());

        mainLayout.add(createSection(I18n.t("ims.parameter.details.section.identity"), identityInfo));

        // Contact / relations — tenant/description
        Div contactInfo = new Div();
        contactInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(contactInfo, VaadinIcon.BUILDING, I18n.t("ims.parameter.details.field.tenant"), param.getTenant());
        addFieldToGrid(contactInfo, VaadinIcon.FILE_TEXT, I18n.t("ims.parameter.details.field.description"), param.getDescription());

        mainLayout.add(createSection(I18n.t("ims.parameter.details.section.contact"), contactInfo));

        // Audit — created/updated by & date
        Div auditInfo = new Div();
        auditInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR, I18n.t("ims.parameter.details.field.created"), param.getCreateDate() != null ? DateHelper.formatToHumanReadable(param.getCreateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.USER_CHECK, I18n.t("ims.parameter.details.field.created.by"), param.getCreatedBy());
        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR_O, I18n.t("ims.parameter.details.field.updated"), param.getUpdateDate() != null ? DateHelper.formatToHumanReadable(param.getUpdateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.EDIT, I18n.t("ims.parameter.details.field.updated.by"), param.getUpdatedBy());

        mainLayout.add(createSection(I18n.t("ims.parameter.details.section.audit"), auditInfo));

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