package eu.isygoit.ui.kms.views.secrets.password.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsDetailsDialog;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;

/**
 * Read-only view of a {@link PasswordConfigDto}: every DTO field except
 * {@code id} (never displayed) is shown; audit fields live in the audit tab.
 */
@CssImport("./styles/kms.scss")
public class PasswordConfigDetailsViewDialog extends KmsDetailsDialog {

    private final PasswordConfigDto dto;

    public PasswordConfigDetailsViewDialog(PasswordConfigDto dto) {
        super(I18n.t("kms.password.details.title"));
        this.dto = dto;
        applyWidth(DialogLayout.WIDTH_M);
        buildContent();
    }

    private void buildContent() {
        Div identityGrid = createDetailGrid();
        // Config code is the identifier used to reference this config elsewhere - worth copying.
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.password.details.field.code"), dto.getCode(), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.password.details.field.tenant"), dto.getTenant());
        addTab(I18n.t("kms.password.details.section.identity"),
                createSection(I18n.t("kms.password.details.section.identity"), identityGrid));

        // Policy / generation parameters: everything that shapes what a generated password looks like.
        Div policyGrid = createDetailGrid();
        policyGrid.add(KmsEnumTag.detailField(
                VaadinIcon.USER, I18n.t("kms.password.card.type"),
                KmsEnumTag.ofOrUnknown(dto.getType(), "kms.enum")));
        addFieldToGrid(policyGrid, VaadinIcon.ARROW_DOWN, I18n.t("kms.password.card.min.length"), asText(dto.getMinLength()));
        addFieldToGrid(policyGrid, VaadinIcon.ARROW_UP, I18n.t("kms.password.card.max.length"), asText(dto.getMaxLength()));
        // Pattern is a regex - precise reuse elsewhere requires an exact copy even if short.
        addFieldToGrid(policyGrid, VaadinIcon.TEXT_INPUT, I18n.t("kms.password.card.pattern"), dto.getPattern(), true);
        policyGrid.add(KmsEnumTag.detailField(
                VaadinIcon.FONT, I18n.t("kms.password.card.char.set"),
                KmsEnumTag.ofOrUnknown(dto.getCharSetType(), "kms.enum")));
        addFieldToGrid(policyGrid, VaadinIcon.CLOCK, I18n.t("kms.password.card.lifetime"), asText(dto.getLifeTime()));
        // Initial value can be seed material - copyable so it can be reused precisely.
        addFieldToGrid(policyGrid, VaadinIcon.FLAG, I18n.t("kms.password.card.initial.value"), dto.getInitial(), true);
        addTab(I18n.t("kms.password.details.section.policy"),
                createSection(I18n.t("kms.password.details.section.policy"), policyGrid));

        addAuditTab(dto.getCreatedBy(), dto.getCreateDate(), dto.getUpdatedBy(), dto.getUpdateDate());
    }

    private static String asText(Integer value) {
        return value != null ? String.valueOf(value) : null;
    }
}
