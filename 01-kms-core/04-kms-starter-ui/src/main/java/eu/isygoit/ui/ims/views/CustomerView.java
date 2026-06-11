package eu.isygoit.ui.ims.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import eu.isygoit.ui.ims.layout.ImsMainLayout;

@Route(value = "ims/customers", layout = ImsMainLayout.class)
@PageTitle("Customer Management")
public class CustomerView extends Div {
    public CustomerView() {
        setText("Customer management (list, images) will be implemented here.");
    }
}