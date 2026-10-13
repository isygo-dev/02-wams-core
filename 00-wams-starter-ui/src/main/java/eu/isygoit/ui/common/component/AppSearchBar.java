package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.i18n.I18n;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Header quick-navigation search: filters {@link INavRegistry#getAll()} by its
 * (locale-resolved) label as the user types, and navigates on selection.
 *
 * <p>Results are presented as a tree: a module header (full name, e.g.
 * "Identity Management" — see {@link INavRegistry#moduleLabelKey}) followed
 * by that module's targets, indented underneath it. ComboBox has no native
 * optgroup/tree support, so this is built as a flat list of a sealed
 * {@link SearchItem} — a non-navigable {@link ModuleHeader} or a real
 * {@link TargetItem} — with headers styled distinctly and never acted on.
 * Typing filters targets by label or module code; a header stays visible
 * whenever at least one of its module's targets still matches, so the tree
 * structure survives filtering.</p>
 *
 * <p>All sizing, spacing and visuals live in
 * {@code styles/scss/_app-search.scss}. The class-name contract emitted here
 * is: {@code wams-app-search}, {@code wams-app-search__item-row},
 * {@code wams-app-search__item-icon}, {@code wams-app-search__group-header},
 * and the per-module modifier {@code wams-module-<key>}.</p>
 */
public class AppSearchBar extends ComboBox<AppSearchBar.SearchItem> {

    private final INavRegistry navRegistry;

    public AppSearchBar(INavRegistry navRegistry) {
        this.navRegistry = navRegistry;
        addClassName("wams-app-search");
        setPlaceholder(I18n.t("common.layout.header.search.placeholder"));
        setClearButtonVisible(true);
        setItemLabelGenerator(this::labelOf);
        setItems(this::matches, buildTree());

        setRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(item ->
                item instanceof ModuleHeader header
                        ? buildHeaderRow(header)
                        : buildTargetRow(((TargetItem) item).target())));

        setPrefixComponent(VaadinIcon.SEARCH.create());

        addValueChangeListener(event -> {
            SearchItem selected = event.getValue();
            // Headers are only ever rendered/styled as non-interactive group
            // labels — ComboBox has no "disabled item" API, so if one still
            // gets selected (e.g. via keyboard), just clear back to empty
            // instead of navigating anywhere.
            if (selected instanceof TargetItem targetItem) {
                UI.getCurrent().navigate(targetItem.target().route());
            }
            clear();
        });
    }

    private List<SearchItem> buildTree() {
        List<SearchItem> result = new ArrayList<>();
        LinkedHashSet<String> moduleOrder = new LinkedHashSet<>();
        this.navRegistry.getAll().forEach(target -> moduleOrder.add(target.moduleKey()));

        for (String moduleKey : moduleOrder) {
            result.add(new ModuleHeader(moduleKey));
            this.navRegistry.getAll().stream()
                    .filter(target -> target.moduleKey().equals(moduleKey))
                    .map(TargetItem::new)
                    .forEach(result::add);
        }
        return result;
    }

    private String labelOf(SearchItem item) {
        return item instanceof ModuleHeader header
                ? I18n.t(this.navRegistry.moduleLabelKey(header.moduleKey()))
                : I18n.t(((TargetItem) item).target().labelKey());
    }

    private Div buildHeaderRow(ModuleHeader header) {
        Div row = new Div();
        row.addClassName("wams-app-search__group-header");
        row.addClassName("wams-module-" + header.moduleKey());
        row.add(new Span(I18n.t(this.navRegistry.moduleLabelKey(header.moduleKey()))));
        return row;
    }

    private Div buildTargetRow(INavRegistry.NavTarget target) {
        Div row = new Div();
        row.addClassName("wams-app-search__item-row");

        Icon icon = target.icon().create();
        icon.addClassName("wams-app-search__item-icon");

        Span label = new Span(I18n.t(target.labelKey()));
        label.addClassName("wams-app-search__item-label");

        row.add(icon, label);
        return row;
    }

    /**
     * Matches the typed text against a target's label or module code; a
     * module header matches whenever its own name matches, or at least one
     * of its targets does, so the tree grouping stays intact around
     * whichever targets survive the filter.
     */
    private boolean matches(SearchItem item, String filterText) {
        if (filterText == null || filterText.isBlank()) {
            return true;
        }
        String term = filterText.trim().toLowerCase();
        if (item instanceof TargetItem targetItem) {
            INavRegistry.NavTarget target = targetItem.target();
            return I18n.t(target.labelKey()).toLowerCase().contains(term)
                    || target.moduleKey().toLowerCase().contains(term);
        }
        ModuleHeader header = (ModuleHeader) item;
        if (I18n.t(this.navRegistry.moduleLabelKey(header.moduleKey())).toLowerCase().contains(term)
                || header.moduleKey().toLowerCase().contains(term)) {
            return true;
        }
        return this.navRegistry.getAll().stream()
                .filter(target -> target.moduleKey().equals(header.moduleKey()))
                .anyMatch(target -> I18n.t(target.labelKey()).toLowerCase().contains(term));
    }

    public sealed interface SearchItem permits ModuleHeader, TargetItem {
    }

    public record ModuleHeader(String moduleKey) implements SearchItem {
    }

    public record TargetItem(INavRegistry.NavTarget target) implements SearchItem {
    }
}