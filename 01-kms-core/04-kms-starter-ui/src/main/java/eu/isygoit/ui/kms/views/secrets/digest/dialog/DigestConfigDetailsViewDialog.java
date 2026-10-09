package eu.isygoit.ui.kms.views.secrets.digest.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsDetailsDialog;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;

/**
 * Read-only view of a {@link DigestConfigDto}: every DTO field except
 * {@code id} (never displayed) is shown; audit fields live in the audit tab.
 */
@CssImport("./styles/kms.scss")
public class DigestConfigDetailsViewDialog extends KmsDetailsDialog {

    private final DigestConfigDto dto;

    public DigestConfigDetailsViewDialog(DigestConfigDto dto) {
        super(I18n.t("kms.digest.details.title"));
        this.dto = dto;
        applyWidth(DialogLayout.WIDTH_L);
        buildContent();
    }

    private void buildContent() {
        Div identityGrid = createDetailGrid();
        // Config code is the identifier used to reference this config elsewhere - worth copying.
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.digest.details.field.code"), dto.getCode(), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.digest.details.field.tenant"), dto.getTenant());
        addTab(I18n.t("kms.digest.details.section.identity"),
                createSection(I18n.t("kms.digest.details.section.identity"), identityGrid));

        // Algorithm / cryptographic parameters: everything that shapes how the digest is computed.
        Div algorithmGrid = createDetailGrid();
        algorithmGrid.add(KmsEnumTag.detailField(VaadinIcon.COG, I18n.t("kms.digest.card.algorithm"),
                KmsEnumTag.ofOrUnknown(dto.getAlgorithm(), "kms.enum")));
        addFieldToGrid(algorithmGrid, VaadinIcon.ROTATE_RIGHT, I18n.t("kms.digest.card.iterations"), asText(dto.getIterations()));
        addFieldToGrid(algorithmGrid, VaadinIcon.DROP, I18n.t("kms.digest.card.salt.size"), asText(dto.getSaltSizeBytes()));
        algorithmGrid.add(KmsEnumTag.detailField(VaadinIcon.DROP, I18n.t("kms.digest.card.salt.generator"),
                KmsEnumTag.ofOrUnknown(dto.getSaltGenerator(), "kms.enum")));
        addFieldToGrid(algorithmGrid, VaadinIcon.FLIP_H, I18n.t("kms.digest.card.invert.salt.position"),
                booleanToText(dto.getInvertPositionOfSaltInMessageBeforeDigesting()));
        addFieldToGrid(algorithmGrid, VaadinIcon.FLIP_H, I18n.t("kms.digest.card.invert.plain.salt"),
                booleanToText(dto.getInvertPositionOfPlainSaltInEncryptionResults()));
        addFieldToGrid(algorithmGrid, VaadinIcon.CHECK, I18n.t("kms.digest.card.lenient.salt"),
                booleanToText(dto.getUseLenientSaltSizeCheck()));
        algorithmGrid.add(KmsEnumTag.detailField(VaadinIcon.UPLOAD, I18n.t("kms.digest.card.output.type"),
                KmsEnumTag.ofOrUnknown(dto.getStringOutputType(), "kms.enum")));
        addTab(I18n.t("kms.digest.details.section.algorithm"),
                createSection(I18n.t("kms.digest.details.section.algorithm"), algorithmGrid));

        // Advanced / pool settings: provider wiring, pooling and text framing, not crypto behavior.
        Div advancedGrid = createDetailGrid();
        addFieldToGrid(advancedGrid, VaadinIcon.SERVER, I18n.t("kms.digest.card.provider"), dto.getProviderName());
        // Provider class is a fully-qualified class name - copyable even if short.
        addFieldToGrid(advancedGrid, VaadinIcon.CODE, I18n.t("kms.digest.dialog.field.provider.class"), dto.getProviderClassName(), true);
        addFieldToGrid(advancedGrid, VaadinIcon.GROUP, I18n.t("kms.digest.card.pool.size"), asText(dto.getPoolSize()));
        addFieldToGrid(advancedGrid, VaadinIcon.FONT, I18n.t("kms.digest.card.ignore.unicode"),
                booleanToText(dto.getUnicodeNormalizationIgnored()));
        addFieldToGrid(advancedGrid, VaadinIcon.TEXT_INPUT, I18n.t("kms.digest.card.prefix"), dto.getPrefix());
        addFieldToGrid(advancedGrid, VaadinIcon.TEXT_INPUT, I18n.t("kms.digest.card.suffix"), dto.getSuffix());
        addTab(I18n.t("kms.digest.details.section.advanced"),
                createSection(I18n.t("kms.digest.details.section.advanced"), advancedGrid));

        addAuditTab(dto.getCreatedBy(), dto.getCreateDate(), dto.getUpdatedBy(), dto.getUpdateDate());
    }

    private static String asText(Integer value) {
        return value != null ? String.valueOf(value) : null;
    }

    private String booleanToText(Boolean value) {
        return Boolean.TRUE.equals(value) ? I18n.t("kms.digest.card.yes") : I18n.t("kms.digest.card.no");
    }
}
