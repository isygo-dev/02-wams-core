package eu.isygoit.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.BaseMainLayout;
import jakarta.annotation.security.PermitAll;

/**
 * All visual styling for this view lives in {@code styles/landing.css}
 * (imported once by {@link BaseMainLayout}). Per-module colors come from the
 * {@code wams-module-<key>} classes defined in {@code styles/modules.css};
 * this class only decides which module key/route/icon/i18n-prefix apply to
 * each card, plus the small set of genuinely dynamic bits (stagger delay,
 * click-ripple position).
 */
@Route("landing")
@PageTitle("Platform - Choose Your Area")
@PermitAll
public class LandingView extends BaseMainLayout implements BeforeEnterObserver {

    private static final String[][] MODULES = {
            // shortName, moduleKey, icon
            {"KMS", "kms"},
            {"IMS", "ims"},
            {"MMS", "mms"},
            {"DMS", "dms"},
            {"SMS", "sms"},
            {"CMS", "cms"},
    };

    private final transient UI ui;

    public LandingView() {
        this.ui = UI.getCurrent();
        addClassName("wams-landing");
        setContent(buildMainContent());
    }

    @Override
    protected String getTitle() {
        return I18n.t("common.landing.title");
    }

    @Override
    protected Component createDrawerContent() {
        // No drawer for landing page
        return null;
    }

    // ─── MAIN CONTENT ────────────────────────────────────────────────────────

    private Component buildMainContent() {
        VerticalLayout main = new VerticalLayout();
        main.setPadding(false);
        main.setSpacing(false);
        main.setSizeFull();
        main.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        main.setAlignItems(FlexComponent.Alignment.CENTER);
        main.addClassName("wams-landing-main");

        main.add(buildHero());
        main.add(buildCardsContainer());
        main.add(buildFooter());

        return main;
    }

    // ─── HERO SECTION ──────────────────────────────────────────────────────

    private Div buildHero() {
        Div hero = new Div();
        hero.addClassName("wams-hero-section");

        // Floating decor
        hero.add(createFloatingDecor(VaadinIcon.KEY, "kms", "-80px", "10%", "0s"));
        hero.add(createFloatingDecor(VaadinIcon.CALENDAR, "cms", "80px", "10%", "1s"));
        hero.add(createFloatingDecor(VaadinIcon.DATABASE, "sms", "-60px", "50%", "2s"));
        hero.add(createFloatingDecor(VaadinIcon.USERS, "ims", "70px", "50%", "0.5s"));

        Div titleContainer = new Div();
        titleContainer.addClassName("wams-title-container");

        H1 headline = new H1();
        headline.addClassName("wams-hero-title");
        headline.add(new Text(I18n.t("common.landing.title.main")));

        Div underline = new Div();
        underline.addClassName("wams-hero-underline");

        Paragraph subtitle = new Paragraph(I18n.t("common.landing.subtitle"));
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.addClassName("wams-subtitle-text");

        Div scrollIndicator = new Div();
        scrollIndicator.addClassName("wams-scroll-indicator");

        Icon scrollIcon = VaadinIcon.ANGLE_DOUBLE_DOWN.create();
        scrollIcon.setSize("24px");
        scrollIcon.setColor("var(--lumo-secondary-text-color)");
        scrollIndicator.add(scrollIcon);
        scrollIndicator.addClickListener(e -> UI.getCurrent().getPage().executeJs(
                "document.querySelector('.wams-cards-container').scrollIntoView({ behavior: 'smooth', block: 'start' });"
        ));

        titleContainer.add(headline);
        hero.add(titleContainer, underline, subtitle, scrollIndicator);

        return hero;
    }

    private Div createFloatingDecor(VaadinIcon icon, String moduleKey, String offsetX, String offsetY, String delay) {
        Div decor = new Div();
        decor.addClassName("wams-floating-decor");
        decor.addClassName("wams-module-" + moduleKey);
        // Position and stagger delay are computed per-call: genuinely dynamic, kept inline.
        decor.getStyle()
                .set("left", offsetX)
                .set("top", offsetY)
                .set("--wams-delay", delay);

        Icon decorIcon = icon.create();
        decorIcon.setSize("40px");
        decor.add(decorIcon);

        return decor;
    }

    // ─── CARDS CONTAINER ──────────────────────────────────────────────────

