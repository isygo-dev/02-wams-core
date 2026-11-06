package eu.isygoit.ui.cms.views.vcalendar.calendar;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.ui.common.time.DailyTableGrid;
import eu.isygoit.ui.common.time.DailyTableGridConfig;

import java.time.LocalDate;
import java.util.List;

public class DailyCalendarView extends VerticalLayout {

    private final DailyTableGrid<CmsCalendarEventSlot> grid;
    private List<CmsCalendarEventSlot> events = List.of();

    public DailyCalendarView() {
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        DailyTableGridConfig<CmsCalendarEventSlot> config =
                DailyTableGridConfig.<CmsCalendarEventSlot>builder()
                        .withStartTimeExtractor(event -> event.getStartTime().toLocalTime())
                        .withEndTimeExtractor(CmsCalendarEventSlot::gridEndTime)
                        .withTitleExtractor(CmsCalendarEventSlot::getTitle)
                        .withLocationExtractor(CmsCalendarEventSlot::getLocation)
                        .withCategoryKeyExtractor(CmsCalendarEventSlot::getStatus)
                        .withColorExtractor(CmsCalendarEventSlot::color)
                        .withTooltipExtractor(CmsCalendarEventSlot::tooltip)
                        .withIsEditable(event -> false)
                        .withSlotContentPopulator((content, event) -> {
                            Span title = new Span(event.getTitle());
                            title.addClassName("timetable-slot-title");
                            content.add(title);
                        })
                        .withStartHour(7)
                        .withEndHour(20)
                        .withStepMinutes(5)
                        .withShowNowIndicator(true)
                        .build();

        grid = new DailyTableGrid<>(config);
        grid.setWidthFull();
        add(grid);
    }

    public void setEvents(List<CmsCalendarEventSlot> events) {
        this.events = events == null ? List.of() : List.copyOf(events);
    }

    public void setDay(LocalDate date) {
        LocalDate target = date == null ? LocalDate.now() : date;
        grid.setDate(target, events.stream()
                .filter(event -> event.getStartTime() != null)
                .filter(event -> event.getStartTime().toLocalDate().equals(target))
                .toList());
    }
}
