package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import eu.isygoit.ui.common.component.LanguageSelectorComponent;

/**
 * Top-right corner of the authentication pages (top-left in right-to-left languages): the language selector, so a
 * user can switch language before signing in.
 */
@Tag("auth-toolbar")
class AuthToolbar extends Component implements HasComponents {

    AuthToolbar() {
        add(new LanguageSelectorComponent());
    }

    @Override
    public void add(Component... components) {
        for (Component component : components) {
            getElement().appendChild(component.getElement());
        }
    }
}
