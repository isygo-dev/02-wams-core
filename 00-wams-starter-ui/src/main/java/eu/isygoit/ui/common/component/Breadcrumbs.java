package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.i18n.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic breadcrumb trail ({@code Home > Products > Electronics > Phones}).
 *
 * <p>Purely presentational: it knows nothing about routes or modules. The caller supplies the {@link Item}s and
 * each link is a plain anchor, which Vaadin's client router handles like any in-app link. Any module can build the
 * trail from its own route metadata and hand it over.
 *
 * <ul>
 *   <li><b>Accessible</b>: a {@code nav} landmark labelled "Breadcrumb", an ordered list, {@code aria-current="page"}
 *       on the current item, decorative separators hidden from assistive technology.</li>
 *   <li><b>Responsive</b>: long trails collapse their middle items behind an ellipsis button (always on narrow
 *       screens, above {@link #setMaxVisible(int)} items otherwise); labels truncate with a tooltip.</li>
 *   <li><b>RTL aware</b>: separators flip with the document direction.</li>
 *   <li><b>Theme aware</b>: styled with Lumo tokens only (see {@code components/_breadcrumbs.scss}).</li>
 * </ul>
 *
 * <pre>{@code
 * breadcrumbs.setItems(List.of(
 *         Breadcrumbs.Item.link(I18n.t("nav.home"), "", VaadinIcon.HOME.create()),
 *         Breadcrumbs.Item.link("Products", "products"),
 *         Breadcrumbs.Item.current("Phones")));
 * }</pre>
 */
public class Breadcrumbs extends Nav {

    private static final int DEFAULT_MAX_VISIBLE = 4;

    private final OrderedList list = new OrderedList();
    private List<Item> items = List.of();
    private int maxVisible = DEFAULT_MAX_VISIBLE;

    public Breadcrumbs() {
        addClassName("wams-breadcrumbs");
        getElement().setAttribute("aria-label", I18n.t("breadcrumbs.label"));
        list.addClassName("wams-breadcrumbs__list");
        add(list);
    }

    public Breadcrumbs(List<Item> items) {
        this();
        setItems(items);
    }

    /**
     * Replaces the trail. The last item is the current page unless an item is explicitly flagged as current.
     */
    public void setItems(List<Item> items) {
        this.items = items == null ? List.of() : List.copyOf(items);
        render();
    }

    /**
     * Number of items above which the middle ones collapse behind an ellipsis (minimum 3, default 4). The first and
     * the last items always stay visible.
     */
    public void setMaxVisible(int maxVisible) {
        this.maxVisible = Math.max(3, maxVisible);
        render();
    }

    /* ══════════════════════════════════════════════════════════════
     * Rendering
     * ══════════════════════════════════════════════════════════════ */

    private void render() {
        list.removeAll();
        removeClassName("wams-breadcrumbs--expanded");
        setVisible(!items.isEmpty());

        int currentIndex = currentIndex();
        // collapsible = everything between the first item and the last two (parent + current stay visible)
        int hiddenFrom = 1;
        int hiddenTo = items.size() - 2;   // inclusive
        boolean collapsible = items.size() > maxVisible && hiddenTo >= hiddenFrom;
        boolean ellipsisAdded = false;

        for (int i = 0; i < items.size(); i++) {
            boolean hideable = i >= hiddenFrom && i <= hiddenTo && i != currentIndex;
            if (hideable && !ellipsisAdded && collapsible) {
                list.add(ellipsisItem());
                ellipsisAdded = true;
            }
            ListItem li = new ListItem();
            li.addClassName("wams-breadcrumbs__item");
            if (collapsible && hideable) {
                li.addClassName("wams-breadcrumbs__item--collapsible");
            }
            if (i > 0) {
                li.add(separator());
            }
            li.add(content(items.get(i), i == currentIndex));
            list.add(li);
        }

        // On narrow screens the middle items are hidden by CSS even below the threshold (see _breadcrumbs.scss)
        if (!ellipsisAdded && items.size() > 3) {
            addClassName("wams-breadcrumbs--compact-on-small");
        } else {
            removeClassName("wams-breadcrumbs--compact-on-small");
        }
    }

    private int currentIndex() {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).current()) {
                return i;
            }
        }
        return items.size() - 1;
    }

    private Component content(Item item, boolean current) {
        Span label = new Span(item.label());
        label.addClassName("wams-breadcrumbs__label");

        boolean interactive = !current && !item.disabled() && item.href() != null;
        Component node;
        if (interactive) {
            Anchor link = new Anchor(item.href());
            link.addClassNames("wams-breadcrumbs__link");
            if (item.icon() != null) {
                link.add(iconWrap(item.icon()));
            }
            link.add(label);
            node = link;
        } else {
            Span text = new Span();
            text.addClassNames("wams-breadcrumbs__text");
            if (current) {
                text.addClassName("wams-breadcrumbs__text--current");
                text.getElement().setAttribute("aria-current", "page");
            }
            if (item.disabled()) {
                text.addClassName("wams-breadcrumbs__text--disabled");
                text.getElement().setAttribute("aria-disabled", "true");
            }
            if (item.icon() != null) {
                text.add(iconWrap(item.icon()));
            }
            text.add(label);
            node = text;
        }
        node.getElement().setAttribute("title", item.label());
        return node;
    }

    private Component iconWrap(Component icon) {
        Span wrap = new Span(icon);
        wrap.addClassName("wams-breadcrumbs__icon");
        wrap.getElement().setAttribute("aria-hidden", "true");
        return wrap;
    }

    private Component separator() {
        Span separator = new Span(VaadinIcon.ANGLE_RIGHT.create());
        separator.addClassName("wams-breadcrumbs__separator");
        separator.getElement().setAttribute("aria-hidden", "true");
        return separator;
    }

    /**
     * The "…" that stands for the hidden middle items; a click shows them all.
     */
    private Component ellipsisItem() {
        Button more = new Button("…");
        more.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        more.addClassName("wams-breadcrumbs__more");
        more.setTooltipText(I18n.t("breadcrumbs.more"));
        more.setAriaLabel(I18n.t("breadcrumbs.more"));
        more.getElement().setAttribute("aria-expanded", "false");
        more.addClickListener(e -> {
            addClassName("wams-breadcrumbs--expanded");
            more.getElement().setAttribute("aria-expanded", "true");
            more.getParent().ifPresent(parent -> parent.setVisible(false));
        });

        ListItem li = new ListItem(separator(), more);
        li.addClassNames("wams-breadcrumbs__item", "wams-breadcrumbs__item--ellipsis");
        return li;
    }

    /**
     * One step of the trail.
     *
     * @param label    text shown (already translated)
     * @param href     target; {@code null} for a non-link step
     * @param icon     optional leading icon
     * @param current  marks the current page (the last item is current by default)
     * @param disabled shown but not clickable
     */
    public record Item(String label, String href, Component icon, boolean current, boolean disabled) {

        public static Item link(String label, String href) {
            return new Item(label, href, null, false, false);
        }

        public static Item link(String label, String href, Component icon) {
            return new Item(label, href, icon, false, false);
        }

        public static Item current(String label) {
            return new Item(label, null, null, true, false);
        }

        public static Item disabled(String label) {
            return new Item(label, null, null, false, true);
        }

        public Item withIcon(Component icon) {
            return new Item(label, href, icon, current, disabled);
        }
    }

    /**
     * Convenience for modules that already have their labels and targets as pairs.
     */
    public static List<Item> trail(String[]... labelAndHref) {
        List<Item> out = new ArrayList<>();
        for (int i = 0; i < labelAndHref.length; i++) {
            String[] step = labelAndHref[i];
            boolean last = i == labelAndHref.length - 1;
            out.add(last ? Item.current(step[0]) : Item.link(step[0], step.length > 1 ? step[1] : null));
        }
        return out;
    }
}
