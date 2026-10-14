package eu.isygoit.ui.common.profile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import eu.isygoit.i18n.I18n;

import java.util.function.IntConsumer;

/**
 * Previous / next controls with a "Page x of y · N sessions" label, shared by the paginated profile lists.
 * The bar only reports the requested page; the owner loads it and calls {@link #update} with the result.
 */
class ProfilePagerBar extends HorizontalLayout {

    private final Button previous = new Button(VaadinIcon.ANGLE_LEFT.create());
    private final Button next = new Button(VaadinIcon.ANGLE_RIGHT.create());
    private final Span label = new Span();
    private final IntConsumer onPage;
    private int page;
    private int totalPages;

    ProfilePagerBar(IntConsumer onPage) {
        this.onPage = onPage;
        addClassName("profile-pager");
        setWidthFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.BETWEEN);

        previous.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        previous.setAriaLabel(I18n.t("profile.pager.previous"));
        previous.addClickListener(e -> go(page - 1));
        next.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        next.setAriaLabel(I18n.t("profile.pager.next"));
        next.addClickListener(e -> go(page + 1));

        label.addClassName("profile-pager-label");
        label.getElement().setAttribute("aria-live", "polite");

        add(previous, label, next);
        update(0, 0, 0);
    }

    private void go(int target) {
        if (target >= 0 && target < totalPages) {
            onPage.accept(target);
        }
    }

    /**
     * Reflects the page that was just loaded.
     */
    void update(int page, int totalPages, long totalElements) {
        this.page = page;
        this.totalPages = totalPages;
        previous.setEnabled(page > 0);
        next.setEnabled(page + 1 < totalPages);
        label.setText(totalPages == 0
                ? ""
                : I18n.t("profile.pager.label", page + 1, totalPages, totalElements));
        setVisible(totalPages > 1);
    }
}
