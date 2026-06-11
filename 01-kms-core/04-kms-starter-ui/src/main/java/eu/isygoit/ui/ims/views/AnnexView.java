package eu.isygoit.ui.ims.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import eu.isygoit.ui.layout.ImsMainLayout;

@Route(value = "ims/annexes", layout = ImsMainLayout.class)
@PageTitle("Annex Management")
public class AnnexView extends Div {
    public AnnexView() {
        setText("Annex management (list, references) will be implemented here.");
    }
}