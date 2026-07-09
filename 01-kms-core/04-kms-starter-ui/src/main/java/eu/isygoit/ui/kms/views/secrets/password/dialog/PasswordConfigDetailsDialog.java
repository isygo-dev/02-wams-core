package eu.isygoit.ui.kms.views.secrets.password.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.NoActionDialog;

/**
 * Read-only dialog showing every field of a {@link PasswordConfigDto}, for use
 * when the compact {@code PasswordConfigCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class PasswordConfigDetailsDialog extends NoActionDialog {

    public PasswordConfigDetailsDialog(PasswordConfigDto dto) {
        super(I18n.t("kms.password.details.title"));

        setWidth("650px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("password-config-details-dialog");

        buildContent(dto);
    }

    private void buildContent(PasswordConfigDto dto) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div identityGrid = new Div();
        identityGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.password.details.field.code"), dto.getCode());
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.password.details.field.tenant"), dto.getTenant());
        addFieldToGrid(identityGrid, VaadinIcon.USER, I18n.t("kms.password.card.type"), dto.getType() != null ? dto.getType().meaning() : null);
        addFieldToGrid(identityGrid, VaadinIcon.ARROW_DOWN, I18n.t("kms.password.card.min.length"), dto.getMinLength() != null ? String.valueOf(dto.getMinLength()) : null);
        addFieldToGrid(identityGrid, VaadinIcon.ARROW_UP, I18n.t("kms.password.card.max.length"), dto.getMaxLength() != null ? String.valueOf(dto.getMaxLength()) : null);
        mainLayout.add(createSection(I18n.t("kms.password.details.section.identity"), identityGrid));

        Div policyGrid = new Div();
        policyGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(policyGrid, VaadinIcon.TEXT_INPUT, I18n.t("kms.password.card.pattern"), dto.getPattern());
        addFieldToGrid(policyGrid, VaadinIcon.FONT, I18n.t("kms.password.card.char.set"), dto.getCharSetType() != null ? dto.getCharSetType().meaning() : null);
        addFieldToGrid(policyGrid, VaadinIcon.CLOCK, I18n.t("kms.password.card.lifetime"), dto.getLifeTime() != null ? String.valueOf(dto.getLifeTime()) : null);
        addFieldToGrid(policyGrid, VaadinIcon.FLAG, I18n.t("kms.password.card.initial.value"), dto.getInitial());
        mainLayout.add(createSection(I18n.t("kms.password.details.section.policy"), policyGrid));

        Div auditGrid = new Div();
        auditGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("kms.password.details.field.created"), dto.getCreateDate() != null ? DateHelper.formatToHumanReadable(dto.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.USER_CHECK, I18n.t("kms.password.details.field.created.by"), dto.getCreatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("kms.password.details.field.updated"), dto.getUpdateDate() != null ? DateHelper.formatToHumanReadable(dto.getUpdateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.EDIT, I18n.t("kms.password.details.field.updated.by"), dto.getUpdatedBy());
        mainLayout.add(createSection(I18n.t("kms.password.details.section.audit"), auditGrid));

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
