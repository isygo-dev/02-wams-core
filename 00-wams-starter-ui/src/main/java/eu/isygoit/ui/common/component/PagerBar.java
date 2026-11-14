package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import eu.isygoit.i18n.I18n;

import java.util.function.IntConsumer;

/**
 * Pager for lists that are paged on the client side (panels that already hold all their items): the total record
 * count ("Showing 1 to 20 of 134") and, when there is more than one page, the page buttons. Same look as the
 * pagination bar of the server-paged views. Hidden when the list is empty. UI thread only.
 */
public class PagerBar extends HorizontalLayout {

    private static final int MAX_VISIBLE_PAGES = 5;

    private final IntConsumer onPage;

    /** @param onPage called with the 0-based page the user asks for */
    public PagerBar(IntConsumer onPage) {
        this.onPage = onPage;
        setWidthFull();
        setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        setAlignItems(FlexComponent.Alignment.CENTER);
        addClassName("pagination-bar");
        setVisible(false);
    }

    /**
     * @param page     current 0-based page
     * @param pageSize items per page
     * @param total    total number of records
     */
    public void update(int page, int pageSize, long total) {
        removeAll();
        if (total <= 0) {
            setVisible(false);
            return;
        }
        setVisible(true);
        long first = (long) page * pageSize + 1;
        long last = Math.min(total, (long) (page + 1) * pageSize);
        Span summary = new Span(String.format(I18n.t("common.pagination.showing"), first, last, total));
        summary.addClassName("pagination-summary");
        summary.getStyle().set("flex", "1");
        add(summary);

        int pages = (int) Math.ceil(total / (double) pageSize);
        if (pages <= 1) {
            return;
        }
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(false);
        buttons.setPadding(false);
        buttons.getStyle().set("gap", "6px");
        buttons.setAlignItems(FlexComponent.Alignment.CENTER);
        buttons.add(nav(VaadinIcon.ANGLE_DOUBLE_LEFT, 0, page == 0),
                nav(VaadinIcon.ANGLE_LEFT, Math.max(0, page - 1), page == 0));
        int start = Math.max(0, Math.min(page - MAX_VISIBLE_PAGES / 2, pages - MAX_VISIBLE_PAGES));
        int end = Math.min(pages - 1, start + MAX_VISIBLE_PAGES - 1);
        for (int p = start; p <= end; p++) {
            buttons.add(number(p, p == page));
        }
        buttons.add(nav(VaadinIcon.ANGLE_RIGHT, Math.min(pages - 1, page + 1), page == pages - 1),
                nav(VaadinIcon.ANGLE_DOUBLE_RIGHT, pages - 1, page == pages - 1));
        add(buttons);
        // mirrors the summary so the buttons stay in the middle
        Span balance = new Span();
        balance.getStyle().set("flex", "1");
        add(balance);
    }

    private Button nav(VaadinIcon icon, int target, boolean disabled) {
        Button button = new Button(new Icon(icon), event -> onPage.accept(target));
        button.addClassName("pagination-btn");
        button.setEnabled(!disabled);
        return button;
    }

    private Button number(int page, boolean active) {
        Button button = new Button(String.valueOf(page + 1), event -> onPage.accept(page));
        button.addClassName("pagination-btn");
        button.addClassName("pagination-btn--page");
        if (active) {
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            button.addClassName("pagination-btn--active");
        }
        return button;
    }
}
