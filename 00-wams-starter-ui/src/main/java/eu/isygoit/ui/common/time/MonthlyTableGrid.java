package eu.isygoit.ui.common.time;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A monthly calendar grid that displays events as colored dots.
 * All behaviour and rendering is controlled via {@link MonthlyTableGridConfig}.
 *
 * @param <E> the type of a single event
 */
public class MonthlyTableGrid<E> extends VerticalLayout {

    private final MonthlyTableGridConfig<E> config;
    private final Div gridContainer = new Div();

    private YearMonth yearMonth;
    private List<E> events;

    public MonthlyTableGrid(MonthlyTableGridConfig<E> config) {
        this.config = config;
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("monthly-grid-wrapper");

        gridContainer.setWidthFull();
        gridContainer.addClassName("monthly-grid");
        add(gridContainer);
    }

    /**
     * Sets the month to display and the list of events for that month.
     */
    public void setMonth(YearMonth yearMonth, List<E> events) {
        this.yearMonth = yearMonth;
        this.events = events != null ? events : List.of();
        buildGrid();
    }

    private void buildGrid() {
        gridContainer.removeAll();

        // Header: days of week (Monday to Saturday, start from Monday)
        Div headerRow = new Div();
        headerRow.addClassName("monthly-grid-header");
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SUNDAY) continue;
            Span dayLabel = new Span(day.getDisplayName(TextStyle.SHORT, Locale.FRENCH));
            dayLabel.addClassName("monthly-grid-header-cell");
            headerRow.add(dayLabel);
        }
        gridContainer.add(headerRow);

        // Grid inner container
        Div gridInner = new Div();
        gridInner.addClassName("monthly-grid-inner");
        gridContainer.add(gridInner);

        // Calculate first day and offset
        LocalDate firstDay = yearMonth.atDay(1);
        int dayOfWeekOffset = firstDay.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        if (dayOfWeekOffset < 0) dayOfWeekOffset += 7;

        int daysInMonth = yearMonth.lengthOfMonth();
        int totalCells = ((dayOfWeekOffset + daysInMonth + 6) / 7) * 7;

        // Group events by date
        Map<LocalDate, List<E>> eventsByDate = events.stream()
                .collect(Collectors.groupingBy(config.getDateExtractor()));

        // Build cells
        for (int i = 0; i < totalCells; i++) {
            int dayNumber = i - dayOfWeekOffset + 1;
            LocalDate date = null;
            if (dayNumber >= 1 && dayNumber <= daysInMonth) {
                date = yearMonth.atDay(dayNumber);
            }
            Div cell = buildCell(date, dayNumber, eventsByDate);
            gridInner.add(cell);
        }
    }

    private Div buildCell(LocalDate date, int dayNumber, Map<LocalDate, List<E>> eventsByDate) {
        Div cell = new Div();
        cell.addClassName("monthly-grid-cell");

        if (date == null) {
            cell.addClassName("monthly-grid-cell--empty");
            return cell;
        }

        // Day number
        Span daySpan = new Span(String.valueOf(dayNumber));
        daySpan.addClassName("monthly-grid-day-number");

        // Today highlight
        if (date.equals(LocalDate.now())) {
            cell.addClassName("monthly-grid-cell--today");
        }

        // Weekend
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            cell.addClassName("monthly-grid-cell--weekend");
        }

        // Events indicators
        Div eventsContainer = new Div();
        eventsContainer.addClassName("monthly-grid-events");
        List<E> dayEvents = eventsByDate.getOrDefault(date, List.of());

        int maxDots = 3;
        int count = 0;
        for (E evt : dayEvents) {
            if (count >= maxDots) {
                Span more = new Span("+" + (dayEvents.size() - maxDots));
                more.addClassName("monthly-grid-event-more");
                eventsContainer.add(more);
                break;
            }
            // Build event dot
            Div dot = new Div();
            dot.addClassName("monthly-grid-event-dot");
            String color = config.getColorExtractor().apply(evt);
            dot.getStyle().set("background-color", color != null ? color : "#1976D2");
            dot.getElement().setAttribute("title", config.getTitleExtractor().apply(evt));

            // Click on dot triggers event click
            if (config.getOnEventClick() != null) {
                dot.addClickListener(e -> config.getOnEventClick().accept(evt));
            }

            // Context menu for event (optional delete)
            if (config.getOnEventDelete() != null) {
                MenuBar menuBar = new MenuBar();
                menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE, MenuBarVariant.LUMO_ICON, MenuBarVariant.LUMO_SMALL);
                menuBar.addClassName("monthly-grid-event-menu");
                MenuItem rootItem = menuBar.addItem(new Icon(VaadinIcon.ELLIPSIS_DOTS_V));
                SubMenu subMenu = rootItem.getSubMenu();
                subMenu.addItem("Supprimer", e -> config.getOnEventDelete().accept(evt));
                // Prevent click propagation to the dot
                menuBar.getElement().executeJs("this.addEventListener('click', (e) => e.stopPropagation());");
                dot.add(menuBar);
            }

            // Custom populator (optional)
            config.getEventDotPopulator().accept(dot, evt);

            eventsContainer.add(dot);
            count++;
        }

        // Click on cell opens day view
        if (config.getOnDayClick() != null) {
            cell.addClickListener(e -> config.getOnDayClick().accept(date));
        }

        cell.add(daySpan, eventsContainer);
        return cell;
    }
}