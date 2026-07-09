package eu.isygoit.ui.kms.views.cryptography.incremental.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;

/**
 * Read-only dialog showing every field of a {@link NextCodeDto}, for use
 * when the compact {@code NextCodeCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class NextCodeDetailsViewDialog extends DetailsViewDialog {

    public NextCodeDetailsViewDialog(NextCodeDto dto) {
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
        Div identityGrid = createDetailGrid();
        addFieldToGrid(identityGrid, VaadinIcon.HASH, I18n.t("kms.nextcode.details.field.id"),
                dto.getId() != null ? String.valueOf(dto.getId()) : null);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.nextcode.details.field.tenant"), dto.getTenant());
        addFieldToGrid(identityGrid, VaadinIcon.FILE_TEXT, I18n.t("kms.nextcode.card.entity"), dto.getEntity());
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.nextcode.card.attribute"), dto.getAttribute());
        addFieldToGrid(identityGrid, VaadinIcon.CLIPBOARD_TEXT, I18n.t("kms.nextcode.details.field.section"), dto.getSectionName());
        mainLayout.add(createSection(I18n.t("kms.nextcode.details.section.identity"), identityGrid));

        // Formatting
        Div formatGrid = createDetailGrid();
        addFieldToGrid(formatGrid, VaadinIcon.ALIGN_LEFT, I18n.t("kms.nextcode.card.prefix"), dto.getPrefix());
        addFieldToGrid(formatGrid, VaadinIcon.ALIGN_RIGHT, I18n.t("kms.nextcode.card.suffix"), dto.getSuffix());
        addFieldToGrid(formatGrid, VaadinIcon.HASH, I18n.t("kms.nextcode.card.value.length"),
                dto.getValueLength() != null ? String.valueOf(dto.getValueLength()) : null);
        addFieldToGrid(formatGrid, VaadinIcon.ARROW_UP, I18n.t("kms.nextcode.card.increment"),
                dto.getIncrement() != null ? String.valueOf(dto.getIncrement()) : null);
        mainLayout.add(createSection(I18n.t("kms.nextcode.details.section.format"), formatGrid));

        // Current value / preview
        Div valueGrid = createDetailGrid();
        addFieldToGrid(valueGrid, VaadinIcon.CALC, I18n.t("kms.nextcode.card.current.value"),
                dto.getCodeValue() != null ? String.valueOf(dto.getCodeValue()) : null);
        addFieldToGrid(valueGrid, VaadinIcon.CODE, I18n.t("kms.nextcode.card.next.code"), dto.getCode(), true);
        mainLayout.add(createSection(I18n.t("kms.nextcode.details.section.value"), valueGrid));

        add(mainLayout);
    }
}
