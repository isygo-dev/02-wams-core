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
        mainLayout.add(createSection(I18n.t("kms.digest.details.section.identity"), identityGrid));

        // Algorithm / cryptographic parameters — everything that shapes how the digest is computed.
        Div algorithmGrid = new Div();
        algorithmGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(algorithmGrid, VaadinIcon.COG, I18n.t("kms.digest.card.algorithm"), dto.getAlgorithm() != null ? dto.getAlgorithm().meaning() : null);
        addFieldToGrid(algorithmGrid, VaadinIcon.ROTATE_RIGHT, I18n.t("kms.digest.card.iterations"), dto.getIterations() != null ? String.valueOf(dto.getIterations()) : null);
        addFieldToGrid(algorithmGrid, VaadinIcon.DROP, I18n.t("kms.digest.card.salt.size"), dto.getSaltSizeBytes() != null ? String.valueOf(dto.getSaltSizeBytes()) : null);
        addFieldToGrid(algorithmGrid, VaadinIcon.DROP, I18n.t("kms.digest.card.salt.generator"), dto.getSaltGenerator() != null ? dto.getSaltGenerator().meaning() : null);
        addFieldToGrid(algorithmGrid, VaadinIcon.FLIP_H, I18n.t("kms.digest.card.invert.salt.position"), booleanToText(dto.getInvertPositionOfSaltInMessageBeforeDigesting()));
        addFieldToGrid(algorithmGrid, VaadinIcon.FLIP_H, I18n.t("kms.digest.card.invert.plain.salt"), booleanToText(dto.getInvertPositionOfPlainSaltInEncryptionResults()));
        addFieldToGrid(algorithmGrid, VaadinIcon.CHECK, I18n.t("kms.digest.card.lenient.salt"), booleanToText(dto.getUseLenientSaltSizeCheck()));
        addFieldToGrid(algorithmGrid, VaadinIcon.UPLOAD, I18n.t("kms.digest.card.output.type"), dto.getStringOutputType() != null ? dto.getStringOutputType().meaning() : null);
        mainLayout.add(createSection(I18n.t("kms.digest.details.section.algorithm"), algorithmGrid));

        // Advanced / pool settings — provider wiring, pooling, and text framing, not crypto behavior.
        Div advancedGrid = new Div();
        advancedGrid.addClassName("wams-card__detail-grid");
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
