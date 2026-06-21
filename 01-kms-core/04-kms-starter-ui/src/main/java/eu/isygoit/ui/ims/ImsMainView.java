package eu.isygoit.ui.ims;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.remote.ims.*;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Dashboard view for IMS, displaying key statistics and quick actions.
 * Accessible at "/ims" and "/ims/home".
 */
@RouteAlias(value = "ims/home", layout = ImsMainLayout.class)
@VaadinSessionScope //(or UIScope)
@Route(value = "ims", layout = ImsMainLayout.class)
@PageTitle("IMS Dashboard")
public class ImsMainView extends VerticalLayout implements BeforeEnterObserver {

    private final AccountService accountService;
    private final TenantService tenantService;
    private final ApplicationService applicationService;
    private final CustomerService customerService;
    private final RoleInfoService roleInfoService;
    private final UI ui;

    @Autowired
    public ImsMainView(AccountService accountService,
                       TenantService tenantService,
                       ApplicationService applicationService,
                       CustomerService customerService,
                       RoleInfoService roleInfoService) {
        this.accountService = accountService;
        this.tenantService = tenantService;
        this.applicationService = applicationService;
        this.customerService = customerService;
        this.roleInfoService = roleInfoService;
        this.ui = UI.getCurrent();

        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("ims-dashboard");

        add(buildHeader());
        add(buildStatisticsRow());
        add(buildRecentActivityPanel());
        add(buildQuickActions());

        injectResponsiveStyles();
    }

    private H2 buildHeader() {
        H2 title = new H2("Identity Management System Dashboard");
        title.getStyle().set("margin-bottom", "10px");
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        return title;
    }

    private HorizontalLayout buildStatisticsRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(true);
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);
        row.addClassName("stats-row");

        row.add(createStatCard("Total Accounts", "0", VaadinIcon.USER, "accounts-link"));
        row.add(createStatCard("Total Tenants", "0", VaadinIcon.BUILDING, "tenants-link"));
        row.add(createStatCard("Total Applications", "0", VaadinIcon.PAPERCLIP, "apps-link"));
        row.add(createStatCard("Total Customers", "0", VaadinIcon.GROUP, "customers-link"));
        row.add(createStatCard("Total Roles", "0", VaadinIcon.SHIELD, "roles-link"));

        return row;
    }

    private Div createStatCard(String title, String value, VaadinIcon icon, String navigateTo) {
        Div card = new Div();
        card.addClassName("stat-card");
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("padding", "var(--lumo-space-m)")
                .set("text-align", "center")
                .set("flex", "1")
                .set("cursor", "pointer");

        Icon iconComponent = icon.create();
        iconComponent.setSize("32px");
        iconComponent.setColor("var(--lumo-primary-color)");

        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontSize.SMALL);
        titleSpan.addClassName(LumoUtility.TextColor.SECONDARY);

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.XXXLARGE);
        valueSpan.addClassName(LumoUtility.FontWeight.BOLD);
        valueSpan.setId(title.toLowerCase().replace(" ", "-") + "-value");

        card.add(iconComponent, titleSpan, valueSpan);
        card.addClickListener(e -> ui.navigate(navigateTo));
        return card;
    }

    private VerticalLayout buildRecentActivityPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.getStyle().set("margin-top", "24px");
        H2 title = new H2("Recent Activity");
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        Div content = new Div();
        content.setText("Latest account creations, tenant updates, and system events will appear here.");
        content.getStyle().set("padding", "var(--lumo-space-m)")
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)");
        panel.add(title, content);
        return panel;
    }

    private VerticalLayout buildQuickActions() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.getStyle().set("gap", "10px").set("margin-top", "24px");
        H2 title = new H2("Quick Actions");
        title.addClassName(LumoUtility.FontSize.MEDIUM);
        Div actions = new Div();
        actions.add(new Span("• Create Account\n• Add Tenant\n• Register Application\n• Add Customer\n• Assign Role"));
        actions.getStyle().set("white-space", "pre-line");
        layout.add(title, actions);
        return layout;
    }

    private void updateStatValue(String elementId, String value) {
        ui.access(() -> ui.getPage().executeJs(
                "const el = document.getElementById($0); if(el) el.innerText = $1;", elementId, value
        ));
    }

    private void injectResponsiveStyles() {
        String css = """
                .ims-dashboard .stat-card {
                    transition: transform 0.2s ease, box-shadow 0.2s ease;
                }
                .ims-dashboard .stat-card:hover {
                    transform: translateY(-4px);
                    box-shadow: var(--lumo-box-shadow-m);
                }
                .ims-dashboard .stats-row {
                    flex-wrap: wrap;
                }
                @media (max-width: 768px) {
                    .ims-dashboard .stat-card {
                        flex-basis: calc(50% - 16px);
                        margin-bottom: 16px;
                    }
                    .ims-dashboard .stats-row {
                        gap: 16px;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    // Helper Span
    private static class Span extends com.vaadin.flow.component.html.Span {
        public Span(String text) {
            super(text);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("user") == null) {
            // User is not authenticated – redirect to login, passing the current URL as a redirect parameter.
            String currentPath = event.getLocation().getPath();
            event.forwardTo("login?redirect=" + currentPath);
        }
    }
}