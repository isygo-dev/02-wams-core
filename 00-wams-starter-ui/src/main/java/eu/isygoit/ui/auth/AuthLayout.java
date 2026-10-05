package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

@Tag("auth-layout")
public class AuthLayout extends Component {

    private final AuthCard card = new AuthCard();

    public AuthLayout() {
        getElement().setAttribute("role", "main");
        getElement().appendChild(card.getElement());
    }

    public void addToCard(Component... components) {
        card.add(components);
    }

    public void setWideLayout(boolean wide) {
        if (wide) {
            getElement().setAttribute("data-size", "wide");
        } else {
            getElement().removeAttribute("data-size");
        }
    }
}
