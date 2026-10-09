package eu.isygoit.ui.kms.views.secrets.peb.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.PEBConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsDetailsDialog;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;

/**
 * Read-only view of a {@link PEBConfigDto}: every DTO field except
 * {@code id} (never displayed) is shown; audit fields live in the audit tab.
 */
@CssImport("./styles/kms.scss")
public class PEBConfigDetailsViewDialog extends KmsDetailsDialog {

    private final PEBConfigDto dto;

    public PEBConfigDetailsViewDialog(PEBConfigDto dto) {
        super(I18n.t("kms.peb.details.title"));
        this.dto = dto;
        applyWidth(DialogLayout.WIDTH_M);
        buildContent();
    }

    private void buildContent() {
        Div identityGrid = createDetailGrid();
        // Config code is the identifier used to reference this config elsewhere - worth copying.
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.peb.details.field.code"), dto.getCode(), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.peb.details.field.tenant"), dto.getTenant());
        addTab(I18n.t("kms.peb.details.section.identity"),
                createSection(I18n.t("kms.peb.details.section.identity"), identityGrid));

        // Algorithm / cryptographic parameters: everything that shapes how encryption is performed.
        Div cryptoGrid = createDetailGrid();
        cryptoGrid.add(KmsEnumTag.detailField(VaadinIcon.COG, I18n.t("kms.peb.card.algorithm"),
                KmsEnumTag.ofOrUnknown(dto.getAlgorithm(), "kms.enum")));
        addFieldToGrid(cryptoGrid, VaadinIcon.ROTATE_RIGHT, I18n.t("kms.peb.card.iterations"),
                dto.getKeyObtentionIterations() != null ? String.valueOf(dto.getKeyObtentionIterations()) : null);
        cryptoGrid.add(KmsEnumTag.detailField(VaadinIcon.DROP, I18n.t("kms.peb.card.salt.generator"),
                KmsEnumTag.ofOrUnknown(dto.getSaltGenerator(), "kms.enum")));
        cryptoGrid.add(KmsEnumTag.detailField(VaadinIcon.RANDOM, I18n.t("kms.peb.card.iv.generator"),
                KmsEnumTag.ofOrUnknown(dto.getIvGenerator(), "kms.enum")));
        cryptoGrid.add(KmsEnumTag.detailField(VaadinIcon.UPLOAD, I18n.t("kms.peb.card.output.type"),
                KmsEnumTag.ofOrUnknown(dto.getStringOutputType(), "kms.enum")));
        addTab(I18n.t("kms.peb.details.section.crypto"),
                createSection(I18n.t("kms.peb.details.section.crypto"), cryptoGrid));

        Div advancedGrid = createDetailGrid();
        addFieldToGrid(advancedGrid, VaadinIcon.SERVER, I18n.t("kms.peb.card.provider"), dto.getProviderName());
        // Provider class is a fully-qualified class name - copyable even if short.
        addFieldToGrid(advancedGrid, VaadinIcon.CODE, I18n.t("kms.peb.dialog.field.provider.class"), dto.getProviderClassName(), true);
        addFieldToGrid(advancedGrid, VaadinIcon.GROUP, I18n.t("kms.peb.card.pool.size"),
                dto.getPoolSize() != null ? String.valueOf(dto.getPoolSize()) : null);
        addTab(I18n.t("kms.peb.details.section.advanced"),
                createSection(I18n.t("kms.peb.details.section.advanced"), advancedGrid));

        addAuditTab(dto.getCreatedBy(), dto.getCreateDate(), dto.getUpdatedBy(), dto.getUpdateDate());
    }
}
