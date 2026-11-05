package eu.isygoit.ui.cms.views.vcalendar.calendar;

import eu.isygoit.dto.common.DayTimeSlot;
import eu.isygoit.dto.data.VCalendarEventDto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public record CmsCalendarEventSlot(
        Long id,
        String tenant,
        String calendar,
        String title,
        String type,
        LocalDateTime startTime,
        LocalDateTime endTime
) implements DayTimeSlot<LocalDateTime, String> {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<String[]> PALETTE = List.of(
            new String[]{"#E9F2FF", "#0c66e4"},
            new String[]{"#F3E5F5", "#8E24AA"},
            new String[]{"#E8F5E9", "#2E7D32"},
            new String[]{"#FFF3E0", "#EF6C00"},
            new String[]{"#FCE4EC", "#C2185B"},
            new String[]{"#E0F7FA", "#00838F"},
            new String[]{"#FFF8E1", "#F9A825"},
            new String[]{"#EDE7F6", "#5E35B1"}
    );

    public static CmsCalendarEventSlot from(VCalendarEventDto event) {
        return new CmsCalendarEventSlot(
                event.getId(),
                event.getTenant(),
                event.getCalendar(),
                event.getTitle() == null || event.getTitle().isBlank()
                        ? event.getName() : event.getTitle(),
                event.getType(),
                toLocalDateTime(event.getStartDate()),
                toLocalDateTime(event.getEndDate()));
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Override
    public java.time.DayOfWeek getDayOfWeek() {
        return startTime == null ? null : startTime.getDayOfWeek();
    }

    @Override
    public LocalDateTime getStartTime() {
        return startTime;
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }

    @Override
    public String getTitle() {
        return title == null ? "" : title;
    }

    @Override
    public String getDescription() {
        return type == null ? "" : type;
    }

    @Override
    public String getLocation() {
        return calendar == null ? "" : calendar;
    }

    @Override
    public String getOwner() {
        return tenant == null ? "" : tenant;
    }

    @Override
    public String getStatus() {
        return type == null ? "" : type;
    }

    public String color() {
        int index = Math.floorMod(type == null ? 0 : type.hashCode(), PALETTE.size());
        return PALETTE.get(index)[1];
    }

    public LocalTime gridEndTime() {
        if (endTime != null) {
            return endTime.toLocalTime();
        }
        return startTime == null ? null : startTime.toLocalTime().plusMinutes(30);
    }

    public String tooltip() {
        StringBuilder text = new StringBuilder(getTitle());
        if (type != null && !type.isBlank()) {
            text.append(" · ").append(type);
        }
        if (startTime != null) {
            text.append("\n").append(startTime.format(DATE_TIME_FORMAT));
            if (endTime != null) {
                text.append(" – ").append(endTime.format(DATE_TIME_FORMAT));
            }
        }
        return text.toString();
    }

}
