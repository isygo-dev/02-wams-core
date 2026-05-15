package eu.isygoit.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import eu.isygoit.ui.MainLayout;

@Route(value = "keys", layout = MainLayout.class)
@PageTitle("Key Management")
public class KeyManagementView extends VerticalLayout {

    private final Grid<String> grid = new Grid<>(String.class);

    public KeyManagementView() {

        TextField search = new TextField("Search Key");
        Button create = new Button("Create Key");

        grid.setSizeFull();

        add(search, create, grid);
    }
}