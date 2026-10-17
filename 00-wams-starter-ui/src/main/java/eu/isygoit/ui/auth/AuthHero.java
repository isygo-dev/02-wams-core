package eu.isygoit.ui.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;

/**
 * Left side of the branded authentication pages: the brand, the promise of the product, three short benefits and an
 * illustration. Decorative only (the sign-in form is the {@code main} content), hidden on narrow screens where the
 * form comes first. Images come from the brand's own SVG folder (see {@link AuthBranding}); every word comes from the
 * message bundles.
 */
@Tag("auth-hero")
class AuthHero extends Component implements HasComponents {

    AuthHero() {
        getElement().setAttribute("aria-label", AuthBranding.title());

        Div brand = new Div();
        brand.addClassName("auth-hero__brand");
        Image logo = AuthBranding.image("logo.svg", "auth-hero__mark");
        if (logo != null) {
            brand.add(logo);
        }
        Span name = new Span(AuthBranding.title());
        name.addClassName("auth-hero__name");
        brand.add(name);

        H2 headline = new H2(AuthBranding.hero("headline"));
        headline.addClassName("auth-hero__headline");
        Paragraph subtitle = new Paragraph(AuthBranding.hero("subtitle"));
        subtitle.addClassName("auth-hero__subtitle");

        UnorderedList benefits = new UnorderedList(
                benefit("icon-school.svg", "benefit.operations"),
                benefit("icon-progress.svg", "benefit.progress"),
                benefit("icon-families.svg", "benefit.families"));
        benefits.addClassName("auth-hero__benefits");

        add(brand, headline, subtitle, benefits);

        Image illustration = AuthBranding.image("illustration.svg", "auth-hero__illustration");
        if (illustration != null) {
            add(illustration);
        }
    }

    private static ListItem benefit(String iconFile, String key) {
        ListItem item = new ListItem();
        item.addClassName("auth-hero__benefit");
        Image icon = AuthBranding.image(iconFile, "auth-hero__icon");
        if (icon != null) {
            item.add(icon);
        }
        item.add(new Span(AuthBranding.hero(key)));
        return item;
    }

    @Override
    public void add(Component... components) {
        for (Component component : components) {
            getElement().appendChild(component.getElement());
        }
    }
}
