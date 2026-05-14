package eu.isygoit.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

@RouteAlias(value = "", layout = MainLayout.class)
@Route(value = "home", layout = MainLayout.class)
@PageTitle("KMS Dashboard")
public class MainView extends VerticalLayout {

    public MainView() {

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(buildHeader());
        add(buildStats());
        add(buildQuickLinks());
    }

    private H2 buildHeader() {
        H2 title = new H2("Key Management Service Dashboard");
        title.getStyle().set("margin-bottom", "10px");
        return title;
    }

    private HorizontalLayout buildStats() {

        HorizontalLayout stats = new HorizontalLayout();
        stats.setWidthFull();
        stats.setSpacing(true);

        stats.add(
                statCard("Keys", "128"),
                statCard("Active Keys", "102"),
                statCard("Grants", "56"),
                statCard("Custom Stores", "4")
        );

        return stats;
    }

    private VerticalLayout statCard(String label, String value) {

        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.setWidth("200px");

        card.getStyle()
                .set("border", "1px solid #ddd")
                .set("border-radius", "10px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)")
                .set("align-items", "center");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "24px")
                .set("font-weight", "bold");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("color", "#666");

        card.add(valueSpan, labelSpan);
        card.setAlignItems(FlexComponent.Alignment.CENTER);

        return card;
    }

    private VerticalLayout buildQuickLinks() {

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);

        H2 title = new H2("Quick Actions");

        Span actions = new Span(
                "• Create Key\n" +
                        "• Encrypt / Decrypt\n" +
                        "• Manage Aliases\n" +
                        "• Configure Policies\n" +
                        "• Manage Grants"
        );

        layout.add(title, actions);

        return layout;
    }
}