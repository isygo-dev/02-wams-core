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

@Route("landing")
@PageTitle("Platform - Choose Your Area")
@PermitAll
public class LandingView extends BaseMainLayout implements BeforeEnterObserver {

    private final transient UI ui;

    // Modern color palette - each module gets a unique color
    private static final String COLOR_KMS = "#6366f1";
    private static final String COLOR_IMS = "#10b981";
    private static final String COLOR_MMS = "#f59e0b";
    private static final String COLOR_DMS = "#ec4899";
    private static final String COLOR_SMS = "#64748b";
    private static final String COLOR_CMS = "#06b6d4";

    private static final String GRADIENT_KMS = "linear-gradient(135deg, #818cf8, #6366f1)";
    private static final String GRADIENT_IMS = "linear-gradient(135deg, #34d399, #10b981)";
    private static final String GRADIENT_MMS = "linear-gradient(135deg, #fbbf24, #f59e0b)";
    private static final String GRADIENT_DMS = "linear-gradient(135deg, #f472b6, #ec4899)";
    private static final String GRADIENT_SMS = "linear-gradient(135deg, #94a3b8, #64748b)";
    private static final String GRADIENT_CMS = "linear-gradient(135deg, #22d3ee, #06b6d4)";

    public LandingView() {
        this.ui = UI.getCurrent();
        setContent(buildMainContent());
        injectResponsiveStyles();
        injectAnimationStyles();
    }

    @Override
    protected String getTitle() {
        return I18n.t("common.landing.title");
    }

    @Override
    protected void createDrawer() {
        // No drawer for landing page
    }

    // ─── MAIN CONTENT ────────────────────────────────────────────────────────

    private Component buildMainContent() {
        VerticalLayout main = new VerticalLayout();
        main.setPadding(false);
        main.setSpacing(false);
        main.setSizeFull();
        main.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        main.setAlignItems(FlexComponent.Alignment.CENTER);
        main.addClassName("landing-main");
        main.getStyle()
                .set("flex", "1")
                .set("padding", "var(--lumo-space-l) var(--lumo-space-m)")
                .set("max-width", "1400px")
                .set("width", "100%")
                .set("margin", "0 auto")
                .set("min-height", "calc(100vh - 80px)")
                .set("background", "radial-gradient(ellipse at top, var(--lumo-primary-color-5pct), transparent 70%)");

        main.add(buildHero());
        main.add(buildCardsContainer());
        main.add(buildFooter());

        return main;
    }

    // ─── HERO SECTION ──────────────────────────────────────────────────────