    private HorizontalLayout buildCardsContainer() {
        HorizontalLayout container = new HorizontalLayout();
        container.addClassName("wams-cards-container");
        container.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        container.setAlignItems(FlexComponent.Alignment.STRETCH);
        container.setSpacing(true);

        for (int i = 0; i < MODULES.length; i++) {
            String shortName = MODULES[i][0];
            String moduleKey = MODULES[i][1];
            String delay = (0.1 * (i + 1)) + "s";
            container.add(createCard(shortName, moduleKey, iconFor(moduleKey), delay));
        }

        return container;
    }

    private VaadinIcon iconFor(String moduleKey) {
        return switch (moduleKey) {
            case "kms" -> VaadinIcon.KEY;
            case "ims" -> VaadinIcon.USERS;
            case "mms" -> VaadinIcon.ENVELOPE;
            case "dms" -> VaadinIcon.FILE_O;
            case "sms" -> VaadinIcon.DATABASE;
            case "cms" -> VaadinIcon.CALENDAR;
            default -> VaadinIcon.QUESTION;
        };
    }

    private Div createCard(String shortName, String moduleKey, VaadinIcon icon, String delay) {
        String i18nPrefix = "common.landing." + moduleKey;

        Div card = new Div();
        card.addClassName("wams-domain-card");
        card.addClassName("wams-module-" + moduleKey);
        // Stagger delay depends on the card's position: genuinely dynamic, kept inline.
        card.getStyle().set("--wams-delay", delay);

        Div accentBar = new Div();
        accentBar.addClassName("wams-accent-bar");
        card.add(accentBar);

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.addClassName("wams-domain-card-content");

        Icon cardIcon = icon.create();
        cardIcon.setSize("28px");
        cardIcon.addClassName("wams-card-icon");

        Div badge = new Div(shortName);
        badge.addClassName("wams-card-badge");

        H2 cardTitle = new H2(I18n.t(i18nPrefix + ".title"));
        cardTitle.addClassName("wams-card-title");

        Paragraph desc = new Paragraph(I18n.t(i18nPrefix + ".description"));
        desc.addClassName(LumoUtility.TextColor.SECONDARY);
        desc.addClassName("wams-card-description");

        Div featuresContainer = new Div();
        featuresContainer.addClassName("wams-features-container");

        String[] features = {
                I18n.t(i18nPrefix + ".feature.1"),
                I18n.t(i18nPrefix + ".feature.2")
        };
        for (String feature : features) {
            Div chip = new Div(feature);
            chip.addClassName("wams-feature-chip");
            featuresContainer.add(chip);
        }

        Button enterBtn = new Button(I18n.t(i18nPrefix + ".button"), VaadinIcon.ARROW_RIGHT.create());
        enterBtn.addClassName("wams-enter-button");
        enterBtn.addClassName("wams-enter-button--accent");
        enterBtn.addClassName(LumoUtility.Width.FULL);
        enterBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        enterBtn.addClickListener(e -> navigateTo(moduleKey));

        card.addClickListener(e -> {
            triggerRippleEffect(card, e.getClientX(), e.getClientY());
            navigateTo(moduleKey);
        });

        content.add(cardIcon, badge, cardTitle, desc, featuresContainer, enterBtn);
        card.add(content);

        return card;
    }

    /**
     * Ripple position depends on where the user clicked: this is the one
     * effect that genuinely needs a runtime JS call rather than static CSS.
     */
    private void triggerRippleEffect(Div card, double clientX, double clientY) {
        UI.getCurrent().getPage().executeJs(
                "const rect = $0.getBoundingClientRect();" +
                        "const ripple = document.createElement('div');" +
                        "ripple.className = 'wams-ripple-effect';" +
                        "ripple.style.left = ($1 - rect.left) + 'px';" +
                        "ripple.style.top = ($2 - rect.top) + 'px';" +
                        "$0.appendChild(ripple);" +
                        "setTimeout(() => ripple.remove(), 600);",
                card.getElement(), clientX, clientY
        );
    }

    private void navigateTo(String route) {
        if (ui != null) {
            ui.navigate(route);
        } else {
            UI.getCurrent().navigate(route);
        }
    }

    // ─── FOOTER ──────────────────────────────────────────────────────────────

    private Div buildFooter() {
        Div footer = new Div();
        footer.addClassName("wams-landing-footer");

        Paragraph footerText = new Paragraph(I18n.t("common.landing.footer"));
        footerText.addClassName(LumoUtility.FontSize.XXSMALL);
        footerText.addClassName(LumoUtility.TextColor.TERTIARY);
        footerText.addClassName("wams-footer-text");

        footer.add(footerText);
        return footer;
    }
}
