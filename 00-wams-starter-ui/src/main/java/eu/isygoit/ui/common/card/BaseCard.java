package eu.isygoit.ui.common.card;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base for all card components.
 *
 * <p>Features:
 * <ul>
 *   <li>Card shell (background, shadow, radius, transition)</li>
 *   <li>Header row (left title area)</li>
 *   <li>Body container with flex‑grow (pushes footer to bottom)</li>
 *   <li><b>Enum-tags row</b> placed at the bottom of the body, immediately
 *       above the footer separator (see {@link #buildEnumTags()}). The row
 *       wraps onto multiple lines on narrow cards.</li>
 *   <li>Footer row with action buttons (right‑aligned, bordered top)</li>
 *   <li>Status chip factory, meta‑row builder, icon buttons</li>
 *   <li>Responsive CSS (header, footer and enum tags wrap on narrow screens)</li>
 * </ul>
 *
 * @param <V> the parent view type
 * @param <S> the service type used by this card
 */
@CssImport("./styles/card.css")
public abstract class BaseCard<V extends Component, S> extends VerticalLayout {

    // ── Infrastructure ────────────────────────────────────────────────────────

    protected final V parentView;
    protected final S objectService;

    // ── Layout components ─────────────────────────────────────────────────────

    protected HorizontalLayout headerRow;
    protected HorizontalLayout headerLeft;
    protected HorizontalLayout footerRow;
    protected HorizontalLayout buttonBar;
    /** Row of enum tags, added at the bottom of the body (above the footer). */
    protected HorizontalLayout enumTagsRow;

    // ── Constructor ───────────────────────────────────────────────────────────

    protected BaseCard(V parentView, S objectService) {
        this.parentView = parentView;
        this.objectService = objectService;
    }

    // ── Template method – call this in subclass constructor ─────────────────

    /**
     * Re-colors an existing chip (e.g. after a status change) by swapping its
     * {@code status-chip--*} class instead of setting inline background/foreground
     * colors.
     */
    protected static void applyChipColor(Span chip, ChipColor color) {
        chip.removeClassName(ChipColor.SUCCESS.cssClass());
        chip.removeClassName(ChipColor.ERROR.cssClass());
        chip.removeClassName(ChipColor.WARNING.cssClass());
        chip.removeClassName(ChipColor.NEUTRAL.cssClass());
        chip.removeClassName(ChipColor.INFO.cssClass());
        chip.removeClassName(ChipColor.CONTRAST.cssClass());
        chip.addClassName("status-chip");
        chip.addClassName(color.cssClass());
    }

    // ── Abstract contract ─────────────────────────────────────────────────────

    protected final void initCard() {
        applyCardShell();
        buildHeader();
        buildBodyRows();
        buildEnumTagsSection();
        buildFooter();
        rearrangeToFlexLayout();
        addClassName(cardCssClassName());
    }

    protected abstract String cardCssClassName();

    /**
     * Returns the component(s) for the left side of the header.
     */
    protected abstract Component buildTitle();

    /**
     * Returns the action buttons to be placed in the footer (right‑aligned).
     */
    protected abstract List<Button> buildActionButtons();

    /**
     * Adds body rows (meta rows, description, tags, …) using {@link #add(Component...)}.
     */
    protected abstract void buildBodyRows();

    // ── Enum-tags hook ────────────────────────────────────────────────────────

    /**
     * Hook for subclasses to provide a row of tags representing enum fields
     * (gender, category, type, …). The returned component is placed at the
     * <b>bottom of the body</b>, immediately above the footer separator.
     *
     * <p>Return {@code null} (default) to skip the enum-tags row entirely.
     */
    protected Component buildEnumTags() {
        return null;
    }

    // ── Shell styling ─────────────────────────────────────────────────────────

    protected void onCardAttach(AttachEvent event) {
        // no‑op
    }

    private void applyCardShell() {
        setWidthFull();
        setHeightFull();
        setMargin(false);
        setPadding(true);
        addClassName(LumoUtility.BorderRadius.LARGE);
        addClassName(LumoUtility.Background.BASE);
        addClassName(LumoUtility.BoxShadow.XSMALL);
        addClassName("wams-card");
    }

    // ── Header assembly ───────────────────────────────────────────────────────

    protected void buildHeader() {
        headerLeft = new HorizontalLayout();
        headerLeft.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLeft.setSpacing(true);
        headerLeft.addClassName("wams-card__header-row");

        Component titleComponent = buildTitle();
        headerLeft.add(titleComponent);

        headerRow = new HorizontalLayout(headerLeft);
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setSpacing(true);
        headerRow.addClassName("wams-card__header-row");
    }

    // ── Enum-tags assembly ────────────────────────────────────────────────────

    /**
     * Builds the enum-tags wrapper from {@link #buildEnumTags()}. The wrapper
     * is stored in {@link #enumTagsRow} and placed by
     * {@link #rearrangeToFlexLayout()} just above the footer.
     *
     * <p>The wrapper wraps onto multiple lines on narrow cards so that a long
     * set of tags never overflows horizontally.
     */
    private void buildEnumTagsSection() {
        Component tags = buildEnumTags();
        if (tags == null) {
            enumTagsRow = null;
            return;
        }
        enumTagsRow = new HorizontalLayout();
        enumTagsRow.setWidthFull();
        enumTagsRow.setSpacing(true);
        enumTagsRow.setPadding(false);
        enumTagsRow.setAlignItems(FlexComponent.Alignment.CENTER);
        enumTagsRow.addClassName("wams-card__enum-tags");
        enumTagsRow.getStyle()
                .set("flex-wrap", "wrap")
                .set("row-gap", "var(--lumo-space-xs)")
                .set("column-gap", "var(--lumo-space-s)");
        enumTagsRow.add(tags);
    }

    // ── Footer assembly ───────────────────────────────────────────────────────

    protected void buildFooter() {
        List<Button> buttons = buildActionButtons();
        if (buttons == null || buttons.isEmpty()) {
            footerRow = null;
            return;
        }

        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.addClassName("wams-card__button-bar");
        buttons.forEach(buttonBar::add);

        footerRow = new HorizontalLayout(buttonBar);
        footerRow.setWidthFull();
        footerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        footerRow.setPadding(true);
        footerRow.addClassName("wams-card__footer-row");
    }

    // ── Rearrangement into header / body / enum-tags / footer ────────────────

    private void rearrangeToFlexLayout() {
        List<Component> children = new ArrayList<>(getChildren().toList());
        removeAll();

        // Header
        add(headerRow);

        // Body container: flex‑grow pushes everything below it to the bottom
        VerticalLayout bodyContainer = new VerticalLayout();
        bodyContainer.setPadding(false);
        bodyContainer.setSpacing(true);
        bodyContainer.setWidthFull();
        bodyContainer.setFlexGrow(1);
        bodyContainer.addClassName("wams-card__body");

        for (Component child : children) {
            if (child != headerRow && child != footerRow && child != enumTagsRow) {
                bodyContainer.add(child);
            }
        }
        add(bodyContainer);

        // Enum tags — right above the footer separator
        if (enumTagsRow != null) {
            add(enumTagsRow);
        }

        // Footer at the bottom
        if (footerRow != null) {
            add(footerRow);
        }
    }

    // ── Chip factories ────────────────────────────────────────────────────────

    protected Span buildStatusChip(String label, ChipColor color) {
        Span chip = new Span(label);
        chip.addClassName(LumoUtility.FontSize.XSMALL);
        chip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
        chip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
        chip.addClassName(LumoUtility.BorderRadius.LARGE);
        chip.addClassName("status-chip");
        chip.addClassName(color.cssClass());
        chip.getElement().setAttribute("title", label);
        return chip;
    }

    protected Span buildStatusChip(String translated, String status) {
        Span chip = new Span(translated);
        chip.getElement().setAttribute("title", status.toLowerCase());
        chip.addClassName("status-chip");
        if (StringUtils.hasText(status)) {
            chip.addClassName("status-chip--" + status.toLowerCase());
        } else {
            chip.addClassName("status-chip--neutral");
        }
        return chip;
    }

    protected Span buildStatusChip(String label, String status, String translated) {
        Span chip = new Span(translated);
        chip.getElement().setAttribute("title", label.toLowerCase());
        chip.addClassName("status-chip");
        if (StringUtils.hasText(status)) {
            chip.addClassName("status-chip--" + status.toLowerCase());
        } else {
            chip.addClassName("status-chip--neutral");
        }
        return chip;
    }

    // ── Title span factory ────────────────────────────────────────────────────

    protected Span buildTitleSpan(String displayText, String fullValue) {
        Span span = new Span(displayText);
        span.addClassName(LumoUtility.FontWeight.BOLD);
        span.addClassName(LumoUtility.FontSize.MEDIUM);
        span.addClassName(LumoUtility.TextColor.PRIMARY);
        span.addClassName("wams-card__title");
        span.getElement().setAttribute("title", fullValue != null ? fullValue : displayText);
        return span;
    }

    // ── Meta-row builder ──────────────────────────────────────────────────────

    protected HorizontalLayout buildMetaRow(String... entries) {
        List<String> valid = new ArrayList<>();
        for (String e : entries) {
            if (e != null && !e.isBlank()) valid.add(e);
        }
        if (valid.isEmpty()) return null;

        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.addClassName(LumoUtility.FontSize.XSMALL);
        row.addClassName(LumoUtility.TextColor.TERTIARY);
        row.addClassName("wams-card__meta-row");

        for (int i = 0; i < valid.size(); i++) {
            if (i > 0) row.add(new Span("•"));
            row.add(new Span(valid.get(i)));
        }
        return row;
    }

    protected void addMetaRow(String... entries) {
        HorizontalLayout row = buildMetaRow(entries);
        if (row != null) add(row);
    }

    // ── Icon button factory ───────────────────────────────────────────────────

    protected Button createIconButton(VaadinIcon icon, String tooltip) {
        Button btn = new Button(new Icon(icon));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        btn.addClassName("wams-action-btn");
        btn.setTooltipText(tooltip);
        return btn;
    }

    protected Button createDangerIconButton(VaadinIcon icon, String tooltip) {
        Button btn = createIconButton(icon, tooltip);
        btn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        btn.addClassName("wams-action-btn--danger");
        return btn;
    }

    /**
     * Standard action-button contract used by every card in the app, so the
     * action bar always reads the same way regardless of module:
     * <b>Details → Edit → (entity-specific actions) → Toggle status → Delete</b>
     * — Delete is always last and always styled as a danger action.
     */
    protected Button createDetailsButton(String tooltip, Runnable onClick) {
        Button btn = createIconButton(VaadinIcon.INFO_CIRCLE, tooltip);
        btn.addClickListener(e -> onClick.run());
        return btn;
    }

    protected Button createEditButton(String tooltip, Runnable onClick) {
        Button btn = createIconButton(VaadinIcon.EDIT, tooltip);
        btn.addClickListener(e -> onClick.run());
        return btn;
    }

    protected Button createToggleButton(boolean enabled, String enableTooltip, String disableTooltip, Runnable onClick) {
        Button btn = createIconButton(enabled ? VaadinIcon.LOCK : VaadinIcon.UNLOCK,
                enabled ? disableTooltip : enableTooltip);
        btn.addClickListener(e -> onClick.run());
        return btn;
    }

    protected Button createDeleteButton(String tooltip, Runnable onClick) {
        Button btn = createDangerIconButton(VaadinIcon.TRASH, tooltip);
        btn.addClickListener(e -> onClick.run());
        return btn;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected final void onAttach(AttachEvent event) {
        super.onAttach(event);
        onCardAttach(event);
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    public record ChipColor(String cssClass) {
        public static final ChipColor SUCCESS  = new ChipColor("status-chip--success");
        public static final ChipColor ERROR    = new ChipColor("status-chip--error");
        public static final ChipColor WARNING  = new ChipColor("status-chip--warning");
        public static final ChipColor NEUTRAL  = new ChipColor("status-chip--neutral");
        public static final ChipColor INFO     = new ChipColor("status-chip--info");
        public static final ChipColor CONTRAST = new ChipColor("status-chip--contrast");

        /** Legacy mapping — kept for backwards compatibility. */
        public static ChipColor fromStatus(String status) {
            if (status == null) return NEUTRAL;
            return switch (status.toUpperCase()) {
                case "ENABLED", "CONNECTED" -> SUCCESS;
                case "DISABLED"             -> ERROR;
                case "DISCONNECTED"         -> NEUTRAL;
                case "PENDING_DELETION"     -> WARNING;
                default                     -> NEUTRAL;
            };
        }

        /** Color mapping for {@link eu.isygoit.enums.IEnumStudentStatus.Types}. */
        public static ChipColor fromStudentStatus(String status) {
            if (status == null) return NEUTRAL;
            return switch (status.toUpperCase()) {
                // Active lifecycle
                case "ACTIVE"                 -> SUCCESS;
                case "PROBATIONARY"           -> WARNING;
                case "PRE_ENROLLED"           -> INFO;
                case "APPLICANT"              -> INFO;
                // Temporary absences
                case "ON_LEAVE",
                     "MEDICAL_LEAVE",
                     "EXCHANGE"               -> WARNING;
                // Restricted
                case "SUSPENDED"              -> ERROR;
                // End of cycle
                case "GRADUATED"              -> SUCCESS;
                case "TRANSFERRED",
                     "WITHDRAWN"              -> NEUTRAL;
                case "DROPPED_OUT",
                     "EXPELLED"               -> ERROR;
                // Fallback
                default                       -> NEUTRAL;
            };
        }

        /** Color mapping for {@link eu.isygoit.enums.IEnumStaffStatus.Types}. */
        public static ChipColor fromStaffStatus(String status) {
            if (status == null) return NEUTRAL;
            return switch (status.toUpperCase()) {
                // Active lifecycle
                case "ACTIVE"                 -> SUCCESS;
                case "PROBATION"              -> WARNING;
                // Temporary absences
                case "ON_LEAVE",
                     "UNPAID_LEAVE",
                     "SICK_LEAVE",
                     "MATERNITY_LEAVE",
                     "PATERNITY_LEAVE"        -> INFO;
                case "TRAINING"               -> INFO;
                // Restricted
                case "SUSPENDED"              -> ERROR;
                // End of cycle
                case "RESIGNED",
                     "TERMINATED"             -> ERROR;
                case "RETIRED"                -> NEUTRAL;
                // Fallback
                default                       -> NEUTRAL;
            };
        }

        /** Color mapping for {@link eu.isygoit.enums.IEnumEnabledBinaryStatus.Types}. */
        public static ChipColor fromEnabledStatus(String status) {
            if (status == null) return NEUTRAL;
            return switch (status.toUpperCase()) {
                case "ENABLED"  -> SUCCESS;
                case "DISABLED" -> NEUTRAL;
                default         -> NEUTRAL;
            };
        }

        /** Color mapping for {@link eu.isygoit.enums.IEnumTeacherStatus.Types}. */
        public static ChipColor fromTeacherStatus(String status) {
            if (status == null) return NEUTRAL;
            return switch (status.toUpperCase()) {
                case "ACTIVE"    -> SUCCESS;
                case "ON_LEAVE"  -> WARNING;
                case "RESIGNED"  -> ERROR;
                default          -> NEUTRAL;
            };
        }
    }

}