package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import eu.isygoit.i18n.I18n;

@Tag("auth-brand")
public class BrandHeader extends Component implements HasComponents {

    public BrandHeader(String title, String subtitle) {
        Avatar logo = new Avatar(I18n.t("auth.common.brand.title"));
        logo.getElement().setAttribute("aria-hidden", "true");

        H1 heading = new H1(title);
        heading.getElement().setAttribute("tabindex", "-1");
        add(logo, heading);

        if (subtitle != null && !subtitle.isBlank()) {
            add(new Paragraph(subtitle));
        }
    }

    @Override
    public void add(Component... components) {
        for (Component component : components) {
            getElement().appendChild(component.getElement());
        }
    }
}
