package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.html.Div;

/**
 * Responsive container for a row of {@link StatCard} tiles: 4 columns on
 * desktop, 3 on tablet, 1 on mobile (see {@code .wams-stat-grid} in
 * {@code styles/card.css}). Used by every module dashboard so the stat-card
 * layout is identical everywhere instead of each dashboard hand-rolling its
 * own {@code HorizontalLayout}/CSS.
 */
public class StatCardGrid extends Div {

    public StatCardGrid(StatCard... cards) {
        addClassName("wams-stat-grid");
        add(cards);
    }
}
