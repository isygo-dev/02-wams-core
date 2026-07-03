package eu.isygoit.ui.ims.views.application.dialog;

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
                addCloseButton();
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.app.details.load.error", extractErrorMessage(ex))));
            addCloseButton();
        } catch (Exception e) {
            add(new Span(I18n.t("ims.app.details.load.error", e.getMessage())));
            addCloseButton();
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(ApplicationDto app) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div basicInfo = new Div();
        basicInfo.addClassName("details-grid");
        basicInfo.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-s)");

        addFieldToGrid(basicInfo, VaadinIcon.PLAY, I18n.t("ims.app.details.field.name"), app.getName());
        addFieldToGrid(basicInfo, VaadinIcon.FUNCTION, I18n.t("ims.app.details.field.title"), app.getTitle());
        addFieldToGrid(basicInfo, VaadinIcon.CODE, I18n.t("ims.app.details.field.code"), app.getCode());
        addFieldToGrid(basicInfo, VaadinIcon.DESKTOP, I18n.t("ims.app.details.field.category"), app.getCategory());
        addFieldToGrid(basicInfo, VaadinIcon.GLOBE, I18n.t("ims.app.details.field.url"), app.getUrl());
        addFieldToGrid(basicInfo, VaadinIcon.SORT, I18n.t("ims.app.details.field.order"), app.getOrder() != null ? String.valueOf(app.getOrder()) : null);

        mainLayout.add(createSection(I18n.t("ims.app.details.section.general"), basicInfo));

        if (app.getDescription() != null && !app.getDescription().isBlank()) {
            HorizontalLayout descRow = new HorizontalLayout();
            descRow.setAlignItems(FlexComponent.Alignment.START);
            descRow.setSpacing(true);
            descRow.setWidthFull();
            Icon descIcon = VaadinIcon.FILE_TEXT.create();
            descIcon.setSize("16px");
            descIcon.getStyle().set("color", "var(--lumo-primary-color)");
            Span descLabel = new Span(I18n.t("ims.app.details.field.description"));
            descLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            Span descValue = new Span(app.getDescription());
            descValue.getStyle().set("flex", "1");
            descRow.add(descIcon, descLabel, descValue);
            descRow.expand(descValue);
            mainLayout.add(descRow);
        }

        Div statusInfo = new Div();
        statusInfo.addClassName("details-grid");
        statusInfo.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-s)");

        addFieldToGrid(statusInfo, VaadinIcon.SHIELD, I18n.t("ims.app.details.field.admin.status"), app.getAdminStatus() != null ? app.getAdminStatus().name() : null);
        addFieldToGrid(statusInfo, VaadinIcon.CALENDAR, I18n.t("ims.app.details.field.created"), app.getCreateDate() != null ? DateHelper.formatToHumanReadable(app.getCreateDate()) : null);
        addFieldToGrid(statusInfo, VaadinIcon.USER_CHECK, I18n.t("ims.app.details.field.created.by"), app.getCreatedBy());
        addFieldToGrid(statusInfo, VaadinIcon.CALENDAR_O, I18n.t("ims.app.details.field.updated"), app.getUpdateDate() != null ? DateHelper.formatToHumanReadable(app.getUpdateDate()) : null);
        addFieldToGrid(statusInfo, VaadinIcon.EDIT, I18n.t("ims.app.details.field.updated.by"), app.getUpdatedBy());

        mainLayout.add(createSection(I18n.t("ims.app.details.section.status"), statusInfo));

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
        Button closeButton = new Button(I18n.t("ims.app.details.close"), e -> close());
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