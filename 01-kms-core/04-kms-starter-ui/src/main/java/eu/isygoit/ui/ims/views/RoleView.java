package eu.isygoit.ui.ims.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import eu.isygoit.ui.layout.ImsMainLayout;

@Route(value = "ims/roles", layout = ImsMainLayout.class)
@PageTitle("Role Management")
public class RoleView extends Div {
    public RoleView() {
        setText("Role and permission management will be implemented here.");
    }
}