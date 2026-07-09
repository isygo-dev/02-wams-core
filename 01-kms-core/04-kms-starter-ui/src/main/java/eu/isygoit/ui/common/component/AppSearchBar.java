package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import eu.isygoit.i18n.I18n;

/**
 * Header quick-navigation search: filters {@link NavRegistry#ALL} by its
 * (locale-resolved) label as the user types, and navigates on selection.
 * Rounded, compact styling lives in {@code wams-app-search} (see layout.css).
 */
public class AppSearchBar extends ComboBox<NavRegistry.NavTarget> {

    public AppSearchBar() {
        addClassName("wams-app-search");
        setPlaceholder(I18n.t("common.layout.header.search.placeholder"));
        setClearButtonVisible(true);
        setItems(NavRegistry.ALL);
        setItemLabelGenerator(target -> I18n.t(target.labelKey()));

        setRenderer(new ComponentRenderer<>(target -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setSpacing(true);
            row.setPadding(false);
            row.setAlignItems(FlexComponent.Alignment.CENTER);

            Icon icon = target.icon().create();
            icon.setSize("14px");
            icon.addClassName("wams-app-search__item-icon");

            Span label = new Span(I18n.t(target.labelKey()));

            row.add(icon, label);
            return row;
        }));

        setPrefixComponent(VaadinIcon.SEARCH.create());

        addValueChangeListener(event -> {
            NavRegistry.NavTarget target = event.getValue();
            if (target != null) {
                UI.getCurrent().navigate(target.route());
                clear();
            }
        });
    }
}
