package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Vertical list of {@link RowCard}s: the responsive replacement of a table. Items are shown
 * in the order given; an empty list shows a boxed message.
 *
 * @param <T> item type
 */
public class RowCardList<T> extends Div {

    private final List<T> items = new ArrayList<>();
    private Function<T, RowCard> cardFactory;
    private String emptyText = "-";

    public RowCardList() {
        addClassName("wams-row-card-list");
        getElement().setAttribute("role", "list");
    }

    /** How one item is drawn. */
    public RowCardList<T> cardFactory(Function<T, RowCard> factory) {
        this.cardFactory = factory;
        render();
        return this;
    }

    /** Message shown when there is no item. */
    public RowCardList<T> emptyText(String text) {
        this.emptyText = text == null || text.isBlank() ? "-" : text;
        render();
        return this;
    }

    public RowCardList<T> items(Collection<? extends T> newItems) {
        setItems(newItems);
        return this;
    }

    public void setItems(Collection<? extends T> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        render();
    }

    /** A copy of the current items. */
    public List<T> getItems() {
        return new ArrayList<>(items);
    }

    private void render() {
        removeAll();
        if (items.isEmpty()) {
            Span empty = new Span(emptyText);
            empty.addClassName("wams-dlg-empty");
            empty.addClassName("wams-dlg-empty--boxed");
            add(empty);
            return;
        }
        if (cardFactory == null) {
            return;
        }
        items.forEach(item -> add(cardFactory.apply(item)));
    }
}
