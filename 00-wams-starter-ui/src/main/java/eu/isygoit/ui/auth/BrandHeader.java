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
        Component logo;
        if (AuthBranding.isBranded()) {
            // the brand logo and wordmark (the hero is hidden on narrow screens, the card must carry the brand)
            com.vaadin.flow.component.html.Image mark = new com.vaadin.flow.component.html.Image("auth/edyra-mark.svg", "");
            mark.addClassName("auth-brand__mark");
            com.vaadin.flow.component.html.Span name = new com.vaadin.flow.component.html.Span(AuthBranding.title());
            name.addClassName("auth-brand__name");
            logo = new com.vaadin.flow.component.html.Div(mark, name);
            logo.getElement().getClassList().add("auth-brand__lockup");
        } else {
            logo = new Avatar(I18n.t("auth.common.brand.title"));
        }
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
