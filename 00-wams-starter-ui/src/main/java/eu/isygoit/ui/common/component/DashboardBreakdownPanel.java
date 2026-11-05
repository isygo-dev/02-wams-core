package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.i18n.I18n;

import java.util.List;
import java.util.Objects;

/**
 * Responsive, data-backed bar chart for module dashboards.
 */
@CssImport("./styles/scss/common.scss")
public class DashboardBreakdownPanel extends VerticalLayout {

    private final Div chart = new Div();
    private final Span error = new Span();
    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final ProgressBar loadingBar = new ProgressBar();
    private Runnable refreshAction;

    public DashboardBreakdownPanel(String title) {
        setSpacing(false);
        setPadding(true);
        setWidthFull();
        addClassName("wams-dashboard-breakdown");

        H3 heading = new H3(title);
        heading.addClassName("wams-dashboard-breakdown__title");
        HorizontalLayout actions = new HorizontalLayout(refreshButton, loadingBar);
        actions.setSpacing(false);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);
        actions.addClassName("wams-dashboard-breakdown__actions");
        HorizontalLayout header = new HorizontalLayout(heading, actions);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.addClassName("wams-dashboard-breakdown__header");

        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("common.stats.refresh.button"));
        refreshButton.setAriaLabel(I18n.t("common.stats.refresh.button"));
        refreshButton.setEnabled(false);
        refreshButton.addClickListener(event -> {
            if (refreshAction != null) {
                refreshAction.run();
            }
        });
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.addClassName("wams-dashboard-breakdown__loader");

        chart.addClassName("wams-dashboard-breakdown__chart");
        error.addClassName("wams-dashboard-breakdown__error");
        error.setVisible(false);
        add(header, chart, error);
    }

    public void setRefreshAction(Runnable action) {
        refreshAction = Objects.requireNonNull(action);
        refreshButton.setEnabled(true);
    }

    public void setLoading(boolean loading) {
        loadingBar.setVisible(loading);
        refreshButton.setEnabled(!loading);
    }

    public void setItems(List<Item> items) {
        chart.removeAll();
        error.setVisible(false);
        setLoading(false);
        long maxValue = items.stream().mapToLong(Item::value).max().orElse(0);

        for (Item item : items) {
            Div row = new Div();
            row.addClassName("wams-dashboard-breakdown__row");

            Span label = new Span(item.label());
            label.addClassName("wams-dashboard-breakdown__label");
            Span value = new Span(String.valueOf(item.value()));
            value.addClassName("wams-dashboard-breakdown__value");

            ProgressBar bar = new ProgressBar();
            bar.addClassName("wams-dashboard-breakdown__progress");
            bar.setWidthFull();
            bar.setValue(maxValue > 0 ? (double) item.value() / maxValue : 0);
            bar.getElement().setAttribute("aria-label", item.label());

            row.add(label, value, bar);
            chart.add(row);
        }
    }

    public void setError(String message) {
        chart.removeAll();
        error.setText(message);
        error.setVisible(true);
        setLoading(false);
    }

    public record Item(String label, long value) {
        public Item {
            if (value < 0) {
                throw new IllegalArgumentException("Dashboard chart values cannot be negative");
            }
        }
    }
}
