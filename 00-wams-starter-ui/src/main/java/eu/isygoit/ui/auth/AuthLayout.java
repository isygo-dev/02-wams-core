package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

@Tag("auth-layout")
public class AuthLayout extends Component {

    private final AuthCard card = new AuthCard();

    public AuthLayout() {
        getElement().setAttribute("role", "main");
        getElement().setAttribute("data-variant", AuthBranding.variant());
        if (AuthBranding.isBranded()) {
            // images of the brand reach the stylesheet as custom properties: the theme names no module's file
            String background = AuthBranding.asset("background.svg");
            if (background != null) {
                getElement().getStyle().set("--auth-brand-background", "url(\"" + background + "\")");
            }
            String input = AuthBranding.asset("input-field.svg");
            if (input != null) {
                getElement().getStyle().set("--auth-brand-input", "url(\"" + input + "\")");
            }
            getElement().appendChild(new AuthHero().getElement());
        }
        getElement().appendChild(new AuthToolbar().getElement());
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
