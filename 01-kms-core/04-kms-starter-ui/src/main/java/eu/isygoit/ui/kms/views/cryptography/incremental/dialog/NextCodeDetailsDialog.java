package eu.isygoit.ui.kms.views.cryptography.incremental.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.NoActionDialog;

/**
 * Read-only dialog showing every field of a {@link NextCodeDto}, for use
 * when the compact {@code NextCodeCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class NextCodeDetailsDialog extends NoActionDialog {

    public NextCodeDetailsDialog(NextCodeDto dto) {
        super(I18n.t("kms.nextcode.details.title"));

        setWidth("650px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("next-code-details-dialog");

        buildContent(dto);
    }

    private void buildContent(NextCodeDto dto) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity
        Div identityGrid = new Div();
        identityGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(identityGrid, VaadinIcon.HASH, I18n.t("kms.nextcode.details.field.id"),
                dto.getId() != null ? String.valueOf(dto.getId()) : null);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.nextcode.details.field.tenant"), dto.getTenant());
        addFieldToGrid(identityGrid, VaadinIcon.FILE_TEXT, I18n.t("kms.nextcode.card.entity"), dto.getEntity());
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.nextcode.card.attribute"), dto.getAttribute());
        addFieldToGrid(identityGrid, VaadinIcon.CLIPBOARD_TEXT, I18n.t("kms.nextcode.details.field.section"), dto.getSectionName());
        mainLayout.add(createSection(I18n.t("kms.nextcode.details.section.identity"), identityGrid));

        // Formatting
        Div formatGrid = new Div();
        formatGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(formatGrid, VaadinIcon.ALIGN_LEFT, I18n.t("kms.nextcode.card.prefix"), dto.getPrefix());
        addFieldToGrid(formatGrid, VaadinIcon.ALIGN_RIGHT, I18n.t("kms.nextcode.card.suffix"), dto.getSuffix());
        addFieldToGrid(formatGrid, VaadinIcon.HASH, I18n.t("kms.nextcode.card.value.length"),
                dto.getValueLength() != null ? String.valueOf(dto.getValueLength()) : null);
        addFieldToGrid(formatGrid, VaadinIcon.ARROW_UP, I18n.t("kms.nextcode.card.increment"),
                dto.getIncrement() != null ? String.valueOf(dto.getIncrement()) : null);
        mainLayout.add(createSection(I18n.t("kms.nextcode.details.section.format"), formatGrid));

        // Current value / preview
        Div valueGrid = new Div();
        valueGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(valueGrid, VaadinIcon.CALC, I18n.t("kms.nextcode.card.current.value"),
                dto.getCodeValue() != null ? String.valueOf(dto.getCodeValue()) : null);
        addFieldToGrid(valueGrid, VaadinIcon.CODE, I18n.t("kms.nextcode.card.next.code"), dto.getCode());
        mainLayout.add(createSection(I18n.t("kms.nextcode.details.section.value"), valueGrid));

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
