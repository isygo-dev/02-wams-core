package eu.isygoit.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import eu.isygoit.ui.MainLayout;

@Route(value = "tags", layout = MainLayout.class)
@PageTitle("Key Tagging")
public class TagsView extends VerticalLayout {

    public TagsView() {

        Grid<String> grid = new Grid<>(String.class);

        Button addTag = new Button("Add Tag");
        Button removeTag = new Button("Remove Tag");

        add(addTag, removeTag, grid);
    }
}
