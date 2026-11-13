package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.extendable.AuditableCancelableDto;
import eu.isygoit.dto.extendable.AuditableDto;
import eu.isygoit.dto.extendable.AuditableIdAssignableDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.BidiLtr;
import eu.isygoit.ui.common.component.ClipboardCopyButton;
import eu.isygoit.ui.common.component.LocaleAwareDateFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Shared base for every read-only "…DetailsViewDialog" in the app: a
 * {@link NoActionDialog} whose content is organized into titled sections
 * (one per data category — identity, status, audit, ...), each section
 * holding a grid of fields laid out vertically (icon + label above value).
 * Values long enough to be painful to select by hand (IDs, hashes, keys,
 * paths, URLs, ...) automatically get an inline {@link ClipboardCopyButton}.
 */
@CssImport("./styles/scss/dialog.scss")
public abstract class DetailsViewDialog extends NoActionDialog {

    /**
     * Values longer than this many characters get a copy button by default,
     * even when the caller didn't explicitly ask for one.
     */
    private static final int LONG_VALUE_THRESHOLD = 24;

    protected DetailsViewDialog(String title) {
        super(title);
    }

    // ── Audit cartouche ───────────────────────────────────────────────────────

    /** Audit cartouche of an {@link AuditableDto}; see {@link #setAudit(String, Object, String, Object)}. */
    protected final void setAudit(AuditableDto dto) {
        if (dto != null) {
            setAudit(dto.getCreatedBy(), dto.getCreateDate(), dto.getUpdatedBy(), dto.getUpdateDate());
        }
    }

    /** Audit cartouche of an {@link AuditableIdAssignableDto}. */
    protected final void setAudit(AuditableIdAssignableDto<?> dto) {
        if (dto != null) {
            setAudit(dto.getCreatedBy(), dto.getCreateDate(), dto.getUpdatedBy(), dto.getUpdateDate());
        }
    }

    /** Audit cartouche of an {@link AuditableCancelableDto}. */
    protected final void setAudit(AuditableCancelableDto<?> dto) {
        if (dto != null) {
            setAudit(dto.getCreatedBy(), dto.getCreateDate(), dto.getUpdatedBy(), dto.getUpdateDate());
        }
    }

    /**
     * The audit cartouche: who created and last updated the record, and when, as one compact line at the bottom of
     * the dialog (left of the close button), the same in every details dialog. Blank values are left out and the
     * cartouche is not shown at all when there is nothing to say.
     */
    protected final void setAudit(String createdBy, Object createDate, String updatedBy, Object updateDate) {
        Div cartouche = new Div();
        cartouche.addClassName("wams-audit-cartouche");
        addAuditItem(cartouche, "common.audit.createdBy", createdBy);
        addAuditItem(cartouche, "common.audit.createdAt", createDate);
        addAuditItem(cartouche, "common.audit.updatedBy", updatedBy);
        addAuditItem(cartouche, "common.audit.updatedAt", updateDate);
        setFooterInfo(cartouche.getChildren().findAny().isPresent() ? cartouche : null);
    }

    private static void addAuditItem(Div cartouche, String labelKey, Object value) {
        String text = auditText(value);
        if (text == null) {
            return;
        }
        Span label = new Span(I18n.t(labelKey));
        label.addClassName("wams-audit-cartouche__label");
        Span content = new Span(text);
        content.addClassName("wams-audit-cartouche__value");
        Span item = new Span(label, content);
        item.addClassName("wams-audit-cartouche__item");
        cartouche.add(item);
    }

    private static String auditText(Object value) {
        if (value == null) {
            return null;
        }
        String text;
        if (value instanceof LocalDateTime dateTime) {
            text = BidiLtr.wrap(LocaleAwareDateFormat.format(dateTime));
        } else if (value instanceof java.time.Instant instant) {
            text = BidiLtr.wrap(LocaleAwareDateFormat.format(
                    LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())));
        } else if (value instanceof java.util.Date date) {
            text = BidiLtr.wrap(LocaleAwareDateFormat.format(
                    LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault())));
        } else if (value instanceof LocalDate date) {
            text = BidiLtr.wrap(LocaleAwareDateFormat.format(date));
        } else {
            text = value.toString();
        }
        // callers may pass an already formatted value, with a dash standing for "no value"
        String trimmed = text.trim();
        return trimmed.isEmpty() || trimmed.equals("-") || trimmed.equals("—") ? null : text;
    }

    /**
     * A titled section that visually classifies one group of fields (e.g.
     * "Identity", "Status", "Audit"). The title gets an underline via the
     * shared {@code wams-section-title} rule in
     * {@code styles/scss/components/_card-details.scss}.
     */
    protected Component createSection(String title, Component content) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.addClassName("wams-dialog-section");
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
