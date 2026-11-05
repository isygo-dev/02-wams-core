package eu.isygoit.ui.cms.views.vcalendar.calendar;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.ui.common.time.MonthlyTableGrid;
import eu.isygoit.ui.common.time.MonthlyTableGridConfig;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Consumer;

public class MonthlyCalendarView extends VerticalLayout {

    private final MonthlyTableGrid<CmsCalendarEventSlot> grid;
    private List<CmsCalendarEventSlot> events = List.of();

    public MonthlyCalendarView(Consumer<LocalDate> onDayClick) {
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        MonthlyTableGridConfig<CmsCalendarEventSlot> config =
                MonthlyTableGridConfig.<CmsCalendarEventSlot>builder()
                        .withDateExtractor(event -> event.getStartTime().toLocalDate())
                        .withStartTimeExtractor(event -> event.getStartTime().toLocalTime())
                        .withEndTimeExtractor(CmsCalendarEventSlot::gridEndTime)
                        .withTitleExtractor(CmsCalendarEventSlot::getTitle)
                        .withColorExtractor(CmsCalendarEventSlot::color)
                        .withTooltipExtractor(CmsCalendarEventSlot::tooltip)
                        .withIsEditable(event -> false)
                        .withOnDayClick(onDayClick)
                        .build();

        grid = new MonthlyTableGrid<>(config);
        grid.setWidthFull();
        add(grid);
    }

    public void setEvents(List<CmsCalendarEventSlot> events) {
        this.events = events == null ? List.of() : List.copyOf(events);
    }

    public void setMonth(YearMonth month) {
        YearMonth target = month == null ? YearMonth.now() : month;
        grid.setMonth(target, events.stream()
                .filter(event -> event.getStartTime() != null)
                .filter(event -> YearMonth.from(event.getStartTime()).equals(target))
                .toList());
    }
}
