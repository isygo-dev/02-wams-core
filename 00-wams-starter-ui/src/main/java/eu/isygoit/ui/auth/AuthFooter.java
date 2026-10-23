package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Paragraph;

@Tag("auth-footer")
public class AuthFooter extends Component implements HasComponents {

    public AuthFooter() {
        add(new Paragraph(AuthBranding.text("auth.common.footer")));
    }

    @Override
    public void add(Component... components) {
        for (Component component : components) {
            getElement().appendChild(component.getElement());
        }
    }
}
