package eu.isygoit.ui.common.card;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.common.view.ManagementVerticalView;
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
        injectResponsiveStyles();
        addClassName(cardCssClassName());
    }

    // ── Abstract contract ─────────────────────────────────────────────────────

    protected abstract String cardCssClassName();

    /** Returns the component(s) for the left side of the header. */
    protected abstract Component buildTitle();

    /** Returns the action buttons to be placed in the footer (right‑aligned). */
    protected abstract List<Button> buildActionButtons();

    /** Adds body rows (meta rows, description, tags, …) using {@link #add(Component...)}. */
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
        getStyle().set("transition", "all 0.2s ease-in-out");
        // Ensure flex column (VerticalLayout does this by default)
    }

    // ── Header assembly ───────────────────────────────────────────────────────

    protected void buildHeader() {
        headerLeft = new HorizontalLayout();
        headerLeft.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLeft.setSpacing(true);
        headerLeft.getStyle().set("flex-wrap", "wrap");

        Component titleComponent = buildTitle();
        headerLeft.add(titleComponent);

        headerRow = new HorizontalLayout(headerLeft);
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.getStyle().set("flex-wrap", "wrap");
        headerRow.setSpacing(true);
        headerRow.addClassName(cardCssClassName() + "__header-row");

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
        buttonBar.getStyle().set("flex-wrap", "wrap");
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.addClassName(cardCssClassName() + "__button-bar");
        buttons.forEach(buttonBar::add);

        footerRow = new HorizontalLayout(buttonBar);
        footerRow.setWidthFull();
        footerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        footerRow.setPadding(true);
        footerRow.getStyle()
                .set("border-top", "1px solid var(--lumo-contrast-10pct)")
                .set("margin-top", "var(--lumo-space-m)")
                .set("padding-top", "var(--lumo-space-m)");
        footerRow.addClassName(cardCssClassName() + "__footer-row");
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
        bodyContainer.addClassName(cardCssClassName() + "__body");

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
        chip.getStyle()
                .set("display", "inline-block")
                .set("white-space", "nowrap")
                .set("background-color", color.background())
                .set("color", color.foreground());
        chip.getElement().setAttribute("title", text);
        return chip;
    }

    protected Span buildStatusChip(String text, String status) {
        return buildStatusChip(text, ChipColor.fromStatus(status));
    }

    // ── Title span factory ────────────────────────────────────────────────────

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

    // ── Responsive CSS injection ──────────────────────────────────────────────

    private void injectResponsiveStyles() {
        String block = cardCssClassName();
        String css = """
                .%1$s {
                    display: flex;
                    flex-direction: column;
                    height: 100%%;
                }
                .%1$s .%1$s__header-row {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    align-items: center;
                    width: 100%%;
                }
                .%1$s .%1$s__body {
                    flex: 1 1 auto;
                    width: 100%%;
                }
                .%1$s .%1$s__footer-row {
                    display: flex;
                    flex-wrap: wrap;
                    justify-content: flex-end;
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
                    .%1$s .%1$s__footer-row {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .%1$s .%1$s__button-bar {
                        width: 100%%;
                        justify-content: center;
                    }
                }
                """.formatted(block);

        injectStyles(css);

        String extra = buildExtraStyles();
        if (extra != null && !extra.isBlank()) {
            injectStyles(extra);
        }
    }

    protected String buildExtraStyles() {
        return null;
    }

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

    public record ChipColor(String background, String foreground) {
        public static final ChipColor SUCCESS = new ChipColor("#E3F7E5", "#1E7B2E");
        public static final ChipColor ERROR = new ChipColor("#FEF3F2", "#C73A2B");
        public static final ChipColor WARNING = new ChipColor("#FFF4E5", "#B25600");
        public static final ChipColor NEUTRAL = new ChipColor("#F2F4F8", "#5E6C84");
        public static final ChipColor INFO = new ChipColor("#E9ECEF", "#495057");

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