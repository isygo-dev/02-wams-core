package eu.isygoit.ui.sms.views.storageconfig.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.sms.views.storageconfig.StorageConfigManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class StorageConfigDetailsDialog extends NoActionDialog {

    private final StorageConfigManagementView parentView;
    private final StorageConfigService storageConfigService;
    private final Long configId;

    public StorageConfigDetailsDialog(StorageConfigManagementView parentView,
                                      StorageConfigService storageConfigService,
                                      Long configId) {
        super(I18n.t("sms.storageconfig.details.title"));
        this.parentView = parentView;
        this.storageConfigService = storageConfigService;
        this.configId = configId;

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("storageconfig-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<StorageConfigDto> response = storageConfigService.findById(configId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("sms.storageconfig.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("sms.storageconfig.details.load.error", extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("sms.storageconfig.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(StorageConfigDto config) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div identityGrid = new Div();
        identityGrid.addClassName("details-grid");
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("sms.storageconfig.details.field.id"), String.valueOf(config.getId()));
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("sms.storageconfig.details.field.tenant"), config.getTenant());
        addFieldToGrid(identityGrid, VaadinIcon.COG, I18n.t("sms.storageconfig.details.field.type"), config.getType() != null ? config.getType().name() : null);
        addFieldToGrid(identityGrid, VaadinIcon.USER, I18n.t("sms.storageconfig.details.field.username"), config.getUserName());
        mainLayout.add(createSection(I18n.t("sms.storageconfig.details.section.identity"), identityGrid));

        Div connectionGrid = new Div();
        connectionGrid.addClassName("details-grid");
        addFieldToGrid(connectionGrid, VaadinIcon.LINK, I18n.t("sms.storageconfig.details.field.url"), config.getUrl());
        mainLayout.add(createSection(I18n.t("sms.storageconfig.details.section.connection"), connectionGrid));

        Div auditGrid = new Div();
        auditGrid.addClassName("details-grid");
        addFieldToGrid(auditGrid, VaadinIcon.USER_CHECK, I18n.t("sms.storageconfig.details.field.created.by"), config.getCreatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("sms.storageconfig.details.field.created.date"),
                config.getCreateDate() != null ? DateHelper.formatToHumanReadable(config.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.EDIT, I18n.t("sms.storageconfig.details.field.updated.by"), config.getUpdatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("sms.storageconfig.details.field.updated.date"),
                config.getUpdateDate() != null ? DateHelper.formatToHumanReadable(config.getUpdateDate()) : null);
        mainLayout.add(createSection(I18n.t("sms.storageconfig.details.section.audit"), auditGrid));

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