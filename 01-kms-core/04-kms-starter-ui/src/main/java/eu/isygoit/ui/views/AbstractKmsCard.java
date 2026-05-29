package eu.isygoit.ui.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.remote.kms.KmsApiService;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base for all KMS card components (KeyCard, StoreCard, AliasCard).
 *
 * <p>Centralises:
 * <ul>
 *   <li>Card shell styling (background, shadow, border-radius, transition)</li>
 *   <li>Header row layout (left title area + right button bar)</li>
 *   <li>Status / type chip factory and colour mapping</li>
 *   <li>Meta-row builder with bullet separators</li>
 *   <li>Copy-button wiring (delegates to {@link eu.isygoit.ui.MainView#createCopyButton})</li>
 *   <li>Icon-button factory</li>
 *   <li>Responsive CSS injection (one template, one variable per card type)</li>
 * </ul>
 *
 * <p>Subclasses must implement:
 * <ul>
 *   <li>{@link #cardCssClassName()} – unique BEM block name (e.g. {@code "key-card"})</li>
 *   <li>{@link #buildTitle()} – the left-hand title/identifier component</li>
 *   <li>{@link #buildActionButtons()} – the ordered list of action buttons for the button bar</li>
 *   <li>{@link #buildBodyRows()} – zero or more body rows added below the header</li>
 *   <li>{@link #onCardAttach(AttachEvent)} – optional lifecycle hook (default: no-op)</li>
 * </ul>
 *
 * @param <V> the parent view type
 */
public abstract class AbstractKmsCard<V extends Component> extends VerticalLayout {

    // ── Infrastructure ────────────────────────────────────────────────────────

    /**
     * The parent view that owns this card (for callbacks such as reload).
     */
    protected final V parentView;

    /**
     * Shared API service.
     */
    protected final KmsApiService kmsApiService;

    // ── Layout components exposed to subclasses ───────────────────────────────

    /**
     * The full-width header row: [leftTitle … buttonBar].
     */
    protected HorizontalLayout headerRow;

    /**
     * The left cluster inside the header (title + chips + extras).
     */
    protected HorizontalLayout headerLeft;

    /**
     * The right action-button cluster.
     */
    protected HorizontalLayout buttonBar;

    // ── Constructor ───────────────────────────────────────────────────────────

    protected AbstractKmsCard(V parentView, KmsApiService kmsApiService) {
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
    }

    // ── Template method – must be called by subclass constructor ─────────────

    /**
     * Assembles the complete card. Subclasses <em>must</em> call this at the
     * end of their own constructor after initialising their fields.
     */
    protected final void initCard() {
        applyCardShell();
        buildHeader();
        buildBodyRows();
        injectResponsiveStyles();
        addClassName(cardCssClassName());
    }

    // ── Abstract contract ─────────────────────────────────────────────────────

    /**
     * CSS class name that acts as the BEM block for this card type, e.g.
     * {@code "key-card"}, {@code "store-card"}, {@code "alias-card"}.
     */
    protected abstract String cardCssClassName();

    /**
     * Returns the component(s) to place on the <em>left</em> side of the
     * header row. Typically a title {@link Span}, optional status chip, and
     * copy button. Implementations should call
     * {@link #buildTitleSpan(String, String)} and {@link #buildStatusChip(String)}
     * as needed.
     */
    protected abstract Component buildTitle();

    /**
     * Returns all action {@link Button}s to be placed in the button bar, in
     * order (left to right). Use {@link #createIconButton(VaadinIcon, String)}
     * to create them consistently.
     */
    protected abstract List<Button> buildActionButtons();

    /**
     * Called after the header is assembled. Subclasses add any number of body
     * rows (meta rows, description, tags, …) by calling {@link #add(Component...)}.
     */
    protected abstract void buildBodyRows();

    // ── Optional hook ─────────────────────────────────────────────────────────

    /**
     * Lifecycle hook called from {@link #onAttach(AttachEvent)} after the base
     * implementation completes. Override to register resize listeners, load
     * async data, etc.
     */
    protected void onCardAttach(AttachEvent event) {
        // no-op by default
    }

    // ── Shell styling ─────────────────────────────────────────────────────────

    private void applyCardShell() {
        setWidthFull();
        setMargin(false);
        setPadding(true);
        addClassName(LumoUtility.BorderRadius.LARGE);
        addClassName(LumoUtility.Background.BASE);
        addClassName(LumoUtility.BoxShadow.XSMALL);
        getStyle().set("transition", "all 0.2s ease-in-out");
    }

    // ── Header assembly ───────────────────────────────────────────────────────

    private void buildHeader() {
        // Left cluster
        headerLeft = new HorizontalLayout();
        headerLeft.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLeft.setSpacing(true);
        headerLeft.getStyle().set("flex-wrap", "wrap");

        Component titleComponent = buildTitle();
        headerLeft.add(titleComponent);

        // Button bar
        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);
        buttonBar.getStyle().set("flex-wrap", "wrap");
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.addClassName(cardCssClassName() + "__button-bar");

        List<Button> buttons = buildActionButtons();
        buttons.forEach(buttonBar::add);

        // Header row
        headerRow = new HorizontalLayout(headerLeft, buttonBar);
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.getStyle().set("flex-wrap", "wrap");
        headerRow.setSpacing(true);
        headerRow.addClassName(cardCssClassName() + "__header-row");
        headerRow.expand(headerLeft);

        add(headerRow);
    }

    // ── Chip factory ──────────────────────────────────────────────────────────

    /**
     * Creates a styled status/type chip {@link Span}.
     *
     * @param text   label text
     * @param preset colour preset; one of {@code "success"}, {@code "error"},
     *               {@code "warning"}, {@code "neutral"}, {@code "info"}, or
     *               any custom hex pair via {@link ChipColor}
     */
    protected Span buildStatusChip(String text, ChipColor color) {
        Span chip = new Span(text);
        chip.addClassName(LumoUtility.FontSize.XSMALL);
        chip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
        chip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
        chip.addClassName(LumoUtility.BorderRadius.LARGE);
        chip.getStyle()
                .set("display", "inline-block")
                .set("white-space", "nowrap")
                .set("background-color", color.background())
                .set("color", color.foreground());
        chip.getElement().setAttribute("title", text);
        return chip;
    }

    /**
     * Convenience overload – resolves a semantic name to a {@link ChipColor}.
     *
     * @param text   label text
     * @param status semantic status: {@code "ENABLED"}, {@code "CONNECTED"},
     *               {@code "DISABLED"}, {@code "DISCONNECTED"},
     *               {@code "PENDING_DELETION"}, or anything else → neutral grey
     */
    protected Span buildStatusChip(String text, String status) {
        return buildStatusChip(text, ChipColor.fromStatus(status));
    }

    // ── Title span factory ────────────────────────────────────────────────────

    /**
     * Creates a bold, primary-coloured title {@link Span} with a tooltip set
     * to the full value (useful when text is truncated).
     */
    protected Span buildTitleSpan(String displayText, String fullValue) {
        Span span = new Span(displayText);
        span.addClassName(LumoUtility.FontWeight.BOLD);
        span.addClassName(LumoUtility.FontSize.MEDIUM);
        span.addClassName(LumoUtility.TextColor.PRIMARY);
        span.getStyle().set("word-break", "break-word");
        span.getElement().setAttribute("title", fullValue != null ? fullValue : displayText);
        return span;
    }

    // ── Meta-row builder ──────────────────────────────────────────────────────

    /**
     * Builds a single horizontal meta-row with bullet-separated {@link Span}s.
     * Empty or null entries are skipped automatically.
     *
     * @param entries vararg label strings; {@code null} / blank entries skipped
     * @return a ready-to-add {@link HorizontalLayout}, or {@code null} if all
     * entries are empty
     */
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
        row.getStyle().set("margin-top", "var(--lumo-space-xs)").set("flex-wrap", "wrap");

        for (int i = 0; i < valid.size(); i++) {
            if (i > 0) row.add(new Span("•"));
            row.add(new Span(valid.get(i)));
        }
        return row;
    }

    /**
     * Adds a meta row built from the supplied entries, silently skipping if
     * all entries are empty. Convenience wrapper around
     * {@link #buildMetaRow(String...)}.
     */
    protected void addMetaRow(String... entries) {
        HorizontalLayout row = buildMetaRow(entries);
        if (row != null) add(row);
    }

    // ── Icon button factory ───────────────────────────────────────────────────

    /**
     * Creates a tertiary-inline icon-only {@link Button} with a tooltip.
     */
    protected Button createIconButton(VaadinIcon icon, String tooltip) {
        Button btn = new Button(new Icon(icon));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        btn.setTooltipText(tooltip);
        return btn;
    }

    /**
     * Creates a danger-styled tertiary-inline icon-only {@link Button}.
     */
    protected Button createDangerIconButton(VaadinIcon icon, String tooltip) {
        Button btn = createIconButton(icon, tooltip);
        btn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        return btn;
    }

    // ── Responsive CSS injection ──────────────────────────────────────────────

    /**
     * Injects a scoped CSS block for responsive behaviour. The block is keyed
     * on {@link #cardCssClassName()} so different card types never clash.
     *
     * <p>Subclasses may call {@link #injectAdditionalStyles(String)} to append
     * extra rules after the base block.
     */
    private void injectResponsiveStyles() {
        String block = cardCssClassName();
        String css = """
                .%1$s .%1$s__header-row {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    align-items: center;
                    justify-content: space-between;
                    width: 100%%;
                }
                .%1$s .%1$s__button-bar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-xs);
                }
                @media (max-width: 640px) {
                    .%1$s .%1$s__header-row {
                        flex-direction: column;
                        align-items: flex-start;
                    }
                    .%1$s .%1$s__button-bar {
                        width: 100%%;
                        justify-content: flex-start;
                    }
                    .%1$s .%1$s__button-bar > * {
                        flex: 1;
                    }
                }
                """.formatted(block);

        injectStyles(css);

        String extra = buildExtraStyles();
        if (extra != null && !extra.isBlank()) {
            injectStyles(extra);
        }
    }

    /**
     * Subclasses may override to supply additional scoped CSS that is injected
     * alongside the base responsive block. Return {@code null} or blank to skip.
     */
    protected String buildExtraStyles() {
        return null;
    }

    /**
     * Low-level style injector – appends a {@code <style>} tag to the document head.
     */
    protected final void injectStyles(String css) {
        UI.getCurrent().getPage().executeJs(
                "const s=document.createElement('style');s.textContent=$0;document.head.appendChild(s);",
                css
        );
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected final void onAttach(AttachEvent event) {
        super.onAttach(event);
        onCardAttach(event);
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    /**
     * Immutable colour pair for status chips.
     *
     * @param background CSS background colour string
     * @param foreground CSS foreground colour string
     */
    public record ChipColor(String background, String foreground) {

        // Semantic presets
        public static final ChipColor SUCCESS = new ChipColor("#E3F7E5", "#1E7B2E");
        public static final ChipColor ERROR = new ChipColor("#FEF3F2", "#C73A2B");
        public static final ChipColor WARNING = new ChipColor("#FFF4E5", "#B25600");
        public static final ChipColor NEUTRAL = new ChipColor("#F2F4F8", "#5E6C84");
        public static final ChipColor INFO = new ChipColor("#E9ECEF", "#495057");

        /**
         * Resolves a semantic status string to a {@link ChipColor}.
         * Matches {@code "ENABLED"} / {@code "CONNECTED"} → SUCCESS,
         * {@code "DISABLED"} → ERROR, {@code "DISCONNECTED"} → NEUTRAL,
         * {@code "PENDING_DELETION"} → WARNING, anything else → NEUTRAL.
         */
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