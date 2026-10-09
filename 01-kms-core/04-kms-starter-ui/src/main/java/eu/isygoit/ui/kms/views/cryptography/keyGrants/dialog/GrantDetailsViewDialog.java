package eu.isygoit.ui.kms.views.cryptography.keyGrants.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsDetailsDialog;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;

/**
 * Read-only dialog showing every field of a {@link KmsDtos.ListGrantsResponse.Grant},
 * for use when the grants grid row isn't enough (i.e. "Details" action).
 *
 * <p>Sectioned the same way as {@code CustomKeyStoreDetailsViewDialog}/{@code NextCodeDetailsViewDialog}:
 * Identity, Constraints, Audit — with a {@code wams-card__detail-grid} field grid per section.
 */
@CssImport("./styles/kms.scss")
public class GrantDetailsViewDialog extends KmsDetailsDialog {

    public GrantDetailsViewDialog(KmsDtos.ListGrantsResponse.Grant grant, ObjectMapper objectMapper) {
        super(I18n.t("kms.grant.details.title"));
        applyWidth(DialogLayout.WIDTH_M);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
        addClassName("grant-details-dialog");

        buildContent(grant, objectMapper);
    }

    private static boolean hasDates(KmsDtos.ListGrantsResponse.Grant grant) {
        return grant.getCreateDate() != null || grant.getUpdateDate() != null
                || grant.getRevocationDate() != null || grant.getRetirementDate() != null;
    }

    private void buildContent(KmsDtos.ListGrantsResponse.Grant grant, ObjectMapper objectMapper) {
        // Identity
        Div identityGrid = createDetailGrid();
        addFieldToGrid(identityGrid, VaadinIcon.KEY, I18n.t("kms.grant.details.field.grant.id"), grant.getGrantId(), true);
        addFieldToGrid(identityGrid, VaadinIcon.KEY, I18n.t("kms.grant.details.field.key.id"), grant.getKeyId(), true);
        addFieldToGrid(identityGrid, VaadinIcon.USER, I18n.t("kms.grant.details.field.grantee"), grant.getGranteePrincipal(), true);
        addFieldToGrid(identityGrid, VaadinIcon.USER_STAR, I18n.t("kms.grant.details.field.retiring"), grant.getRetiringPrincipal(), true);
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.grant.details.field.name"), grant.getName());
        identityGrid.add(KmsEnumTag.detailField(VaadinIcon.FLAG,
                I18n.t("kms.grant.details.field.status"),
                KmsEnumTag.ofValue(grant.getStatus(), "kms.enum")));
        identityGrid.add(KmsEnumTag.detailField(VaadinIcon.COG, I18n.t("kms.grant.details.field.operations"),
                KmsEnumTag.ofValues(grant.getOperations(), "kms.enum")));
        Component identitySection = createSection(I18n.t("kms.grant.details.section.identity"), identityGrid);
        if (grant.getConstraints() == null && !hasDates(grant)) {
            add(identitySection);
        } else {
            addTab(I18n.t("kms.grant.details.section.identity"), identitySection);
        }

        // Constraints
        if (grant.getConstraints() != null) {
            VerticalLayout constraintsSection = new VerticalLayout();
            constraintsSection.setPadding(false);
            constraintsSection.setSpacing(false);
            Span titleSpan = new Span(I18n.t("kms.grant.details.section.constraints"));
            titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
            titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
            titleSpan.addClassName("wams-section-title");
            constraintsSection.add(titleSpan);

            try {
                String constraintsJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(grant.getConstraints());
                Pre pre = new Pre(constraintsJson);
                pre.addClassName("grant-detail-constraints-pre");
                constraintsSection.add(pre);
            } catch (Exception e) {
                Pre pre = new Pre(grant.getConstraints().toString());
                pre.addClassName("grant-detail-constraints-pre");
                constraintsSection.add(pre);
            }
            addTab(I18n.t("kms.grant.details.section.constraints"), constraintsSection);
        }

        // Dates: lifecycle timestamps of the grant (empty values are skipped by the grid)
        Div datesGrid = createDetailGrid();
        addFieldToGrid(datesGrid, VaadinIcon.CALENDAR, I18n.t("kms.grant.details.field.creation.date"),
                grant.getCreateDate() != null ? DateHelper.formatToHumanReadable(grant.getCreateDate()) : null);
        addFieldToGrid(datesGrid, VaadinIcon.CALENDAR_O, I18n.t("kms.grant.details.field.update.date"), grant.getUpdateDate());
        addFieldToGrid(datesGrid, VaadinIcon.CALENDAR_CLOCK, I18n.t("kms.grant.details.field.revocation.date"),
                grant.getRevocationDate() != null ? DateHelper.formatToHumanReadable(grant.getRevocationDate()) : null);
        addFieldToGrid(datesGrid, VaadinIcon.CALENDAR_CLOCK, I18n.t("kms.grant.details.field.retirement.date"),
                grant.getRetirementDate() != null ? DateHelper.formatToHumanReadable(grant.getRetirementDate()) : null);
        if (datesGrid.getChildren().findAny().isPresent()) {
            addTab(I18n.t("kms.grant.details.section.dates"),
                    createSection(I18n.t("kms.grant.details.section.dates"), datesGrid));
        }
    }
}
