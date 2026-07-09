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
        mainLayout.add(createSection(I18n.t("kms.password.details.section.identity"), identityGrid));

        // Policy / generation parameters — everything that shapes what a generated password looks like.
        Div policyGrid = new Div();
        policyGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(policyGrid, VaadinIcon.USER, I18n.t("kms.password.card.type"), dto.getType() != null ? dto.getType().meaning() : null);
        addFieldToGrid(policyGrid, VaadinIcon.ARROW_DOWN, I18n.t("kms.password.card.min.length"), dto.getMinLength() != null ? String.valueOf(dto.getMinLength()) : null);
        addFieldToGrid(policyGrid, VaadinIcon.ARROW_UP, I18n.t("kms.password.card.max.length"), dto.getMaxLength() != null ? String.valueOf(dto.getMaxLength()) : null);
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
