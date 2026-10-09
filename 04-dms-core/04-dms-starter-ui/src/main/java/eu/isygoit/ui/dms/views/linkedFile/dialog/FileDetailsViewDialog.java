package eu.isygoit.ui.dms.views.linkedFile.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.LinkedFileResponseDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.dms.views.common.DmsDetailsDialog;
import eu.isygoit.ui.dms.views.common.DmsDialogSupport;

import java.util.List;

/**
 * Read-only file details, built entirely from the {@link LinkedFileResponseDto}
 * already held by the calling {@code LinkedFileCard}: one details tab (identity,
 * tags and categories) plus the audit tab.
 */
public class FileDetailsViewDialog extends DmsDetailsDialog {

    public FileDetailsViewDialog(LinkedFileResponseDto file) {
        super(I18n.t("dms.linkedfile.details.title"));
        applyWidth(DialogLayout.WIDTH_M);
        buildContent(file);
    }

    private void buildContent(LinkedFileResponseDto file) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.FILE, I18n.t("dms.linkedfile.details.field.code"), dash(file.getCode()), true);
        addFieldToGrid(grid, VaadinIcon.FILE_O, I18n.t("dms.linkedfile.details.field.original.name"),
                dash(file.getOriginalFileName()));
        addFieldToGrid(grid, VaadinIcon.FOLDER_O, I18n.t("dms.linkedfile.details.field.path"),
                dash(file.getPath()), true);
        addFieldToGrid(grid, VaadinIcon.BUILDING, I18n.t("dms.linkedfile.details.field.tenant"),
                dash(file.getTenant()));

        VerticalLayout content = new VerticalLayout(grid);
        content.setPadding(false);
        content.setSpacing(true);
        if (file.getTags() != null && !file.getTags().isEmpty()) {
            content.add(buildChipRow(VaadinIcon.TAGS, I18n.t("dms.linkedfile.details.field.tags"),
                    file.getTags(), "wams-tag-chip"));
        }
        if (file.getCategoryNames() != null && !file.getCategoryNames().isEmpty()) {
            content.add(buildChipRow(VaadinIcon.LIST, I18n.t("dms.linkedfile.details.field.categories"),
                    file.getCategoryNames(), "wams-category-chip"));
        }
        addTab(I18n.t("dms.linkedfile.details.section.identity"),
                createSection(I18n.t("dms.linkedfile.details.section.identity"), content));

        addAuditTab(file.getCreatedBy(), DmsDialogSupport.formatDateTime(file.getCreateDate()),
                file.getUpdatedBy(), DmsDialogSupport.formatDateTime(file.getUpdateDate()));
    }

    private VerticalLayout buildChipRow(VaadinIcon icon, String label, List<String> values, String chipClass) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);

        com.vaadin.flow.component.icon.Icon iconComponent = icon.create();
        iconComponent.addClassName("detail-field-icon");
        iconComponent.getElement().setAttribute("aria-hidden", "true");
        Span labelSpan = new Span(label);
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        HorizontalLayout header = new HorizontalLayout(iconComponent, labelSpan);
        header.setSpacing(true);

        HorizontalLayout chips = new HorizontalLayout();
        chips.addClassName(DialogLayout.CLASS_ROW);
        for (String value : values) {
            Span chip = new Span(value);
            chip.addClassNames(chipClass, LumoUtility.FontSize.XXSMALL);
            chips.add(chip);
        }

        section.add(header, chips);
        return section;
    }
}
