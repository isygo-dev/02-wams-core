package eu.isygoit.ui.kms.views.secrets.peb.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.PEBConfigDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.NoActionDialog;

/**
 * Read-only dialog showing every field of a {@link PEBConfigDto}, for use
 * when the compact {@code PEBConfigCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class PEBConfigDetailsDialog extends NoActionDialog {

    public PEBConfigDetailsDialog(PEBConfigDto dto) {
        super(I18n.t("kms.peb.details.title"));

        setWidth("650px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("peb-config-details-dialog");

        buildContent(dto);
    }

    private void buildContent(PEBConfigDto dto) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div identityGrid = new Div();
        identityGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.peb.details.field.code"), dto.getCode());
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.peb.details.field.tenant"), dto.getTenant());
        addFieldToGrid(identityGrid, VaadinIcon.COG, I18n.t("kms.peb.card.algorithm"), dto.getAlgorithm() != null ? dto.getAlgorithm().meaning() : null);
        mainLayout.add(createSection(I18n.t("kms.peb.details.section.identity"), identityGrid));

        Div cryptoGrid = new Div();
        cryptoGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(cryptoGrid, VaadinIcon.ROTATE_RIGHT, I18n.t("kms.peb.card.iterations"), dto.getKeyObtentionIterations() != null ? String.valueOf(dto.getKeyObtentionIterations()) : null);
        addFieldToGrid(cryptoGrid, VaadinIcon.DROP, I18n.t("kms.peb.card.salt.generator"), dto.getSaltGenerator() != null ? dto.getSaltGenerator().meaning() : null);
        addFieldToGrid(cryptoGrid, VaadinIcon.RANDOM, I18n.t("kms.peb.card.iv.generator"), dto.getIvGenerator() != null ? dto.getIvGenerator().meaning() : null);
        addFieldToGrid(cryptoGrid, VaadinIcon.UPLOAD, I18n.t("kms.peb.card.output.type"), dto.getStringOutputType() != null ? dto.getStringOutputType().meaning() : null);
        mainLayout.add(createSection(I18n.t("kms.peb.details.section.crypto"), cryptoGrid));

        Div advancedGrid = new Div();
        advancedGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(advancedGrid, VaadinIcon.SERVER, I18n.t("kms.peb.card.provider"), dto.getProviderName());
        addFieldToGrid(advancedGrid, VaadinIcon.CODE, I18n.t("kms.peb.dialog.field.provider.class"), dto.getProviderClassName());
        addFieldToGrid(advancedGrid, VaadinIcon.GROUP, I18n.t("kms.peb.card.pool.size"), dto.getPoolSize() != null ? String.valueOf(dto.getPoolSize()) : null);
        mainLayout.add(createSection(I18n.t("kms.peb.details.section.advanced"), advancedGrid));

        Div auditGrid = new Div();
        auditGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("kms.peb.details.field.created"), dto.getCreateDate() != null ? DateHelper.formatToHumanReadable(dto.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.USER_CHECK, I18n.t("kms.peb.details.field.created.by"), dto.getCreatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("kms.peb.details.field.updated"), dto.getUpdateDate() != null ? DateHelper.formatToHumanReadable(dto.getUpdateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.EDIT, I18n.t("kms.peb.details.field.updated.by"), dto.getUpdatedBy());
        mainLayout.add(createSection(I18n.t("kms.peb.details.section.audit"), auditGrid));

        add(mainLayout);
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

    private VerticalLayout createSection(String title, Div content) {
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
}
