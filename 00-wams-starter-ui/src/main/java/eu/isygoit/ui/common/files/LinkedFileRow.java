package eu.isygoit.ui.common.files;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import eu.isygoit.i18n.I18n;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Consumer;

/**
 * The one row layout of a linked file, shared by {@link AdditionalFilesManager} and {@link OneFilesManager} so both
 * look the same. The columns are fixed and left aligned (the stylesheet lays them out as a grid):
 * <b>icon · file name (+ "pending" tag) · category · upload date · size · type · download · delete</b>.
 * The look is entirely in the shared stylesheet ({@code file-row__*} classes).
 */
final class LinkedFileRow {

    private LinkedFileRow() {
    }

    /**
     * What a row shows; any value may be {@code null} (its cell is then left empty so the columns stay aligned).
     *
     * @param mimetype         mime type, gives the icon (and the type when there is no extension)
     * @param name             file name
     * @param category         category of the file
     * @param categoryEditable {@code true} while the file is not stored yet: the category is an input; once stored it
     *                         is shown as a tag
     * @param onCategory       called with the typed category
     * @param pending          the file is not stored yet: shows the "pending" tag
     * @param date             upload date
     * @param size             size in bytes
     * @param extension        file extension, preferred for the type
     * @param actions          the download and delete actions, in this order
     */
    record Content(String mimetype, String name, String category, boolean categoryEditable,
                   Consumer<String> onCategory, boolean pending, LocalDateTime date, Long size, String extension,
                   Component... actions) {
    }

    static Div build(Content c) {
        Div row = new Div();
        row.addClassName("file-row");
        if (c.pending()) {
            row.addClassName("file-row--pending");
        }

        Icon icon = LinkedFilesSupport.getFileIcon(c.mimetype());
        icon.addClassName("file-row__icon");
        Div iconBox = new Div(icon);
        iconBox.addClassName("file-row__icon-box");

        String name = c.name() != null && !c.name().isBlank() ? c.name() : "-";
        Span nameSpan = new Span(name);
        nameSpan.addClassName("file-row__name");
        nameSpan.setTitle(name);
        Div main = new Div(nameSpan);
        main.addClassName("file-row__main");
        if (c.pending()) {
            Span pendingChip = new Span(I18n.t("files.pending"));
            pendingChip.addClassNames("status-chip", "status-chip--pending");
            main.add(pendingChip);
        }

        Div category = new Div();
        category.addClassName("file-row__category");
        if (c.categoryEditable()) {
            TextField input = new TextField();
            input.addThemeVariants(TextFieldVariant.LUMO_SMALL);
            input.setPlaceholder(I18n.t("files.category.placeholder"));
            input.setAriaLabel(I18n.t("files.category.placeholder"));
            input.setValue(c.category() != null ? c.category() : "");
            input.setWidthFull();
            if (c.onCategory() != null) {
                input.addValueChangeListener(e -> c.onCategory().accept(e.getValue()));
            }
            category.add(input);
        } else if (c.category() != null && !c.category().isBlank()) {
            Span tag = new Span(c.category());
            tag.addClassName("file-row__tag");
            tag.setTitle(c.category());
            category.add(tag);
        }

        Span date = new Span(c.date() != null
                ? c.date().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) : "");
        date.addClassName("file-row__date");

        Span size = new Span(c.size() != null ? LinkedFilesSupport.formatFileSize(c.size()) : "");
        size.addClassName("file-row__size");

        Span type = new Span(typeLabel(c.extension(), c.mimetype()));
        type.addClassName("file-row__type");

        Div actions = new Div(c.actions());
        actions.addClassName("file-row__actions");

        row.add(iconBox, main, category, date, size, type, actions);
        return row;
    }

    /**
     * The extension when known, else the subtype of the mime type ("PDF" for "application/pdf").
     */
    static String typeLabel(String extension, String mimetype) {
        String type = extension != null && !extension.isBlank() ? extension
                : mimetype != null && mimetype.contains("/") ? mimetype.substring(mimetype.indexOf('/') + 1) : mimetype;
        return type == null ? "" : type.replaceFirst("^\\.", "").toUpperCase();
    }
}
