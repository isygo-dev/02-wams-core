package eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.ListAliasesResponse;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;

/**
 * Read-only dialog showing every field of a key alias, for use when the
 * compact {@code AliasCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class AliasDetailsViewDialog extends DetailsViewDialog {

    public AliasDetailsViewDialog(ListAliasesResponse.AliasEntry entry) {
        super(I18n.t("kms.alias.details.title"));

        setWidth("600px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("alias-details-dialog");

        buildContent(entry);
    }

    private void buildContent(ListAliasesResponse.AliasEntry entry) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity — free-text identifying fields only.
        Div identityGrid = createDetailGrid();
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.alias.details.field.alias.name"), entry.getAliasName(), true);
        addFieldToGrid(identityGrid, VaadinIcon.KEY, I18n.t("kms.alias.details.field.target.key"), entry.getTargetKeyId(), true);
        addFieldToGrid(identityGrid, VaadinIcon.HASH, I18n.t("kms.alias.details.field.wrn"), entry.getAliasWrn(), true);
        mainLayout.add(createSection(I18n.t("kms.alias.details.section.identity"), identityGrid));

        // Classification & status — the primary-key boolean flag.
        Div classificationGrid = createDetailGrid();
        addFieldToGrid(classificationGrid, VaadinIcon.EXCLAMATION_CIRCLE, I18n.t("kms.alias.details.field.primary"),
                Boolean.TRUE.equals(entry.getPrimaryKey()) ? I18n.t("kms.alias.details.yes") : I18n.t("kms.alias.details.no"));
        mainLayout.add(createSection(I18n.t("kms.alias.details.section.classification"), classificationGrid));

        // Dates — creation / last-updated timestamps.
        Div datesGrid = createDetailGrid();
        addFieldToGrid(datesGrid, VaadinIcon.CALENDAR, I18n.t("kms.alias.details.field.created"), entry.getCreateDate());
        addFieldToGrid(datesGrid, VaadinIcon.CALENDAR_O, I18n.t("kms.alias.details.field.updated"), entry.getUpdateDate());
        mainLayout.add(createSection(I18n.t("kms.alias.details.section.dates"), datesGrid));

        if (Boolean.TRUE.equals(entry.getPrimaryKey())) {
            HorizontalLayout warningRow = new HorizontalLayout();
            warningRow.setAlignItems(FlexComponent.Alignment.CENTER);
            warningRow.setSpacing(true);
            warningRow.addClassName(LumoUtility.Background.ERROR_10);
            warningRow.addClassName(LumoUtility.TextColor.ERROR);
            warningRow.addClassName(LumoUtility.Padding.SMALL);
            warningRow.addClassName(LumoUtility.BorderRadius.MEDIUM);
            Icon warningIcon = VaadinIcon.WARNING.create();
            warningIcon.setColor("var(--lumo-error-color)");
            warningRow.add(warningIcon, new Span(I18n.t("kms.alias.details.primary.note")));
            mainLayout.add(warningRow);
        }

        add(mainLayout);
    }
}
