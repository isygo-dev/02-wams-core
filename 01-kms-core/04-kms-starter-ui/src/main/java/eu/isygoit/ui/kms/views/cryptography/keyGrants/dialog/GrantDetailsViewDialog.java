package eu.isygoit.ui.kms.views.cryptography.keyGrants.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import eu.isygoit.ui.common.dialog.DetailsViewDialog;

/**
 * Read-only dialog showing every field of a {@link KmsDtos.ListGrantsResponse.Grant},
 * for use when the grants grid row isn't enough (i.e. "Details" action).
 *
 * <p>Sectioned the same way as {@code CustomKeyStoreDetailsViewDialog}/{@code NextCodeDetailsViewDialog}:
 * Identity, Constraints, Audit — with a {@code wams-card__detail-grid} field grid per section.
 */
@CssImport("./styles/kms.css")
public class GrantDetailsViewDialog extends DetailsViewDialog {

    public GrantDetailsViewDialog(KmsDtos.ListGrantsResponse.Grant grant, ObjectMapper objectMapper) {
        super(I18n.t("kms.grant.details.title"));
        setWidth("600px");
        setMaxWidth("95%");
        setResizable(true);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
        addClassName("grant-details-dialog");

        buildContent(grant, objectMapper);
    }

    private void buildContent(KmsDtos.ListGrantsResponse.Grant grant, ObjectMapper objectMapper) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity
        Div identityGrid = createDetailGrid();
        addFieldToGrid(identityGrid, VaadinIcon.KEY, I18n.t("kms.grant.details.field.grant.id"), grant.getGrantId(), true);
        addFieldToGrid(identityGrid, VaadinIcon.USER, I18n.t("kms.grant.details.field.grantee"), grant.getGranteePrincipal(), true);
        addFieldToGrid(identityGrid, VaadinIcon.USER_STAR, I18n.t("kms.grant.details.field.retiring"), grant.getRetiringPrincipal(), true);
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.grant.details.field.name"), grant.getName());
        addFieldToGrid(identityGrid, VaadinIcon.FLAG, I18n.t("kms.grant.details.field.status"), grant.getStatus());
        addFieldToGrid(identityGrid, VaadinIcon.COG, I18n.t("kms.grant.details.field.operations"),
                grant.getOperations() != null ? String.join(", ", grant.getOperations()) : null);
        mainLayout.add(createSection(I18n.t("kms.grant.details.section.identity"), identityGrid));

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
            mainLayout.add(constraintsSection);
        }

        // Audit
        if (grant.getCreateDate() != null) {
            Div auditGrid = createDetailGrid();
            addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("kms.grant.details.field.creation.date"),
                    DateHelper.formatToHumanReadable(grant.getCreateDate()));
            mainLayout.add(createSection(I18n.t("kms.grant.details.section.audit"), auditGrid));
        }

        add(mainLayout);
    }
}
