package eu.isygoit.ui.common.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic, server‑side paginated management view.
 * <p>
 * Provides:
 * <ul>
 *   <li>Header + toolbar (search + create button)</li>
 *   <li>Loading bar and empty‑state block</li>
 *   <li>Server‑side pagination via {@link #fetchPage(int, int)}</li>
 *   <li>Material‑UI‑style rounded {@link PaginationBar}</li>
 * </ul>
 *
 * @param <T> the row/card DTO type (e.g. {@code StudentMinDto})
 */
@Slf4j
public abstract class AbstractPaginatedView<T> extends ManagementVerticalView implements HasDynamicTitle {

    protected static final int DEFAULT_PAGE_SIZE = 20;

    // ─── Layout ────────────────────────────────────────────────────
    protected final Div cardsContainer = new Div();
    protected final ProgressBar loadingBar = new ProgressBar();
    protected final TextField searchField = new TextField();
    protected final Button createButton = new Button();
    protected final PaginationBar paginationBar = new PaginationBar();

    // ─── Pagination state ──────────────────────────────────────────
    protected int currentPage = 0;
    protected int pageSize = DEFAULT_PAGE_SIZE;
    protected int totalPages = 0;
    protected long totalElements = 0;

    // ─── Data (current page only) ──────────────────────────────────
    protected List<T> currentItems = new ArrayList<>();

    // ══════════════════════════════════════════════════════════════
    //  Abstract contract — subclasses must implement
    // ══════════════════════════════════════════════════════════════

    /** Fetch one page from the backend. Return {@code null} for an empty page. */
    protected abstract PaginatedResponseDto<T> fetchPage(int page, int size);

    /** Build the visual component for a single item (card, row, …). */
    protected abstract Component renderItem(T item);

    /** Client‑side match over the current page only. */
    protected abstract boolean matchesSearch(T item, String search);

    /** Open the create dialog. Remember to call {@link #refresh()} on success. */
    protected abstract void openCreateDialog();

    // i18n keys — one method per label so each view defines its own bundle keys
    protected abstract String getTitleKey();
    protected abstract String getSubtitleKey();
    protected abstract String getCreateButtonKey();
    protected abstract String getSearchPlaceholderKey();
    protected abstract String getEmptyTitleKey();
    protected abstract String getEmptyDescriptionKey();
    protected abstract VaadinIcon getEmptyIcon();

    // ══════════════════════════════════════════════════════════════
    //  Optional hooks — subclasses may override
    // ══════════════════════════════════════════════════════════════

    /** Extra root CSS class names (e.g. {@code "student-view"}). */
    protected String[] getRootCssClasses() { return new String[0]; }

    /**
     * Extra component placed between the toolbar and the loading bar
     * (for example a {@code FilterBar}). {@code null} means "none".
     */
    protected Component buildAdditionalToolbar() { return null; }

    /** i18n key used when the backend call fails. */
    protected String getLoadErrorKey() { return "common.load.error"; }

    // ══════════════════════════════════════════════════════════════
    //  Template method — call from the subclass constructor
    // ══════════════════════════════════════════════════════════════

    /**
     * Builds the entire view shell and triggers the initial page load.
     * Must be called from the concrete constructor (after fields are set)
     * because it relies on abstract {@code getXxxKey()} methods.
     */
    protected void initPaginatedView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("management-view");
        addClassName("administration-view");
        addClassName("paginated-view");
        for (String css : getRootCssClasses()) {
            addClassName(css);
        }

        // ─── Header ────────────────────────────────────────────────
        Div headerBlock = new Div();
        headerBlock.addClassName("administration-header");
        H2 header = new H2(I18n.t(getTitleKey()));
        Paragraph subtitle = new Paragraph(I18n.t(getSubtitleKey()));
        subtitle.addClassName("administration-subtitle");
        headerBlock.add(header, subtitle);
        add(headerBlock);

        // ─── Panel shell ───────────────────────────────────────────
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(false);
        panel.setSpacing(false);
        panel.setWidthFull();
        panel.addClassName("admin-panel");

        panel.add(buildToolbar());

        Component extra = buildAdditionalToolbar();
        if (extra != null) {
            panel.add(extra);
        }

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.addClassName("panel__loading");
        panel.add(loadingBar);

        cardsContainer.setWidthFull();
        cardsContainer.addClassName("cards-grid");
        panel.add(cardsContainer);

        paginationBar.setVisible(false);
        panel.add(paginationBar);

        add(panel);

        loadPage(0);
    }

    /** Toolbar: search field + create button. Override to customise. */
    protected HorizontalLayout buildToolbar() {
        searchField.addClassName("toolbar-search");
        searchField.setLabel(I18n.t("common.filter.search"));
        searchField.setPlaceholder(I18n.t(getSearchPlaceholderKey()));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> applyLocalFilter());

        createButton.setText(I18n.t(getCreateButtonKey()));
        createButton.setIcon(new Icon(VaadinIcon.PLUS_CIRCLE));
        createButton.addClassName("toolbar-add-btn");
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.addClickListener(e -> openCreateDialog());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, createButton);
        toolbar.setWidthFull();
        toolbar.setSpacing(false);
        toolbar.setPadding(false);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("administration-toolbar");
        return toolbar;
    }

    // ══════════════════════════════════════════════════════════════
    //  Data loading
    // ══════════════════════════════════════════════════════════════

    /**
     * Loads the given 0‑based page from the backend, refreshes the cards
     * and updates the pagination bar.
     */
    public void loadPage(int page) {
        showLoading(true);
        try {
            PaginatedResponseDto<T> body = fetchPage(page, pageSize);
            if (body != null) {
                currentItems = body.getContent() != null ? body.getContent() : new ArrayList<>();
                totalPages = body.getTotalPages();
                totalElements = body.getTotalElements();
                currentPage = body.getPageNumber();
            } else {
                currentItems = new ArrayList<>();
                totalPages = 0;
                totalElements = 0;
                currentPage = 0;
            }
            applyLocalFilter();
            paginationBar.update(currentPage, totalPages);
        } catch (Exception e) {
            log.error("Error loading page {} of {}", page, getClass().getSimpleName(), e);
            Notification.show(I18n.t(getLoadErrorKey(), e.getMessage()),
                            6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    /** Reload the current page — call this after a create / update / delete. */
    public void refresh() {
        loadPage(currentPage);
    }

    public void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        createButton.setEnabled(!show);
        paginationBar.setVisible(!show && totalPages > 0);
    }

    // ══════════════════════════════════════════════════════════════
    //  Filtering & rendering
    // ══════════════════════════════════════════════════════════════

    /** Re‑apply the local search filter to the current page. */
    protected void applyLocalFilter() {
        String search = searchField.getValue();
        List<T> filtered = currentItems.stream()
                .filter(item -> matchesSearch(item, search))
                .collect(Collectors.toList());
        renderItems(filtered);
    }

    protected void renderItems(List<T> items) {
        cardsContainer.removeAll();
        if (items.isEmpty()) {
            cardsContainer.add(buildEmptyState());
            return;
        }
        for (T item : items) {
            cardsContainer.add(renderItem(item));
        }
    }

    protected Component buildEmptyState() {
        Div empty = new Div();
        empty.addClassName("panel__empty");

        Div iconWrap = new Div(getEmptyIcon().create());
        iconWrap.addClassName("panel__empty__icon");

        H4 title = new H4(I18n.t(getEmptyTitleKey()));
        title.addClassName("panel__empty__title");

        Paragraph description = new Paragraph(I18n.t(getEmptyDescriptionKey()));
        description.addClassName("panel__empty__description");

        empty.add(iconWrap, title, description);
        return empty;
    }

    // ══════════════════════════════════════════════════════════════
    //  Material‑UI‑style rounded pagination bar
    // ══════════════════════════════════════════════════════════════

    protected class PaginationBar extends HorizontalLayout {

        protected static final int MAX_VISIBLE_PAGES = 5;

        public PaginationBar() {
            setWidthFull();
            setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            setAlignItems(FlexComponent.Alignment.CENTER);
            addClassName("pagination-bar");
        }

        public void update(int current, int total) {
            removeAll();
            if (total <= 1) {
                setVisible(false);
                return;
            }
            setVisible(true);

            add(createNavButton(VaadinIcon.ANGLE_DOUBLE_LEFT, 0, current == 0));
            add(createNavButton(VaadinIcon.ANGLE_LEFT, Math.max(0, current - 1), current == 0));

            for (int p : computePageNumbers(current, total)) {
                add(p == -1 ? createEllipsis() : createPageButton(p, p == current));
            }

            add(createNavButton(VaadinIcon.ANGLE_RIGHT, Math.min(total - 1, current + 1),
                    current == total - 1));
            add(createNavButton(VaadinIcon.ANGLE_DOUBLE_RIGHT, total - 1,
                    current == total - 1));
        }

        protected Button createNavButton(VaadinIcon icon, int targetPage, boolean disabled) {
            Button btn = new Button(new Icon(icon));
            btn.addClassName("pagination-btn");
            btn.setEnabled(!disabled);
            btn.addClickListener(e -> {
                if (targetPage != currentPage) loadPage(targetPage);
            });
            return btn;
        }

        protected Button createPageButton(int page, boolean active) {
            Button btn = new Button(String.valueOf(page + 1));   // 1‑based label
            btn.addClassName("pagination-btn");
            btn.addClassName("pagination-btn--page");
            if (active) {
                btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                btn.addClassName("pagination-btn--active");
            }
            btn.addClickListener(e -> {
                if (page != currentPage) loadPage(page);
            });
            return btn;
        }

        protected Span createEllipsis() {
            Span span = new Span("…");
            span.addClassName("pagination-ellipsis");
            return span;
        }

        /**
         * Compute the list of visible page indices (0‑based).
         * {@code -1} represents an ellipsis.
         */
        protected List<Integer> computePageNumbers(int current, int total) {
            List<Integer> result = new ArrayList<>();
            if (total <= MAX_VISIBLE_PAGES) {
                for (int i = 0; i < total; i++) result.add(i);
                return result;
            }

            int half = MAX_VISIBLE_PAGES / 2;
            int start = Math.max(0, current - half);
            int end = Math.min(total - 1, start + MAX_VISIBLE_PAGES - 1);

            if (start > 0) {
                result.add(0);
                if (start > 1) result.add(-1);
            }
            for (int i = start; i <= end; i++) result.add(i);
            if (end < total - 1) {
                if (end < total - 2) result.add(-1);
                result.add(total - 1);
            }
            return result;
        }
    }

    // in AbstractPaginatedView

    /** Hook for additional client‑side filters (e.g. an enum status filter). */
    protected boolean matchesAdditionalFilters(T item) { return true; }

    /**
     * Extra component placed at the very top of the panel, before the toolbar.
     * Use this for KPI/statistics rows. {@code null} means "none".
     */
    protected Component buildPreToolbarContent() { return null; }

    /** Called at the end of every successful {@link #loadPage(int)}. */
    protected void onPageLoaded() { /* no‑op by default */ }

    // ─── New extension points ──────────────────────────────────────────

    /** Extract a user-readable message from a thrown exception. */
    protected String extractErrorMessage(Exception e) {
        return e.getMessage();
    }

    /** Extra components inserted in the toolbar, right after the search field. */
    protected List<Component> buildToolbarFilters() { return List.of(); }

    /** Extra components inserted in the toolbar, just before the create button. */
    protected List<Component> buildToolbarActions() { return List.of(); }

    /**
     * Called when the search field value changes.
     * Default: re-filter the current page in place.
     */
    protected void onSearchChanged() {
        applyLocalFilter();
    }
}