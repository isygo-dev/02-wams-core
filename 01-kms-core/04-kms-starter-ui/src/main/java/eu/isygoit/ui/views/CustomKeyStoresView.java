package eu.isygoit.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import eu.isygoit.ui.MainLayout;

@Route(value = "custom-key-stores", layout = MainLayout.class)
@PageTitle("Custom Key Stores")
public class CustomKeyStoresView extends VerticalLayout {

    public CustomKeyStoresView() {

        Grid<String> grid = new Grid<>(String.class);

        Button create = new Button("Create Store");
        Button connect = new Button("Connect");
        Button disconnect = new Button("Disconnect");

        add(create, connect, disconnect, grid);
    }
}
