package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;

import java.util.function.Function;

public final class ManagementGridSupport {

    private ManagementGridSupport() {
    }

    public static <T> Grid.Column<T> addCardActionsColumn(Grid<T> grid,
                                                           Function<T, Component> cardRenderer) {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.addClassName("management-grid");
        return grid.addComponentColumn(item -> {
            Div cell = new Div();
            cell.addClassName("management-grid__actions");
            Component actionBar = findByClass(cardRenderer.apply(item), "wams-card__button-bar");
            if (actionBar != null) {
                actionBar.getElement().removeFromParent();
                cell.add(actionBar);
            }
            return cell;
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);
    }

    private static Component findByClass(Component root, String cssClass) {
        if (root.getElement().getClassList().contains(cssClass)) {
            return root;
        }
        for (Component child : root.getChildren().toList()) {
            Component found = findByClass(child, cssClass);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
