package eu.isygoit.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import eu.isygoit.ui.MainLayout;

@Route(value = "policies", layout = MainLayout.class)
@PageTitle("Key Policies")
public class PoliciesView extends VerticalLayout {

    public PoliciesView() {

        TextArea policyEditor = new TextArea("Policy JSON");

        Button save = new Button("Save Policy");
        Button load = new Button("Load Policy");

        add(policyEditor, save, load);
    }
}