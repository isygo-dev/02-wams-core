package eu.isygoit.ui.views;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import eu.isygoit.ui.MainLayout;

@Route(value = "aliases", layout = MainLayout.class)
@PageTitle("Key Aliases")
public class AliasesView extends VerticalLayout {

    public AliasesView() {

        Grid<String> grid = new Grid<>(String.class);

        Button create = new Button("Create Alias");
        Button delete = new Button("Delete Alias");

        add(create, delete, grid);
    }
}