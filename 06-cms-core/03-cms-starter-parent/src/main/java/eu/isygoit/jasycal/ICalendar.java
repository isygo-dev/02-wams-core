package eu.isygoit.jasycal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentGroup;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Uid;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * The type Calendar.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ICalendar {

    private String icsPath;

    private Calendar calendar;

    /**
     * Load calendar.
     *
     * @param icsPath the ics path
     * @return the calendar
     * @throws IOException     the io exception
     * @throws ParserException the parser exception
     */
    public ICalendar load(String icsPath) throws IOException, ParserException {
        this.icsPath = icsPath;
        FileInputStream icsInput = new FileInputStream(this.getIcsPath());
        this.calendar = new CalendarBuilder().build(icsInput);
        return this;
    }

    /**
     * Store calendar.
     *
     * @param icsPath the ics path
     * @return the calendar
     * @throws IOException the io exception
     */
    public ICalendar store(String icsPath) throws IOException {
        this.icsPath = icsPath;
        FileOutputStream fout = new FileOutputStream(this.getIcsPath());
        CalendarOutputter outputter = new CalendarOutputter();
        outputter.output(this.calendar, fout);
        return this;
    }

    /**
     * Load calendar.
     *
     * @return the calendar
     * @throws IOException     the io exception
     * @throws ParserException the parser exception
     */
    public ICalendar load() throws IOException, ParserException {
        FileInputStream icsInput = new FileInputStream(this.getIcsPath());
        this.calendar = new CalendarBuilder().build(icsInput);
        return this;
    }

    /**
     * Store calendar.
     *
     * @return the calendar
     * @throws IOException the io exception
     */
    public ICalendar store() throws IOException {
        FileOutputStream fout = new FileOutputStream(this.getIcsPath());
        CalendarOutputter outputter = new CalendarOutputter();
        outputter.output(this.calendar, fout);
        return this;
    }

    /**
     * Add event calendar.
     *
     * @param event the event
     * @return the calendar
     */
    public ICalendar addEvent(IEvent event) {
        this.calendar.add(event.getEvent());
        return this;
    }

    /**
     * Update event calendar.
     *
     * @param event the event
     * @return the calendar
     */
    public ICalendar updateEvent(IEvent event) {
        ComponentGroup<VEvent> group = new ComponentGroup(
                this.getCalendar().getComponents(Component.VEVENT),
                new Uid(event.getUid().getValue()));

        group.getLatestRevision().setPropertyList(new PropertyList(Arrays.asList(
                event.getUid(),
                event.getName(),
                event.getDescription(),
                event.getSummary(),
                event.getDtStart(),
                event.getDtEnd())
        ));
        return this;
    }

    /**
     * Remove calendar.
     *
     * @param uid the uid
     * @return the calendar
     */
    public ICalendar remove(Uid uid) {
        ComponentGroup<VEvent> group = new ComponentGroup(
                this.getCalendar().getComponents(Component.VEVENT),
                new Uid(uid.getValue()));

        this.getCalendar().remove(group.getLatestRevision());
        return this;
    }
}
