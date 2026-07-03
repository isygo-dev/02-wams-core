package eu.isygoit.ui.mms;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.mms.layout.MmsMainLayout;
import eu.isygoit.ui.mms.views.dashboard.EmailStatisticsPanel;
import eu.isygoit.ui.mms.views.dashboard.SenderConfigPanel;
import eu.isygoit.ui.mms.views.dashboard.TemplateStatisticsPanel;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

@RouteAlias(value = "mms/home", layout = MmsMainLayout.class)
@UIScope
@Route(value = "mms", layout = MmsMainLayout.class)
@PageTitle("MMS Dashboard")
@PermitAll
public class MmsMainView extends ManagementVerticalView {

    private final EmailStatisticsPanel emailStatsPanel;
    private final TemplateStatisticsPanel templateStatsPanel;
    private final SenderConfigPanel senderConfigPanel;

    @Autowired
    public MmsMainView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("mms-dashboard");

        add(buildHeader());
        add(buildStatsOverview());
        emailStatsPanel = new EmailStatisticsPanel();
        add(emailStatsPanel);
        templateStatsPanel = new TemplateStatisticsPanel();
        add(templateStatsPanel);
        senderConfigPanel = new SenderConfigPanel();
        add(senderConfigPanel);
        add(buildQuickLinks());

        injectResponsiveStyles();
    }

    private H2 buildHeader() {
        H2 title = new H2(I18n.t("mms.dashboard.title"));
        title.getStyle().set("margin-bottom", "10px");
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        return title;
    }

    private HorizontalLayout buildStatsOverview() {
        HorizontalLayout stats = new HorizontalLayout();
        stats.setWidthFull();
        stats.setSpacing(true);
        stats.addClassName("stats-overview");

        stats.add(createStatCard(VaadinIcon.MAILBOX, "mms.dashboard.senders", "2", I18n.t("mms.dashboard.senders.subtitle")));
        stats.add(createStatCard(VaadinIcon.FILE_TEXT, "mms.dashboard.templates", "5", I18n.t("mms.dashboard.templates.subtitle")));
        stats.add(createStatCard(VaadinIcon.ENVELOPE, "mms.dashboard.emails.sent", "1,234", I18n.t("mms.dashboard.emails.sent.subtitle")));
        stats.add(createStatCard(VaadinIcon.INFO_CIRCLE, "mms.dashboard.queued", "12", I18n.t("mms.dashboard.queued.subtitle")));

        return stats;
    }

    private VerticalLayout createStatCard(VaadinIcon icon, String titleKey, String value, String subtitle) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("stat-card");
        card.setSpacing(false);
        card.setPadding(true);

        Icon iconComponent = icon.create();
        iconComponent.setSize("32px");
        iconComponent.getStyle().set("color", "var(--lumo-primary-color)");

        H2 valueLabel = new H2(value);
        valueLabel.addClassName(LumoUtility.FontSize.XLARGE);
        valueLabel.addClassName(LumoUtility.Margin.NONE);

        Paragraph titleLabel = new Paragraph(I18n.t(titleKey));
        titleLabel.addClassName(LumoUtility.TextColor.SECONDARY);
        titleLabel.addClassName(LumoUtility.FontSize.SMALL);
        titleLabel.addClassName(LumoUtility.Margin.NONE);

        Paragraph subtitleLabel = new Paragraph(subtitle);
        subtitleLabel.addClassName(LumoUtility.TextColor.TERTIARY);
        subtitleLabel.addClassName(LumoUtility.FontSize.XSMALL);
        subtitleLabel.addClassName(LumoUtility.Margin.NONE);

        card.add(iconComponent, valueLabel, titleLabel, subtitleLabel);
        card.setAlignItems(Alignment.START);

        return card;
    }

    private VerticalLayout buildQuickLinks() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.getStyle().set("gap", "10px").set("margin-top", "24px");

        H2 title = new H2(I18n.t("mms.dashboard.quick.actions"));
        title.addClassName(LumoUtility.FontSize.MEDIUM);

        VerticalLayout actions = new VerticalLayout();
        actions.setSpacing(false);
        actions.add(
                createQuickLink(VaadinIcon.PLUS_CIRCLE, "mms.dashboard.quick.create.sender"),
                createQuickLink(VaadinIcon.PLUS_CIRCLE, "mms.dashboard.quick.create.template"),
                createQuickLink(VaadinIcon.START_COG, "mms.dashboard.quick.compose.email"),
                createQuickLink(VaadinIcon.COG, "mms.dashboard.quick.settings")
        );

        layout.add(title, actions);
        return layout;
    }

    private HorizontalLayout createQuickLink(VaadinIcon icon, String labelKey) {
        HorizontalLayout link = new HorizontalLayout();
        link.setSpacing(true);
        link.setAlignItems(Alignment.CENTER);
        link.addClassName("quick-link");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-primary-color)");

        Paragraph label = new Paragraph(I18n.t(labelKey));
        label.addClassName(LumoUtility.FontSize.SMALL);
        label.addClassName(LumoUtility.Margin.NONE);

        link.add(iconComponent, label);
        link.getStyle().set("cursor", "pointer");
        link.addClickListener(e -> {
            // Navigate to appropriate view
        });

        return link;
    }

    private void injectResponsiveStyles() {
        String css = """
                .mms-dashboard .stat-card {
                    background: var(--lumo-base-color);
                    padding: var(--lumo-space-m);
                    border-radius: var(--lumo-border-radius-l);
                    box-shadow: var(--lumo-box-shadow-xs);
                    transition: transform 0.2s ease, box-shadow 0.2s ease;
                    flex: 1;
                    min-width: 150px;
                }
                .mms-dashboard .stat-card:hover {
                    transform: translateY(-4px);
                    box-shadow: var(--lumo-box-shadow-m);
                }
                .mms-dashboard .stats-overview {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-m);
                    margin-bottom: var(--lumo-space-l);
                }
                .mms-dashboard .quick-link {
                    padding: var(--lumo-space-xs) var(--lumo-space-s);
                    border-radius: var(--lumo-border-radius-m);
                    transition: background 0.2s;
                }
                .mms-dashboard .quick-link:hover {
                    background: var(--lumo-primary-color-10pct);
                }
                @media (max-width: 768px) {
                    .mms-dashboard .stat-card {
                        flex-basis: calc(50% - 16px);
                    }
                    .mms-dashboard .stats-overview {
                        flex-direction: column;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}