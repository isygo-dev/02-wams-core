package eu.isygoit.ui.kms.views.secrets.digest.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.NoActionDialog;

/**
 * Read-only dialog showing every field of a {@link DigestConfigDto}, for use
 * when the compact {@code DigestConfigCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class DigestConfigDetailsDialog extends NoActionDialog {

    public DigestConfigDetailsDialog(DigestConfigDto dto) {
        super(I18n.t("kms.digest.details.title"));

        setWidth("650px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("digest-config-details-dialog");

        buildContent(dto);
    }

    private void buildContent(DigestConfigDto dto) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div identityGrid = new Div();
        identityGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.digest.details.field.code"), dto.getCode());
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.digest.details.field.tenant"), dto.getTenant());
        addFieldToGrid(identityGrid, VaadinIcon.COG, I18n.t("kms.digest.card.algorithm"), dto.getAlgorithm() != null ? dto.getAlgorithm().meaning() : null);
        addFieldToGrid(identityGrid, VaadinIcon.ROTATE_RIGHT, I18n.t("kms.digest.card.iterations"), dto.getIterations() != null ? String.valueOf(dto.getIterations()) : null);
        mainLayout.add(createSection(I18n.t("kms.digest.details.section.identity"), identityGrid));

        Div saltGrid = new Div();
        saltGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(saltGrid, VaadinIcon.DROP, I18n.t("kms.digest.card.salt.size"), dto.getSaltSizeBytes() != null ? String.valueOf(dto.getSaltSizeBytes()) : null);
        addFieldToGrid(saltGrid, VaadinIcon.DROP, I18n.t("kms.digest.card.salt.generator"), dto.getSaltGenerator() != null ? dto.getSaltGenerator().meaning() : null);
        addFieldToGrid(saltGrid, VaadinIcon.FLIP_H, I18n.t("kms.digest.card.invert.salt.position"), booleanToText(dto.getInvertPositionOfSaltInMessageBeforeDigesting()));
        addFieldToGrid(saltGrid, VaadinIcon.FLIP_H, I18n.t("kms.digest.card.invert.plain.salt"), booleanToText(dto.getInvertPositionOfPlainSaltInEncryptionResults()));
        addFieldToGrid(saltGrid, VaadinIcon.CHECK, I18n.t("kms.digest.card.lenient.salt"), booleanToText(dto.getUseLenientSaltSizeCheck()));
        mainLayout.add(createSection(I18n.t("kms.digest.details.section.salt"), saltGrid));

        Div advancedGrid = new Div();
        advancedGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(advancedGrid, VaadinIcon.UPLOAD, I18n.t("kms.digest.card.output.type"), dto.getStringOutputType() != null ? dto.getStringOutputType().meaning() : null);
        addFieldToGrid(advancedGrid, VaadinIcon.SERVER, I18n.t("kms.digest.card.provider"), dto.getProviderName());
        addFieldToGrid(advancedGrid, VaadinIcon.CODE, I18n.t("kms.digest.dialog.field.provider.class"), dto.getProviderClassName());
        addFieldToGrid(advancedGrid, VaadinIcon.GROUP, I18n.t("kms.digest.card.pool.size"), dto.getPoolSize() != null ? String.valueOf(dto.getPoolSize()) : null);
        addFieldToGrid(advancedGrid, VaadinIcon.FONT, I18n.t("kms.digest.card.ignore.unicode"), booleanToText(dto.getUnicodeNormalizationIgnored()));
        addFieldToGrid(advancedGrid, VaadinIcon.TEXT_INPUT, I18n.t("kms.digest.card.prefix"), dto.getPrefix());
        addFieldToGrid(advancedGrid, VaadinIcon.TEXT_INPUT, I18n.t("kms.digest.card.suffix"), dto.getSuffix());
        mainLayout.add(createSection(I18n.t("kms.digest.details.section.advanced"), advancedGrid));

        Div auditGrid = new Div();
        auditGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("kms.digest.details.field.created"), dto.getCreateDate() != null ? DateHelper.formatToHumanReadable(dto.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.USER_CHECK, I18n.t("kms.digest.details.field.created.by"), dto.getCreatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("kms.digest.details.field.updated"), dto.getUpdateDate() != null ? DateHelper.formatToHumanReadable(dto.getUpdateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.EDIT, I18n.t("kms.digest.details.field.updated.by"), dto.getUpdatedBy());
        mainLayout.add(createSection(I18n.t("kms.digest.details.section.audit"), auditGrid));

        add(mainLayout);
    }

    private String booleanToText(Boolean value) {
        return Boolean.TRUE.equals(value) ? I18n.t("kms.digest.card.yes") : I18n.t("kms.digest.card.no");
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
