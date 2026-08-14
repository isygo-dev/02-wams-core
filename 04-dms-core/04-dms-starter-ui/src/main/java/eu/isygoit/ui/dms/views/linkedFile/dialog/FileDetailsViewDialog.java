package eu.isygoit.ui.dms.views.linkedFile.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.LinkedFileResponseDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Read-only file details, built entirely from the {@link LinkedFileResponseDto}
 * already held by the calling {@code LinkedFileCard}.
 */
public class FileDetailsViewDialog extends DetailsViewDialog {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

    public FileDetailsViewDialog(LinkedFileResponseDto file) {
        super(I18n.t("dms.linkedfile.details.title"));

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("file-details-dialog");

        buildContent(file);
    }

    private void buildContent(LinkedFileResponseDto file) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity section
        Div identityGrid = createDetailGrid();
        addFieldToGrid(identityGrid, VaadinIcon.FILE, I18n.t("dms.linkedfile.details.field.code"), file.getCode(), true);
        addFieldToGrid(identityGrid, VaadinIcon.FILE_O, I18n.t("dms.linkedfile.details.field.original.name"), file.getOriginalFileName());
        addFieldToGrid(identityGrid, VaadinIcon.FOLDER_O, I18n.t("dms.linkedfile.details.field.path"), file.getPath(), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("dms.linkedfile.details.field.tenant"), file.getTenant());
        mainLayout.add(createSection(I18n.t("dms.linkedfile.details.section.identity"), identityGrid));

        // Tags section
        if (file.getTags() != null && !file.getTags().isEmpty()) {
            mainLayout.add(createSection(I18n.t("dms.linkedfile.details.section.tags"),
                    buildChipRow(VaadinIcon.TAGS, I18n.t("dms.linkedfile.details.field.tags"), file.getTags(), "wams-tag-chip")));
        }

        // Categories section
        if (file.getCategoryNames() != null && !file.getCategoryNames().isEmpty()) {
            mainLayout.add(createSection(I18n.t("dms.linkedfile.details.section.categories"),
                    buildChipRow(VaadinIcon.LIST, I18n.t("dms.linkedfile.details.field.categories"), file.getCategoryNames(), "wams-category-chip")));
        }

        // Audit section
        Div auditGrid = createDetailGrid();
        if (file.getCreatedBy() != null) {
            addFieldToGrid(auditGrid, VaadinIcon.USER, I18n.t("dms.linkedfile.details.field.created.by"), file.getCreatedBy());
        }
        if (file.getCreateDate() != null) {
            addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("dms.linkedfile.details.field.created.date"), formatDateTime(file.getCreateDate()));
        }
        if (file.getUpdatedBy() != null) {
            addFieldToGrid(auditGrid, VaadinIcon.USER_CHECK, I18n.t("dms.linkedfile.details.field.updated.by"), file.getUpdatedBy());
        }
        if (file.getUpdateDate() != null) {
            addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_CLOCK, I18n.t("dms.linkedfile.details.field.updated.date"), formatDateTime(file.getUpdateDate()));
        }
        mainLayout.add(createSection(I18n.t("dms.linkedfile.details.section.audit"), auditGrid));

        add(mainLayout);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DATE_FORMATTER);
    }

    private VerticalLayout buildChipRow(VaadinIcon icon, String label, List<String> values, String chipClass) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        com.vaadin.flow.component.icon.Icon iconComponent = icon.create();
        iconComponent.setSize("14px");
        iconComponent.addClassName("detail-field-icon");
        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        header.add(iconComponent, labelSpan);

        HorizontalLayout chipsContainer = new HorizontalLayout();
        chipsContainer.setSpacing(true);
        chipsContainer.getStyle().set("flex-wrap", "wrap");
        chipsContainer.getStyle().set("gap", "4px");

        for (String value : values) {
            Span chip = new Span(value);
            chip.addClassName(chipClass);
            chip.addClassName(LumoUtility.FontSize.XXSMALL);
            chipsContainer.add(chip);
        }

        section.add(header, chipsContainer);
        return section;
    }
}