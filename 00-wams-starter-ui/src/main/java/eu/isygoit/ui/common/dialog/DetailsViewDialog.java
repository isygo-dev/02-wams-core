package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.ui.common.component.ClipboardCopyButton;

/**
 * Shared base for every read-only "…DetailsViewDialog" in the app: a
 * {@link NoActionDialog} whose content is organized into titled sections
 * (one per data category — identity, status, audit, ...), each section
 * holding a grid of fields laid out vertically (icon + label above value).
 * Values long enough to be painful to select by hand (IDs, hashes, keys,
 * paths, URLs, ...) automatically get an inline {@link ClipboardCopyButton}.
 */
public abstract class DetailsViewDialog extends NoActionDialog {

    /**
     * Values longer than this many characters get a copy button by default,
     * even when the caller didn't explicitly ask for one.
     */
    private static final int LONG_VALUE_THRESHOLD = 24;

    protected DetailsViewDialog(String title) {
        super(title);
    }

    /**
     * A titled section that visually classifies one group of fields (e.g.
     * "Identity", "Status", "Audit"). The title gets an underline via the
     * shared {@code wams-section-title} rule in {@code card.css}.
     */
    protected Component createSection(String title, Component content) {
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

    /**
     * A responsive grid container for a section's fields.
     */
    protected Div createDetailGrid() {
        Div grid = new Div();
        grid.addClassName("wams-card__detail-grid");
        return grid;
    }

    /**
     * Adds one field (icon + label above value). Skipped entirely when
     * {@code value} is blank. A copy-to-clipboard button is added
     * automatically when the value is longer than {@link #LONG_VALUE_THRESHOLD}
     * characters — use the 5-arg overload to force it on/off explicitly.
     */
    protected void addFieldToGrid(Div container, VaadinIcon icon, String label, String value) {
        addFieldToGrid(container, icon, label, value, value != null && value.length() > LONG_VALUE_THRESHOLD);
    }

    /**
     * Same as {@link #addFieldToGrid(Div, VaadinIcon, String, String)} but
     * with explicit control over whether the copy-to-clipboard button is
     * shown (e.g. force it on for a short-but-sensitive value like a code, or
     * off for a long-but-not-copyable value like a free-text description).
     */
    protected void addFieldToGrid(Div container, VaadinIcon icon, String label, String value, boolean copyable) {
        if (value == null || value.isBlank()) {
            return;
        }

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

        HorizontalLayout valueRow = new HorizontalLayout();
        valueRow.setAlignItems(FlexComponent.Alignment.CENTER);
        valueRow.setSpacing(false);
        valueRow.addClassName("wams-card__detail-field-value-row");

        Span valueSpan = new Span(value);
        valueSpan.addClassName("wams-card__detail-field-value");
        valueRow.add(valueSpan);
        valueRow.expand(valueSpan);

        if (copyable) {
            valueRow.add(new ClipboardCopyButton(value));
        }

        field.add(labelRow, valueRow);
        container.add(field);
    }
}
