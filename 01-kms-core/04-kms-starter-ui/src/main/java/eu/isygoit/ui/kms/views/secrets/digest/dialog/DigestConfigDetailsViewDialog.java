package eu.isygoit.ui.kms.views.secrets.digest.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;

/**
 * Read-only dialog showing every field of a {@link DigestConfigDto}, for use
 * when the compact {@code DigestConfigCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class DigestConfigDetailsViewDialog extends DetailsViewDialog {

    public DigestConfigDetailsViewDialog(DigestConfigDto dto) {
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

        Div identityGrid = createDetailGrid();
        // Config code is the identifier used to reference this config elsewhere — worth copying.
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.digest.details.field.code"), dto.getCode(), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.digest.details.field.tenant"), dto.getTenant());
        mainLayout.add(createSection(I18n.t("kms.digest.details.section.identity"), identityGrid));

        // Algorithm / cryptographic parameters — everything that shapes how the digest is computed.
        Div algorithmGrid = createDetailGrid();
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
        Div advancedGrid = createDetailGrid();
        addFieldToGrid(advancedGrid, VaadinIcon.SERVER, I18n.t("kms.digest.card.provider"), dto.getProviderName());
        // Provider class is a fully-qualified class name — copyable even if short.
        addFieldToGrid(advancedGrid, VaadinIcon.CODE, I18n.t("kms.digest.dialog.field.provider.class"), dto.getProviderClassName(), true);
        addFieldToGrid(advancedGrid, VaadinIcon.GROUP, I18n.t("kms.digest.card.pool.size"), dto.getPoolSize() != null ? String.valueOf(dto.getPoolSize()) : null);
        addFieldToGrid(advancedGrid, VaadinIcon.FONT, I18n.t("kms.digest.card.ignore.unicode"), booleanToText(dto.getUnicodeNormalizationIgnored()));
        addFieldToGrid(advancedGrid, VaadinIcon.TEXT_INPUT, I18n.t("kms.digest.card.prefix"), dto.getPrefix());
        addFieldToGrid(advancedGrid, VaadinIcon.TEXT_INPUT, I18n.t("kms.digest.card.suffix"), dto.getSuffix());
        mainLayout.add(createSection(I18n.t("kms.digest.details.section.advanced"), advancedGrid));

        Div auditGrid = createDetailGrid();
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
}
