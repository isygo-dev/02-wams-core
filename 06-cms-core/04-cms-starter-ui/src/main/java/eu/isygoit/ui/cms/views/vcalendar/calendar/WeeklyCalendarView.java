package eu.isygoit.ui.cms.views.vcalendar.calendar;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.ui.common.time.WeeklyTableGrid;
import eu.isygoit.ui.common.time.WeeklyTableGridConfig;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class WeeklyCalendarView extends VerticalLayout {

    private final WeeklyTableGrid<CmsCalendarEventSlot> grid;
    private List<CmsCalendarEventSlot> events = List.of();

    public WeeklyCalendarView() {
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("weekly-view");

        WeeklyTableGridConfig<CmsCalendarEventSlot> config =
                WeeklyTableGridConfig.<CmsCalendarEventSlot>builder()
                        .withDayOfWeekExtractor(CmsCalendarEventSlot::getDayOfWeek)
                        .withDateExtractor(event -> event.getStartTime().toLocalDate())
                        .withStartTimeExtractor(event -> event.getStartTime().toLocalTime())
                        .withEndTimeExtractor(CmsCalendarEventSlot::gridEndTime)
                        .withTitleExtractor(CmsCalendarEventSlot::getTitle)
                        .withLocationExtractor(CmsCalendarEventSlot::getLocation)
                        .withCategoryKeyExtractor(CmsCalendarEventSlot::getStatus)
                        .withColorExtractor(CmsCalendarEventSlot::color)
                        .withTooltipExtractor(CmsCalendarEventSlot::tooltip)
                        .withIsEditable(event -> false)
                        .withSlotContentPopulator((component, event) -> {
                            if (component instanceof VerticalLayout content) {
                                Span title = new Span(event.getTitle());
                                title.addClassName("timetable-slot-title");
                                content.add(title);
                            }
                        })
                        .withDaysOfWeek(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
                        .withStartHour(7)
                        .withEndHour(20)
                        .withStepMinutes(5)
                        .withShowNowIndicator(true)
                        .build();

        grid = new WeeklyTableGrid<>(config);
        grid.setWidthFull();
        add(grid);
    }

    public void setEvents(List<CmsCalendarEventSlot> events) {
        this.events = events == null ? List.of() : List.copyOf(events);
    }

    public void setWeek(LocalDate date) {
        LocalDate start = (date == null ? LocalDate.now() : date).with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);
        grid.setWeekStartDate(start);
        grid.setItems(events.stream()
                .filter(event -> event.getStartTime() != null)
                .filter(event -> {
                    LocalDate eventDate = event.getStartTime().toLocalDate();
                    return !eventDate.isBefore(start) && !eventDate.isAfter(end);
                })
                .toList(), false);
    }
}
