package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A titled sidebar navigation section whose items collapse/expand behind a
 * clickable header, with an accordion-style option (see
 * {@link #setOtherSections}) to keep only one section expanded per group.
 * <p>
 * This component only assembles DOM structure and toggles state via CSS
 * class names — every visual detail (colors, spacing, hover, the expand/
 * collapse transition, and hiding the header when the whole sidebar
 * collapses to its icon-only rail) lives in {@code styles/nav.css}. Used by
 * every {@code <Module>MainLayout} so grouped navigation looks and behaves
 * identically across modules.
 */
@CssImport("./styles/nav.css")
public class CollapsibleNavSection extends Div {

    private static final String EXPANDED_CLASS = "wams-collapsible-nav-section--expanded";

    private final Div header;
    private final List<SideNavItem> navItems = new ArrayList<>();
    private boolean expanded = false;
    private List<CollapsibleNavSection> otherSections;

    public CollapsibleNavSection(String title, SideNavItem... items) {
        addClassName("wams-collapsible-nav-section");

        header = new Div();
        header.addClassName("wams-collapsible-nav-section__header");
        header.getElement().setAttribute("data-title", title);
        header.addClickListener(e -> toggle());

        Span titleSpan = new Span(title);
        titleSpan.addClassName("wams-collapsible-nav-section__title");

        Icon toggleIcon = VaadinIcon.CHEVRON_RIGHT.create();
        toggleIcon.addClassName("wams-collapsible-nav-section__toggle-icon");

        header.add(titleSpan, toggleIcon);

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
