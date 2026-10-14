package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A titled sidebar navigation section whose items collapse/expand behind a
 * clickable header, with an accordion-style option (see
 * {@link #setOtherSections}) to keep only one section expanded per group.
 * <p>
 * On the icon-only rail the section is just its icon: hovering or clicking it
 * opens a flyout (a {@link Popover}) with the section's items. The flyout is
 * positioned on the logical "end" side, so it opens on the right in LTR and on
 * the left in RTL, and it is only enabled while the layout reports rail mode
 * through {@link #setRailMode}.
 * <p>
 * Every visual detail (colors, spacing, hover, the expand/collapse transition,
 * RTL mirroring and the rail layout) lives in {@code styles/scss/nav.scss}. Used
 * by every {@code <Module>MainLayout} so grouped navigation looks and behaves
 * identically across modules.
 */
@CssImport("./styles/scss/nav.scss")
public class CollapsibleNavSection extends Div {

    private static final String EXPANDED_CLASS = "wams-collapsible-nav-section--expanded";

    private final Div header;
    private final Popover flyout;
    private final List<SideNavItem> navItems = new ArrayList<>();
    private boolean expanded = false;
    private boolean railMode = false;
    private List<CollapsibleNavSection> otherSections;

    public CollapsibleNavSection(String title, SideNavItem... items) {
        this(title, null, items);
    }

    /**
     * @param icon shown for the section on the icon rail (a generic list icon when null)
     */
    public CollapsibleNavSection(String title, VaadinIcon icon, SideNavItem... items) {
        addClassName("wams-collapsible-nav-section");

        header = new Div();
        header.addClassName("wams-collapsible-nav-section__header");
        header.getElement().setAttribute("data-title", title);
        header.addClickListener(e -> {
            if (!railMode) {
                toggle();
            }
        });

        Icon railIcon = railIconFor(icon, items);
        railIcon.setSize("20px");
        railIcon.addClassName("wams-collapsible-nav-section__rail-icon");

        Span titleSpan = new Span(title);
        titleSpan.addClassName("wams-collapsible-nav-section__title");

        Icon toggleIcon = VaadinIcon.CHEVRON_RIGHT.create();
        toggleIcon.addClassName("wams-collapsible-nav-section__toggle-icon");

        header.add(railIcon, titleSpan, toggleIcon);

        Div content = new Div();
        content.addClassName("wams-collapsible-nav-section__content");

        SideNav nav = new SideNav();
        nav.addClassName("wams-collapsible-nav-section__items");
        for (SideNavItem item : items) {
            nav.addItem(item);
            navItems.add(item);
        }
        content.add(nav);

        add(header, content);
        flyout = buildFlyout(title, items);
    }

    public List<SideNavItem> getAllNavItems() {
        return new ArrayList<>(navItems);
    }

    /**
     * Links this section into an accordion group: expanding it collapses
     * every other section in {@code sections} (itself included in the list).
     */
    public void setOtherSections(List<CollapsibleNavSection> sections) {
        this.otherSections = sections;
    }

    public void expand() {
        if (!expanded) {
            toggle();
        }
    }

    public void collapse() {
        if (expanded) {
            toggle();
        }
    }

    /**
     * Switches the section between its normal accordion behaviour and the
     * icon-rail behaviour (the flyout opens on hover or click).
     */
    public void setRailMode(boolean rail) {
        this.railMode = rail;
        flyout.setOpenOnHover(rail);
        flyout.setOpenOnClick(rail);
        if (!rail) {
            flyout.close();
        }
    }

    private Popover buildFlyout(String title, SideNavItem[] items) {
        Popover popover = new Popover();
        popover.setTarget(header);
        popover.setPosition(PopoverPosition.END_TOP);
        popover.addClassName("wams-nav-flyout");
        // Popover opens on click by default: stay inert until rail mode is switched on.
        popover.setOpenOnClick(false);
        popover.setOpenOnHover(false);
        popover.setOpenOnFocus(false);

        Span flyoutTitle = new Span(title);
        flyoutTitle.addClassName("wams-nav-flyout__title");

        SideNav flyoutNav = new SideNav();
        flyoutNav.addClassName("wams-nav-flyout__items");
        for (SideNavItem item : items) {
            SideNavItem copy = copyOf(item);
            copy.getElement().addEventListener("click", e -> popover.close());
            flyoutNav.addItem(copy);
        }
        popover.add(flyoutTitle, flyoutNav);
        return popover;
    }

    /**
     * The group's rail icon: the explicit one if given, otherwise the icon of its first item (so groups are told
     * apart without every module naming an icon), otherwise a generic list icon.
     */
    private static Icon railIconFor(VaadinIcon icon, SideNavItem[] items) {
        if (icon != null) {
            return icon.create();
        }
        for (SideNavItem item : items) {
            if (item.getPrefixComponent() instanceof Icon firstIcon) {
                String iconName = firstIcon.getElement().getAttribute("icon");
                if (iconName != null && !iconName.isBlank()) {
                    return new Icon(iconName);
                }
            }
        }
        return VaadinIcon.LIST.create();
    }

    /**
     * A component can have only one parent, so the flyout gets its own copy of each item (same label, route and
     * icon).
     */
    private static SideNavItem copyOf(SideNavItem item) {
        SideNavItem copy = new SideNavItem(item.getLabel(), item.getPath());
        Component prefix = item.getPrefixComponent();
        if (prefix instanceof Icon icon) {
            String iconName = icon.getElement().getAttribute("icon");
            if (iconName != null && !iconName.isBlank()) {
                Icon iconCopy = new Icon(iconName);
                iconCopy.setSize("20px");
                copy.setPrefixComponent(iconCopy);
            }
        }
        return copy;
    }

    private void toggle() {
        if (!expanded && otherSections != null) {
            for (CollapsibleNavSection section : otherSections) {
                if (section != this && section.expanded) {
                    section.toggle();
                }
            }
        }
        expanded = !expanded;
        setClassName(EXPANDED_CLASS, expanded);
    }
}