    private Div buildHero() {
        Div hero = new Div();
        hero.addClassName("hero-section");
        hero.getStyle()
                .set("text-align", "center")
                .set("margin-bottom", "var(--lumo-space-l)")
                .set("width", "100%")
                .set("padding", "var(--lumo-space-s) 0")
                .set("position", "relative");

        // Floating decor with animations
        hero.add(createFloatingDecor(VaadinIcon.KEY, COLOR_KMS, "-80px", "10%", "0s"));
        hero.add(createFloatingDecor(VaadinIcon.CALENDAR, COLOR_CMS, "80px", "10%", "1s"));
        hero.add(createFloatingDecor(VaadinIcon.DATABASE, COLOR_SMS, "-60px", "50%", "2s"));
        hero.add(createFloatingDecor(VaadinIcon.USERS, COLOR_IMS, "70px", "50%", "0.5s"));

        // Title with typewriter effect
        Div titleContainer = new Div();
        titleContainer.addClassName("title-container");
        titleContainer.getStyle()
                .set("position", "relative")
                .set("display", "inline-block")
                .set("margin-bottom", "var(--lumo-space-xs)")
                .set("animation", "fadeInDown 0.8s ease-out");

        H1 headline = new H1();
        headline.addClassName("hero-title");
        headline.getStyle()
                .set("font-size", "clamp(2rem, 5vw, 4rem)")
                .set("font-weight", "800")
                .set("margin", "0")
                .set("line-height", "1.15")
                .set("letter-spacing", "-0.03em")
                .set("position", "relative")
                .set("z-index", "1")
                .set("color", "var(--lumo-header-text-color)");

        // Animated text with gradient underline
        Text titleText = new Text(I18n.t("common.landing.title.main"));
        headline.add(titleText);

        // Animated underline
        Div underline = new Div();
        underline.addClassName("hero-underline");
        underline.getStyle()
                .set("width", "0")
                .set("height", "3px")
                .set("background", "linear-gradient(90deg, #6366f1, #06b6d4, #10b981, #f59e0b)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("margin", "var(--lumo-space-xs) auto 0")
                .set("animation", "underlineExpand 1.2s ease-out 0.5s forwards")
                .set("background-size", "300% 100%")
                .set("animation", "underlineExpand 1.2s ease-out 0.5s forwards, shimmer 3s ease-in-out infinite 1.7s");

        // Subtitle with slide in
        Paragraph subtitle = new Paragraph(I18n.t("common.landing.subtitle"));
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.addClassName("subtitle-text");
        subtitle.getStyle()
                .set("font-size", "clamp(0.9rem, 1.2vw, 1.25rem)")
                .set("margin", "var(--lumo-space-xs) 0 0")
                .set("font-weight", "400")
                .set("letter-spacing", "0.02em")
                .set("animation", "fadeInUp 0.8s ease-out 0.6s both")
                .set("max-width", "600px")
                .set("margin-left", "auto")
                .set("margin-right", "auto");

        // Scroll indicator
        Div scrollIndicator = new Div();
        scrollIndicator.addClassName("scroll-indicator");
        scrollIndicator.getStyle()
                .set("margin-top", "var(--lumo-space-m)")
                .set("animation", "bounce 2s ease-in-out infinite 1.5s")
                .set("opacity", "0.5")
                .set("cursor", "pointer");

        Icon scrollIcon = VaadinIcon.ANGLE_DOUBLE_DOWN.create();
        scrollIcon.setSize("24px");
        scrollIcon.setColor("var(--lumo-secondary-text-color)");
        scrollIndicator.add(scrollIcon);
        scrollIndicator.addClickListener(e -> {
            UI.getCurrent().getPage().executeJs(
                    "document.querySelector('.cards-container').scrollIntoView({ behavior: 'smooth', block: 'start' });"
            );
        });

        titleContainer.add(headline);
        hero.add(titleContainer, underline, subtitle, scrollIndicator);

        return hero;
    }

    private Div createFloatingDecor(VaadinIcon icon, String color, String offsetX, String offsetY, String delay) {
        Div decor = new Div();
        decor.addClassName("floating-decor");
        decor.getStyle()
                .set("position", "absolute")
                .set("opacity", "0.08")
                .set("pointer-events", "none")
                .set("animation", "float 8s ease-in-out infinite")
                .set("animation-delay", delay);

        Icon decorIcon = icon.create();
        decorIcon.setSize("40px");
        decorIcon.setColor(color);

        decor.add(decorIcon);
        decor.getStyle()
                .set("left", offsetX)
                .set("top", offsetY);

        return decor;
    }

    // ─── CARDS CONTAINER ──────────────────────────────────────────────────

    private HorizontalLayout buildCardsContainer() {
        HorizontalLayout container = new HorizontalLayout();
        container.addClassName("cards-container");
        container.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        container.setAlignItems(FlexComponent.Alignment.STRETCH);
        container.setSpacing(true);
        container.getStyle()
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("justify-content", "center")
                .set("padding", "var(--lumo-space-s) 0");

        // All cards with staggered animations
        container.add(
                createCard("KMS", VaadinIcon.KEY, "common.landing.kms", COLOR_KMS, GRADIENT_KMS, ButtonVariant.LUMO_PRIMARY, "0.1s"),
                createCard("IMS", VaadinIcon.USERS, "common.landing.ims", COLOR_IMS, GRADIENT_IMS, ButtonVariant.LUMO_SUCCESS, "0.2s"),
                createCard("MMS", VaadinIcon.ENVELOPE, "common.landing.mms", COLOR_MMS, GRADIENT_MMS, ButtonVariant.LUMO_WARNING, "0.3s"),
                createCard("DMS", VaadinIcon.FILE_O, "common.landing.dms", COLOR_DMS, GRADIENT_DMS, ButtonVariant.LUMO_ERROR, "0.4s"),
                createCard("SMS", VaadinIcon.DATABASE, "common.landing.sms", COLOR_SMS, GRADIENT_SMS, ButtonVariant.LUMO_CONTRAST, "0.5s"),
                createCard("CMS", VaadinIcon.CALENDAR, "common.landing.cms", COLOR_CMS, GRADIENT_CMS, ButtonVariant.LUMO_PRIMARY, "0.6s")
        );

        return container;
    }

    private Div createCard(String shortName, VaadinIcon icon, String i18nPrefix,
                           String color, String gradient, ButtonVariant buttonVariant, String delay) {
        Div card = new Div();
        card.addClassName("domain-card");
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)")
                .set("padding", "var(--lumo-space-m)")
                .set("width", "180px")
                .set("min-height", "260px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("transition", "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("cursor", "pointer")
                .set("position", "relative")
                .set("overflow", "hidden")
                .set("transform", "translateY(30px) scale(0.95)")
                .set("opacity", "0")
                .set("animation", "cardFadeIn 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) " + delay + " forwards");

        // Shimmer effect on accent bar
        Div accentBar = new Div();
        accentBar.addClassName("accent-bar");
        accentBar.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0")
                .set("right", "0")
                .set("height", "3px")
                .set("background", gradient)
                .set("background-size", "200% 100%")
                .set("border-radius", "var(--lumo-border-radius-l) var(--lumo-border-radius-l) 0 0")
                .set("animation", "shimmer 3s ease-in-out infinite " + delay);

        card.add(accentBar);

        // Content
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.getStyle()
                .set("flex", "1")
                .set("padding-top", "var(--lumo-space-s)")
                .set("gap", "var(--lumo-space-xs)");

        // Icon with pulse animation
        Icon cardIcon = icon.create();
        cardIcon.setSize("28px");
        cardIcon.setColor(color);
        cardIcon.addClassName("card-icon");
        cardIcon.getStyle()
                .set("filter", "drop-shadow(0 2px 4px " + color + "33)")
                .set("margin-bottom", "var(--lumo-space-xs)")
                .set("animation", "iconPulse 3s ease-in-out infinite " + delay);

        // Badge with glow
        Div badge = new Div(shortName);
        badge.addClassName("card-badge");
        badge.getStyle()
                .set("color", color)
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("font-weight", "700")
                .set("letter-spacing", "0.06em")
                .set("text-transform", "uppercase")
                .set("background", color + "15")
                .set("padding", "2px var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("border", "1px solid " + color + "30")
                .set("transition", "all 0.3s ease");

        // Title
        H2 cardTitle = new H2(I18n.t(i18nPrefix + ".title"));
        cardTitle.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "600")
                .set("margin", "0")
                .set("text-align", "center")
                .set("color", "var(--lumo-header-text-color)")
                .set("line-height", "1.3");

        // Description
        Paragraph desc = new Paragraph(I18n.t(i18nPrefix + ".description"));
        desc.addClassName(LumoUtility.TextColor.SECONDARY);
        desc.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("text-align", "center")
                .set("margin", "0")
                .set("line-height", "1.4")
                .set("display", "-webkit-box")
                .set("-webkit-line-clamp", "2")
                .set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden")
                .set("min-height", "2.8em");

        // Features
        Div featuresContainer = new Div();
        featuresContainer.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "4px")
                .set("justify-content", "center")
                .set("margin", "var(--lumo-space-xs) 0")
                .set("width", "100%");

        String[] features = new String[]{
                I18n.t(i18nPrefix + ".feature.1"),
                I18n.t(i18nPrefix + ".feature.2")
        };

        int chipDelay = 0;
        for (String feature : features) {
            Div chip = new Div(feature);
            chip.addClassName("feature-chip");
            chip.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "var(--lumo-border-radius-s)")
                    .set("padding", "1px var(--lumo-space-s)")
                    .set("font-size", "var(--lumo-font-size-xxs)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("white-space", "nowrap")
                    .set("transition", "all 0.2s ease")
                    .set("animation", "chipFadeIn 0.4s ease-out " + (0.8 + chipDelay * 0.1) + "s both");
            featuresContainer.add(chip);
            chipDelay++;
        }

        // Enter button with hover scale
        Button enterBtn = new Button(I18n.t(i18nPrefix + ".button"), VaadinIcon.ARROW_RIGHT.create());
        enterBtn.addClassName("enter-button");

        // Apply different button styles
        if (shortName.equals("KMS")) {
            enterBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            enterBtn.getStyle().set("background", GRADIENT_KMS).set("color", "white");
        } else if (shortName.equals("IMS")) {
            enterBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        } else if (shortName.equals("MMS")) {
            enterBtn.addThemeVariants(ButtonVariant.LUMO_WARNING);
        } else if (shortName.equals("DMS")) {
            enterBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        } else if (shortName.equals("SMS")) {
            enterBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        } else if (shortName.equals("CMS")) {
            enterBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            enterBtn.getStyle()
                    .set("background", GRADIENT_CMS)
                    .set("color", "white")
                    .set("border", "none");
        }

        enterBtn.addClassName(LumoUtility.Width.FULL);
        enterBtn.getStyle()
                .set("margin-top", "auto")
                .set("font-weight", "500")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("transition", "all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)")
                .set("position", "relative")
                .set("overflow", "hidden");

        // Button hover effect - ripple
        enterBtn.addAttachListener(e -> {
            enterBtn.getElement().executeJs(
                    "this.addEventListener('mouseenter', function() {" +
                            "  this.style.transform = 'scale(1.05)';" +
                            "  this.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';" +
                            "});" +
                            "this.addEventListener('mouseleave', function() {" +
                            "  this.style.transform = 'scale(1)';" +
                            "  this.style.boxShadow = 'none';" +
                            "});"
            );
        });

        String route = i18nPrefix.substring(i18nPrefix.lastIndexOf('.') + 1);
        enterBtn.addClickListener(e -> navigateTo(route));

        // Card click with ripple effect
        card.addClickListener(e -> {
            // Ripple effect
            card.getStyle().set("transform", "scale(0.97)");
            UI.getCurrent().getPage().executeJs(
                    "setTimeout(() => { $0.style.transform = 'scale(1)'; }, 150)",
                    card.getElement()
            );
            // Create ripple
            UI.getCurrent().getPage().executeJs(
                    "const rect = $0.getBoundingClientRect();" +
                            "const ripple = document.createElement('div');" +
                            "ripple.className = 'ripple-effect';" +
                            "ripple.style.left = (event.clientX - rect.left) + 'px';" +
                            "ripple.style.top = (event.clientY - rect.top) + 'px';" +
                            "$0.appendChild(ripple);" +
                            "setTimeout(() => ripple.remove(), 600);",
                    card.getElement()
            );
            navigateTo(route);
        });

        // Card hover effects
        card.addAttachListener(e -> {
            card.getElement().executeJs(
                    "this.addEventListener('mouseenter', function() {" +
                            "  this.style.transform = 'translateY(-8px) scale(1.03)';" +
                            "  this.style.boxShadow = 'var(--lumo-box-shadow-l)';" +
                            "  this.style.borderColor = '" + color + "40';" +
                            "  this.querySelector('.card-badge').style.transform = 'scale(1.05)';" +
                            "  this.querySelector('.card-icon').style.transform = 'scale(1.1) rotate(5deg)';" +
                            "});" +
                            "this.addEventListener('mouseleave', function() {" +
                            "  this.style.transform = 'translateY(0) scale(1)';" +
                            "  this.style.boxShadow = '0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)';" +
                            "  this.style.borderColor = 'var(--lumo-contrast-10pct)';" +
                            "  this.querySelector('.card-badge').style.transform = 'scale(1)';" +
                            "  this.querySelector('.card-icon').style.transform = 'scale(1) rotate(0deg)';" +
                            "});"
            );
        });

        content.add(cardIcon, badge, cardTitle, desc, featuresContainer, enterBtn);
        card.add(content);

        return card;
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
        footer.addClassName("landing-footer");
        footer.getStyle()
                .set("padding", "var(--lumo-space-s) 0")
                .set("text-align", "center")
                .set("border-top", "1px solid var(--lumo-contrast-5pct)")
                .set("background", "transparent")
                .set("flex-shrink", "0")
                .set("margin-top", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("animation", "fadeInUp 0.8s ease-out 1s both");

        Paragraph footerText = new Paragraph(I18n.t("common.landing.footer"));
        footerText.addClassName(LumoUtility.FontSize.XXSMALL);
        footerText.addClassName(LumoUtility.TextColor.TERTIARY);
        footerText.getStyle().set("margin", "0");

        footer.add(footerText);
        return footer;
    }

    // ─── ANIMATION STYLES ──────────────────────────────────────────────────

    private void injectAnimationStyles() {
        String css = """
                /* ── Ripple Effect ── */
                .ripple-effect {
                    position: absolute;
                    border-radius: 50%;
                    background: rgba(255, 255, 255, 0.3);
                    width: 100px;
                    height: 100px;
                    margin-top: -50px;
                    margin-left: -50px;
                    animation: ripple 0.6s ease-out forwards;
                    pointer-events: none;
                }
                
                @keyframes ripple {
                    0% { transform: scale(0); opacity: 1; }
                    100% { transform: scale(4); opacity: 0; }
                }
                
                /* ── Underline Expand ── */
                @keyframes underlineExpand {
                    0% { width: 0; opacity: 0; }
                    100% { width: 140px; opacity: 1; }
                }
                
                /* ── Shimmer ── */
                @keyframes shimmer {
                    0% { background-position: -200% 0; }
                    100% { background-position: 200% 0; }
                }
                
                /* ── Card Animations ── */
                @keyframes cardFadeIn {
                    0% { 
                        opacity: 0; 
                        transform: translateY(30px) scale(0.95);
                    }
                    100% { 
                        opacity: 1; 
                        transform: translateY(0) scale(1);
                    }
                }
                
                @keyframes chipFadeIn {
                    0% { opacity: 0; transform: scale(0.8); }
                    100% { opacity: 1; transform: scale(1); }
                }
                
                @keyframes iconPulse {
                    0%, 100% { transform: scale(1); }
                    50% { transform: scale(1.08); }
                }
                
                /* ── Floating Decor ── */
                @keyframes float {
                    0%, 100% { transform: translateY(0px) rotate(0deg); }
                    25% { transform: translateY(-15px) rotate(5deg); }
                    75% { transform: translateY(10px) rotate(-5deg); }
                }
                
                /* ── Text Animations ── */
                @keyframes fadeInDown {
                    0% { opacity: 0; transform: translateY(-30px); }
                    100% { opacity: 1; transform: translateY(0); }
                }
                
                @keyframes fadeInUp {
                    0% { opacity: 0; transform: translateY(30px); }
                    100% { opacity: 1; transform: translateY(0); }
                }
                
                /* ── Scroll Indicator ── */
                @keyframes bounce {
                    0%, 100% { transform: translateY(0); }
                    50% { transform: translateY(8px); }
                }
                
                /* ── Card Hover States ── */
                .domain-card {
                    position: relative;
                    overflow: hidden;
                }
                
                .domain-card::after {
                    content: '';
                    position: absolute;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                    background: linear-gradient(135deg, transparent 50%, rgba(255,255,255,0.05) 100%);
                    pointer-events: none;
                    opacity: 0;
                    transition: opacity 0.3s ease;
                }
                
                .domain-card:hover::after {
                    opacity: 1;
                }
                
                .domain-card:active {
                    transform: scale(0.97) !important;
                }
                
                /* ── Button Enhancements ── */
                .enter-button {
                    position: relative;
                    overflow: hidden;
                }
                
                .enter-button::before {
                    content: '';
                    position: absolute;
                    top: 0;
                    left: -100%;
                    width: 100%;
                    height: 100%;
                    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
                    transition: left 0.5s ease;
                }
                
                .enter-button:hover::before {
                    left: 100%;
                }
                
                /* ── Feature Chips ── */
                .feature-chip:hover {
                    background: var(--lumo-contrast-10pct);
                    transform: translateY(-2px);
                    box-shadow: 0 2px 4px rgba(0,0,0,0.05);
                }
                
                /* ── Responsive adjustments ── */
                @media (max-width: 480px) {
                    .domain-card {
                        width: 100% !important;
                        min-height: 200px;
                        padding: var(--lumo-space-m);
                    }
                    
                    .domain-card vaadin-icon {
                        width: 24px !important;
                        height: 24px !important;
                    }
                    
                    .floating-decor {
                        display: none !important;
                    }
                    
                    .hero-section {
                        padding: var(--lumo-space-xs) 0 !important;
                        margin-bottom: var(--lumo-space-m) !important;
                    }
                    
                    .scroll-indicator {
                        display: none !important;
                    }
                }
                
                @media (min-width: 481px) and (max-width: 768px) {
                    .domain-card {
                        width: calc(50% - var(--lumo-space-m));
                        min-height: 220px;
                        flex: 0 0 calc(50% - var(--lumo-space-m));
                    }
                    
                    .floating-decor {
                        display: none !important;
                    }
                }
                
                @media (min-width: 769px) and (max-width: 1024px) {
                    .domain-card {
                        width: calc(33.33% - var(--lumo-space-m));
                        min-height: 240px;
                        flex: 0 0 calc(33.33% - var(--lumo-space-m));
                    }
                    
                    .domain-card .feature-chip {
                        font-size: var(--lumo-font-size-xxs) !important;
                    }
                }
                
                @media (min-width: 1025px) and (max-width: 1200px) {
                    .domain-card {
                        width: calc(20% - var(--lumo-space-m));
                        min-height: 260px;
                        flex: 0 0 calc(20% - var(--lumo-space-m));
                    }
                }
                
                @media (min-width: 1201px) {
                    .domain-card {
                        width: 190px;
                        min-height: 260px;
                        flex: 0 0 190px;
                    }
                }
                """;

        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    // ─── RESPONSIVE STYLES ──────────────────────────────────────────────────

    private void injectResponsiveStyles() {
        String css = """
                /* ── Reset & Base ── */
                .landing-view {
                    background: var(--lumo-shade-5pct);
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                }

                .landing-main {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                }

                /* ── Cards Container ── */
                .cards-container {
                    gap: var(--lumo-space-m);
                }

                .domain-card {
                    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                    position: relative;
                    overflow: hidden;
                }

                .domain-card:active {
                    transform: scale(0.97) !important;
                }

                .domain-card vaadin-button {
                    width: 100%;
                }

                /* ── Accessibility ── */
                .domain-card:focus-visible {
                    outline: 2px solid var(--lumo-primary-color);
                    outline-offset: 2px;
                }

                @media (prefers-reduced-motion: reduce) {
                    *, *::before, *::after {
                        animation-duration: 0.01ms !important;
                        animation-iteration-count: 1 !important;
                        transition-duration: 0.01ms !important;
                    }
                    .domain-card {
                        opacity: 1 !important;
                        transform: none !important;
                    }
                    .floating-decor {
                        display: none !important;
                    }
                    .ripple-effect {
                        display: none !important;
                    }
                    .scroll-indicator {
                        display: none !important;
                    }
                }
                """;

        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}