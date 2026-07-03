package eu.isygoit.ui.common.card;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base for all KMS card components.
 *
 * <p>Features:
 * <ul>
 *   <li>Card shell (background, shadow, radius, transition)</li>
 *   <li>Header row (left title area)</li>
 *   <li>Body container with flex‑grow (pushes footer to bottom)</li>
 *   <li>Footer row with action buttons (right‑aligned, bordered top)</li>
 *   <li>Status chip factory, meta‑row builder, icon buttons</li>
 *   <li>Responsive CSS (header and footer wrap on narrow screens)</li>
 * </ul>
 *
 * @param <V> the parent view type
 * @param <S> the service type used by this card
 */
public abstract class BaseCard<V extends Component, S> extends VerticalLayout {

    // ── Infrastructure ────────────────────────────────────────────────────────

    protected final V parentView;
    protected final S objectService;

    // ── Layout components ─────────────────────────────────────────────────────

    protected HorizontalLayout headerRow;
    protected HorizontalLayout headerLeft;
    protected HorizontalLayout footerRow;
    protected HorizontalLayout buttonBar;

    // ── Constructor ───────────────────────────────────────────────────────────

    protected BaseCard(V parentView, S objectService) {
        this.parentView = parentView;
        this.objectService = objectService;
    }

    // ── Template method – call this in subclass constructor ─────────────────

    protected final void initCard() {
        applyCardShell();
        buildHeader();
        buildBodyRows();
        buildFooter();
        rearrangeToFlexLayout();
        addClassName(cardCssClassName());
    }

    // ── Abstract contract ─────────────────────────────────────────────────────

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

    // ── Optional hook ─────────────────────────────────────────────────────────

    protected void onCardAttach(AttachEvent event) {
        // no‑op
    }

    // ── Shell styling ─────────────────────────────────────────────────────────

    private void applyCardShell() {
        setWidthFull();
        setHeightFull();                  // allows flex‑grow to work when parent stretches
        setMargin(false);
        setPadding(true);
        addClassName(LumoUtility.BorderRadius.LARGE);
        addClassName(LumoUtility.Background.BASE);
        addClassName(LumoUtility.BoxShadow.XSMALL);
        addClassName("wams-card");
        // Ensure flex column (VerticalLayout does this by default)
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

        // We add headerRow later after rearrangement
    }

    // ── Footer assembly ───────────────────────────────────────────────────────

    protected void buildFooter() {
        List<Button> buttons = buildActionButtons();
        if (buttons.isEmpty()) {
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

    // ── Rearrangement into header / body / footer ────────────────────────────

    private void rearrangeToFlexLayout() {
        // Collect all children added so far (headerRow, body components, footerRow)
        List<Component> children = new ArrayList<>(getChildren().toList());
        removeAll();

        // Header is the first component we built
        add(headerRow);

        // Body container: flex‑grow to push footer down
        VerticalLayout bodyContainer = new VerticalLayout();
        bodyContainer.setPadding(false);
        bodyContainer.setSpacing(true);
        bodyContainer.setWidthFull();
        bodyContainer.setFlexGrow(1);
        bodyContainer.addClassName("wams-card__body");

        // Move all remaining components (except header and footer) into the body container
        for (Component child : children) {
            if (child != headerRow && child != footerRow) {
                bodyContainer.add(child);
            }
        }
        add(bodyContainer);

        // Footer at the bottom (if any)
        if (footerRow != null) {
            add(footerRow);
        }
    }

    // ── Chip factory ──────────────────────────────────────────────────────────

    protected Span buildStatusChip(String text, ChipColor color) {
        Span chip = new Span(text);
        chip.addClassName(LumoUtility.FontSize.XSMALL);
        chip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
        chip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
        chip.addClassName(LumoUtility.BorderRadius.LARGE);
        chip.addClassName("wams-chip");
        chip.addClassName(color.cssClass());
        chip.getElement().setAttribute("title", text);
        return chip;
    }

    protected Span buildStatusChip(String text, String status) {
        return buildStatusChip(text, ChipColor.fromStatus(status));
    }

    /**
     * Re-colors an existing chip (e.g. after a status change) by swapping its
     * {@code wams-chip--*} class instead of setting inline background/foreground
     * colors.
     */
    protected static void applyChipColor(Span chip, ChipColor color) {
        chip.removeClassName(ChipColor.SUCCESS.cssClass());
        chip.removeClassName(ChipColor.ERROR.cssClass());
        chip.removeClassName(ChipColor.WARNING.cssClass());
        chip.removeClassName(ChipColor.NEUTRAL.cssClass());
        chip.removeClassName(ChipColor.INFO.cssClass());
        chip.addClassName("wams-chip");
        chip.addClassName(color.cssClass());
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
        btn.setTooltipText(tooltip);
        return btn;
    }

    protected Button createDangerIconButton(VaadinIcon icon, String tooltip) {
        Button btn = createIconButton(icon, tooltip);
        btn.addThemeVariants(ButtonVariant.LUMO_ERROR);
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
        public static final ChipColor SUCCESS = new ChipColor("wams-chip--success");
        public static final ChipColor ERROR = new ChipColor("wams-chip--error");
        public static final ChipColor WARNING = new ChipColor("wams-chip--warning");
        public static final ChipColor NEUTRAL = new ChipColor("wams-chip--neutral");
        public static final ChipColor INFO = new ChipColor("wams-chip--info");

        public static ChipColor fromStatus(String status) {
            if (status == null) return NEUTRAL;
            return switch (status.toUpperCase()) {
                case "ENABLED", "CONNECTED" -> SUCCESS;
                case "DISABLED" -> ERROR;
                case "DISCONNECTED" -> NEUTRAL;
                case "PENDING_DELETION" -> WARNING;
                default -> NEUTRAL;
            };
        }
    }
}