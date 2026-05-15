package eu.isygoit.ui.views;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import eu.isygoit.ui.MainLayout;

@Route(value = "byok", layout = MainLayout.class)
@PageTitle("BYOK")
public class ByokView extends VerticalLayout {

    public ByokView() {

        TextArea keyMaterial = new TextArea("Key Material (Base64)");

        Button importKey = new Button("Import Key");
        Button generateParams = new Button("Get Import Parameters");

        add(keyMaterial, importKey, generateParams);
    }
}
