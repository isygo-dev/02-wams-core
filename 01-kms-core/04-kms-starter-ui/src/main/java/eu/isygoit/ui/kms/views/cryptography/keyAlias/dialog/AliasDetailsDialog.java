package eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog;

import com.vaadin.flow.component.Component;
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
import eu.isygoit.ui.common.dialog.NoActionDialog;

/**
 * Read-only dialog showing every field of a key alias, for use when the
 * compact {@code AliasCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class AliasDetailsDialog extends NoActionDialog {

    public AliasDetailsDialog(ListAliasesResponse.AliasEntry entry) {
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

        Div infoGrid = new Div();
        infoGrid.addClassName("wams-card__detail-grid");

        addFieldToGrid(infoGrid, VaadinIcon.TAG, I18n.t("kms.alias.details.field.alias.name"), entry.getAliasName());
        addFieldToGrid(infoGrid, VaadinIcon.KEY, I18n.t("kms.alias.details.field.target.key"), entry.getTargetKeyId());
        addFieldToGrid(infoGrid, VaadinIcon.HASH, I18n.t("kms.alias.details.field.wrn"), entry.getAliasWrn());
        addFieldToGrid(infoGrid, VaadinIcon.CALENDAR, I18n.t("kms.alias.details.field.created"), entry.getCreateDate());
        addFieldToGrid(infoGrid, VaadinIcon.CALENDAR_O, I18n.t("kms.alias.details.field.updated"), entry.getUpdateDate());
        addFieldToGrid(infoGrid, VaadinIcon.EXCLAMATION_CIRCLE, I18n.t("kms.alias.details.field.primary"),
                Boolean.TRUE.equals(entry.getPrimaryKey()) ? I18n.t("kms.alias.details.yes") : I18n.t("kms.alias.details.no"));

        mainLayout.add(createSection(I18n.t("kms.alias.details.section.info"), infoGrid));

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

    private Component createSection(String title, Component content) {
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
