package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Left side of the branded authentication pages: the brand, the promise of the product, three short benefits and an
 * illustration. Decorative only (the sign-in form is the {@code main} content), hidden on narrow screens where the
 * form comes first.
 */
@Tag("auth-hero")
class AuthHero extends Component implements HasComponents {

    AuthHero() {
        getElement().setAttribute("aria-label", AuthBranding.title());

        Image mark = new Image("auth/edyra-mark.svg", "");
        mark.addClassName("auth-hero__mark");
        mark.getElement().setAttribute("aria-hidden", "true");
        Span name = new Span(AuthBranding.title());
        name.addClassName("auth-hero__name");
        com.vaadin.flow.component.html.Div brand = new com.vaadin.flow.component.html.Div(mark, name);
        brand.addClassName("auth-hero__brand");

        H2 headline = new H2(AuthBranding.hero("headline"));
        headline.addClassName("auth-hero__headline");
        Paragraph subtitle = new Paragraph(AuthBranding.hero("subtitle"));
        subtitle.addClassName("auth-hero__subtitle");

        com.vaadin.flow.component.html.UnorderedList benefits = new com.vaadin.flow.component.html.UnorderedList(
                benefit(VaadinIcon.INSTITUTION, "benefit.operations"),
                benefit(VaadinIcon.CHART, "benefit.progress"),
                benefit(VaadinIcon.USERS, "benefit.families"));
        benefits.addClassName("auth-hero__benefits");

        Image illustration = new Image(AuthBranding.heroImage(), "");
        illustration.addClassName("auth-hero__illustration");
        illustration.getElement().setAttribute("aria-hidden", "true");

        add(brand, headline, subtitle, benefits, illustration);
    }

    private static com.vaadin.flow.component.html.ListItem benefit(VaadinIcon icon, String key) {
        Icon glyph = icon.create();
        glyph.getElement().setAttribute("aria-hidden", "true");
        Span label = new Span(AuthBranding.hero(key));
        com.vaadin.flow.component.html.ListItem item = new com.vaadin.flow.component.html.ListItem(glyph, label);
        item.addClassName("auth-hero__benefit");
        return item;
    }

    @Override
    public void add(Component... components) {
        for (Component component : components) {
            getElement().appendChild(component.getElement());
        }
    }
}
