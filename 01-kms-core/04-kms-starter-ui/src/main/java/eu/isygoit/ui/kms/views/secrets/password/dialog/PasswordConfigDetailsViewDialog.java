package eu.isygoit.ui.kms.views.secrets.password.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;

/**
 * Read-only dialog showing every field of a {@link PasswordConfigDto}, for use
 * when the compact {@code PasswordConfigCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class PasswordConfigDetailsViewDialog extends DetailsViewDialog {

    public PasswordConfigDetailsViewDialog(PasswordConfigDto dto) {
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

        Div identityGrid = createDetailGrid();
        // Config code is the identifier used to reference this config elsewhere — worth copying.
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.password.details.field.code"), dto.getCode(), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.password.details.field.tenant"), dto.getTenant());
        mainLayout.add(createSection(I18n.t("kms.password.details.section.identity"), identityGrid));

        // Policy / generation parameters — everything that shapes what a generated password looks like.
        Div policyGrid = createDetailGrid();
        addFieldToGrid(policyGrid, VaadinIcon.USER, I18n.t("kms.password.card.type"), dto.getType() != null ? dto.getType().meaning() : null);
        addFieldToGrid(policyGrid, VaadinIcon.ARROW_DOWN, I18n.t("kms.password.card.min.length"), dto.getMinLength() != null ? String.valueOf(dto.getMinLength()) : null);
        addFieldToGrid(policyGrid, VaadinIcon.ARROW_UP, I18n.t("kms.password.card.max.length"), dto.getMaxLength() != null ? String.valueOf(dto.getMaxLength()) : null);
        // Pattern is a regex — precise reuse elsewhere requires exact copy even if short.
        addFieldToGrid(policyGrid, VaadinIcon.TEXT_INPUT, I18n.t("kms.password.card.pattern"), dto.getPattern(), true);
        addFieldToGrid(policyGrid, VaadinIcon.FONT, I18n.t("kms.password.card.char.set"), dto.getCharSetType() != null ? dto.getCharSetType().meaning() : null);
        addFieldToGrid(policyGrid, VaadinIcon.CLOCK, I18n.t("kms.password.card.lifetime"), dto.getLifeTime() != null ? String.valueOf(dto.getLifeTime()) : null);
        // Initial value can be a seed/starting secret material — copyable so it can be reused precisely.
        addFieldToGrid(policyGrid, VaadinIcon.FLAG, I18n.t("kms.password.card.initial.value"), dto.getInitial(), true);
        mainLayout.add(createSection(I18n.t("kms.password.details.section.policy"), policyGrid));

        Div auditGrid = createDetailGrid();
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("kms.password.details.field.created"), dto.getCreateDate() != null ? DateHelper.formatToHumanReadable(dto.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.USER_CHECK, I18n.t("kms.password.details.field.created.by"), dto.getCreatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("kms.password.details.field.updated"), dto.getUpdateDate() != null ? DateHelper.formatToHumanReadable(dto.getUpdateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.EDIT, I18n.t("kms.password.details.field.updated.by"), dto.getUpdatedBy());
        mainLayout.add(createSection(I18n.t("kms.password.details.section.audit"), auditGrid));

        add(mainLayout);
    }
}
