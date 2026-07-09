package eu.isygoit.ui.kms.views.secrets.peb.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.PEBConfigDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;

/**
 * Read-only dialog showing every field of a {@link PEBConfigDto}, for use
 * when the compact {@code PEBConfigCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class PEBConfigDetailsViewDialog extends DetailsViewDialog {

    public PEBConfigDetailsViewDialog(PEBConfigDto dto) {
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

        Div identityGrid = createDetailGrid();
        // Config code is the identifier used to reference this config elsewhere — worth copying.
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.peb.details.field.code"), dto.getCode(), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.peb.details.field.tenant"), dto.getTenant());
        mainLayout.add(createSection(I18n.t("kms.peb.details.section.identity"), identityGrid));

        // Algorithm / cryptographic parameters — everything that shapes how encryption is performed.
        Div cryptoGrid = createDetailGrid();
        addFieldToGrid(cryptoGrid, VaadinIcon.COG, I18n.t("kms.peb.card.algorithm"), dto.getAlgorithm() != null ? dto.getAlgorithm().meaning() : null);
        addFieldToGrid(cryptoGrid, VaadinIcon.ROTATE_RIGHT, I18n.t("kms.peb.card.iterations"), dto.getKeyObtentionIterations() != null ? String.valueOf(dto.getKeyObtentionIterations()) : null);
        addFieldToGrid(cryptoGrid, VaadinIcon.DROP, I18n.t("kms.peb.card.salt.generator"), dto.getSaltGenerator() != null ? dto.getSaltGenerator().meaning() : null);
        addFieldToGrid(cryptoGrid, VaadinIcon.RANDOM, I18n.t("kms.peb.card.iv.generator"), dto.getIvGenerator() != null ? dto.getIvGenerator().meaning() : null);
        addFieldToGrid(cryptoGrid, VaadinIcon.UPLOAD, I18n.t("kms.peb.card.output.type"), dto.getStringOutputType() != null ? dto.getStringOutputType().meaning() : null);
        mainLayout.add(createSection(I18n.t("kms.peb.details.section.crypto"), cryptoGrid));

        Div advancedGrid = createDetailGrid();
        addFieldToGrid(advancedGrid, VaadinIcon.SERVER, I18n.t("kms.peb.card.provider"), dto.getProviderName());
        // Provider class is a fully-qualified class name — copyable even if short.
        addFieldToGrid(advancedGrid, VaadinIcon.CODE, I18n.t("kms.peb.dialog.field.provider.class"), dto.getProviderClassName(), true);
        addFieldToGrid(advancedGrid, VaadinIcon.GROUP, I18n.t("kms.peb.card.pool.size"), dto.getPoolSize() != null ? String.valueOf(dto.getPoolSize()) : null);
        mainLayout.add(createSection(I18n.t("kms.peb.details.section.advanced"), advancedGrid));

        Div auditGrid = createDetailGrid();
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("kms.peb.details.field.created"), dto.getCreateDate() != null ? DateHelper.formatToHumanReadable(dto.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.USER_CHECK, I18n.t("kms.peb.details.field.created.by"), dto.getCreatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("kms.peb.details.field.updated"), dto.getUpdateDate() != null ? DateHelper.formatToHumanReadable(dto.getUpdateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.EDIT, I18n.t("kms.peb.details.field.updated.by"), dto.getUpdatedBy());
        mainLayout.add(createSection(I18n.t("kms.peb.details.section.audit"), auditGrid));

        add(mainLayout);
    }
}
