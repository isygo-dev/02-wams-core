package eu.isygoit.ui.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import eu.isygoit.ui.ims.layout.ImsMainLayout;

@Route(value = "ims/tenants", layout = ImsMainLayout.class)
@PageTitle("Tenant Management")
public class TenantView extends Div {
    public TenantView() {
        setText("Tenant management (list, details, images) will be implemented here.");
    }
}