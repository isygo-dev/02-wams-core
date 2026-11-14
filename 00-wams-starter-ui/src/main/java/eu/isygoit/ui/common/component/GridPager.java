package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.grid.Grid;

import java.util.List;

/**
 * Pages a {@link Grid} on the client side, for grids that are loaded whole: the grid shows one page at a time and a
 * {@link PagerBar} right under it gives the total record count and the page buttons. Use {@link #setItems(List)}
 * instead of {@code grid.setItems(...)}. The grid must already be in its parent when items are first set. UI thread
 * only.
 */
public final class GridPager<T> {

    public static final int DEFAULT_PAGE_SIZE = 20;

    private final Grid<T> grid;
    private final int pageSize;
    private final PagerBar pager = new PagerBar(this::goToPage);
    private List<T> items = List.of();
    private int page;

    public GridPager(Grid<T> grid) {
        this(grid, DEFAULT_PAGE_SIZE);
    }

    public GridPager(Grid<T> grid, int pageSize) {
        this.grid = grid;
        this.pageSize = Math.max(1, pageSize);
    }

    /** Replaces the items; the current page is kept when it still exists. */
    public void setItems(java.util.Collection<? extends T> newItems) {
        items = newItems == null ? List.of() : new java.util.ArrayList<T>(newItems);
        page = Math.min(page, Math.max(0, (items.size() - 1) / pageSize));
        draw();
    }

    private void goToPage(int target) {
        page = target;
        draw();
    }

    private void draw() {
        int from = Math.min(page * pageSize, items.size());
        grid.setItems(items.subList(from, Math.min(items.size(), from + pageSize)));
        if (pager.getParent().isEmpty()) {
            grid.getParent().ifPresent(parent -> {
                var element = parent.getElement();
                element.insertChild(element.indexOfChild(grid.getElement()) + 1, pager.getElement());
            });
        }
        pager.update(page, pageSize, items.size());
    }
}
