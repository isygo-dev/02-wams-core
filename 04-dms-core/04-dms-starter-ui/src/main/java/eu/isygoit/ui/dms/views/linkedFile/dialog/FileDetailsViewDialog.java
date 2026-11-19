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
import java.util.Map;

/**
 * Read-only file details, built entirely from the {@link LinkedFileResponseDto}
 * already held by the calling {@code LinkedFileCard}: one details tab (identity,
 * tags and categories) plus the audit tab.
 */
public class FileDetailsViewDialog extends DmsDetailsDialog {

    public FileDetailsViewDialog(LinkedFileResponseDto file) {
        super(I18n.t("dms.linkedfile.details.title"));
        applyWidth(DialogLayout.WIDTH_M);

        VerticalLayout content = buildContent(file);

        addTab(I18n.t("dms.linkedfile.details.section.identity"),
                createSection(I18n.t("dms.linkedfile.details.section.identity"), content));

        addAuditTab(file.getCreatedBy(), DmsDialogSupport.formatDateTime(file.getCreateDate()),
                file.getUpdatedBy(), DmsDialogSupport.formatDateTime(file.getUpdateDate()));
    }

    /**
     * Builds the identity tab's body. Pure: does not touch the dialog.
     */
    private VerticalLayout buildContent(LinkedFileResponseDto file) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.FILE, I18n.t("dms.linkedfile.details.field.code"),
                dash(file.getCode()), true);
        addFieldToGrid(grid, VaadinIcon.FILE_O, I18n.t("dms.linkedfile.details.field.original.name"),
                dash(file.getOriginalFileName()));
        addFieldToGrid(grid, VaadinIcon.FOLDER_O, I18n.t("dms.linkedfile.details.field.path"),
                dash(file.getPath()), true);
        addFieldToGrid(grid, VaadinIcon.BUILDING, I18n.t("dms.linkedfile.details.field.tenant"),
                dash(file.getTenant()));

        VerticalLayout content = new VerticalLayout(grid);
        content.setPadding(false);
        content.setSpacing(true);

        // Use getTags() so derived tags (mimetype/category/extension fallback) are shown too.
        Map<String, String> tagMap = file.getTags();
        if (tagMap != null && !tagMap.isEmpty()) {
            content.add(buildTagRow(VaadinIcon.TAGS,
                    I18n.t("dms.linkedfile.details.field.tags"), tagMap));
        }
        if (file.getCategoryNames() != null && !file.getCategoryNames().isEmpty()) {
            content.add(buildListRow(VaadinIcon.LIST,
                    I18n.t("dms.linkedfile.details.field.categories"),
                    file.getCategoryNames(), "wams-category-chip"));
        }

        return content;
    }

    /**
     * Renders {@code key: value} chips for a {@link Map} (tags).
     */
    private VerticalLayout buildTagRow(VaadinIcon icon, String label, Map<String, String> tags) {
        VerticalLayout section = headerSection(icon, label);

        HorizontalLayout chips = new HorizontalLayout();
        chips.addClassName(DialogLayout.CLASS_ROW);
        chips.getStyle().set("flex-wrap", "wrap");
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Span chip = new Span(key + ": " + (value != null ? value : "—"));
            chip.addClassNames("wams-tag-chip", LumoUtility.FontSize.XXSMALL);
            chip.getElement().setAttribute("title", key + ": " + value);
            chips.add(chip);
        }

        section.add(chips);
        return section;
    }

    /**
     * Renders value-only chips for a {@link List} (categories).
     */
    private VerticalLayout buildListRow(VaadinIcon icon, String label, List<String> values, String chipClass) {
        VerticalLayout section = headerSection(icon, label);

        HorizontalLayout chips = new HorizontalLayout();
        chips.addClassName(DialogLayout.CLASS_ROW);
        chips.getStyle().set("flex-wrap", "wrap");
        for (String value : values) {
            Span chip = new Span(value != null ? value : "—");
            chip.addClassNames(chipClass, LumoUtility.FontSize.XXSMALL);
            chips.add(chip);
        }

        section.add(chips);
        return section;
    }

    /**
     * Shared icon + label header used by every chip row.
     */
    private VerticalLayout headerSection(VaadinIcon icon, String label) {
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

        section.add(header);
        return section;
    }
}