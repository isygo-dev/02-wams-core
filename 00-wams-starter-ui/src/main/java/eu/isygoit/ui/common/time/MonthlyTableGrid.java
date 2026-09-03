package eu.isygoit.ui.common.time;

import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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

    public void setMonth(YearMonth yearMonth, List<E> events) {
        this.yearMonth = yearMonth;
        this.events = events != null ? events : List.of();
        buildGrid();
    }

    private void buildGrid() {
        gridContainer.removeAll();

        // Header
        Div headerRow = new Div();
        headerRow.addClassName("monthly-grid-header");
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SUNDAY) continue;
            Span dayLabel = new Span(day.getDisplayName(TextStyle.SHORT, Locale.FRENCH));
            dayLabel.addClassName("monthly-grid-header-cell");
            headerRow.add(dayLabel);
        }
        gridContainer.add(headerRow);

        // Grid inner
        Div gridInner = new Div();
        gridInner.addClassName("monthly-grid-inner");
        gridContainer.add(gridInner);

        LocalDate firstDay = yearMonth.atDay(1);
        int dayOfWeekOffset = firstDay.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        if (dayOfWeekOffset < 0) dayOfWeekOffset += 7;

        int daysInMonth = yearMonth.lengthOfMonth();
        int totalCells = ((dayOfWeekOffset + daysInMonth + 6) / 7) * 7;

        Map<LocalDate, List<E>> eventsByDate = events.stream()
                .collect(Collectors.groupingBy(config.getDateExtractor()));

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

        Span daySpan = new Span(String.valueOf(dayNumber));
        daySpan.addClassName("monthly-grid-day-number");

        if (date.equals(LocalDate.now())) {
            cell.addClassName("monthly-grid-cell--today");
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            cell.addClassName("monthly-grid-cell--weekend");
        }

        // ─── Enhanced Events Container ────────────────────────────────
        Div eventsContainer = new Div();
        eventsContainer.addClassName("monthly-grid-events");
        List<E> dayEvents = eventsByDate.getOrDefault(date, List.of());

        int maxDots = 4; // Increased from 3 to 4
        int count = 0;
        for (E evt : dayEvents) {
            if (count >= maxDots) {
                Span more = new Span("+" + (dayEvents.size() - maxDots));
                more.addClassName("monthly-grid-event-more");
                eventsContainer.add(more);
                break;
            }
            // ─── Enhanced Event Dot ────────────────────────────────────
            Div dot = buildEnhancedEventDot(evt);
            eventsContainer.add(dot);
            count++;
        }

        if (config.getOnDayClick() != null) {
            cell.addClickListener(e -> config.getOnDayClick().accept(date));
        }

        cell.add(daySpan, eventsContainer);
        return cell;
    }

    /**
     * Builds an enhanced event dot with better visibility:
     * - Larger size (16px)
     * - White border for contrast
     * - Box-shadow for depth
     * - Tooltip with event details
     * - Clickable to open event
     */
    private Div buildEnhancedEventDot(E evt) {
        Div dot = new Div();
        dot.addClassName("monthly-grid-event-dot-enhanced");

        // Color
        String color = config.getColorExtractor().apply(evt);
        dot.getStyle().set("background-color", color != null ? color : "#1976D2");

        // Tooltip with full event details
        String tooltip = buildTooltipText(evt);
        dot.getElement().setAttribute("title", tooltip);

        // Click on dot triggers event click
        if (config.getOnEventClick() != null) {
            dot.addClickListener(e -> config.getOnEventClick().accept(evt));
        }

        // Context menu for delete (if configured)
        if (config.getOnEventDelete() != null) {
            MenuBar menuBar = new MenuBar();
            menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE,
                    MenuBarVariant.LUMO_ICON, MenuBarVariant.LUMO_SMALL);
            menuBar.addClassName("monthly-grid-event-menu");
            MenuItem rootItem = menuBar.addItem(new Icon(VaadinIcon.ELLIPSIS_DOTS_V));
            SubMenu subMenu = rootItem.getSubMenu();
            subMenu.addItem("Supprimer", e -> config.getOnEventDelete().accept(evt));
            menuBar.getElement().executeJs("this.addEventListener('click', (e) => e.stopPropagation());");
            dot.add(menuBar);
        }

        // Custom populator (optional)
        config.getEventDotPopulator().accept(dot, evt);

        return dot;
    }

    /**
     * Builds a rich tooltip text for the event dot.
     */
    private String buildTooltipText(E evt) {
        StringBuilder sb = new StringBuilder();
        String title = config.getTitleExtractor().apply(evt);
        String desc = config.getDescriptionExtractor().apply(evt);
        LocalDate date = config.getDateExtractor().apply(evt);

        sb.append(title != null ? title : "Event");
        if (desc != null && !desc.isBlank()) {
            sb.append("\n").append(desc);
        }
        if (date != null) {
            sb.append("\n").append(date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        return sb.toString();
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Nothing to do
    }
}