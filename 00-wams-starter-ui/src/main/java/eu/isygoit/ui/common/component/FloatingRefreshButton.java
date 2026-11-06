package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.i18n.I18n;

/**
 * The page-level refresh action: a round floating button pinned at the bottom-right of the window.
 *
 * <p>It reloads the page's data in place (no browser reload), so the page keeps its filters, selection,
 * page number and page size, which is the job of the {@code onRefresh} action passed in. Every routed page
 * that shows reloadable data uses this component instead of an inline refresh button, so the action looks
 * and behaves the same everywhere. See {@code .wams-refresh-fab} in
 * {@code styles/scss/components/_enterprise.scss}.</p>
 *
 * <p>Pages are laid out inside size containers ({@code container-type}), and a container also becomes the
 * containing block of {@code position: fixed} descendants: a button added to the page would then sit at
 * the end of the page content, right-aligned, instead of floating over the window. So when the button is
 * added to a page, it re-attaches itself to the UI root (like a notification) and is removed again when the
 * page is detached. Callers just {@code add(...)} it to the page.</p>
 *
 * <pre>{@code
 * FloatingRefreshButton refresh = new FloatingRefreshButton(this::reload);
 * add(refresh);
 * ...
 * refresh.setLoading(true);     // spins and is disabled while the data is loading
 * }</pre>
 */
@CssImport("./styles/scss/common.scss")
public class FloatingRefreshButton extends Button {

    private static final String LOADING_CLASS = "wams-refresh-fab--loading";

    /**
     * The page the button was added to; it owns the button's lifetime.
     */
    private Component owner;
    private boolean relocating;

    public FloatingRefreshButton(Runnable onRefresh) {
        super(VaadinIcon.REFRESH.create());
        addClassName("wams-refresh-fab");
        addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ICON);
        String label = I18n.t("common.tooltip.refresh");
        setTooltipText(label);
        setAriaLabel(label);
        addClickListener(e -> onRefresh.run());
        // Placement only (the look is in .wams-refresh-fab): keeps the button pinned to the window's
        // bottom-right even if the stylesheet is not loaded yet, e.g. a stale frontend bundle. Without it
        // the button falls back to a plain, start-aligned button at the end of the page.
        getStyle().set("position", "fixed");
        getStyle().set("inset-block-end", "var(--lumo-space-l)");
        getStyle().set("inset-inline-end", "var(--lumo-space-l)");
        getStyle().set("z-index", "20");
    }

    /**
     * Shows the loading state: the icon spins and the button is disabled until loading ends.
     */
    public void setLoading(boolean loading) {
        setEnabled(!loading);
        getElement().getClassList().set(LOADING_CLASS, loading);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        if (relocating) {
            return;
        }
        UI ui = event.getUI();
        Component parent = getParent().orElse(null);
        if (parent == null || parent == ui) {
            return;
        }
        // Added to a page: float over the window instead of inside the page's containment.
        relocating = true;
        if (owner != parent) {
            owner = parent;
            owner.addDetachListener(e -> getElement().removeFromParent());
            owner.addAttachListener(e -> e.getUI().beforeClientResponse(e.getUI(), ctx -> moveToUi(e.getUI())));
        }
        ui.beforeClientResponse(ui, ctx -> moveToUi(ui));
    }

    private void moveToUi(UI ui) {
        if (owner == null || !owner.isAttached()) {
            relocating = false;
            return;
        }
        relocating = true;
        getElement().removeFromParent();
        ui.add(this);
        relocating = false;
    }
}
