package eu.isygoit.ui.ims.views.annex.dialog;

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
import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.ims.views.annex.AnnexManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class AnnexDetailsDialog extends NoActionDialog {

    private final AnnexManagementView parentView;
    private final AnnexService annexService;
    private final Long annexId;

    public AnnexDetailsDialog(AnnexManagementView parentView,
                              AnnexService annexService,
                              Long annexId) {
        super(I18n.t("ims.annex.details.title"));
        this.parentView = parentView;
        this.annexService = annexService;
        this.annexId = annexId;

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("annex-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<AnnexDto> response = annexService.findById(annexId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("ims.annex.details.not.found")));
                addCloseButton();
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.annex.details.load.error", extractErrorMessage(ex))));
            addCloseButton();
        } catch (Exception e) {
            add(new Span(I18n.t("ims.annex.details.load.error", e.getMessage())));
            addCloseButton();
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(AnnexDto annex) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div infoGrid = new Div();
        infoGrid.addClassName("wams-card__detail-grid");

        addFieldToGrid(infoGrid, VaadinIcon.CODE, I18n.t("ims.annex.details.field.table.code"), annex.getTableCode());
        addFieldToGrid(infoGrid, VaadinIcon.LOCATION_ARROW_CIRCLE, I18n.t("ims.annex.details.field.language"), annex.getLanguage() != null ? annex.getLanguage().name() : null);
        addFieldToGrid(infoGrid, VaadinIcon.FONT, I18n.t("ims.annex.details.field.value"), annex.getValue());
        addFieldToGrid(infoGrid, VaadinIcon.FILE_TEXT, I18n.t("ims.annex.details.field.description"), annex.getDescription());
        addFieldToGrid(infoGrid, VaadinIcon.LINK, I18n.t("ims.annex.details.field.reference"), annex.getReference());
        addFieldToGrid(infoGrid, VaadinIcon.SORT, I18n.t("ims.annex.details.field.order"), annex.getAnnexOrder() != null ? String.valueOf(annex.getAnnexOrder()) : null);
        addFieldToGrid(infoGrid, VaadinIcon.CALENDAR, I18n.t("ims.annex.details.field.created"), annex.getCreateDate() != null ? DateHelper.formatToHumanReadable(annex.getCreateDate()) : null);
        addFieldToGrid(infoGrid, VaadinIcon.USER_CHECK, I18n.t("ims.annex.details.field.created.by"), annex.getCreatedBy());
        addFieldToGrid(infoGrid, VaadinIcon.CALENDAR_O, I18n.t("ims.annex.details.field.updated"), annex.getUpdateDate() != null ? DateHelper.formatToHumanReadable(annex.getUpdateDate()) : null);
        addFieldToGrid(infoGrid, VaadinIcon.EDIT, I18n.t("ims.annex.details.field.updated.by"), annex.getUpdatedBy());

        mainLayout.add(createSection(I18n.t("ims.annex.details.section.info"), infoGrid));

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
        Button closeButton = new Button(I18n.t("ims.annex.details.close"), e -> close());
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