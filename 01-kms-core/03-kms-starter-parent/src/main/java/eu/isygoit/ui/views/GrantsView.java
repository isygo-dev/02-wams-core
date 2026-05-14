package eu.isygoit.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import eu.isygoit.ui.MainLayout;

@Route(value = "grants", layout = MainLayout.class)
@PageTitle("Grants")
public class GrantsView extends VerticalLayout {

    public GrantsView() {

        Grid<String> grid = new Grid<>(String.class);

        Button createGrant = new Button("Create Grant");
        Button revokeGrant = new Button("Revoke Grant");

        add(createGrant, revokeGrant, grid);
    }
}