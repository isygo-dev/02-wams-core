package eu.isygoit.ui.ims.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import eu.isygoit.ui.ims.layout.ImsMainLayout;

@Route(value = "ims/applications", layout = ImsMainLayout.class)
@PageTitle("Application Management")
public class ApplicationView extends Div {
    public ApplicationView() {
        setText("Application management (list, images, parameters) will be implemented here.");
    }
}